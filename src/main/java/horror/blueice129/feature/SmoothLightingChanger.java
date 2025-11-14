package horror.blueice129.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Changes smooth lighting (Ambient Occlusion) settings
 * This feature can disable smooth lighting to create a more unsettling visual atmosphere
 */
public class SmoothLightingChanger {
    
    /**
     * Disables smooth lighting (Ambient Occlusion)
     * This creates a harsher, more jarring lighting effect
     */
    public static void disableSmoothLighting() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        SimpleOption<Boolean> ao = client.options.getAo();
        if (ao == null) {
            return;
        }

        ao.setValue(false);
        client.options.write();
    }

    /**
     * Enables smooth lighting (Ambient Occlusion)
     * This restores the normal lighting effect
     */
    public static void enableSmoothLighting() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        SimpleOption<Boolean> ao = client.options.getAo();
        if (ao == null) {
            return;
        }

        ao.setValue(true);
        client.options.write();
    }

    /**
     * Gets the current smooth lighting state
     * @return true if smooth lighting is enabled, false otherwise
     */
    public static boolean isSmoothLightingEnabled() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return true; // Default to enabled
        }

        SimpleOption<Boolean> ao = client.options.getAo();
        if (ao == null) {
            return true; // Default to enabled
        }

        return ao.getValue();
    }

    /**
     * Toggles smooth lighting on/off
     */
    public static void toggleSmoothLighting() {
        if (isSmoothLightingEnabled()) {
            disableSmoothLighting();
        } else {
            enableSmoothLighting();
        }
    }
}
