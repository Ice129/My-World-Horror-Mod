package horror.blueice129.mixin;

import net.minecraft.client.sound.BiomeEffectSoundPlayer;
import net.minecraft.sound.BiomeAdditionsSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import java.util.Optional;

@Mixin(BiomeEffectSoundPlayer.class) // Target the class that handles ambient biome sounds
public class AmbientSoundHandlerMixin {

    /**
     * Injects code at the beginning of the tick method to add additional chances
     * for cave sounds to play.
     * 
     * WHAT THIS METHOD DOES:
     * ---------------------
     * 1. Examining BiomeEffectSoundPlayer's tick() method, we found this code:
     * this.additionsSound.ifPresent(sound -> {
     * if (this.random.nextDouble() < sound.getChance()) {
     * this.soundManager.play(PositionedSoundInstance.ambient(sound.getSound().value()));
     * }
     * });
     * 
     * 2. Cave sounds are part of "additionsSound" and the chance of them playing
     * is determined by the condition: random.nextDouble() < sound.getChance()
     * 
     * 3. Since we can't easily modify that comparison directly with @ModifyArg,
     * we're using @Inject to add an additional check with the same logic
     * at the beginning of the tick method, effectively giving a second chance
     * for the sounds to play.
     * 
     * HOW @Inject WORKS:
     * ------------------
     * - method = "tick": Targets the tick() method in BiomeEffectSoundPlayer
     * - at = @At("HEAD"): Injects at the very beginning of the method
     * - The callback info provides context about the method being injected into
     *
     * @param ci The callback info provided by the mixin system
     */
    @Inject(method = "tick", // Target the tick method in BiomeEffectSoundPlayer
            at = @At("HEAD") // Inject at the beginning of the method
    )
    private void addExtraCaveSoundChance(CallbackInfo ci) {
        // This is a cast to access BiomeEffectSoundPlayer's fields
        BiomeEffectSoundPlayer self = (BiomeEffectSoundPlayer) (Object) this;

        try {
            // Get the additionsSound field using reflection since it's private
            java.lang.reflect.Field additionsSoundField = BiomeEffectSoundPlayer.class
                    .getDeclaredField("additionsSound");
            additionsSoundField.setAccessible(true);
            @SuppressWarnings("unchecked") // This cast is safe because we know the field type
            Optional<BiomeAdditionsSound> additionsSound = (Optional<BiomeAdditionsSound>) additionsSoundField
                    .get(self);

            // Get the random field using reflection
            java.lang.reflect.Field randomField = BiomeEffectSoundPlayer.class.getDeclaredField("random");
            randomField.setAccessible(true);
            net.minecraft.util.math.random.Random random = (net.minecraft.util.math.random.Random) randomField
                    .get(self);

            // Get the soundManager field using reflection
            java.lang.reflect.Field soundManagerField = BiomeEffectSoundPlayer.class.getDeclaredField("soundManager");
            soundManagerField.setAccessible(true);
            net.minecraft.client.sound.SoundManager soundManager = (net.minecraft.client.sound.SoundManager) soundManagerField
                    .get(self);

            // Replicate the same logic as in the original method to give a second chance
            // for sounds to play
            additionsSound.ifPresent(sound -> {
                // This is the same check as in the original code
                if (random.nextDouble() < sound.getChance()) {
                    // If chance succeeds, play the sound just like the original code does
                    soundManager.play(PositionedSoundInstance.ambient(sound.getSound().value()));
                }
            });
        } catch (Exception e) {
            // Log any errors that occur during reflection
            horror.blueice129.HorrorMod129.LOGGER.error("Error in cave sound frequency modifier: " + e.getMessage());
        }
    }
}