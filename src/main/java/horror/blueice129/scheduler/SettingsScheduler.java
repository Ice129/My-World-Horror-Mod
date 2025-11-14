package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.RenderDistanceChanger;
import horror.blueice129.feature.MusicVolumeLocker;
import horror.blueice129.feature.BrightnessChanger;
import horror.blueice129.feature.FpsLimiter;
import horror.blueice129.feature.MouseSensitivityChanger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import net.minecraft.util.math.random.Random;

/**
 * This class schedules settings modification events
 * Each setting has its own timer and triggers independently
 * - Render Distance: Decreases by 2 every 20 - 40-agro*2 minutes
 * - Music Volume: Locks to minimum 50% every 20 ticks (1 second)
 * - Brightness: decreases to moody gradually over 100 ticks, every 10 - 40-agro*2 minutes
 * - FPS: Caps to 30 every 30 - 60-agro*2 minutes
 * - Mouse Sensitivity: Decreases to minimum over 300 ticks, every 15 - 45-agro*2 minutes
 */
public class SettingsScheduler {
    private static final Random random = Random.create();

    // Render distance timer settings (20-40 minutes, reduced by agro*2)
    private static final int RENDER_BASE_MAX_DELAY = 48000; // 40 minutes
    private static final int RENDER_BASE_MIN_DELAY = 24000; // 20 minutes
    private static final String TIMER_ID_RENDER = "settingsTimerRenderDistance";

    // Music volume timer settings (every 20 ticks / 1 second)
    private static final int MUSIC_CHECK_INTERVAL = 20; // 20 ticks (1 second)
    private static final String TIMER_ID_MUSIC = "settingsTimerMusicVolume";

    // Brightness timer settings (10-40 minutes, reduced by agro*2)
    private static final int BRIGHTNESS_BASE_MAX_DELAY = 48000; // 40 minutes
    private static final int BRIGHTNESS_BASE_MIN_DELAY = 12000; // 10 minutes
    private static final int BRIGHTNESS_REDUCTION_TICKS = 100; // 100 ticks (5 seconds) for gradual reduction
    private static final String TIMER_ID_BRIGHTNESS = "settingsTimerBrightness";
    private static final String TIMER_ID_BRIGHTNESS_PROGRESS = "settingsTimerBrightnessProgress";

    // FPS timer settings (30-60 minutes, reduced by agro*2)
    private static final int FPS_BASE_MAX_DELAY = 72000; // 60 minutes
    private static final int FPS_BASE_MIN_DELAY = 36000; // 30 minutes
    private static final String TIMER_ID_FPS = "settingsTimerFps";

    // Mouse sensitivity timer settings (15-45 minutes, reduced by agro*2)
    private static final int SENSITIVITY_BASE_MAX_DELAY = 54000; // 45 minutes
    private static final int SENSITIVITY_BASE_MIN_DELAY = 18000; // 15 minutes 
    private static final int SENSITIVITY_REDUCTION_TICKS = 300; // 300 ticks (15 seconds) for gradual reduction
    private static final String TIMER_ID_SENSITIVITY = "settingsTimerMouseSensitivity";
    private static final String TIMER_ID_SENSITIVITY_PROGRESS = "settingsTimerMouseSensitivityProgress";

