package horror.blueice129.feature;

import horror.blueice129.HorrorMod129;

/**
 * Manages sound-related features for the horror mod.
 * Currently includes features to increase cave ambient sound frequency.
 */
public class SoundFeatures {
    
    // The factor by which cave sounds are more likely to play
    private static final int CAVE_SOUND_FREQUENCY_MULTIPLIER = 2;
    
    /**
     * Initialize sound features
     */
    public static void register() {
        HorrorMod129.LOGGER.info("Registering sound features: Cave sounds are {}x more frequent", 
                                CAVE_SOUND_FREQUENCY_MULTIPLIER);
    }
}