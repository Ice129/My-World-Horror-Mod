package horror.blueice129.scheduler;

public class Unifier {
    public static final float MULTI_EVENT_CHANCE = 0.2f; // 20% chance for a multi-event

    public static final String[] TIMERS = {
            "caveMinerTimer", // CaveMinerScheduler: main countdown to run cave pre-mining.
            "blueice129_spawn_timer", // Blueice129SpawnScheduler: countdown to next spawn attempt.
            "fakeFootstepTimer", // FakeFootstepScheduler: delay before trying a fake footsteps event.
            "homeEventTimer", // HomeEventScheduler: global delay before home event becomes ready.
            "homeTriggerCountdown_<playerUuid>", // HomeEventScheduler: per-player short countdown before trigger.
            "player_death_items_timer", // PlayerDeathItemsScheduler: delay before next fake death-items event.
            "ledgePusherCooldown", // LedgePusherScheduler: cooldown between ledge push opportunities.
            "screenshotTimer", // ScreenshotScheduler: delay before next screenshot attempt.
            "smallStructureTimer", // SmallStructureScheduler: delay before next structure event.
            "stalkingFootstepTimer", // StalkingFootstepScheduler: delay before starting stalking footsteps.
            "settingsTimer", // SettingsScheduler: delay before next client setting disruption.
            "entityProximityCooldown", // SettingsScheduler: cooldown when entity-near condition is met.
            "twoPlayerSleepModeTimer", // TwoPlayerSleepScheduler: window before switching fake sleeper mode.
            "twoPlayerSleepDelayTimer", // TwoPlayerSleepScheduler: delay before forcing fake sleeper asleep.
            "ambianceCooldown_<playerUuid>", // AmbianceScheduler: per-player damage-trigger sound cooldown.
            "fakeFootstepPlaybackTimer", // FakeFootsteps: step interval during active playback.
            "stalkingStepTimer", // StalkingFootsteps: step interval while stalking is walking.
            "stalkingPauseTimer" // StalkingFootsteps: check interval while stalking is paused.
    };

}
