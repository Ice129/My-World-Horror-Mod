package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.UndergroundTunnelEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.UUID;

public final class UndergroundTunnelScheduler {
    private static final Random RANDOM = Random.create();
    private static final String TIMER_ID = "undergroundTunnelTimer";
    private static final int MIN_DELAY = 20 * 60 * 45;
    private static final int MAX_DELAY = 20 * 60 * 70;
    private static final int RETRY_DELAY = 20 * 60 * 2;
    private static final int MIN_START_DAY = 5;
    private static final int SAMPLE_INTERVAL_TICKS = 10;

    private static UUID trackedPlayerUuid;

    private UndergroundTunnelScheduler() {
    }

    public static void register() {
        UndergroundTunnelEvent.register();
        ServerTickEvents.END_SERVER_TICK.register(UndergroundTunnelEvent::tick);
        ServerTickEvents.END_SERVER_TICK.register(UndergroundTunnelScheduler::onServerTick);
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) {
                return;
            }

            if (world.getRegistryKey() != World.OVERWORLD) {
                return;
            }

            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            if (!state.hasTimer(TIMER_ID)) {
                state.setTimer(TIMER_ID, getRandomDelay());
                selectTrackedPlayer(server);
                UndergroundTunnelEvent.resetPlayerLocationHistory();
                HorrorMod129.LOGGER.info("UndergroundTunnelScheduler initialized with timer: {} ticks", state.getTimer(TIMER_ID));
            }
        });
        HorrorMod129.LOGGER.info("Registered UndergroundTunnelScheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (!state.hasTimer(TIMER_ID)) {
            state.setTimer(TIMER_ID, getRandomDelay());
            selectTrackedPlayer(server);
            UndergroundTunnelEvent.resetPlayerLocationHistory();
            return;
        }

        ServerPlayerEntity trackedPlayer = getTrackedPlayer(server);
        if (trackedPlayer == null) {
            selectTrackedPlayer(server);
            trackedPlayer = getTrackedPlayer(server);
        }

        if (trackedPlayer != null && server.getTicks() % SAMPLE_INTERVAL_TICKS == 0) {
            UndergroundTunnelEvent.recordPlayerLocation(trackedPlayer);
        }

        int timer = state.getTimer(TIMER_ID);
        if (timer > 0) {
            state.setTimer(TIMER_ID, timer - 1);
            return;
        }

        if (getActualDay(server) <= MIN_START_DAY) {
            state.setTimer(TIMER_ID, RETRY_DELAY);
            return;
        }

        if (trackedPlayer == null) {
            state.setTimer(TIMER_ID, RETRY_DELAY);
            selectTrackedPlayer(server);
            UndergroundTunnelEvent.resetPlayerLocationHistory();
            return;
        }

        if (!UndergroundTunnelEvent.shouldTriggerEvent(server, trackedPlayer)) {
            state.setTimer(TIMER_ID, RETRY_DELAY);
            return;
        }

        if (UndergroundTunnelEvent.triggerEvent(server, trackedPlayer)) {
            state.setTimer(TIMER_ID, getRandomDelay());
            selectTrackedPlayer(server);
            UndergroundTunnelEvent.resetPlayerLocationHistory();
        } else {
            state.setTimer(TIMER_ID, RETRY_DELAY);
        }
    }

    private static int getRandomDelay() {
        return MIN_DELAY + RANDOM.nextInt(MAX_DELAY - MIN_DELAY + 1);
    }

    private static int getActualDay(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (server.getOverworld() == null) {
            return 0;
        }

        long worldTime = server.getOverworld().getTimeOfDay();
        long worldTimeOffset = state.getLongValue("worldTimeOffset", 0L);
        return (int) ((worldTime - worldTimeOffset) / 24000L);
    }

    private static void selectTrackedPlayer(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            trackedPlayerUuid = null;
            return;
        }

        trackedPlayerUuid = server.getPlayerManager().getPlayerList().get(RANDOM.nextInt(server.getPlayerManager().getPlayerList().size())).getUuid();
    }

    private static ServerPlayerEntity getTrackedPlayer(MinecraftServer server) {
        if (trackedPlayerUuid == null) {
            return null;
        }

        return server.getPlayerManager().getPlayer(trackedPlayerUuid);
    }
}