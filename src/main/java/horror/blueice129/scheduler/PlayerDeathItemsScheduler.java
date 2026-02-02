package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.PlayerDeathItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import net.minecraft.util.math.random.Random;

/**
 * Scheduler for the player death items event.
 * This event spawns items as if a player had died in a random location.
 * Occurs every 30-120 minutes of playtime.
 */
public class PlayerDeathItemsScheduler {
    private static final String TIMER_ID = "player_death_items_timer";
    private static final Random RANDOM = Random.create();

    private static final int MIN_DELAY = 20 * 60 * 10; // 10 minutes
    private static final int MAX_DELAY = 20 * 60 * 30; // 30 minutes

    /**
     * Gets a random delay for the next event.
     * @return Random delay in ticks
     */
    private static int getRandomDelay() {
        return MIN_DELAY + RANDOM.nextInt(MAX_DELAY - MIN_DELAY);
    }

    /**
     * Server tick handler to check and trigger the player death items event.
     * @param server The Minecraft server instance
     */
    private static void onServerTick(MinecraftServer server) {
        // Skip if server is empty (pause timers)
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }
        
        // Only run in the overworld
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (!state.hasTimer(TIMER_ID)) {
            return; // Timer not initialized yet
        }

        int timer = state.getTimer(TIMER_ID);
        if (timer > 0) {
            state.setTimer(TIMER_ID, timer - 1);
        } else {
            // Check if there are any players online
            if (server.getPlayerManager().getPlayerList().isEmpty()) {
                // No players online, try again in 1 minutes
                state.setTimer(TIMER_ID, 1200); // 1 minutes in ticks
                HorrorMod129.LOGGER.info("No players online for PlayerDeathItems event, retrying in 1 minutes.");
                return;
            }
            
            // Select a random player to center the death items around
            net.minecraft.server.network.ServerPlayerEntity player = server.getPlayerManager().getPlayerList()
                    .get(RANDOM.nextInt(server.getPlayerManager().getPlayerList().size()));
            
            // Time to trigger the player death items event
            boolean eventTriggered = PlayerDeathItems.triggerEvent(server, player);
            if (eventTriggered) {
                HorrorMod129.LOGGER.info("PlayerDeathItems event triggered successfully around player {}.", 
                                        player.getName().getString());
                // Reset the timer with a new random delay
                state.setTimer(TIMER_ID, getRandomDelay());
            } else {
                // If event could not be triggered, retry in 1 minutes
                state.setTimer(TIMER_ID, 1200); // 1 minutes in ticks
                HorrorMod129.LOGGER.info("PlayerDeathItems event failed, retrying in 1 minutes.");
            }
        }
    }

    /**
     * Registers the tick event to handle the player death items scheduling.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerDeathItemsScheduler::onServerTick);

        // Register server world loading event to initialize timer if needed
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient())
                return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
                // If the timer is not set, initialize it
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay());
                    HorrorMod129.LOGGER.info("PlayerDeathItemsScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered PlayerDeathItemsScheduler");
    }
    
    /**
     * Debug / test helper to set the player death items timer directly (in ticks).
     */
    public static void setTimer(MinecraftServer server, int ticks) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setTimer(TIMER_ID, Math.max(ticks, 1));
        HorrorMod129.LOGGER.info("PlayerDeathItemsScheduler timer set to {} ticks via debug command", 
                                 state.getTimer(TIMER_ID));
    }
}