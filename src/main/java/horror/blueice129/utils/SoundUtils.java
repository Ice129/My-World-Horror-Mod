package horror.blueice129.utils;

import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SoundUtils {

    public static void playSoundAtBlock(World world, BlockPos pos, SoundEvent sound) {
        if (!world.isClient()) {
            world.playSound(
                    null, // No specific source player
                    pos, // Position of the sound
                    sound, // Sound to play
                    SoundCategory.BLOCKS, // Sound category
                    1.0f, // Volume
                    1.0f // Pitch
            );
        }
    }

    public static void playSoundAtEntity(Entity entity, SoundEvent sound) {
        if (!entity.getWorld().isClient()) {
            entity.playSound(sound, 1.0f, 1.0f);
        }

    }
}
