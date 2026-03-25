package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.TwoPlayerSleep;
import horror.blueice129.utils.EntityLoginState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

public class TwoPlayerSleepScheduler {
    private static final Random RANDOM = new Random();
    private static final String MODE_TIMER_ID = "twoPlayerSleepModeTimer";
    private static final String SLEEP_DELAY_TIMER_ID = "twoPlayerSleepDelayTimer";
    private static final String PLAYER_SLEEPING_KEY = "twoPlayerSleepPlayerSleeping";
    private static final int MODE_WINDOW_TICKS = 20 * 60 * 5; // 5 minutes
    private static final int SLEEP_DELAY_TICKS = 20 * 10; // 10 seconds

    private TwoPlayerSleepScheduler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TwoPlayerSleepScheduler::onServerTick);
        HorrorMod129.LOGGER.info("Registered TwoPlayerSleepScheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

        if (!EntityLoginState.isEntityOnline(state)) {
            TwoPlayerSleep.setFakeSleeperMode(TwoPlayerSleep.FakeSleeperMode.LOGGED_OUT);
            state.setTimer(MODE_TIMER_ID, 0);
            state.setTimer(SLEEP_DELAY_TIMER_ID, 0);
            state.setIntValue(PLAYER_SLEEPING_KEY, 0);
            return;
        }

        if (TwoPlayerSleep.getFakeSleeperMode() == TwoPlayerSleep.FakeSleeperMode.LOGGED_OUT) {
            TwoPlayerSleep.setFakeSleeperMode(getRandomAwakeOrAsleep());
            state.setTimer(MODE_TIMER_ID, MODE_WINDOW_TICKS);
        }

        boolean playerSleeping = isAnyPlayerSleeping(server);
        boolean wasPlayerSleeping = state.getIntValue(PLAYER_SLEEPING_KEY, 0) == 1;

        if (playerSleeping) {
            handleSleepingPlayer(state, wasPlayerSleeping);
        } else {
            state.setTimer(SLEEP_DELAY_TIMER_ID, 0);
        }

        int modeTimer = state.getTimer(MODE_TIMER_ID);
        if (modeTimer <= 0) {
            state.setTimer(MODE_TIMER_ID, MODE_WINDOW_TICKS);
            modeTimer = MODE_WINDOW_TICKS;
        }

        modeTimer = state.decrementTimer(MODE_TIMER_ID, 1);
        if (modeTimer == 0) {
            TwoPlayerSleep.setFakeSleeperMode(getRandomAwakeOrAsleep());
            state.setTimer(MODE_TIMER_ID, MODE_WINDOW_TICKS);
            state.setTimer(SLEEP_DELAY_TIMER_ID, 0);
        }

        state.setIntValue(PLAYER_SLEEPING_KEY, playerSleeping ? 1 : 0);
    }

    private static void handleSleepingPlayer(HorrorModPersistentState state, boolean wasPlayerSleeping) {
        TwoPlayerSleep.FakeSleeperMode mode = TwoPlayerSleep.getFakeSleeperMode();

        if (mode == TwoPlayerSleep.FakeSleeperMode.AWAKE) {
            int sleepDelay = state.getTimer(SLEEP_DELAY_TIMER_ID);
            if (sleepDelay <= 0) {
                state.setTimer(SLEEP_DELAY_TIMER_ID, SLEEP_DELAY_TICKS);
                return;
            }

            sleepDelay = state.decrementTimer(SLEEP_DELAY_TIMER_ID, 1);
            if (sleepDelay == 0) {
                TwoPlayerSleep.setFakeSleeperMode(TwoPlayerSleep.FakeSleeperMode.ASLEEP);
                state.setTimer(MODE_TIMER_ID, MODE_WINDOW_TICKS);
            }
            return;
        }

        if (mode == TwoPlayerSleep.FakeSleeperMode.ASLEEP && !wasPlayerSleeping) {
            state.setTimer(MODE_TIMER_ID, MODE_WINDOW_TICKS);
        }
    }

    private static TwoPlayerSleep.FakeSleeperMode getRandomAwakeOrAsleep() {
        return RANDOM.nextBoolean() ? TwoPlayerSleep.FakeSleeperMode.AWAKE : TwoPlayerSleep.FakeSleeperMode.ASLEEP;
    }

    private static boolean isAnyPlayerSleeping(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isSpectator() && player.isSleeping()) {
                return true;
            }
        }
        return false;
    }

}
