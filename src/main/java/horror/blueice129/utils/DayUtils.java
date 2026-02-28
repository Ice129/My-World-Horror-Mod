package horror.blueice129.utils;

public class DayUtils {

    /**
     * get the current actual day, not the adjusted day based on world time offset
     */
    public static int getCurrentActualDay(long worldTime, long worldTimeOffset) {
        long adjustedTime = worldTime - worldTimeOffset;
        return (int) (adjustedTime / 24000L);
    }
}