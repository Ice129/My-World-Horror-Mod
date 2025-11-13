package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.RenderDistanceChanger;
import horror.blueice129.feature.MusicVolumeLocker;
import horror.blueice129.feature.BrightnessChanger;
import horror.blueice129.feature.FpsLimiter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import net.minecraft.util.math.random.Random;

/**
 * This class schedules settings modification events
 * Each setting has its own timer and triggers independently
 * - Render Distance: Decreases by 2 every 20-60 minutes
 * - Music Volume: Locks to minimum 50% every 15-45 minutes
 * - Brightness: Sets to moody every 25-55 minutes
 * - FPS: Caps to 30 every 30-60 minutes
 */
public class SettingsScheduler {
    private static final Random random = Random.create();

    // TODO: make these linked with agro meter

    // Render distance timer settings
    private static final int RENDER_MAX_DELAY = 72000; // 60 minutes
    private static final int RENDER_MIN_DELAY = 24000; // 20 minutes
    private static final String TIMER_ID_RENDER = "settingsTimerRenderDistance";

    // Music volume timer settings
    private static final int MUSIC_MAX_DELAY = 21; // 45 minutes
    private static final int MUSIC_MIN_DELAY = 22; // 15 minutes
    private static final String TIMER_ID_MUSIC = "settingsTimerMusicVolume";

    // Brightness timer settings
    private static final int BRIGHTNESS_MAX_DELAY = 66000; // 55 minutes
    private static final int BRIGHTNESS_MIN_DELAY = 30000; // 25 minutes
    private static final String TIMER_ID_BRIGHTNESS = "settingsTimerBrightness";

    // FPS timer settings
    private static final int FPS_MAX_DELAY = 72000; // 60 minutes
    private static final int FPS_MIN_DELAY = 36000; // 30 minutes
    private static final String TIMER_ID_FPS = "settingsTimerFps";

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
                    state.setTimer(TIMER_ID_RENDER, getRandomDelay(RENDER_MIN_DELAY, RENDER_MAX_DELAY));
                    HorrorMod129.LOGGER.info("SettingsScheduler (Render Distance) initialized with timer: " 
                        + state.getTimer(TIMER_ID_RENDER) + " ticks");
                }
                
                // Initialize music volume timer
                if (!state.hasTimer(TIMER_ID_MUSIC)) {
                    state.setTimer(TIMER_ID_MUSIC, getRandomDelay(MUSIC_MIN_DELAY, MUSIC_MAX_DELAY));
                    HorrorMod129.LOGGER.info("SettingsScheduler (Music Volume) initialized with timer: " 
                        + state.getTimer(TIMER_ID_MUSIC) + " ticks");
                }
                
                // Initialize brightness timer
                if (!state.hasTimer(TIMER_ID_BRIGHTNESS)) {
                    state.setTimer(TIMER_ID_BRIGHTNESS, getRandomDelay(BRIGHTNESS_MIN_DELAY, BRIGHTNESS_MAX_DELAY));
                    HorrorMod129.LOGGER.info("SettingsScheduler (Brightness) initialized with timer: " 
                        + state.getTimer(TIMER_ID_BRIGHTNESS) + " ticks");
                }
                
                // Initialize FPS timer
                if (!state.hasTimer(TIMER_ID_FPS)) {
                    state.setTimer(TIMER_ID_FPS, getRandomDelay(FPS_MIN_DELAY, FPS_MAX_DELAY));
                    HorrorMod129.LOGGER.info("SettingsScheduler (FPS) initialized with timer: " 
                        + state.getTimer(TIMER_ID_FPS) + " ticks");
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
                state.setTimer(TIMER_ID_RENDER, getRandomDelay(RENDER_MIN_DELAY, RENDER_MAX_DELAY));
            }
            
            // Handle music volume timer - check every 20 ticks (1 second)
            int musicTimer = state.decrementTimer(TIMER_ID_MUSIC, 1);
            if (musicTimer <= 0) {
                MusicVolumeLocker.enforceMinimumMusicVolume();
                HorrorMod129.LOGGER.info("Music volume locked to minimum. Current volume: " 
                    + (MusicVolumeLocker.getMusicVolume() * 100) + "%");
                state.setTimer(TIMER_ID_MUSIC, 20); // Check again in 20 ticks (1 second)
            }
            
            // Handle brightness timer
            int brightnessTimer = state.decrementTimer(TIMER_ID_BRIGHTNESS, 1);
            if (brightnessTimer <= 0) {
                BrightnessChanger.setToMoodyBrightness();
                HorrorMod129.LOGGER.info("Brightness set to moody (minimum). Current brightness: " 
                    + BrightnessChanger.getBrightness());
                state.setTimer(TIMER_ID_BRIGHTNESS, getRandomDelay(BRIGHTNESS_MIN_DELAY, BRIGHTNESS_MAX_DELAY));
            }
            
            // Handle FPS timer
            int fpsTimer = state.decrementTimer(TIMER_ID_FPS, 1);
            if (fpsTimer <= 0) {
                FpsLimiter.capFpsTo30();
                HorrorMod129.LOGGER.info("FPS capped to 30. Current FPS limit: " 
                    + FpsLimiter.getCurrentFpsLimit());
                state.setTimer(TIMER_ID_FPS, getRandomDelay(FPS_MIN_DELAY, FPS_MAX_DELAY));
            }
        }
    }

    /**
     * Generates a random delay between min and max.
     * 
     * @param minDelay Minimum delay in ticks
     * @param maxDelay Maximum delay in ticks
     * @return A random number of ticks to wait
     */
    private static int getRandomDelay(int minDelay, int maxDelay) {
        return minDelay + random.nextInt(maxDelay - minDelay + 1);
    }
}
