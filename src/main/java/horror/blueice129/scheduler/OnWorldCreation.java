package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import net.minecraft.util.math.random.Random;
import horror.blueice129.data.HorrorModPersistentState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.FilledMapItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Runs code once when a world is first created.
 * Uses persistent state to track whether the world has been initialized.
 */
public class OnWorldCreation {
    private static final String WORLD_INITIALIZED_KEY = "worldInitialized";
    private static final String MAP_IDS_INITIALIZED_KEY = "mapIdsInitialized";
    private static final int INITIAL_MAP_IDS_TO_CONSUME = 15;

    /**
     * Registers the world load event to detect world creation.
     */
    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient())
                return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

                // Check if this world has been initialized before
                if (state.getIntValue(WORLD_INITIALIZED_KEY, 0) == 0) {
                    // First time loading this world - run initialization
                    onWorldCreated(server);

                    // Mark world as initialized
                    state.setIntValue(WORLD_INITIALIZED_KEY, 1);
                    HorrorMod129.LOGGER.info("World initialization complete - OnWorldCreation will not run again");
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            if (state.getIntValue(MAP_IDS_INITIALIZED_KEY, 0) == 0) {
                initializeMapIds(server);
                state.setIntValue(MAP_IDS_INITIALIZED_KEY, 1);
                HorrorMod129.LOGGER.info("Map id initialization complete on first player join");
            }
        });

        HorrorMod129.LOGGER.info("Registered OnWorldCreation");
    }

    /**
     * Called once when a world is first created.
     * 
     * @param server The Minecraft server instance
     */
    private static void onWorldCreated(MinecraftServer server) {
        HorrorMod129.LOGGER.info("OnWorldCreation: World created for the first time!");

        modifyWorldDate(server);

    }

    /**
     * Creates and discards a set of filled maps so the first player-created map starts at a higher ID.
     */
    private static void initializeMapIds(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            HorrorMod129.LOGGER.warn("OnWorldCreation: Overworld was null, skipping initial map id setup");
            return;
        }

        int spawnX = overworld.getSpawnPos().getX();
        int spawnZ = overworld.getSpawnPos().getZ();

        for (int i = 0; i < INITIAL_MAP_IDS_TO_CONSUME; i++) {
            // Create map data to consume IDs and then immediately discard the item stack.
            FilledMapItem.createMap(overworld, spawnX, spawnZ, (byte) 0, true, false);
        }

        HorrorMod129.LOGGER.info("OnWorldCreation: Consumed " + INITIAL_MAP_IDS_TO_CONSUME
                + " map IDs so first player map should start around id 15");
    }

    /**
     * Modifies the world's date to start at a specific value.
     * 
     * @param server The Minecraft server instance
     */
    private static void modifyWorldDate(MinecraftServer server) {
        // get irl date
        java.time.LocalDate currentDate = java.time.LocalDate.now();

        // get difference between 20th June 2022 and current date
        java.time.LocalDate targetDate = java.time.LocalDate.of(2022, 6, 20);
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(targetDate, currentDate);

        // Random between 0.55 and 0.9
        Random random = Random.create();
        double sleepPercent = 0.55 + (0.35 * random.nextDouble());
        long ticksToAdjust = (long) (daysDifference * (24000L * 3 * 24 * sleepPercent));
        // make sure ticksToAdjust is the closest multiple of 24000
        ticksToAdjust = (ticksToAdjust / 24000L) * 24000L;
        ticksToAdjust = ticksToAdjust - 12000L; // adjust to midday

        
        // adjust world time
        server.getOverworld().setTimeOfDay(server.getOverworld().getTimeOfDay() + ticksToAdjust);
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setLongValue("worldTimeOffset", ticksToAdjust);
        HorrorMod129.LOGGER.info("Modified world date by " + ticksToAdjust + " ticks to align with target date.");
    }
}
