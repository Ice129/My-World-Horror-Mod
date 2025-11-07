package horror.blueice129.debug;

import horror.blueice129.utils.LineOfSightUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LineOfSightChecker {

    /**
     * Fills all non-air blocks that are within the player's field of view with blue stained glass blocks.
     * Optimized to only check blocks within the FOV cone.
     * 
     * @param player The player to check from
     * @param maxDistance The maximum distance to check
     */
    public static void fillFieldOfViewWithGlass(PlayerEntity player, double maxDistance) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        BlockPos playerPos = player.getBlockPos();
        int range = (int) Math.ceil(maxDistance);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos targetPos = playerPos.add(x, y, z);
                    
                    // Skip air blocks
                    if (world.isAir(targetPos)) {
                        continue;
                    }
                    
                    // Quick distance check
                    double distSquared = eyePos.squaredDistanceTo(
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5
                    );
                    if (distSquared > maxDistance * maxDistance) {
                        continue;
                    }
                    
                    // Calculate direction to the block
                    Vec3d direction = new Vec3d(
                            targetPos.getX() + 0.5 - eyePos.getX(),
                            targetPos.getY() + 0.5 - eyePos.getY(),
                            targetPos.getZ() + 0.5 - eyePos.getZ()
                    ).normalize();
                    
                    // Check if the block is within the player's field of view
                    if (LineOfSightUtils.isWithinFieldOfView(player, direction)) {
                        world.setBlockState(targetPos, net.minecraft.block.Blocks.BLUE_STAINED_GLASS.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * Fills all non-air blocks that are rendered on the player's screen with yellow stained glass blocks.
     * Uses isBlockRenderedOnScreen to determine if the block is rendered on the screen.
     * Optimized with early FOV filtering before expensive raycast checks.
     * 
     * @param player The player to check from
     * @param maxDistance The maximum distance to check
     */
    public static void fillRenderedBlocksWithGlass(PlayerEntity player, double maxDistance) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        int range = (int) Math.ceil(maxDistance);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos targetPos = playerPos.add(x, y, z);
                    
                    // Skip air blocks
                    if (world.isAir(targetPos)) {
                        continue;
                    }
                    
                    
                    // Use the isBlockRenderedOnScreen method to check if the block is rendered
                    if (LineOfSightUtils.isBlockRenderedOnScreen(player, targetPos, maxDistance)) {
                        world.setBlockState(targetPos, net.minecraft.block.Blocks.BLACK_CONCRETE.getDefaultState());
                    }
                }
            }
        }
    }
}
