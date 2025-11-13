package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class TempAggroIncrease {
    private static final String LAST_DAY_KEY = "aggroLastDayChecked";
    private static final int MAX_AGGRO = 10; // Maximum aggro meter value

    /**
     * Registers the tick event to handle aggro meter increases.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TempAggroIncrease::onServerTick);

        // Initialize last day counter when world loads
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
                // If not initialized, set to current day
                if (!state.hasIntValue(LAST_DAY_KEY)) {
                    long currentDay = world.getTimeOfDay() / 24000L;
                    state.setIntValue(LAST_DAY_KEY, (int)currentDay);
                    HorrorMod129.LOGGER.info("TempAggroIncrease initialized at day: " + currentDay);
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered TempAggroIncrease scheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        // Get the overworld
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return;
        
        // Get current day from world time
        long currentDay = overworld.getTimeOfDay() / 24000L;
        
        // Get the persistent state
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int lastCheckedDay = state.getIntValue(LAST_DAY_KEY, 0);
        
        // Check if a new day has started
        if (currentDay > lastCheckedDay) {
            // Calculate how many days have passed
            int daysPassed = (int)(currentDay - lastCheckedDay);
            
            // Increase aggro for each day passed
            for (int i = 0; i < daysPassed; i++) {
                increaseAggroMeter(state);
            }
            
            // Update last checked day
            state.setIntValue(LAST_DAY_KEY, (int)currentDay);
            
            if (daysPassed > 1) {
                HorrorMod129.LOGGER.info("Multiple days passed (" + daysPassed + "), aggro increased accordingly");
            }
        }
    }

    /**
     * Increases the aggro meter by 1, capped at MAX_AGGRO.
     * 
     * @param state The persistent state to update
     */
    private static void increaseAggroMeter(HorrorModPersistentState state) {
        int currentAggro = state.getIntValue("agroMeter", 0);
        
        if (currentAggro < MAX_AGGRO) {
            int newAggro = currentAggro + 1;
            state.setIntValue("agroMeter", newAggro);
            HorrorMod129.LOGGER.info("Aggro meter increased: " + currentAggro + " -> " + newAggro);
        } else {
            HorrorMod129.LOGGER.info("Aggro meter already at maximum (" + MAX_AGGRO + "), not increasing");
        }
    }
}