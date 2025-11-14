package horror.blueice129.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Changes smooth lighting (Ambient Occlusion) settings
 * This feature can disable smooth lighting to create a more unsettling visual atmosphere
 */
@Environment(EnvType.CLIENT)
public class SmoothLightingChanger {
    
    /**
     * Disables smooth lighting (Ambient Occlusion)
     * This creates a harsher, more jarring lighting effect
     * MUST be called from the client thread
     */
    public static void disableSmoothLighting() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Execute on client thread to avoid RenderSystem threading issues
        client.execute(() -> {
            SimpleOption<Boolean> ao = client.options.getAo();
            if (ao == null) {
                return;
            }

            ao.setValue(false);
            client.options.write();
        });
    }

    /**
     * Enables smooth lighting (Ambient Occlusion)
     * This restores the normal lighting effect
     * MUST be called from the client thread
     */
    public static void enableSmoothLighting() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Execute on client thread to avoid RenderSystem threading issues
        client.execute(() -> {
            SimpleOption<Boolean> ao = client.options.getAo();
            if (ao == null) {
                return;
            }

            ao.setValue(true);
            client.options.write();
        });
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

