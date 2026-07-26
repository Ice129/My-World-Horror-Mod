package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
// import net.minecraft.util.math.random.Random;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.utils.DayUtils;
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
                    state.setIntValue(WORLD_INITIALIZED_KEY, 2);
                    HorrorMod129.LOGGER.info("World initialization complete - OnWorldCreation will not run again");
                }
                else if (state.getIntValue(WORLD_INITIALIZED_KEY, 0) == 1) {
                    // update from previous version to new implimentation
                    updateDayCount(server);
                    state.setIntValue(WORLD_INITIALIZED_KEY, 2);
                    HorrorMod129.LOGGER.info("World initialization updated to new implementation");
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
        
        long daysDifference = 129;

        long ticksToAdjust = (long) (daysDifference * 24000L); // 
        // make sure ticksToAdjust is the closest multiple of 24000
        ticksToAdjust = (ticksToAdjust / 24000L) * 24000L;
        ticksToAdjust = ticksToAdjust - 12000L; // adjust to midday

        
        // adjust world time
        server.getOverworld().setTimeOfDay(server.getOverworld().getTimeOfDay() + ticksToAdjust);
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setLongValue("worldTimeOffset", ticksToAdjust);
        HorrorMod129.LOGGER.info("Modified world date by " + ticksToAdjust + " ticks to align with target date.");
    }

    /**
     * Updates the day count in the persistent state to align with the new implementation.
     * 
     * @param server The Minecraft server instance
     */
    private static void updateDayCount(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        long currentWorldTime = server.getOverworld().getTimeOfDay();
        // new implimentation will use a static offset of 129 days, so we need to adjust the world time acordingly
        // change world day count to 129 + actual day count, and change day offset to 129 so real day count stays acurate
        long currentDayCount = DayUtils.getCurrentActualDay(currentWorldTime, state.getLongValue("worldTimeOffset", 0));
        long newDayCount = 129 + currentDayCount;
        long newWorldTime = (newDayCount * 24000L) + (currentWorldTime % 24000L); // new day count is in days, so convert to ticks, and add the current time of day in ticks
        long newWorldTimeOffset = newWorldTime - currentWorldTime;
        // adjust world time
        server.getOverworld().setTimeOfDay(newWorldTime);
        state.setLongValue("worldTimeOffset", newWorldTimeOffset);
        HorrorMod129.LOGGER.info("Updated world day count to align with new implementation. New world time offset: " + newWorldTimeOffset + " ticks.");
    }
}
