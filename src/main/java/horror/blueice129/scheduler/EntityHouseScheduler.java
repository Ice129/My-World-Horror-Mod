package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.house.AreaClearer;
import horror.blueice129.feature.house.EntityHouse;
import horror.blueice129.feature.house.EntityHouse.FlatnessResult;
import horror.blueice129.feature.house.EntityHouseInteractionTracker;
import horror.blueice129.feature.house.EntityHouseInteractionTracker.InteractionRecord;
import horror.blueice129.feature.house.EntityHouseInteractionTracker.StructureSnapshot;
import horror.blueice129.feature.house.EntityHousePhase;
import horror.blueice129.feature.house.HouseModificationPlanner;
import horror.blueice129.feature.house.HousePlacer;
import horror.blueice129.utils.PlayerBaseLocator;
import horror.blueice129.utils.StructurePlacer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;

public class EntityHouseScheduler {

    private static final String FLATNESS_CHECK_TIMER = "flatnessCheckTimer";
    // each row stores [x, y, z, flatness] — keeps position and score together to avoid sync issues
    private static final String CANDIDATE_DATA_KEY = "entityHouseCandidates";
    private static final String PHASE_KEY = "entityHousePhase";
    private static final String STAGE_KEY = "entityHouseStageNum";
    private static final String HOUSE_POS_KEY = "entityHousePos";

    private static final int TICKS_BETWEEN_CHECKS = 20 * 60;
    private static final int SCAN_POINTS_PER_CYCLE = 3;
    private static final int MAX_CANDIDATES = 5;
    private static final int BASE_CONFIRMATION_THRESHOLD = 8;
    private static final int MIN_HOUSE_CHUNKS = 20;
    private static final int MAX_HOUSE_CHUNKS = 30;
    private static final int PLACEMENT_MIN_DISTANCE = 100;
    private static final int MAX_STAGE = 5;
    // aggro value required to place each stage (index 0 = stage 1, index 4 = stage 5)
    private static final int[] STAGE_AGGRO_THRESHOLDS = { 2, 4, 6, 8, 10 };

