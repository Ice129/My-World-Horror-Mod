package horror.blueice129.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Modifies mouse sensitivity settings
 * This feature can gradually reduce the player's mouse sensitivity to create disorientation
 */
@Environment(EnvType.CLIENT)
public class MouseSensitivityChanger {
    private static final double MIN_SENSITIVITY = 0.0; // Minimum sensitivity
    private static final double MAX_SENSITIVITY = 1.0; // Maximum sensitivity

    /**
     * Gets the current mouse sensitivity
     * @return Current sensitivity (0.0 = minimum, 1.0 = maximum)
     */
    public static double getMouseSensitivity() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 0.5; // Default sensitivity
        }

        SimpleOption<Double> sensitivity = client.options.getMouseSensitivity();
        if (sensitivity == null) {
            return 0.5;
        }

        return sensitivity.getValue();
    }

    /**
     * Sets the mouse sensitivity to a specific value
     * MUST be called from the client thread
     * @param sensitivity The target sensitivity (0.0 = minimum, 1.0 = maximum)
     */
    public static void setMouseSensitivity(double sensitivity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Execute on client thread to avoid RenderSystem threading issues
        client.execute(() -> {
            SimpleOption<Double> mouseSensitivity = client.options.getMouseSensitivity();
            if (mouseSensitivity == null) {
                return;
            }

            // Clamp sensitivity between 0.0 and 1.0
            double clampedSensitivity = Math.max(MIN_SENSITIVITY, Math.min(MAX_SENSITIVITY, sensitivity));
            mouseSensitivity.setValue(clampedSensitivity);
            client.options.write();
        });
    }

    /**
     * Decreases mouse sensitivity by a specific amount.
     * @param amount The amount to decrease, in the range 0.0 to 1.0 (e.g., 0.20 for 20%).
     */
    public static void decreaseMouseSensitivity(double amount) {
        double currentSensitivity = getMouseSensitivity();
        setMouseSensitivity(currentSensitivity - amount);
    }

    /**
     * Sets the mouse sensitivity to minimum
     */
    public static void setToMinimumSensitivity() {
        setMouseSensitivity(MIN_SENSITIVITY);
    }

    /**
     * Increases mouse sensitivity by a specific amount
     * @param amount The amount to increase (can be negative to decrease)
     */
    public static void adjustMouseSensitivity(double amount) {
        double currentSensitivity = getMouseSensitivity();
        setMouseSensitivity(currentSensitivity + amount);
    }
}
