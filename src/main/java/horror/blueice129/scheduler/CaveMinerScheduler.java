package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
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
    private static final Random random = Random.create();

    private static int MAX_DELAY = 72000; // 60 minutes
    private static int MIN_DELAY = 20000; // 20 minutes

    private static final String TIMER_ID = "caveMinerTimer";

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
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
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
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        // Select a random player from the server
        ServerPlayerEntity player = server.getPlayerManager().getPlayerList()
                .get(random.nextInt(server.getPlayerManager().getPlayerList().size()));
            
            // Get our persistent state
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            
        // Get current timer value and decrement (only happens if players are online)
            if (currentTimer <= 0) {
                // Trigger the cave miner event
                while (!CavePreMiner.preMineCave(player.getWorld(), player.getBlockPos(), player)) {
                    // If unsuccessful, set a short retry delay
                    int retryDelay = 1200; // 1 minute
                    state.setTimer(TIMER_ID, retryDelay);
                    HorrorMod129.LOGGER.info("CavePreMiner attempt failed, retrying in 1 minute.");
                    return;
                }
                HorrorMod129.LOGGER.info("CavePreMiner event executed successfully.");

                // Reset the timer with a new random delay
                state.setTimer(TIMER_ID, getRandomDelay());
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