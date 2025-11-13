package horror.blueice129.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Modifies mouse sensitivity settings
 * This feature can gradually reduce the player's mouse sensitivity to create disorientation
 */
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
     * @param sensitivity The target sensitivity (0.0 = minimum, 1.0 = maximum)
     */
    public static void setMouseSensitivity(double sensitivity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        SimpleOption<Double> mouseSensitivity = client.options.getMouseSensitivity();
        if (mouseSensitivity == null) {
            return;
        }

        // Clamp sensitivity between 0.0 and 1.0
        double clampedSensitivity = Math.max(MIN_SENSITIVITY, Math.min(MAX_SENSITIVITY, sensitivity));
        mouseSensitivity.setValue(clampedSensitivity);
        client.options.write();
    }

    /**
     * Gradually decreases mouse sensitivity over time
     * This method should be called repeatedly to create a smooth transition
     * @param targetSensitivity The target sensitivity to reach
     * @param currentProgress Current progress (0 to totalTicks)
     * @param totalTicks Total number of ticks for the transition
     */
    public static void decreaseMouseSensitivityGradually(double targetSensitivity, int currentProgress, int totalTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        SimpleOption<Double> mouseSensitivity = client.options.getMouseSensitivity();
        if (mouseSensitivity == null) {
            return;
        }

        double currentSensitivity = mouseSensitivity.getValue();
        
        // Calculate the new sensitivity based on progress
        // Linear interpolation from current to target
        double progress = (double) currentProgress / totalTicks;
        double newSensitivity = currentSensitivity - ((currentSensitivity - targetSensitivity) * progress / totalTicks);
        
        // Clamp and set the new sensitivity
        double clampedSensitivity = Math.max(targetSensitivity, Math.min(currentSensitivity, newSensitivity));
        mouseSensitivity.setValue(clampedSensitivity);
        client.options.write();
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
