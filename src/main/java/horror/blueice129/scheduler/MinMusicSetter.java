package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.feature.MusicVolumeLocker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Enforces minimum music volume every tick
 * This ensures the player cannot lower the music volume below 50% during gameplay
 * The check runs every client tick (20 times per second)
 */
@Environment(EnvType.CLIENT)
public class MinMusicSetter {
    private static boolean isInitialized = false;
    private static int tickCooldown = 0;
    private static final int TICK_COOLDOWN_DURATION = 20; // 1 second cooldown

    /**
     * Registers the tick event that checks and enforces minimum music volume
     * Should be called during client initialization
     */
    public static void initialize() {
        if (isInitialized) {
            HorrorMod129.LOGGER.warn("MinMusicSetter already initialized, skipping...");
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only run if in-game and not paused
            if (client.world == null || client.isPaused()) {
                return;
            }

            // Check and enforce minimum music volume
            // This runs every tick, so music volume cannot stay below 50%
            if (tickCooldown > 0) {
                tickCooldown--;
                return;
            }
            MusicVolumeLocker.enforceMinimumMusicVolume();
            tickCooldown = TICK_COOLDOWN_DURATION;
        });

        isInitialized = true;
        HorrorMod129.LOGGER.info("MinMusicSetter initialized - enforcing minimum music volume every tick");
    }

    /**
     * Checks if the MinMusicSetter has been initialized
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
}