    private static final Random random = Random.create();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(EntityHouseScheduler::onServerTick);

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                if (!state.hasTimer(FLATNESS_CHECK_TIMER)) {
                    state.setTimer(FLATNESS_CHECK_TIMER, TICKS_BETWEEN_CHECKS);
                }
            }
        });
        HorrorMod129.LOGGER.info("Registered EntityHouseScheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) return;

        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int timer = state.decrementTimer(FLATNESS_CHECK_TIMER, 1);
        if (timer > 0) return;

        state.setTimer(FLATNESS_CHECK_TIMER, TICKS_BETWEEN_CHECKS);

        PlayerEntity player = server.getPlayerManager().getPlayerList()
                .get(random.nextInt(server.getPlayerManager().getPlayerList().size()));

        if (player.getWorld().getRegistryKey() != World.OVERWORLD) return;

        ServerWorld world = server.getOverworld();

        switch (getPhase(state)) {
            case FINDING_BASE      -> handleFindingBase(world, player, state);
            case PREPARING         -> handlePreparing(world, player, state);
            case AWAITING_PLACEMENT -> handleAwaitingPlacement(world, player, state);
            case STAGE_ACTIVE      -> handleStageActive(world, player, state);
            case COMPLETE          -> {}
        }
    }

    // --- phase handlers ---

    private static void handleFindingBase(ServerWorld world, PlayerEntity player, HorrorModPersistentState state) {
        runFlatnessCheck(world, player, state);

        PlayerBaseLocator.updateBaseLocationHistory(world, player);
        int[][] history = state.getInt2DArray(PlayerBaseLocator.PLAYER_SPAWNPOINT_HISTORY_TRACKER_ID);

        BlockPos confirmedBase = null;
        for (int[] entry : history) {
            if (entry[3] >= BASE_CONFIRMATION_THRESHOLD) {
                confirmedBase = new BlockPos(entry[0], entry[1], entry[2]);
                break;
            }
        }
        if (confirmedBase == null) return;

        int[][] candidates = state.getInt2DArray(CANDIDATE_DATA_KEY);
        FlatnessResult best = EntityHouse.pickBestCandidateNearBase(candidates, confirmedBase, MIN_HOUSE_CHUNKS, MAX_HOUSE_CHUNKS);
        if (best == null) return;

        state.setPosition(HOUSE_POS_KEY, best.pos);
        setPhase(state, EntityHousePhase.PREPARING);
        HorrorMod129.LOGGER.info("EntityHouseScheduler: base confirmed at {}, house location {} selected",
                confirmedBase.toShortString(), best.pos.toShortString());
    }

    private static void handlePreparing(ServerWorld world, PlayerEntity player, HorrorModPersistentState state) {
        BlockPos housePos = state.getPosition(HOUSE_POS_KEY);
        if (housePos == null) {
            setPhase(state, EntityHousePhase.FINDING_BASE);
            return;
        }

        if (!isPlacementWindowOpen(player, housePos)) return;

        AreaClearer.clearArea(world, housePos);
        AreaClearer.placeTorches(world, housePos);

        setPhase(state, EntityHousePhase.AWAITING_PLACEMENT);
        HorrorMod129.LOGGER.info("EntityHouseScheduler: area prepared at {}", housePos.toShortString());
    }

    private static void handleAwaitingPlacement(ServerWorld world, PlayerEntity player, HorrorModPersistentState state) {
        if (state.getIntValue("agroMeter", 0) < STAGE_AGGRO_THRESHOLDS[0]) return;

        BlockPos housePos = state.getPosition(HOUSE_POS_KEY);
        if (housePos == null) {
            setPhase(state, EntityHousePhase.FINDING_BASE);
            return;
        }

        if (!isPlacementWindowOpen(player, housePos)) return;

        // Place stage 1 and capture initial snapshot
        HousePlacer.placeHouse(1, housePos, world);
        StructureSnapshot snapshot = EntityHouseInteractionTracker.captureStructureSnapshot(world, housePos, 1);
        EntityHouseInteractionTracker.saveStructureSnapshot(world.getServer(), 1, snapshot);
        
        state.setIntValue(STAGE_KEY, 1);
        setPhase(state, EntityHousePhase.STAGE_ACTIVE);
        HorrorMod129.LOGGER.info("EntityHouseScheduler: placed stage 1 at {}, captured snapshot", housePos.toShortString());
    }

    private static void handleStageActive(ServerWorld world, PlayerEntity player, HorrorModPersistentState state) {
        int currentStage = state.getIntValue(STAGE_KEY, 0);

        // If stage state is missing/corrupt, restart from first placement instead of skipping ahead.
        if (currentStage < 1) {
            setPhase(state, EntityHousePhase.AWAITING_PLACEMENT);
            state.setIntValue(STAGE_KEY, 0);
            HorrorMod129.LOGGER.warn("EntityHouseScheduler: invalid current stage {}, resetting to awaiting placement", currentStage);
            return;
        }

        if (currentStage >= MAX_STAGE) {
            state.setIntValue(STAGE_KEY, MAX_STAGE);
            setPhase(state, EntityHousePhase.COMPLETE);
            return;
        }

        int nextStage = currentStage + 1;
        if (state.getIntValue("agroMeter", 0) < STAGE_AGGRO_THRESHOLDS[nextStage - 1]) return;

        BlockPos housePos = state.getPosition(HOUSE_POS_KEY);
        if (housePos == null) return;

        if (!isPlacementWindowOpen(player, housePos)) return;

        advanceToNextStage(world, housePos, state);

        int stageAfterAdvance = state.getIntValue(STAGE_KEY, 0);
        if (stageAfterAdvance == MAX_STAGE) {
            HorrorMod129.LOGGER.info("EntityHouseScheduler: placed final stage {} at {}", MAX_STAGE, housePos.toShortString());
        } else {
            HorrorMod129.LOGGER.info("EntityHouseScheduler: placed stage {} at {}", stageAfterAdvance, housePos.toShortString());
        }
    }

    // --- helpers ---

    /**
     * Main advancement logic: detect interactions from previous stage,
     * apply dynamic modifications, place next stage, and capture new snapshot.
     */
    public static void advanceToNextStage(ServerWorld world, BlockPos housePos, HorrorModPersistentState state) {
        int currentStage = state.getIntValue(STAGE_KEY, 0);
        int nextStage = currentStage + 1;

        // Step 1: Load snapshot from current stage to detect player interactions
        StructureSnapshot snapshot = EntityHouseInteractionTracker.loadStructureSnapshot(world.getServer(), currentStage);
        if (snapshot == null) {
            HorrorMod129.LOGGER.warn("EntityHouseScheduler: no snapshot found for stage {}, skipping interaction detection", currentStage);
        } else {
            // Step 2: Build diff comparing snapshot to current world state
            InteractionRecord diff = EntityHouseInteractionTracker.buildDiffFromSnapshot(world, snapshot);
            EntityHouseInteractionTracker.saveInteractionsForStage(world.getServer(), currentStage, diff);
            
            // Step 3: Plan and apply modifications based on interactions
            var modifications = HouseModificationPlanner.planModifications(diff);
            HouseModificationPlanner.applyModifications(world, modifications);
            HorrorMod129.LOGGER.info("EntityHouseScheduler: applied {} modifications based on player interactions",
                    modifications.size());
        }

        // Step 4: Prepare for next stage - kill old creatures
        HousePlacer.killPreviousPhaseCreatures(housePos, world);

        // Step 5: Place next stage structure
        HousePlacer.placeHouse(nextStage, housePos, world);

        // Step 6: Capture snapshot of newly placed structure (with modifications applied)
        StructureSnapshot newSnapshot = EntityHouseInteractionTracker.captureStructureSnapshot(world, housePos, nextStage);
        EntityHouseInteractionTracker.saveStructureSnapshot(world.getServer(), nextStage, newSnapshot);

        // Step 7: Update phase state
        state.setIntValue(STAGE_KEY, nextStage);
        if (nextStage >= MAX_STAGE) {
            setPhase(state, EntityHousePhase.COMPLETE);
        } else {
            setPhase(state, EntityHousePhase.STAGE_ACTIVE);
        }
    }

    private static EntityHousePhase getPhase(HorrorModPersistentState state) {
        int ordinal = state.getIntValue(PHASE_KEY, 0);
        EntityHousePhase[] values = EntityHousePhase.values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : EntityHousePhase.FINDING_BASE;
    }

    private static void setPhase(HorrorModPersistentState state, EntityHousePhase phase) {
        state.setIntValue(PHASE_KEY, phase.ordinal());
    }

    /**
     * Checks if the player is far enough away and not looking at the house location, to
     * allow placement. This is to prevent the house from suddenly appearing in front of the player.
     * @param player
     * @param housePos
     * @return
    */
    private static boolean isPlacementWindowOpen(PlayerEntity player, BlockPos housePos) {
        if (player.getBlockPos().getSquaredDistance(housePos) < PLACEMENT_MIN_DISTANCE * PLACEMENT_MIN_DISTANCE) {
            return false;
        }
        Vec3d toHouse = Vec3d.ofCenter(housePos).subtract(player.getEyePos()).normalize();
        Vec3d lookVec = player.getRotationVec(1.0f);
        return lookVec.dotProduct(toHouse) < 0.5;
    }

    private static void runFlatnessCheck(ServerWorld world, PlayerEntity player, HorrorModPersistentState state) {
        int[][] candidates = state.getInt2DArray(CANDIDATE_DATA_KEY);

        for (int i = 0; i < SCAN_POINTS_PER_CYCLE; i++) {
            BlockPos seed = StructurePlacer.findSurfaceLocation(world, player.getBlockPos(), player, 16 * 15, 16 * 25, true);
            if (seed == null) continue;

            FlatnessResult result = EntityHouse.getBestLocalFlatness(world, seed);
            candidates = insertCandidate(candidates, result);
        }

        state.setInt2DArray(CANDIDATE_DATA_KEY, candidates);
    }

    private static int[][] insertCandidate(int[][] candidates, FlatnessResult result) {
        int[] newEntry = { result.pos.getX(), result.pos.getY(), result.pos.getZ(), result.flatness };

        if (candidates.length < MAX_CANDIDATES) {
            int[][] updated = new int[candidates.length + 1][];
            System.arraycopy(candidates, 0, updated, 0, candidates.length);
            updated[candidates.length] = newEntry;
            return updated;
        }

        // replace the worst candidate if the new one is flatter
        int worstIndex = 0;
        for (int i = 1; i < candidates.length; i++) {
            if (candidates[i][3] > candidates[worstIndex][3]) {
                worstIndex = i;
            }
        }

        if (result.flatness < candidates[worstIndex][3]) {
            candidates[worstIndex] = newEntry;
        }

        return candidates;
    }
}
