package horror.blueice129.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.sound.SoundCategory;

/**
 * Locks music volume to a minimum of 50%
 * This feature can be disabled in the config
 */
public class MusicVolumeLocker {
    private static final double MIN_MUSIC_VOLUME = 0.5; // 50%

    /**
     * Ensures music volume is at least 50%
     * If current volume is below minimum, it will be set to the minimum
     * @return true if volume was changed, false otherwise
     */
    public static boolean enforceMinimumMusicVolume() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }

        SimpleOption<Double> musicVolume = client.options.getSoundVolumeOption(SoundCategory.MUSIC);
        if (musicVolume == null) {
            return false;
        }

        double currentVolume = musicVolume.getValue();
        if (currentVolume < MIN_MUSIC_VOLUME) {
            musicVolume.setValue(MIN_MUSIC_VOLUME);
            client.options.write();
            return true;
        }
        return false;
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
