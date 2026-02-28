package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.config.ConfigManager;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.network.ModNetworking;
import horror.blueice129.network.SettingsTriggerPayload;
import horror.blueice129.utils.EntityUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import net.minecraft.util.math.random.Random;

/**
 * Server-side scheduler for settings modification events.
 * Sends packets to random clients when timers expire, triggering client-side setting changes.
 * Each setting has its own timer and triggers independently:
 * - Render Distance: Decreases by 4 every 30-60 minutes (adjusted by agro)
 * - Brightness: Sets to moody every 30-60 minutes (adjusted by agro)
 * - FPS: Caps to 30 every 30-60 minutes (adjusted by agro)
 * - Mouse Sensitivity: Decreases by 10% every 30-60 minutes (adjusted by agro)
 * - Smooth Lighting: Disables every 30-60 minutes (adjusted by agro)
 */
public class SettingsScheduler {
    private static final Random random = Random.create();
    private static final int minProximityToEntity = 15;
    private static final String TIMER_ID = "settingsTimer";
    private static final String ENTITY_COOLDOWN_ID = "entityProximityCooldown";

    /**
     * Registers the tick event to handle the settings scheduler.
     * This runs on the server side and sends packets to clients.
     */
    public static void register() {
        if (!ConfigManager.getConfig().enableSettingsModifications) return;
        
        ServerTickEvents.END_SERVER_TICK.register(SettingsScheduler::onServerTick);

        // Register server world loading event to initialize timer if needed
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
                // Initialize timer on first run
                if (!state.hasTimer(TIMER_ID)) {
                    int agroMeter = state.getIntValue("agroMeter", 0);
                    state.setTimer(TIMER_ID, Math.max(1, 20 * 60 * 60 - (agroMeter * 2 * 60 * 20))); // 1 hour - (agro*2 minutes)
                    HorrorMod129.LOGGER.info("SettingsScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered SettingsScheduler (server-side)");
    }

    /**
     * Called every server tick.
     * Updates timers and sends setting trigger packets to random clients when timers expire.
     * 
     * @param server The Minecraft server instance
     */
    private static void onServerTick(MinecraftServer server) {
        // Skip if server is empty (pause timers)
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

        int mainTimer = state.decrementTimer(TIMER_ID, 1);
        if (mainTimer > 0) {
            return;
        }

        // Check entity cooldown
        if (state.getTimer(ENTITY_COOLDOWN_ID) > 0) {
            state.decrementTimer(ENTITY_COOLDOWN_ID, 1);
            // Reset main timer to avoid repeated checks during cooldown
            state.setTimer(TIMER_ID, getRandomDelayWithAgro(state, 20 * 60 * 30, 20 * 60 * 60));
            return;
        }

        // Select a random player to trigger settings change
        ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayerList()
                .get(random.nextInt(server.getPlayerManager().getPlayerList().size()));

        // TODO: Implement proper entity proximity check when entity is added
        // For now, using placeholder - will always return false
        boolean isNearEntity = EntityUtils.isEntityNearPlayer(null, null, minProximityToEntity);
        if (isNearEntity) {
            // Set cooldown to avoid immediate retriggering
            state.setTimer(ENTITY_COOLDOWN_ID, 20 * 60 * 6); // 6 minute cooldown
        }

        // Build list of enabled settings based on config
        java.util.List<SettingsTriggerPayload.SettingType> enabledSettings = new java.util.ArrayList<>();
        if (ConfigManager.getConfig().enableRenderDistanceChange) {
            enabledSettings.add(SettingsTriggerPayload.SettingType.RENDER_DISTANCE);
        }
        if (ConfigManager.getConfig().enableBrightnessChange) {
            enabledSettings.add(SettingsTriggerPayload.SettingType.BRIGHTNESS);
        }
        if (ConfigManager.getConfig().enableFpsChange) {
            enabledSettings.add(SettingsTriggerPayload.SettingType.FPS);
        }
        if (ConfigManager.getConfig().enableMouseSensitivityChange) {
            enabledSettings.add(SettingsTriggerPayload.SettingType.MOUSE_SENSITIVITY);
        }
        if (ConfigManager.getConfig().enableSmoothLightingChange) {
            enabledSettings.add(SettingsTriggerPayload.SettingType.SMOOTH_LIGHTING);
        }
        
        // Skip if no settings are enabled
        if (enabledSettings.isEmpty()) {
            state.setTimer(TIMER_ID, getRandomDelayWithAgro(state, 20 * 60 * 30, 20 * 60 * 60));
            return;
        }
        
        // Choose a random setting to trigger from enabled settings
        SettingsTriggerPayload.SettingType settingToTrigger = enabledSettings.get(random.nextInt(enabledSettings.size()));
        
        // Send packet to the target player
        ModNetworking.sendSettingsTrigger(targetPlayer, settingToTrigger);
        
        // Reset main timer to 30-60 minutes (adjusted by agro)
        state.setTimer(TIMER_ID, getRandomDelayWithAgro(state, 20 * 60 * 30, 20 * 60 * 60));
    }

    /**
     * Generates a random delay between min and max, adjusted by the agro meter.
     * The delay is reduced by (agro * 2) minutes.
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
