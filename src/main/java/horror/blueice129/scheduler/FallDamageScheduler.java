package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.FallDamageEvent;
import horror.blueice129.utils.DayUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class FallDamageScheduler {

    private static final String TIMER_ID = "fallDamageTimer";
    private static final String POST_DAY3_TIMER_STARTED_ID = "fallDamagePostDay3TimerStarted";

    private static final int MIN_WORLD_AGE_DAYS = 3; // must be over day 3
    private static final int RETRY_DELAY_TICKS = 20 * 60; // 1 minute
    private static final int POST_DAY3_DELAY_TICKS = 20 * 60 * 5; // 5 minutes
    private static final int BASE_COOLDOWN_TICKS = 20 * 60 * 60; // 1 hour
    private static final int MIN_COOLDOWN_TICKS = 20 * 60 * 15; // 15 minutes

    private static final Random RANDOM = new Random();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FallDamageScheduler::onServerTick);

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) {
                return;
            }
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                if (!state.hasTimer(TIMER_ID)) {
                    if (isWorldOldEnough(server, state)) {
                        state.setTimer(TIMER_ID, POST_DAY3_DELAY_TICKS);
                        state.setIntValue(POST_DAY3_TIMER_STARTED_ID, 1);
                    } else {
                        state.setTimer(TIMER_ID, RETRY_DELAY_TICKS);
                        state.setIntValue(POST_DAY3_TIMER_STARTED_ID, 0);
                    }

                    HorrorMod129.LOGGER.info(
                            "FallDamageScheduler initialized with timer: {} ticks",
                            state.getTimer(TIMER_ID));
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered FallDamageScheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (!state.hasTimer(TIMER_ID)) {
            return;
        }

        int timer = state.getTimer(TIMER_ID);
        if (timer > 0) {
            state.setTimer(TIMER_ID, timer - 1);
            return;
        }

        if (!isWorldOldEnough(server, state)) {
            state.setIntValue(POST_DAY3_TIMER_STARTED_ID, 0);
            state.setTimer(TIMER_ID, RETRY_DELAY_TICKS);
            return;
        }

        if (state.getIntValue(POST_DAY3_TIMER_STARTED_ID, 0) == 0) {
            state.setIntValue(POST_DAY3_TIMER_STARTED_ID, 1);
            state.setTimer(TIMER_ID, POST_DAY3_DELAY_TICKS);
            return;
        }

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            state.setTimer(TIMER_ID, RETRY_DELAY_TICKS);
            return;
        }

        ServerPlayerEntity target = players.get(RANDOM.nextInt(players.size()));
        boolean triggered = FallDamageEvent.triggerEvent(target);

        if (triggered) {
            state.setTimer(TIMER_ID, getCooldownWithAgro(state));
        } else {
            state.setTimer(TIMER_ID, RETRY_DELAY_TICKS);
        }
    }

    private static boolean isWorldOldEnough(MinecraftServer server, HorrorModPersistentState state) {
        World overworld = server.getOverworld();
        if (overworld == null) {
            return false;
        }

        long worldTime = overworld.getTimeOfDay();
        long worldTimeOffset = state.getLongValue("worldTimeOffset", 0L);
        int actualDay = DayUtils.getCurrentActualDay(worldTime, worldTimeOffset);

        return actualDay > MIN_WORLD_AGE_DAYS;
    }

    private static int getCooldownWithAgro(HorrorModPersistentState state) {
        int agroMeter = Math.max(0, Math.min(10, state.getIntValue("agroMeter", 0)));
        int cooldownRange = BASE_COOLDOWN_TICKS - MIN_COOLDOWN_TICKS;
        int reduction = (cooldownRange * agroMeter) / 10;
        return Math.max(MIN_COOLDOWN_TICKS, BASE_COOLDOWN_TICKS - reduction);
    }

    public static void setTimer(MinecraftServer server, int ticks) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setTimer(TIMER_ID, Math.max(ticks, 1));
        HorrorMod129.LOGGER.info("FallDamageScheduler timer set to {} ticks via debug command", state.getTimer(TIMER_ID));
    }
}