package horror.blueice129.feature;

import horror.blueice129.utils.PlayerUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class FallDamageEvent {

    public static boolean triggerEvent(PlayerEntity player) {
        // returns true if player is inside a building, and is within a ui
        if (player.getY() < 64) {
            return false; // don't trigger if player is below y=64
        }
        boolean isInsideHouse = PlayerUtils.isPlayerInsideHouseOrStructure(player);
        boolean isInUI = PlayerUtils.isPlayerInUI(player);
        BlockPos suitableFallSpot = findSuitableFallSpot(player);
        if (isInsideHouse && isInUI && suitableFallSpot != null) {
            // play fall sound
            playFallSound(player, suitableFallSpot);
            // place water block and make sure its updated so it flows
            player.getWorld().setBlockState(suitableFallSpot, net.minecraft.block.Blocks.WATER.getDefaultState());
            return true;

        }
        return false;
    }

    private static BlockPos findSuitableFallSpot(PlayerEntity player) {
        for (int i = 0; i < 3; i++) {
            String[] cardinalDirections = { "N", "E", "S", "W" };
            // pick a random cardinal direction
            String randomDirection = cardinalDirections[(int) (Math.random() * cardinalDirections.length)];
            // get the player's current position
            BlockPos playerPos = player.getBlockPos();
            // loop in dir until wall, then go 2 more blocks in that direction
            BlockPos checkPos = playerPos;
            while (player.getWorld().isAir(checkPos)) {
                checkPos = PlayerUtils.getRelativeBlockPos(checkPos, randomDirection);
            }
            checkPos = PlayerUtils.getRelativeBlockPos(checkPos, randomDirection);
            checkPos = PlayerUtils.getRelativeBlockPos(checkPos, randomDirection);
            // get ground position below checkPos if it's air, if not, just loop again
            if (player.getWorld().isAir(checkPos)) {
                while (player.getWorld().isAir(checkPos)) {
                    checkPos = checkPos.down();
                }
                return checkPos.up(); // return the block above the ground
            } else {
                continue; // try another direction
            }

        }
        return null; // no suitable spot found after 3 tries
    }

private static void playFallSound(PlayerEntity player, BlockPos pos) {
        var world = player.getWorld();
        var blockState = world.getBlockState(pos);
        var fallSound = blockState.getSoundGroup().getFallSound();
        var damageSound = SoundEvents.ENTITY_PLAYER_HURT;
        
        float volume = 2.5f;
        float pitch = 0.8f + world.getRandom().nextFloat() * 0.3f;
        
        world.playSound(
            null,
            pos,
            fallSound,
            SoundCategory.BLOCKS,
            volume,
            pitch
        );
        // also play player damage sound
        world.playSound(
            null,
            pos,
            damageSound,
            SoundCategory.PLAYERS,
            volume-1.0f,
            pitch
        );
        // also play water bucket place sound
        world.playSound(
            null,
            pos,
            SoundEvents.ITEM_BUCKET_EMPTY,
            SoundCategory.BLOCKS,
            volume-1.5f,
            pitch
        );
    }

}
