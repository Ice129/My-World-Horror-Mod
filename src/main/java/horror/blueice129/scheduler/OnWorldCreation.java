package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import java.util.Random;
import horror.blueice129.data.HorrorModPersistentState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

/**
 * Runs code once when a world is first created.
 * Uses persistent state to track whether the world has been initialized.
 */
public class OnWorldCreation {
    private static final String WORLD_INITIALIZED_KEY = "worldInitialized";

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

        HorrorMod129.LOGGER.info("Registered OnWorldCreation");
    }

    /**
     * Called once when a world is first created.
     * 
     * @param server The Minecraft server instance
     */
    private static void onWorldCreated(MinecraftServer server) {
        HorrorMod129.LOGGER.info("OnWorldCreation: World created for the first time!");

        // TODO: Add your world creation initialization code here
        // This will only run once per world

        modifyWorldDate(server);

    }

    /**
     * Modifies the world's date to start at a specific value.
     * 
     * @param server The Minecraft server instance
     */
    private static void modifyWorldDate(MinecraftServer server) {
        // get irl date
        java.time.LocalDate currentDate = java.time.LocalDate.now();

        // get difference between 20th June and current date
        java.time.LocalDate targetDate = java.time.LocalDate.of(currentDate.getYear(), 6, 20);
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(targetDate, currentDate);
        // Random between 0.55 and 0.9
        Random random = new Random();
        double sleepPercent = 0.55 + (0.35 * random.nextDouble());
        long ticksToAdjust = (long) (daysDifference * (24000L * 3 * 24 * sleepPercent))  % 24000L;
        // adjust world time
        server.getOverworld().setTimeOfDay(server.getOverworld().getTimeOfDay() + ticksToAdjust);
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setLongValue("worldTimeOffset", state.getLongValue("worldTimeOffset", 0L) + ticksToAdjust);
        HorrorMod129.LOGGER.info("Modified world date by " + ticksToAdjust + " ticks to align with target date.");
    }
}
