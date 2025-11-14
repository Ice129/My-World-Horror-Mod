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
     * Gradually decreases mouse sensitivity over time
     * This method should be called repeatedly to create a smooth transition
     * MUST be called from the client thread
     * @param initialSensitivity The initial sensitivity at the start of the transition
     * @param targetSensitivity The target sensitivity to reach (must be <= initialSensitivity)
     * @param currentProgress Current progress (0 to totalTicks)
     * @param totalTicks Total number of ticks for the transition
     * @throws IllegalArgumentException if targetSensitivity > initialSensitivity
     */
    public static void decreaseMouseSensitivityGradually(double initialSensitivity, double targetSensitivity, int currentProgress, int totalTicks) {
        if (targetSensitivity > initialSensitivity) {
            throw new IllegalArgumentException("targetSensitivity (" + targetSensitivity + ") must be <= initialSensitivity (" + initialSensitivity + ") for decreaseMouseSensitivityGradually");
        }

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
            
            // Calculate the new sensitivity based on progress
            // Linear interpolation from initial to target
            double progress = (double) currentProgress / totalTicks;
            double newSensitivity = initialSensitivity + ((targetSensitivity - initialSensitivity) * progress);
            
            // Clamp and set the new sensitivity
            double clampedSensitivity = Math.max(targetSensitivity, Math.min(initialSensitivity, newSensitivity));
            mouseSensitivity.setValue(clampedSensitivity);
            client.options.write();
        });
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
