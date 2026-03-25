package horror.blueice129.feature;

public class TwoPlayerSleep {
    public enum FakeSleeperMode {
        LOGGED_OUT,
        AWAKE,
        ASLEEP
    }

    private static FakeSleeperMode fakeSleeperMode = FakeSleeperMode.LOGGED_OUT;

    private TwoPlayerSleep() {
    }

    // Getters and setters for the fake sleeper mode
    public static FakeSleeperMode getFakeSleeperMode() {
        return fakeSleeperMode;
    }

    public static void setFakeSleeperMode(FakeSleeperMode mode) {
        fakeSleeperMode = mode == null ? FakeSleeperMode.LOGGED_OUT : mode;
    }

    public static int getExtraOnlinePlayers() {
        return fakeSleeperMode == FakeSleeperMode.LOGGED_OUT ? 0 : 1;
    }

    public static int getExtraSleepingPlayers() {
        return fakeSleeperMode == FakeSleeperMode.ASLEEP ? 1 : 0;
    }

}
