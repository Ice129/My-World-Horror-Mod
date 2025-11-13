package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class TempAggroIncrease {
    private static final int MAX_AGGRO = 10; // Maximum aggro meter value

    /**
     * Registers the tick event to set aggro meter based on current day.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TempAggroIncrease::onServerTick);
        HorrorMod129.LOGGER.info("Registered TempAggroIncrease scheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        // Get the overworld
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return;
        
        // Get current day from world time (every 24000 ticks = 1 day)
        long currentDay = overworld.getTimeOfDay() / 24000L;
        
        // Set aggro meter to current day, capped at MAX_AGGRO
        int targetAggro = Math.min((int)currentDay, MAX_AGGRO);
        
        // Get the persistent state
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int currentAggro = state.getIntValue("agroMeter", 0);
        
        // Only update and log if the value changed
        if (currentAggro != targetAggro) {
            state.setIntValue("agroMeter", targetAggro);
            HorrorMod129.LOGGER.info("Aggro meter set to day " + currentDay + " (value: " + targetAggro + ")");
        }
    }
}