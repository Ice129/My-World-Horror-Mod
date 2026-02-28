package horror.blueice129.sounds;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.FootstepPathUtils;
import horror.blueice129.utils.LineOfSightUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public class StalkingFootsteps {

    static final int MIN_FOLLOW_DISTANCE = 14;
    static final int MAX_FOLLOW_DISTANCE = 23;
    private static final int CHECK_INTERVAL = 7;    // ticks between footstep sounds
    private static final int MAX_PATH_LENGTH = 80;  // total steps before aborting
    private static final int MAX_TIME_ON_PATH = 20 * 60; // 1200 ticks = 1 minute
    private static final int ABORT_DISTANCE = 6;    // abort if player within this many blocks of last step
    private static final int MAX_RADIUS = 32;
    private static final double LOS_CHECK_DISTANCE = 64.0;

    // --- State key constants ---
    private static final String KEY_ACTIVE       = "stalkingActive";       // 0=idle 1=walking 2=paused
    private static final String KEY_PATH         = "stalkingPath";
    private static final String KEY_STEP         = "stalkingCurrentStep";
    private static final String KEY_STEP_TIMER   = "stalkingStepTimer";
    private static final String KEY_ELAPSED      = "stalkingElapsedTicks";
    private static final String KEY_TOTAL_STEPS  = "stalkingTotalSteps";
    private static final String KEY_LAST_POS     = "stalkingLastPos";
    private static final String KEY_PAUSED_POS   = "stalkingPausedPlayerPos";

    public static boolean isActive(MinecraftServer server) {
        return HorrorModPersistentState.getServerState(server).getIntValue(KEY_ACTIVE, 0) != 0;
    }

    /**
     * Starts a new stalking event for the given player.
     * Generates an initial path from behind the player toward them.
     */
    public static boolean startStalking(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        if (!ChunkLoader.loadChunksInRadius(world, playerPos, 2)) {
            HorrorMod129.LOGGER.warn("StalkingFootsteps: failed to load chunks for path generation");
            return false;
        }

        Direction behind = player.getHorizontalFacing().getOpposite();
        List<BlockPos> path = FootstepPathUtils.floodFillPath(
                world, playerPos, behind,
                MIN_FOLLOW_DISTANCE, MAX_FOLLOW_DISTANCE, MAX_RADIUS);

        if (path == null || path.size() < 3) {
            HorrorMod129.LOGGER.warn("StalkingFootsteps: initial path too short or not found");
            return false;
        }

        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setPositionList(KEY_PATH, path);
        state.setIntValue(KEY_STEP, 0);
        state.setIntValue(KEY_STEP_TIMER, CHECK_INTERVAL);
        state.setIntValue(KEY_ELAPSED, 0);
        state.setIntValue(KEY_TOTAL_STEPS, 0);
        state.setPosition(KEY_LAST_POS, path.get(0));
        state.setIntValue(KEY_ACTIVE, 1);

        HorrorMod129.LOGGER.info("StalkingFootsteps: started with " + path.size() + " initial steps");
        return true;
    }

    /**
     * Drives stalking logic. Called every server tick while the event is active.
     */
    public static void tickStalking(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int active = state.getIntValue(KEY_ACTIVE, 0);
        if (active == 0) return;

        // Global timeout
        int elapsed = state.getIntValue(KEY_ELAPSED, 0) + 1;
        state.setIntValue(KEY_ELAPSED, elapsed);
        if (elapsed >= MAX_TIME_ON_PATH) {
            clearState(state);
            HorrorMod129.LOGGER.info("StalkingFootsteps: event timed out");
            return;
        }

        if (active == 1) {
            tickWalking(server, state);
        } else if (active == 2) {
            tickPaused(server, state);
        }
    }

    // -------------------------------------------------------------------------
    // Walking state
    // -------------------------------------------------------------------------

    private static void tickWalking(MinecraftServer server, HorrorModPersistentState state) {
        int stepTimer = state.getIntValue(KEY_STEP_TIMER, 0);
        if (stepTimer > 0) {
            state.setIntValue(KEY_STEP_TIMER, stepTimer - 1);
            return;
        }

        List<BlockPos> path = state.getPositionList(KEY_PATH);
        int stepIndex = state.getIntValue(KEY_STEP, 0);
        BlockPos lastPos = state.getPosition(KEY_LAST_POS);

        // Current segment exhausted — try to generate the next one
        if (stepIndex >= path.size()) {
            if (!tryRegenerateSegment(server, state, lastPos)) {
                pauseStalker(server, state);
            }
            return;
        }

        BlockPos stepPos = path.get(stepIndex);
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return;

        ServerPlayerEntity closestPlayer = getClosestPlayer(server, stepPos);
        if (closestPlayer == null) return;

        // Abort: player reached the stalker's last known position
        if (lastPos != null && lastPos.getSquaredDistance(closestPlayer.getBlockPos()) <= ABORT_DISTANCE * ABORT_DISTANCE) {
            clearState(state);
            HorrorMod129.LOGGER.info("StalkingFootsteps: aborted — player within " + ABORT_DISTANCE + " blocks of last step");
            return;
        }

        // Abort: cumulative step limit reached
        int totalSteps = state.getIntValue(KEY_TOTAL_STEPS, 0);
        if (totalSteps >= MAX_PATH_LENGTH) {
            clearState(state);
            HorrorMod129.LOGGER.info("StalkingFootsteps: aborted — max path length reached");
            return;
        }

        // Pause: stalker too close to player or the step would be visible
        boolean tooClose = stepPos.getSquaredDistance(closestPlayer.getBlockPos()) <= MIN_FOLLOW_DISTANCE * MIN_FOLLOW_DISTANCE;
        boolean visible = isStepVisible(overworld, closestPlayer, stepPos);
        if (!visible && stepIndex + 1 < path.size()) {
            visible = isStepVisible(overworld, closestPlayer, path.get(stepIndex + 1));
        }

        if (tooClose || visible) {
            pauseStalker(server, state);
            HorrorMod129.LOGGER.info("StalkingFootsteps: paused (tooClose=" + tooClose + ", visible=" + visible + ")");
            return;
        }

        // Play the footstep and advance
        float pitchVariation = overworld.getRandom().nextFloat();
        FakeFootsteps.playFootstepAt(overworld, stepPos, pitchVariation);

        state.setPosition(KEY_LAST_POS, stepPos);
        state.setIntValue(KEY_STEP, stepIndex + 1);
        state.setIntValue(KEY_TOTAL_STEPS, totalSteps + 1);
        state.setIntValue(KEY_STEP_TIMER, CHECK_INTERVAL);
    }

    // -------------------------------------------------------------------------
    // Paused state
    // -------------------------------------------------------------------------

    private static void tickPaused(MinecraftServer server, HorrorModPersistentState state) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return;

        BlockPos lastPos = state.getPosition(KEY_LAST_POS);
        if (lastPos == null) {
            clearState(state);
            return;
        }

        ServerPlayerEntity closestPlayer = getClosestPlayer(server, lastPos);
        if (closestPlayer == null) return;

        // Stay paused while the stalker's last position is still visible
        if (isStepVisible(overworld, closestPlayer, lastPos)) return;

        // LoS broken — wait for player to move before re-pathing
        BlockPos pausedPlayerPos = state.getPosition(KEY_PAUSED_POS);
        BlockPos currentPlayerPos = closestPlayer.getBlockPos();
        if (pausedPlayerPos != null && pausedPlayerPos.equals(currentPlayerPos)) return;

        // Player moved and stalker is hidden — resume
        if (!tryRegenerateSegment(server, state, lastPos)) {
            // Can't find a walkable path yet; update reference so any further move retries
            state.setPosition(KEY_PAUSED_POS, currentPlayerPos);
            return;
        }

        state.setIntValue(KEY_ACTIVE, 1);
        state.setIntValue(KEY_STEP_TIMER, CHECK_INTERVAL);
        state.removePosition(KEY_PAUSED_POS);
        HorrorMod129.LOGGER.info("StalkingFootsteps: resumed");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Regenerates the path from {@code lastPos} toward the closest player. */
    private static boolean tryRegenerateSegment(MinecraftServer server, HorrorModPersistentState state, BlockPos lastPos) {
        if (lastPos == null) return false;
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return false;

        ServerPlayerEntity closestPlayer = getClosestPlayer(server, lastPos);
        if (closestPlayer == null) return false;

        List<BlockPos> newPath = FootstepPathUtils.walkToward(
                overworld, lastPos, closestPlayer.getBlockPos(),
                MIN_FOLLOW_DISTANCE, MAX_PATH_LENGTH);

        if (newPath == null) {
            HorrorMod129.LOGGER.warn("StalkingFootsteps: failed to generate next segment from " + lastPos);
            return false;
        }

        state.setPositionList(KEY_PATH, newPath);
        state.setIntValue(KEY_STEP, 0);
        HorrorMod129.LOGGER.info("StalkingFootsteps: new segment with " + newPath.size() + " steps");
        return true;
    }

    /** Transitions to paused state and records the player's current position. */
    private static void pauseStalker(MinecraftServer server, HorrorModPersistentState state) {
        state.setIntValue(KEY_ACTIVE, 2);
        BlockPos lastPos = state.getPosition(KEY_LAST_POS);
        ServerPlayerEntity closest = getClosestPlayer(server, lastPos);
        if (closest != null) {
            state.setPosition(KEY_PAUSED_POS, closest.getBlockPos());
        }
    }

    /**
     * Returns true if the player has line of sight to the foot level, mid body,
     * or head level of the position — matching the space a walking entity occupies.
     */
    private static boolean isStepVisible(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        return LineOfSightUtils.hasLineOfSight(player, pos, LOS_CHECK_DISTANCE)
                || LineOfSightUtils.hasLineOfSight(player, pos.up(), LOS_CHECK_DISTANCE)
                || LineOfSightUtils.hasLineOfSight(player, pos.up(2), LOS_CHECK_DISTANCE);
    }

    /** Returns the player currently closest to {@code pos}, or null if no players online. */
    private static ServerPlayerEntity getClosestPlayer(MinecraftServer server, BlockPos pos) {
        if (pos == null) return null;
        ServerPlayerEntity closest = null;
        double bestSq = Double.MAX_VALUE;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            double sq = player.getBlockPos().getSquaredDistance(pos);
            if (sq < bestSq) {
                bestSq = sq;
                closest = player;
            }
        }
        return closest;
    }

    /** Clears all stalking state, returning the event to idle. */
    static void clearState(HorrorModPersistentState state) {
        state.setIntValue(KEY_ACTIVE, 0);
        state.removePositionList(KEY_PATH);
        state.removeIntValue(KEY_STEP);
        state.removeIntValue(KEY_STEP_TIMER);
        state.removeIntValue(KEY_ELAPSED);
        state.removeIntValue(KEY_TOTAL_STEPS);
        state.removePosition(KEY_LAST_POS);
        state.removePosition(KEY_PAUSED_POS);
    }
}
