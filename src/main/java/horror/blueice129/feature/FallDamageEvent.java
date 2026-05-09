package horror.blueice129.feature;

import horror.blueice129.utils.PlayerUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class FallDamageEvent {

    public static boolean triggerEvent(PlayerEntity player) {
        // returns true if player is inside a building, and is within a ui
        boolean isInsideHouse = PlayerUtils.isPlayerInsideHouseOrStructure(player);
        boolean isInUI = PlayerUtils.isPlayerInUI(player);
        BlockPos suitableFallSpot = findSuitableFallSpot(player);
        if( isInsideHouse && isInUI && suitableFallSpot != null){
            // play fall sound
            playFallSound(suitableFallSpot);
            return true;
            
        }
        return false;
    }

    private static BlockPos findSuitableFallSpot(PlayerEntity player) {
        // block pos should be after a fall of at least 5 blocks, and within hearing distance of the player
        // it should also be outside the house or structure the player is in
        return null;

    }

    private static BlockPos[] findWallBounds(BlockPos startPos){
        return null;
    }

    private static void playFallSound(BlockPos pos){ {
        // play a sound effect of falling at the given position
        
    }

}
