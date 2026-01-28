package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.utils.DayUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

/**
 * Scheduler that tracks actual days passed and sets the aggro meter accordingly.
 * The aggro meter increases to match the current day number, starting at day 1.
 * Maximum aggro meter value is capped at 10.
 */
public class AgroMeterScheduler {
    private static final String LAST_KNOWN_DAY_KEY = "lastKnownDay";
    
    /**
     * Registers the tick event to track day changes and update aggro meter.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AgroMeterScheduler::onServerTick);
        
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient())
                return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
                // Initialize on first load (check if lastKnownDay is uninitialized)
                int lastKnownDay = state.getIntValue(LAST_KNOWN_DAY_KEY, -1);
                if (lastKnownDay == -1) {
                    long worldTime = server.getOverworld().getTimeOfDay();
                    long worldTimeOffset = state.getLongValue("worldTimeOffset", 0L);
                    int currentDay = DayUtils.getCurrentActualDay(worldTime, worldTimeOffset);
                    
                    state.setIntValue(LAST_KNOWN_DAY_KEY, currentDay);
                    // Set initial aggro meter to current day (minimum 1, capped at 10)
                    int initialAggro = Math.min(10, Math.max(1, currentDay));
                    state.setIntValue("agroMeter", initialAggro);
                    
                    HorrorMod129.LOGGER.info("AgroMeterScheduler initialized: Day {}, Aggro {}", currentDay, initialAggro);
                }
            }
        });
        
        HorrorMod129.LOGGER.info("Registered AgroMeterScheduler");
    }
    
    /**
     * Called every server tick to check for day changes.
     */
    private static void onServerTick(MinecraftServer server) {
        // Only check once per 200 ticks (10 seconds)
        if (server.getTicks() % (200) != 0) {
            return;
        }
        
        World overworld = server.getOverworld();
        if (overworld == null) {
            return;
        }
        
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        
        long worldTime = overworld.getTimeOfDay();
        long worldTimeOffset = state.getLongValue("worldTimeOffset", 0L);
        int currentDay = DayUtils.getCurrentActualDay(worldTime, worldTimeOffset);
        int lastKnownDay = state.getIntValue(LAST_KNOWN_DAY_KEY, 0);
        
        // Check if a new day has started
        if (currentDay > lastKnownDay) {
            // Update last known day
            state.setIntValue(LAST_KNOWN_DAY_KEY, currentDay);
            
            // Set aggro meter to current day (minimum 1, capped at 10)
            int newAggro = Math.min(10, Math.max(1, currentDay));
            state.setIntValue("agroMeter", newAggro);
            
            HorrorMod129.LOGGER.info("Day changed from {} to {}. Aggro meter set to {}", 
                lastKnownDay, currentDay, newAggro);
        }
    }
}
