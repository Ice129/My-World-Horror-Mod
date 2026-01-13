package horror.blueice129.network;

import horror.blueice129.HorrorMod129;
import horror.blueice129.feature.BrightnessChanger;
import horror.blueice129.feature.FpsLimiter;
import horror.blueice129.feature.MouseSensitivityChanger;
import horror.blueice129.feature.RenderDistanceChanger;
import horror.blueice129.feature.SmoothLightingChanger;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
// import net.minecraft.network.PacketByteBuf;
// import net.minecraft.util.Identifier;

/**
 * Handles client-side packet reception and processing.
 * Registers listeners for packets sent from the server.
 */
public class ClientPacketHandler {

    /**
     * Registers client-side packet receivers.
     * Should be called during client initialization.
     */
    public static void registerClientReceivers() {
        // Register receiver for settings trigger packets
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SETTINGS_TRIGGER_ID, (client, handler, buf, responseSender) -> {
            // Read payload on network thread and execute handling on main thread
            final SettingsTriggerPayload.SettingType type = SettingsTriggerPayload.read(buf);
            client.execute(() -> handleSettingsTrigger(type));
        });

        HorrorMod129.LOGGER.info("Registered client packet receivers");
    }

    /**
     * Handles settings trigger packets received from the server.
     * Modifies the appropriate client setting based on the packet type.
     *
     * @param settingType The type of setting to modify
     */
    private static void handleSettingsTrigger(SettingsTriggerPayload.SettingType settingType) {
        switch (settingType) {
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
                HorrorMod129.LOGGER.warn("Unknown settings trigger type: " + settingType);
                break;
        }
    }
}
