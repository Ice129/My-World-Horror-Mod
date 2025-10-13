package horror.blueice129.utils;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerUtils {
    public static boolean isPlayerCrouching(PlayerEntity player) {
        return player.isSneaking();
    }
    public static float getPlayerLookDirection(PlayerEntity player) {
        return player.getYaw();
    }
}
