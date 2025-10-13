package horror.blueice129.scheduler;

import net.minecraft.util.math.random.Random;

public class StripMineScheduler {
    private static final Random random = Random.create();
    private static final int MIN_START_DAY = 4;
    private static final int MAX_START_DAY = 6;
    private static final String TIMER_ID = "stripMineEventTimer";
    private static final String EVENT_READY_ID = "stripMineEventReady";
}
