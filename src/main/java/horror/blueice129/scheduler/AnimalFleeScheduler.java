package horror.blueice129.scheduler;

import java.util.ArrayList;
import java.util.List;

import horror.blueice129.HorrorMod129;
import horror.blueice129.feature.AnimalFleeEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;

public class AnimalFleeScheduler {
    private static final Random RANDOM = Random.create();
    private static final String TIMER_ID = "animalFleeTimer";
    private static final int SCHEDULE_DELAY_TICKS = 20 * 60 * 60 * 2;
    private static final int RETRY_DELAY_TICKS = 20 * 60 * 5;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AnimalFleeScheduler::onServerTick);

        HorrorMod129.LOGGER.info("Registered AnimalFleeScheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        AnimalFleeEvent.tick();

        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        horror.blueice129.data.HorrorModPersistentState state = horror.blueice129.data.HorrorModPersistentState.getServerState(server);
        if (!state.hasTimer(TIMER_ID)) {
            state.setTimer(TIMER_ID, SCHEDULE_DELAY_TICKS);
        }

        int timer = state.getTimer(TIMER_ID);
        if (timer > 0) {
            state.setTimer(TIMER_ID, timer - 1);
            return;
        }

        if (attemptTrigger(server)) {
            state.setTimer(TIMER_ID, SCHEDULE_DELAY_TICKS);
        } else {
            state.setTimer(TIMER_ID, RETRY_DELAY_TICKS);
        }
    }

    private static boolean attemptTrigger(MinecraftServer server) {
        List<ServerPlayerEntity> eligiblePlayers = new ArrayList<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (AnimalFleeEvent.getNearbyAnimalCount(player) >= AnimalFleeEvent.MIN_ANIMALS) {
                eligiblePlayers.add(player);
            }
        }

        if (eligiblePlayers.isEmpty()) {
            HorrorMod129.LOGGER.info("AnimalFleeScheduler trigger failed: not enough animals near any player");
            return false;
        }

        ServerPlayerEntity targetPlayer = eligiblePlayers.get(RANDOM.nextInt(eligiblePlayers.size()));
        boolean success = AnimalFleeEvent.triggerEvent(targetPlayer);

        if (success) {
            HorrorMod129.LOGGER.info("AnimalFleeScheduler triggered around " + targetPlayer.getName().getString());
        }

        return success;
    }
}