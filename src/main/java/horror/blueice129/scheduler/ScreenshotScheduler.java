package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.ScreenshotTaker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class ScreenshotScheduler {

    private static final Random RANDOM = Random.create();
    private static final int MIN_DELAY = 20 * 60 * 40;
    private static final int MAX_DELAY = 20 * 60 * 70;
    private static final String TIMER_ID = "screenshotTimer";

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ScreenshotScheduler::onServerTick);

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) {
                return;
            }
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay(state));
                    HorrorMod129.LOGGER.info("ScreenshotScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered ScreenshotScheduler");
    }

    private static int getRandomDelay(HorrorModPersistentState state) {
        int agroMeter = state.getIntValue("agroMeter", 0);
        int agroReduction = (-agroMeter * agroMeter) * 50;
        int delay = RANDOM.nextBetween(MIN_DELAY, MAX_DELAY + 1) + agroReduction;
        return Math.max(delay, 20 * 60 * 15);
    }

    private static void onServerTick(MinecraftServer server) {
        ScreenshotTaker.tick();

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
        } else {
            ServerPlayerEntity player = server.getPlayerManager().getPlayerList()
                    .get(RANDOM.nextInt(server.getPlayerManager().getPlayerList().size()));

            boolean success = ScreenshotTaker.takeScreenshotOfPlayer(player);
            if (success) {
                state.setTimer(TIMER_ID, getRandomDelay(state));
                HorrorMod129.LOGGER.info("Screenshot event triggered for player: " + player.getName().getString());
            } else {
                state.setTimer(TIMER_ID, 60 * 20); // Retry after 1 minute if screenshot failed
                HorrorMod129.LOGGER.warn("Screenshot event failed for player: " + player.getName().getString());
            }
        }
    }

    public static void setTimer(MinecraftServer server, int ticks) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setTimer(TIMER_ID, Math.max(ticks, 1));
        HorrorMod129.LOGGER.info("ScreenshotScheduler timer set to " + state.getTimer(TIMER_ID) + " ticks via debug command");
    }
}
