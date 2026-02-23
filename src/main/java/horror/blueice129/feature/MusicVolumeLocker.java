package horror.blueice129.feature;

import horror.blueice129.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.sound.SoundCategory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Locks music volume to a minimum of 50%
 * This feature can be disabled in the config
 */
@Environment(EnvType.CLIENT)
public class MusicVolumeLocker {
    private static final double MIN_MUSIC_VOLUME = 0.01; // 1% volume

    /**
     * Ensures music volume is at least 50%
     * If current volume is below minimum, it will be set to the minimum
     * MUST be called from the client thread
     * @return true if volume was below 50% and was increased to 50%, false otherwise
     */
    public static boolean enforceMinimumMusicVolume() {
        if (!ConfigManager.getConfig().enableMusicVolumeLocking) {
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }

        // Use CompletableFuture to get the result from the client thread execution
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        client.execute(() -> {
            try {
                SimpleOption<Double> musicVolume = client.options.getSoundVolumeOption(SoundCategory.MUSIC);
                if (musicVolume == null) {
                    future.complete(false);
                    return;
                }

                double currentVolume = musicVolume.getValue();
                if (currentVolume < MIN_MUSIC_VOLUME) {
                    musicVolume.setValue(MIN_MUSIC_VOLUME);
                    client.options.write();
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        // Wait for the result with a timeout
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // If we can't get the result, assume no change was made
            return false;
        }
    }

    /**
     * Gets the current music volume
     * @return Current music volume (0.0 to 1.0)
     */
    public static double getMusicVolume() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 1.0;
        }

        SimpleOption<Double> musicVolume = client.options.getSoundVolumeOption(SoundCategory.MUSIC);
        if (musicVolume == null) {
            return 1.0;
        }

        return musicVolume.getValue();
    }

    /**
     * Gets the minimum music volume threshold
     * @return Minimum music volume (0.5 = 50%)
     */
    public static double getMinimumMusicVolume() {
        return MIN_MUSIC_VOLUME;
    }
}
