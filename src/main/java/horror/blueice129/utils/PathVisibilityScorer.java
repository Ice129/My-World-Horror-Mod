package horror.blueice129.utils;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Utility class for scoring navigation paths based on visibility to the player.
 * Lower scores indicate paths with fewer visible blocks (better for hiding).
 */
public class PathVisibilityScorer {
    
    /**
     * Score a path by counting how many positions along it are visible to the player.
     * Samples positions along a straight line from start to end and checks visibility
     * of the 2 air blocks above each ground position.
     * 
     * @param player The player to check visibility against
     * @param world The world to check blocks in
     * @param start Starting position of the path
     * @param end Ending position of the path
     * @param maxDistance Maximum raycast distance for visibility checks
     * @return Visibility score (lower = better for hiding). Returns -1 if player is null.
     */
    public static int scorePath(PlayerEntity player, World world, BlockPos start, BlockPos end, double maxDistance) {
        if (player == null) {
            return -1;
        }
        
        int visibleBlockCount = 0;
        
        // Calculate path distance
        double pathDistance = Math.sqrt(start.getSquaredDistance(end));
        
        // Sample every block along the path
        int sampleCount = (int) Math.ceil(pathDistance);
        if (sampleCount == 0) {
            sampleCount = 1;
        }
        
        for (int i = 0; i <= sampleCount; i++) {
            // Interpolate position along the path
            double t = sampleCount > 0 ? (double) i / sampleCount : 0.0;
            int x = (int) Math.round(start.getX() + (end.getX() - start.getX()) * t);
            int y = (int) Math.round(start.getY() + (end.getY() - start.getY()) * t);
            int z = (int) Math.round(start.getZ() + (end.getZ() - start.getZ()) * t);
            
            BlockPos checkPos = new BlockPos(x, y, z);
            
            // Find the actual ground level (skip non-collision blocks like grass, torches, snow)
            BlockPos groundPos = findGroundLevel(world, checkPos);
            BlockPos airPos1 = groundPos.up(1);
            BlockPos airPos2 = groundPos.up(2);
            
            // Check if either air block is visible (entity occupies 2 blocks height)
            boolean air1Visible = LineOfSightUtils.isBlockRenderedOnScreen(
                player, airPos1, maxDistance);
            boolean air2Visible = LineOfSightUtils.isBlockRenderedOnScreen(
                player, airPos2, maxDistance);
            
            if (air1Visible || air2Visible) {
                visibleBlockCount++;
            }
        }
        
        return visibleBlockCount;
    }
    
    /**
     * Quick visibility check for a single position.
     * Checks if the 2 air blocks above the ground position are visible.
     * 
     * @param player The player to check visibility against
     * @param world The world to check blocks in
     * @param groundPos The ground position to check
     * @param maxDistance Maximum raycast distance
     * @return true if any of the air blocks are visible, false otherwise
     */
    public static boolean isPositionVisible(PlayerEntity player, World world, BlockPos groundPos, double maxDistance) {
        if (player == null) {
            return false;
        }
        
        // Find actual ground level (skip non-collision blocks)
        BlockPos actualGround = findGroundLevel(world, groundPos);
        BlockPos airPos1 = actualGround.up(1);
        BlockPos airPos2 = actualGround.up(2);
        
        boolean air1Visible = LineOfSightUtils.isBlockRenderedOnScreen(
            player, airPos1, maxDistance);
        boolean air2Visible = LineOfSightUtils.isBlockRenderedOnScreen(
            player, airPos2, maxDistance);
        
        return air1Visible || air2Visible;
    }
    
    /**
     * Find the actual ground level by skipping non-collision blocks.
     * Handles snow, grass, torches, and other decoration blocks.
     * 
     * @param world The world to check blocks in
     * @param startPos The starting position to check from
     * @return The ground block position (solid, walkable surface)
     */
    private static BlockPos findGroundLevel(World world, BlockPos startPos) {
        BlockPos currentPos = startPos;
        
        // Search downward for a solid block (max 5 blocks)
        for (int i = 0; i < 5; i++) {
            BlockState state = world.getBlockState(currentPos);
            
            // Check if this is a solid block with collision
            if (!state.isAir() && state.isSolidBlock(world, currentPos)) {
                return currentPos;
            }
            
            // Move down one block
            currentPos = currentPos.down();
        }
        
        // If no solid block found, return the original position
        return startPos;
    }
}
