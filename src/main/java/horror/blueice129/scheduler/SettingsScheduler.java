package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.RenderDistanceChanger;
// import horror.blueice129.feature.MusicVolumeLocker;
import horror.blueice129.feature.BrightnessChanger;
import horror.blueice129.feature.FpsLimiter;
import horror.blueice129.feature.MouseSensitivityChanger;
import horror.blueice129.feature.SmoothLightingChanger;
import horror.blueice129.utils.EntityUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

import net.minecraft.util.math.random.Random;

/**
 * This class schedules settings modification events
 * Each setting has its own timer and triggers independently
 * - Render Distance: Decreases by 2 every 20 - 40-agro*2 minutes
 * - Music Volume: Locks to minimum 50% every 20 ticks (1 second)
 * - Brightness: decreases to moody gradually over 100 ticks, every 10 -
 * 40-agro*2 minutes
 * - FPS: Caps to 30 every 30 - 60-agro*2 minutes
 * - Mouse Sensitivity: Decreases to minimum over 300 ticks, every 15 -
 * 45-agro*2 minutes
 * - Smooth Lighting: Disables smooth lighting every 10 - 25-agro*2 minutes
 * 
 * Runs on the client side but accesses server's persistent state for timer management
 */
@Environment(EnvType.CLIENT)
public class SettingsScheduler {
    private static final Random random = Random.create();

    private enum SettingType {
        RENDER_DISTANCE,
        // MUSIC_VOLUME,
        BRIGHTNESS,
        FPS,
        MOUSE_SENSITIVITY,
        SMOOTH_LIGHTING
    }

    private static final int minProximityToEntity = 15;
    private static final String TIMER_ID = "settingsTimer";

    private static final String ENTITY_COOLDOWN_ID = "entityProximityCooldown";

    /**
     * Registers the tick event to handle the settings scheduler.
     * This should be called during client initialization.
     * Runs on client side but accesses server persistent state for timers.
     */
    public static void register() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return; // Exit if not running on the client side
        }

        ClientTickEvents.END_CLIENT_TICK.register(SettingsScheduler::onClientTick);

        HorrorMod129.LOGGER.info("Registered SettingsScheduler (client-side)");
    }

    /**
     * Called every client tick.
     * Updates all timers and triggers the appropriate setting modifications when
     * timers reach zero.
     * Accesses server persistent state through the integrated server.
     * 
     * @param client The Minecraft client instance
     */
    private static void onClientTick(MinecraftClient client) {
        // Skip if not in-game or no server available
        if (client.world == null || client.getServer() == null) {
            return;
        }

        MinecraftServer server = client.getServer();
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

        // Initialize timer on first run
        if (!state.hasTimer(TIMER_ID)) {
            int agroMeter = state.getIntValue("agroMeter", 0);
            state.setTimer(TIMER_ID, Math.max(1, 20 * 60 * 60 - (agroMeter * 2 * 60 * 20))); // Initial timer set to 1 hour - (agro*2 minutes), minimum 1 tick
            HorrorMod129.LOGGER.info("SettingsScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
        }

        int mainTimer = state.decrementTimer(TIMER_ID, 1);
        if (mainTimer > 0) {
            return;
        }

        if (state.getTimer(ENTITY_COOLDOWN_ID) > 0) {
            state.decrementTimer(ENTITY_COOLDOWN_ID, 1);
            // Reset main timer to avoid repeated checks during cooldown
            state.setTimer(TIMER_ID, getRandomDelayWithAgro(state, 20 * 60 * 30, 20 * 60 * 60));
            return;
        }

        // TODO: Implement proper entity proximity check when entity is added
        // For now, using placeholder - will always return false
        boolean isNearEntity = EntityUtils.isEntityNearPlayer(null, null, minProximityToEntity);
        if (isNearEntity) {
            // Set cooldown to avoid immediate retriggering
            state.setTimer(ENTITY_COOLDOWN_ID, 20 * 60 * 6); // 6 minute cooldown
            // continue
        }



        SettingType[] settings = SettingType.values();
        SettingType settingToTrigger = settings[random.nextInt(settings.length)];
        switch (settingToTrigger) {
            case RENDER_DISTANCE:
                RenderDistanceChanger.decreaseRenderDistance(4);
                HorrorMod129.LOGGER.info("Render distance decreased by 4. New render distance: "
                        + RenderDistanceChanger.getRenderDistance());
                break;
            case BRIGHTNESS:
                BrightnessChanger.setToMoodyBrightness();
                HorrorMod129.LOGGER.info("Brightness set to moody");
                break;
            case FPS:
                FpsLimiter.capFpsTo30();
                HorrorMod129.LOGGER.info("FPS capped to 30");
                break;
            case MOUSE_SENSITIVITY:
                MouseSensitivityChanger.decreaseMouseSensitivity(0.10);
                HorrorMod129.LOGGER.info("Mouse sensitivity decreased by 10%. New sensitivity: "
                        + MouseSensitivityChanger.getMouseSensitivity());
                break;
            case SMOOTH_LIGHTING:
                SmoothLightingChanger.disableSmoothLighting();
                HorrorMod129.LOGGER.info("Smooth lighting disabled");
                break;
            default:
                break;
        }
        state.setTimer(TIMER_ID, getRandomDelayWithAgro(state, 20 * 60 * 30, 20 * 60 * 60)); // 30-60 minutes
        // Reset main timer to 30-60 minutes - (agro*2 minutes)
    }

    /**
     * Generates a random delay between min and max, adjusted by the agro meter.
     * The delay is reduced by (agro * 2) minutes as per the Javadoc formula.
     * 
     * @param state        The persistent state containing the agro meter
     * @param baseMinDelay Base minimum delay in ticks
     * @param baseMaxDelay Base maximum delay in ticks
     * @return A random number of ticks to wait, adjusted for agro level
     */
    private static int getRandomDelayWithAgro(HorrorModPersistentState state, int baseMinDelay, int baseMaxDelay) {
        int agroMeter = state.getIntValue("agroMeter", 0);
        // Reduce delays by agro*2 minutes (agro*2 * 60 seconds * 20 ticks)
        int agroReduction = agroMeter * 2 * 60 * 20;

        int adjustedMinDelay = Math.max(1, baseMinDelay - agroReduction);
        int adjustedMaxDelay = Math.max(adjustedMinDelay, baseMaxDelay - agroReduction);

        return adjustedMinDelay + random.nextInt(adjustedMaxDelay - adjustedMinDelay + 1);
    }
}
