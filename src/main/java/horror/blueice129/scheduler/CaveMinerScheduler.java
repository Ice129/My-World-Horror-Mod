package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.StateSaverAndLoader;
import horror.blueice129.feature.CavePreMiner;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import net.minecraft.util.math.random.Random;

/**
 * This class schedules the cave miner event
 * it occurs every 20-60 minutes
 * if last event was unsuccessful, it will try again in 1 minute
 */
public class CaveMinerScheduler {
    // Random generator for timing
    private static final Random random = Random.create();

    // Maximum delay between cave miner events (in ticks)
    private static int MAX_DELAY = 72000; // 60 minutes

    // Minimum delay between cave miner events (in ticks)
    private static int MIN_DELAY = 20000; // 20 minutes

    private static final String TIMER_ID = "caveMinerTimer";

    // // testing purposes, shorter delays
    // private static final int MAX_DELAY = 2000; // 2 minutes
    // private static final int MIN_DELAY = 1000; // 1 minute

    // Timer is now stored in persistent state

    /**
     * Registers the tick event to handle the cave miner scheduling.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CaveMinerScheduler::onServerTick);
        
        // Register server world loading event to initialize timer if needed
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);
                
                // If the timer is not set, initialize it
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay());
                    HorrorMod129.LOGGER.info("CaveMinerScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });
        
        HorrorMod129.LOGGER.info("Registered CaveMinerScheduler");
    }

    /**
     * Called every server tick.
     * Updates timers and triggers the cave miner event when the timer reaches zero.
     * 
     * @param server The Minecraft server instance
     */
    private static void onServerTick(MinecraftServer server) {
        // Skip if the server is empty
        if (server.getCurrentPlayerCount() == 0) {
            return;
        }

        // Get the first online player (if any)
        if (!server.getPlayerManager().getPlayerList().isEmpty()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayerList().get(0);
            
            // Get our persistent state
            StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);
            
            // Get current timer value
            int currentTimer = state.decrementTimer(TIMER_ID, 1); // Decrement timer by 1 tick

            // If the timer has reached zero
            if (currentTimer <= 0) {
                // Trigger the cave miner event
                while (!CavePreMiner.preMineCave(player.getWorld(), player.getBlockPos(), player)) {
                    // If unsuccessful, set a short retry delay
                    int retryDelay = 1200; // 1 minute
                    // For testing purposes, shorter retry delay
                    // int retryDelay = 1;
                    state.setTimer(TIMER_ID, retryDelay);
                    HorrorMod129.LOGGER.info("CavePreMiner attempt failed, retrying in 1 minute.");
                    return;
                }
                HorrorMod129.LOGGER.info("CavePreMiner event executed successfully.");

                // Reset the timer with a new random delay
                state.setTimer(TIMER_ID, getRandomDelay());
            }
        }
    }

    /**
     * Generates a random delay between MIN_DELAY and MAX_DELAY.
     * 
     * @return A random number of ticks to wait
     */
    private static int getRandomDelay() {
        return MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY + 1);
    }
}