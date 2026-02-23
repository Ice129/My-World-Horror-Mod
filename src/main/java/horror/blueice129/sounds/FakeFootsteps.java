package horror.blueice129.sounds;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeFootsteps {

    private static final int MIN_LIGHT_LEVEL = 4;
    private static final int MIN_SOLID_OVERHEAD = 5;
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
     * @return null when conditions are met; otherwise, a human-readable reason.
     */
    public static String validateFootstepConditions(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        int lightLevel = world.getLightLevel(LightType.BLOCK, playerPos);

        if (lightLevel >= MIN_LIGHT_LEVEL) {
            return "Requires block light below " + MIN_LIGHT_LEVEL + " (current " + lightLevel + ")";
        }

        // if (!hasOverheadCover(world, playerPos)) {
        //     return "Requires at least " + MIN_SOLID_OVERHEAD + " solid blocks overhead";
        // }

        return null;
    }

    /**
     * Generates an intelligent walking path from behind the player.
     * Path starts 20-30 blocks away and approaches the player's back in a realistic manner.
     */
    public static SoundPath getFootstepLocation(ServerWorld world, ServerPlayerEntity player) {
        Direction behind = player.getHorizontalFacing().getOpposite();
        BlockPos playerPos = player.getBlockPos();

        if (!ChunkLoader.loadChunksInRadius(world, playerPos, 2)) {
            HorrorMod129.LOGGER.warn("Failed to load chunks for fake footstep path generation");
            return null;
        }

        SoundPath path = floodFillPath(world, playerPos, behind);
        if (path == null || path.blockPosList.size() < MIN_PATH_STEPS) {
            HorrorMod129.LOGGER.warn("Generated footstep path too short or missing");
            return null;
        }

        HorrorMod129.LOGGER.info("Generated footstep path with " + path.blockPosList.size() + " steps");
        return path;
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
            pitch
        );
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

            if (isNearAnyPlayer(server, stepPos, 5.0)) {
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

    private static boolean isNearAnyPlayer(MinecraftServer server, BlockPos pos, double maxDistance) {
        double maxSq = maxDistance * maxDistance;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getBlockPos().getSquaredDistance(pos) <= maxSq) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOverheadCover(ServerWorld world, BlockPos pos) {
        int surfaceY = SurfaceFinder.findPointSurfaceY(world, pos.getX(), pos.getZ(), true, true, true);

        // If we're at or above the surface, there's no overhead cover
        if (pos.getY() >= surfaceY) {
            return false;
        }

        // Count total solid blocks between the player position and the surface, skipping over air gaps
        int solidCount = 0;
        for (int checkY = pos.getY() + 1; checkY <= surfaceY; checkY++) {
            BlockPos checkPos = new BlockPos(pos.getX(), checkY, pos.getZ());
            BlockState state = world.getBlockState(checkPos);

            if (state.isSolidBlock(world, checkPos)) {
                solidCount++;
                if (solidCount >= MIN_SOLID_OVERHEAD) {
                    return true; // Early stop once we have enough solid cover
                }
            }
        }

        return false;
    }

    private static boolean isWalkable(ServerWorld world, BlockPos pos) {
        BlockState ground = world.getBlockState(pos);
        BlockState air = world.getBlockState(pos.up());
        BlockState head = world.getBlockState(pos.up(2));
        return !ground.isAir() && ground.isSolidBlock(world, pos) && air.isAir() && head.isAir();
    }

    private static SoundPath floodFillPath(ServerWorld world, BlockPos start, Direction behind) {
        // BFS from player outward; pick farthest reachable node in a cone behind the player
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        queue.add(start);
        parent.put(start, null);

        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;

        // Direction vector for "behind"
        int bx = behind.getOffsetX();
        int bz = behind.getOffsetZ();

        int[][] neighbors = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // Evaluate candidate target
            int dx = current.getX() - start.getX();
            int dz = current.getZ() - start.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double dotBehind = dx * bx + dz * bz;

            if (dist >= MIN_TARGET_DISTANCE && dist <= MAX_TARGET_DISTANCE && dotBehind > 0) {
                double score = dotBehind; // prefer farther behind
                if (score > bestScore) {
                    bestScore = score;
                    best = current;
                }
            }

            // Stop expanding past max radius
            if (dist > MAX_RADIUS) {
                continue;
            }

            for (int[] n : neighbors) {
                BlockPos candidateBase = current.add(n[0], 0, n[1]);
                BlockPos candidate = findWalkableNearby(world, candidateBase);
                if (candidate == null) continue;

                if (!parent.containsKey(candidate)) {
                    parent.put(candidate, current);
                    queue.add(candidate);
                }
            }
        }

        if (best == null) {
            return null;
        }

        List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = best;
        while (cursor != null) {
            path.add(cursor); // path order: far target -> ... -> player
            cursor = parent.get(cursor);
        }

        return new SoundPath(path);
    }

    private static BlockPos findWalkableNearby(ServerWorld world, BlockPos base) {
        // Check base, then one down (step down), then one up (step up)
        BlockPos[] candidates = new BlockPos[] {
                base,
                base.down(1),
                base.up(1)
        };
        for (BlockPos pos : candidates) {
            if (isWalkable(world, pos)) {
                return pos;
            }
        }
        return null;
    }
}
