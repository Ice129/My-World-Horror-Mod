package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Enforces a maximum gamma value every tick
 * This ensures the player cannot set gamma above 100% during gameplay
 * The check runs every second (20 ticks)
 */
@Environment(EnvType.CLIENT)
public class FullbrightKiller {
    private static final double MAX_ALLOWED_GAMMA = 1.0;
    private static final double ENFORCED_GAMMA = 0.7;
    private static final int TICK_CHECK_INTERVAL = 20; // Check every second

    private static boolean isInitialized = false;
    private static int tickCounter = 0;

    /**
        * Registers the tick event that enforces gamma limits
     * Should be called during client initialization
     */
    public static void initialize() {
        if (isInitialized) {
            HorrorMod129.LOGGER.warn("FullbrightKiller already initialized, skipping...");
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only run if in-game and not paused
            if (client.world == null) {
                return;
            }


            if (tickCounter > 0) {
                tickCounter--;
                return;
            }

            double currentGamma = client.options.getGamma().getValue();
            if (currentGamma > MAX_ALLOWED_GAMMA) {
                client.options.getGamma().setValue(ENFORCED_GAMMA);
                client.options.write();
                HorrorMod129.LOGGER.info("FullbrightKiller: Detected gamma above 100%, resetting to 70%");
            }

            HorrorMod129.LOGGER.debug("FullbrightKiller: Current gamma: " + client.options.getGamma().getValue());
            tickCounter = TICK_CHECK_INTERVAL;
        });

        isInitialized = true;
        HorrorMod129.LOGGER.info("FullbrightKiller initialized - enforcing gamma cap every second");
    }
}
