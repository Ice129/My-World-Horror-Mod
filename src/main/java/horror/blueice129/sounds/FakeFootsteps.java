package horror.blueice129.sounds;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.FootstepPathUtils;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;

import java.util.List;

public class FakeFootsteps {

    private static final int MIN_LIGHT_LEVEL = 4;
    // private static final int MIN_SOLID_OVERHEAD = 5;
    private static final int MIN_PATH_STEPS = 10;
    private static final int STEP_TICKS = 3;
    private static final int MAX_RADIUS = 32;
    private static final int MIN_TARGET_DISTANCE = 20;
    private static final int MAX_TARGET_DISTANCE = 30;

    public static class SoundPath {
        public List<BlockPos> blockPosList;
        public int length;

        public SoundPath(List<BlockPos> blockPosList) {
            this.blockPosList = blockPosList;
            this.length = blockPosList.size();
        }
    }

    /**
     * Checks if conditions are right to trigger fake footsteps.
     * Requires: 5+ solid blocks overhead AND light level < 4
     */
    public static boolean shouldTriggerFootsteps(ServerWorld world, ServerPlayerEntity player) {
        return validateFootstepConditions(world, player) == null;
    }

    /**
     * Validates whether footsteps can trigger at the player's location.
     * 
     * @return null when conditions are met; otherwise, a human-readable reason.
     */
    public static String validateFootstepConditions(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        int lightLevel = world.getLightLevel(LightType.BLOCK, playerPos);

        if (lightLevel >= MIN_LIGHT_LEVEL) {
            return "Requires block light below " + MIN_LIGHT_LEVEL + " (current " + lightLevel + ")";
        }

        // if (!FootstepPathUtils.hasOverheadCover(world, playerPos,
        // MIN_SOLID_OVERHEAD)) {
        // return "Requires at least " + MIN_SOLID_OVERHEAD + " solid blocks overhead";
        // }

        return null;
    }

    /**
     * Generates an intelligent walking path from behind the player.
     * Path starts 20-30 blocks away and approaches the player's back in a realistic
     * manner.
     */
    public static SoundPath getFootstepLocation(ServerWorld world, ServerPlayerEntity player) {
        Direction behind = player.getHorizontalFacing().getOpposite();
        BlockPos playerPos = player.getBlockPos();

        if (!ChunkLoader.loadChunksInRadius(world, playerPos, 2)) {
            HorrorMod129.LOGGER.warn("Failed to load chunks for fake footstep path generation");
            return null;
        }

        List<BlockPos> pathPositions = FootstepPathUtils.floodFillPath(
                world,
                playerPos,
                behind,
                MIN_TARGET_DISTANCE,
                MAX_TARGET_DISTANCE,
                MAX_RADIUS);

        if (pathPositions == null || pathPositions.size() < MIN_PATH_STEPS) {
            HorrorMod129.LOGGER.warn("Generated footstep path too short or missing");
            return null;
        }

        HorrorMod129.LOGGER.info("Generated footstep path with " + pathPositions.size() + " steps");
        return new SoundPath(pathPositions);
    }

    /**
     * Gets the appropriate step sound for a block type.
     * Uses Minecraft's built-in sound groups for realistic footstep sounds.
     */
    public static SoundEvent getStepSoundForBlock(BlockState blockState) {
        return blockState.getSoundGroup().getStepSound();
    }

    /**
     * Plays a single footstep sound at the specified position.
     * Pitch is slightly randomized for realism.
     */
    public static void playFootstepAt(ServerWorld world, BlockPos pos, float pitchVariation) {
        BlockState groundBlock = world.getBlockState(pos);
        SoundEvent stepSound = getStepSoundForBlock(groundBlock);

        float pitch = 0.95f + pitchVariation * 0.1f; // 0.95 - 1.05

        world.playSound(
                null,
                pos.up(),
                stepSound,
                net.minecraft.sound.SoundCategory.BLOCKS,
                1.0f,
                pitch);
    }

    /**
     * Initiates playback of a sound path.
     * Stores the path in persistent state for sequential playback by scheduler.
     */
    public static void playSoundPath(MinecraftServer server, SoundPath path) {
        if (path == null || path.length == 0) {
            return;
        }

        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

        // Store path in persistent state
        state.setPositionList("fakeFootstepPath", path.blockPosList);
        state.setIntValue("fakeFootstepCurrentStep", 0);
        state.setIntValue("fakeFootstepPlaybackTimer", STEP_TICKS);
        state.setIntValue("fakeFootstepActive", 1);

        HorrorMod129.LOGGER.info("Started footstep path playback with " + path.length + " steps");
    }

    /**
     * Advances footstep playback by one step.
     * Called by scheduler when playback timer reaches 0.
     * Returns true if playback continues, false if complete.
     */
    public static boolean tickFootstepPlayback(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

        // Check if playback is active
        int active = state.getIntValue("fakeFootstepActive", 0);
        if (active == 0) {
            return false;
        }

        // Get path and current step
        List<BlockPos> path = state.getPositionList("fakeFootstepPath");
        int currentStep = state.getIntValue("fakeFootstepCurrentStep", 0);

        if (path == null || currentStep >= path.size()) {
            // Playback complete, clear state
            state.setIntValue("fakeFootstepActive", 0);
            state.removePositionList("fakeFootstepPath");
            HorrorMod129.LOGGER.info("Footstep playback complete");
            return false;
        }

        // Play sound at current position unless too close to player
        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            BlockPos stepPos = path.get(currentStep);

            if (FootstepPathUtils.isNearAnyPlayer(server, stepPos, 5.0)) {
                state.setIntValue("fakeFootstepActive", 0);
                state.removePositionList("fakeFootstepPath");
                HorrorMod129.LOGGER.info("Footstep playback stopped: within 5 blocks of a player");
                return false;
            }

            float pitchVariation = overworld.getRandom().nextFloat();
            playFootstepAt(overworld, stepPos, pitchVariation);
        }

        // Advance to next step
        state.setIntValue("fakeFootstepCurrentStep", currentStep + 1);
        state.setIntValue("fakeFootstepPlaybackTimer", STEP_TICKS);

        return true;
    }

}
