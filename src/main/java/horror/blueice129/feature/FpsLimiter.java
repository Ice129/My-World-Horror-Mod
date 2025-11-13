package horror.blueice129.feature;

import net.minecraft.client.MinecraftClient;

/**
 * Caps FPS to 30 or lower
 * This feature can create lag and tension when an entity is nearby
 */
public class FpsLimiter {
    private static final int TARGET_FPS = 30;

    /**
     * Sets the FPS limit to 30
     * This will cap the game's frame rate to create a more tense atmosphere
     */
    public static void capFpsTo30() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.options.getMaxFps().setValue(TARGET_FPS);
        client.options.write();
    }

    /**
     * Gets the current FPS limit
     * @return Current FPS limit
     */
    public static int getCurrentFpsLimit() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 260; // Default max FPS
        }

        return client.options.getMaxFps().getValue();
    }

    /**
     * Sets a custom FPS limit
     * @param fps The desired FPS limit (minimum 10, maximum 260)
     */
    public static void setFpsLimit(int fps) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Clamp FPS between 10 and 260
        int clampedFps = Math.max(10, Math.min(260, fps));
        client.options.getMaxFps().setValue(clampedFps);
        client.options.write();
    }

    /**
     * Gets the target FPS for limiting
     * @return Target FPS (30)
     */
    public static int getTargetFps() {
        return TARGET_FPS;
    }
}
