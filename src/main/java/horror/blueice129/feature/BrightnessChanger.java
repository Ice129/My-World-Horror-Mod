package horror.blueice129.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Changes brightness settings to moody (minimum)
 * This feature creates an eerie atmosphere by forcing dark lighting
 */
@Environment(EnvType.CLIENT)
public class BrightnessChanger {
    private static final double MOODY_BRIGHTNESS = 0.0; // Minimum brightness (moody)

    /**
     * Sets brightness to moody (minimum)
     * This is the darkest lighting setting in Minecraft
     * MUST be called from the client thread
     */
    public static void setToMoodyBrightness() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Execute on client thread to avoid RenderSystem threading issues
        client.execute(() -> {
            SimpleOption<Double> gamma = client.options.getGamma();
            if (gamma == null) {
                return;
            }

            gamma.setValue(MOODY_BRIGHTNESS);
            client.options.write();
        });
    }

    /**
     * Gets the current brightness level
     * @return Current brightness (0.0 = moody, 1.0 = bright)
     */
    public static double getBrightness() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 1.0;
        }

        SimpleOption<Double> gamma = client.options.getGamma();
        if (gamma == null) {
            return 1.0;
        }

        return gamma.getValue();
    }

    /**
     * Gets the moody brightness value
     * @return Moody brightness level (0.0)
     */
    public static double getMoodyBrightness() {
        return MOODY_BRIGHTNESS;
    }

    /**
     * Gradually decreases brightness over time
     * This method should be called repeatedly to create a smooth transition
     * MUST be called from the client thread
     * @param initialBrightness The initial brightness at the start of the transition
     * @param targetBrightness The target brightness to reach (must be <= initialBrightness)
     * @param currentProgress Current progress (0 to totalTicks)
     * @param totalTicks Total number of ticks for the transition
     * @throws IllegalArgumentException if targetBrightness > initialBrightness
     */
    public static void decreaseBrightnessGradually(double initialBrightness, double targetBrightness, int currentProgress, int totalTicks) {
        if (targetBrightness > initialBrightness) {
            throw new IllegalArgumentException("targetBrightness (" + targetBrightness + ") must be <= initialBrightness (" + initialBrightness + ") for decreaseBrightnessGradually");
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Execute on client thread to avoid RenderSystem threading issues
        client.execute(() -> {
            SimpleOption<Double> gamma = client.options.getGamma();
            if (gamma == null) {
                return;
            }
            
            // Calculate the new brightness based on progress
            // Linear interpolation from initial to target
            double progress = (double) currentProgress / totalTicks;
            double newBrightness = initialBrightness + ((targetBrightness - initialBrightness) * progress);
            
            // Clamp and set the new brightness
            double clampedBrightness = Math.max(targetBrightness, Math.min(initialBrightness, newBrightness));
            gamma.setValue(clampedBrightness);
            client.options.write();
        });
    }
}
