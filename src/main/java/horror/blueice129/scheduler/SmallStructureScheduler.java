package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.SmallStructureEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import net.minecraft.util.math.random.Random;

public class SmallStructureScheduler {
    // Event delay is randomized between 5-15 minutes and further reduced based on
    // the square of the agro meter value
    private static final Random random = Random.create();
    private static final int MAX_DELAY = 18000; // 15 minutes
    private static final int MIN_DELAY = 6000; // 5 minutes
    private static final String TIMER_ID = "smallStructureTimer";

    /**
     * Registers the tick event to handle the small structure scheduling.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SmallStructureScheduler::onServerTick);

        // Register server world loading event to initialize timer if needed
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient())
                return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

                // If the timer is not set, initialize it
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay(state));
                    HorrorMod129.LOGGER.info(
                            "SmallStructureScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered SmallStructureScheduler");
    }

    private static int getRandomDelay(HorrorModPersistentState state) {
        // get agro meter from persistent state
        int agroMeter = state.getIntValue("agroMeter", 0);
        // The delay is reduced based on the square of the agro meter value
        int agroReduction = (-agroMeter * agroMeter) * 200;
        int delay = random.nextBetween(MIN_DELAY, MAX_DELAY + 1) + agroReduction;
        // Clamp delay to at least 1 tick to avoid negative or zero values
        return Math.max(delay, 1);
    }

    private static void onServerTick(MinecraftServer server) {
        // Skip if server is empty (pause timers)
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }
        
        // Only run on the overworld
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (!state.hasTimer(TIMER_ID)) {
            return; // Timer not initialized yet
        }

        int timer = state.getTimer(TIMER_ID);
        if (timer > 0) {
            state.setTimer(TIMER_ID, timer - 1);
        } else {
            // Time to trigger the small structure event
            boolean eventTriggered = SmallStructureEvent.triggerEvent(server);
            if (eventTriggered) {
                HorrorMod129.LOGGER.info("SmallStructureEvent triggered successfully.");
                // Reset the timer with a new random delay
                state.setTimer(TIMER_ID, getRandomDelay(state));
            } else {
                // If event could not be triggered, retry in 1 minute
                state.setTimer(TIMER_ID, 1200); // 1 minute in ticks
            }
        }
    }

    /**
     * Debug / test helper to set the small structure timer directly (in ticks).
     */
    public static void setTimer(MinecraftServer server, int ticks) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setTimer(TIMER_ID, Math.max(ticks, 1));
        HorrorMod129.LOGGER.info("SmallStructureScheduler timer set to " + state.getTimer(TIMER_ID) + " ticks via debug command");
    }

}