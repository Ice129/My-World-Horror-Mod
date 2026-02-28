package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.sounds.FakeFootsteps;
import horror.blueice129.sounds.StalkingFootsteps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Random;

public class StalkingFootstepScheduler {

    private static final Random random = new Random();
    private static final String TIMER_ID = "stalkingFootstepTimer";
    private static final int MIN_DELAY = 20 * 60 * 25; // 25 minutes in ticks
    private static final int MAX_DELAY = 20 * 60 * 50; // 50 minutes in ticks
    private static final int MIN_AGRO = 4;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(StalkingFootstepScheduler::onServerTick);

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay());
                    HorrorMod129.LOGGER.info(
                            "StalkingFootstepScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered StalkingFootstepScheduler");
    }

    private static void onServerTick(MinecraftServer server) {

        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (state.getIntValue("agroMeter", 0) < MIN_AGRO) {
            return; // Don't run stalking logic if agro level is too low
        }

        // While a stalking event is active, drive its tick logic every tick
        if (StalkingFootsteps.isActive(server)) {
            StalkingFootsteps.tickStalking(server);
            return;
        }

        // Main trigger timer
        // HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int currentTimer = state.getTimer(TIMER_ID);
        if (currentTimer <= 0) return;

        currentTimer = state.decrementTimer(TIMER_ID, 1);
        if (currentTimer == 0) {
            boolean triggered = tryTriggerStalking(server);
            // Reset timer: full delay on success, short retry on failure
            state.setTimer(TIMER_ID, triggered ? getRandomDelay() : 20 * 60 * 3);
        }
    }

    private static boolean tryTriggerStalking(MinecraftServer server) {
        var players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) return false;

        ServerPlayerEntity target = players.get(random.nextInt(players.size()));

        if (FakeFootsteps.validateFootstepConditions(target.getServerWorld(), target) != null) {
            HorrorMod129.LOGGER.info("StalkingFootstepScheduler: conditions not met for " + target.getName().getString());
            return false;
        }

        HorrorMod129.LOGGER.info("StalkingFootstepScheduler: triggering for " + target.getName().getString());
        return StalkingFootsteps.startStalking(server, target);
    }

    private static int getRandomDelay() {
        return MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY + 1);
    }
}