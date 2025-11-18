package horror.blueice129.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class for visualizing entity paths with glowstone blocks.
 * Useful for debugging pathfinding behavior in development.
 */
public class PathVisualizer {
    
    /**
     * DEBUG FLAG: Set to true to enable glowstone trail visualization.
     * Should be false in production builds.
     */
    private static final boolean ENABLE_DEBUG_VISUALIZATION = true;
    
    /**
     * Duration (in ticks) before glowstone blocks are restored to original state.
     * 100 ticks = 5 seconds at 20 TPS
     */
    private static final int CLEANUP_DELAY_TICKS = 100;
    
    /**
     * Tracks placed glowstone blocks with their original state and placement time
     */
    private static final Map<BlockPos, PlacedBlock> placedBlocks = new HashMap<>();
    
    /**
     * Data class to track a placed debug block
     */
    private static class PlacedBlock {
        final BlockState originalState;
        final long placementTime;
        
        PlacedBlock(BlockState originalState, long placementTime) {
            this.originalState = originalState;
            this.placementTime = placementTime;
        }
    }
    
    /**
     * Visualize a path by placing glowstone blocks at each position.
     * Only works if ENABLE_DEBUG_VISUALIZATION is true.
     * 
     * @param world The server world
     * @param start Starting position of the path
     * @param end Ending position of the path
     * @param currentTick Current game tick for cleanup scheduling
     */
    public static void visualizePath(ServerWorld world, BlockPos start, BlockPos end, long currentTick) {
        if (!ENABLE_DEBUG_VISUALIZATION) {
            return;
        }
        
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
            
            BlockPos groundPos = new BlockPos(x, y, z);
            
            // Don't replace if already a glowstone marker
            if (placedBlocks.containsKey(groundPos)) {
                continue;
            }
            
            // Store original block state
            BlockState originalState = world.getBlockState(groundPos);
            
            // Only place glowstone on solid blocks (not air or liquids)
            if (!originalState.isAir() && originalState.isSolidBlock(world, groundPos)) {
                placedBlocks.put(groundPos, new PlacedBlock(originalState, currentTick));
                world.setBlockState(groundPos, Blocks.GLOWSTONE.getDefaultState(), 3);
            }
        }
    }
    
    /**
     * Clean up expired glowstone blocks by restoring their original state.
     * Should be called every tick to maintain cleanup.
     * 
     * @param world The server world
     * @param currentTick Current game tick
     */
    public static void cleanupExpiredBlocks(ServerWorld world, long currentTick) {
        if (!ENABLE_DEBUG_VISUALIZATION) {
            return;
        }
        
        Iterator<Map.Entry<BlockPos, PlacedBlock>> iterator = placedBlocks.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, PlacedBlock> entry = iterator.next();
            BlockPos pos = entry.getKey();
            PlacedBlock placedBlock = entry.getValue();
            
            // Check if block has expired
            if (currentTick - placedBlock.placementTime >= CLEANUP_DELAY_TICKS) {
                // Restore original block state
                world.setBlockState(pos, placedBlock.originalState, 3);
                iterator.remove();
            }
        }
    }
    
    /**
     * Immediately clear all placed glowstone blocks.
     * Useful for cleaning up when entity state changes or is removed.
     * 
     * @param world The server world
     */
    public static void clearAll(ServerWorld world) {
        if (!ENABLE_DEBUG_VISUALIZATION) {
            return;
        }
        
        for (Map.Entry<BlockPos, PlacedBlock> entry : placedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            PlacedBlock placedBlock = entry.getValue();
            world.setBlockState(pos, placedBlock.originalState, 3);
        }
        
        placedBlocks.clear();
    }
    
    /**
     * Check if debug visualization is currently enabled.
     * 
     * @return true if visualization is enabled
     */
    public static boolean isVisualizationEnabled() {
        return ENABLE_DEBUG_VISUALIZATION;
    }
}