    /**
     * Registers the tick event to handle the settings scheduler.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SettingsScheduler::onServerTick);
        
        // Register server world loading event to initialize timers if needed
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
                // Initialize render distance timer
                if (!state.hasTimer(TIMER_ID_RENDER)) {
                    int delay = getRandomDelayWithAgro(state, RENDER_BASE_MIN_DELAY, RENDER_BASE_MAX_DELAY);
                    state.setTimer(TIMER_ID_RENDER, delay);
                    HorrorMod129.LOGGER.info("SettingsScheduler (Render Distance) initialized with timer: " 
                        + state.getTimer(TIMER_ID_RENDER) + " ticks");
                }
                
                // Initialize music volume timer
                if (!state.hasTimer(TIMER_ID_MUSIC)) {
                    state.setTimer(TIMER_ID_MUSIC, MUSIC_CHECK_INTERVAL);
                    HorrorMod129.LOGGER.info("SettingsScheduler (Music Volume) initialized with timer: " 
                        + state.getTimer(TIMER_ID_MUSIC) + " ticks");
                }
                
                // Initialize brightness timer
                if (!state.hasTimer(TIMER_ID_BRIGHTNESS)) {
                    int delay = getRandomDelayWithAgro(state, BRIGHTNESS_BASE_MIN_DELAY, BRIGHTNESS_BASE_MAX_DELAY);
                    state.setTimer(TIMER_ID_BRIGHTNESS, delay);
                    HorrorMod129.LOGGER.info("SettingsScheduler (Brightness) initialized with timer: " 
                        + state.getTimer(TIMER_ID_BRIGHTNESS) + " ticks");
                }
                
                // Initialize brightness progress timer (starts at 0)
                if (!state.hasTimer(TIMER_ID_BRIGHTNESS_PROGRESS)) {
                    state.setTimer(TIMER_ID_BRIGHTNESS_PROGRESS, 0);
                }
                
                // Initialize FPS timer
                if (!state.hasTimer(TIMER_ID_FPS)) {
                    int delay = getRandomDelayWithAgro(state, FPS_BASE_MIN_DELAY, FPS_BASE_MAX_DELAY);
                    state.setTimer(TIMER_ID_FPS, delay);
                    HorrorMod129.LOGGER.info("SettingsScheduler (FPS) initialized with timer: " 
                        + state.getTimer(TIMER_ID_FPS) + " ticks");
                }
                
                // Initialize mouse sensitivity timer
                if (!state.hasTimer(TIMER_ID_SENSITIVITY)) {
                    int delay = getRandomDelayWithAgro(state, SENSITIVITY_BASE_MIN_DELAY, SENSITIVITY_BASE_MAX_DELAY);
                    state.setTimer(TIMER_ID_SENSITIVITY, delay);
                    HorrorMod129.LOGGER.info("SettingsScheduler (Mouse Sensitivity) initialized with timer: " 
                        + state.getTimer(TIMER_ID_SENSITIVITY) + " ticks");
                }
                
                // Initialize mouse sensitivity progress timer (starts at 0)
                if (!state.hasTimer(TIMER_ID_SENSITIVITY_PROGRESS)) {
                    state.setTimer(TIMER_ID_SENSITIVITY_PROGRESS, 0);
                }
            }
        });
        
        HorrorMod129.LOGGER.info("Registered SettingsScheduler");
    }

    /**
     * Called every server tick.
     * Updates all timers and triggers the appropriate setting modifications when timers reach zero.
     * Music volume is checked every 20 ticks (1 second) to enforce the minimum.
     * 
     * @param server The Minecraft server instance
     */
    private static void onServerTick(MinecraftServer server) {
        // Skip if the server is empty
        if (server.getCurrentPlayerCount() == 0) {
            return;
        }

        // Get the first online player (if any)
        if (!server.getPlayerManager().getPlayerList().isEmpty()) {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            
            // Handle render distance timer
            int renderTimer = state.decrementTimer(TIMER_ID_RENDER, 1);
            if (renderTimer <= 0) {
                RenderDistanceChanger.decreaseRenderDistance(2);
                HorrorMod129.LOGGER.info("Render distance decreased by 2. New render distance: " 
                    + RenderDistanceChanger.getRenderDistance());
                int delay = getRandomDelayWithAgro(state, RENDER_BASE_MIN_DELAY, RENDER_BASE_MAX_DELAY);
                state.setTimer(TIMER_ID_RENDER, delay);
            }
            
            // Handle music volume timer - check every 20 ticks (1 second)
            int musicTimer = state.decrementTimer(TIMER_ID_MUSIC, 1);
            if (musicTimer <= 0) {
                MusicVolumeLocker.enforceMinimumMusicVolume();
                HorrorMod129.LOGGER.info("Music volume locked to minimum. Current volume: " 
                    + (MusicVolumeLocker.getMusicVolume() * 100) + "%");
                state.setTimer(TIMER_ID_MUSIC, MUSIC_CHECK_INTERVAL); // Check again in 20 ticks (1 second)
            }
            
            // Handle brightness timer
            int brightnessTimer = state.decrementTimer(TIMER_ID_BRIGHTNESS, 1);
            if (brightnessTimer <= 0) {
                // Start the gradual reduction process
                int progress = state.getTimer(TIMER_ID_BRIGHTNESS_PROGRESS);
                if (progress < BRIGHTNESS_REDUCTION_TICKS) {
                    // Continue reducing brightness
                    double initialBrightness = BrightnessChanger.getBrightness();
                    if (progress == 0) {
                        // Store initial brightness when starting
                        HorrorMod129.LOGGER.info("Starting brightness reduction from " 
                            + (initialBrightness * 100) + "%");
                    }
                    BrightnessChanger.decreaseBrightnessGradually(0.0, progress, BRIGHTNESS_REDUCTION_TICKS);
                    state.setTimer(TIMER_ID_BRIGHTNESS_PROGRESS, progress + 1);
                    state.setTimer(TIMER_ID_BRIGHTNESS, 1); // Check again next tick
                } else {
                    // Reduction complete, reset for next cycle
                    HorrorMod129.LOGGER.info("Brightness reduction complete. Current brightness: " 
                        + (BrightnessChanger.getBrightness() * 100) + "%");
                    state.setTimer(TIMER_ID_BRIGHTNESS_PROGRESS, 0);
                    int delay = getRandomDelayWithAgro(state, BRIGHTNESS_BASE_MIN_DELAY, BRIGHTNESS_BASE_MAX_DELAY);
                    state.setTimer(TIMER_ID_BRIGHTNESS, delay);
                }
            }
            
            // Handle FPS timer
            int fpsTimer = state.decrementTimer(TIMER_ID_FPS, 1);
            if (fpsTimer <= 0) {
                FpsLimiter.capFpsTo30();
                HorrorMod129.LOGGER.info("FPS capped to 30. Current FPS limit: " 
                    + FpsLimiter.getCurrentFpsLimit());
                int delay = getRandomDelayWithAgro(state, FPS_BASE_MIN_DELAY, FPS_BASE_MAX_DELAY);
                state.setTimer(TIMER_ID_FPS, delay);
            }
            
            // Handle mouse sensitivity timer
            int sensitivityTimer = state.decrementTimer(TIMER_ID_SENSITIVITY, 1);
            if (sensitivityTimer <= 0) {
                // Start the gradual reduction process
                int progress = state.getTimer(TIMER_ID_SENSITIVITY_PROGRESS);
                if (progress < SENSITIVITY_REDUCTION_TICKS) {
                    // Continue reducing sensitivity
                    double initialSensitivity = MouseSensitivityChanger.getMouseSensitivity();
                    if (progress == 0) {
                        // Store initial sensitivity when starting
                        HorrorMod129.LOGGER.info("Starting mouse sensitivity reduction from " 
                            + (initialSensitivity * 100) + "%");
                    }
                    MouseSensitivityChanger.decreaseMouseSensitivityGradually(0.0, progress, SENSITIVITY_REDUCTION_TICKS);
                    state.setTimer(TIMER_ID_SENSITIVITY_PROGRESS, progress + 1);
                    state.setTimer(TIMER_ID_SENSITIVITY, 1); // Check again next tick
                } else {
                    // Reduction complete, reset for next cycle
                    HorrorMod129.LOGGER.info("Mouse sensitivity reduction complete. Current sensitivity: " 
                        + (MouseSensitivityChanger.getMouseSensitivity() * 100) + "%");
                    state.setTimer(TIMER_ID_SENSITIVITY_PROGRESS, 0);
                    int delay = getRandomDelayWithAgro(state, SENSITIVITY_BASE_MIN_DELAY, SENSITIVITY_BASE_MAX_DELAY);
                    state.setTimer(TIMER_ID_SENSITIVITY, delay);
                }
            }
        }
    }

    /**
     * Generates a random delay between min and max, adjusted by the agro meter.
     * The delay is reduced by (agro * 2) minutes as per the Javadoc formula.
     * 
     * @param state The persistent state containing the agro meter
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
