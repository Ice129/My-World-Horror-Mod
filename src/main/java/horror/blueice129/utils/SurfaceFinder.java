package horror.blueice129.utils;

// import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
// import net.minecraft.block.FlowerBlock;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.ArrayList;
// import horror.blueice129.utils.BlockTypes;
// import horror.blueice129.utils.ChunkLoader;

/**
 * Utility class for finding the surface level in a Minecraft world.
 * This class provides methods to locate the highest non-air block at a given
 * (x, z) coordinate.
 */

public class SurfaceFinder {

    /**
     * Finds the surface Y coordinate at the given (x, z) position in the specified
     * world.
     * 
     * @param world         The server world to search in.
     * @param x             The X coordinate.
     * @param z             The Z coordinate.
     * @param ignoreFoliage If true, foliage blocks (like grass, leaves) will be
     *                      ignored when determining the surface.
     * @param avoidWater    If true, water blocks will be avoided when determining
     *                      the surface.
     * @return The Y coordinate of the surface, or -1 if no suitable surface is
     *         found.
     */
    public static int findPointSurfaceY(ServerWorld world, int x, int z, boolean ignoreFoliage, boolean avoidWater,
            boolean includeSnow) {
        // Start from the top of the world and move downwards
        int y = world.getTopY();
        BlockPos.Mutable pos = new BlockPos.Mutable(x, y, z);

        // Make sure the chunk is loaded before accessing blocks
        if (!ChunkLoader.loadChunksInRadius(world,
                new BlockPos(x, world.getBottomY() + (world.getTopY() - world.getBottomY()) / 2, z), 1)) {
            return -1; // Return -1 if chunk couldn't be loaded
        }

        while (y > world.getBottomY()) {
            pos.setY(y);
            BlockState block = world.getBlockState(pos);

            // Check if the block is not air
            if (!block.isAir()) {
                // If ignoring foliage, check if the block is foliage
                if (ignoreFoliage && BlockTypes.isFoliage(block.getBlock(), includeSnow)) {
                    y--;
                    continue;
                }
                // If avoiding water, check if the block is water
                if (avoidWater && BlockTypes.isWater(block.getBlock())) {
                    return -1;
                }
                // Found a suitable surface block
                return y;
            }

            y--;
        }

        // Return -1 if no suitable surface was found
        return -1;
    }

    public static BlockPos[] findTreePositions(ServerWorld world, BlockPos pos, int radius) {
        List<BlockPos> treePositions = new ArrayList<>();
        int startX = pos.getX() - radius;
        int startZ = pos.getZ() - radius;
        int endX = pos.getX() + radius;
        int endZ = pos.getZ() + radius;

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                // Make sure chunks are loaded before accessing blocks
                BlockPos chunkPos = new BlockPos(x, world.getBottomY() + (world.getTopY() - world.getBottomY()) / 2, z);
                if (!ChunkLoader.loadChunksInRadius(world, chunkPos, 1)) {
                    continue; // Skip if chunk couldn't be loaded
                }

                // Include snow when finding the surface to work properly in snowy biomes
                int topBlockY = findPointSurfaceY(world, x, z, false, true, true);
                if (topBlockY == -1) {
                    continue; // No suitable surface found
                }

                BlockPos topBlock = new BlockPos(x, topBlockY, z);
                BlockState topBlockState = world.getBlockState(topBlock);
                
                // Handle snow on top of leaves or directly check below snow for leaves
                if (topBlockState.getBlock() == Blocks.SNOW) {
                    // Check the block below snow
                    BlockState belowSnow = world.getBlockState(topBlock.down(1));
                    if (!(belowSnow.getBlock() instanceof LeavesBlock)) {
                        continue; // If the block below snow isn't leaves, skip
                    }
                    topBlock = topBlock.down(1); // Use the block below snow as top block
                    topBlockY--; // Adjust Y coordinate
                }
                
                // Check if the top block is leaves or we've already confirmed leaves below snow
                if (world.getBlockState(topBlock).getBlock() instanceof LeavesBlock) {
                    int trunkHeight = 0;
                    for (int y = 0; y <= 30; y++) {
                        BlockPos checkPos = new BlockPos(x, topBlockY - y, z);
                        BlockState blockState = world.getBlockState(checkPos);
                        if (blockState.getBlock() instanceof LeavesBlock) {
                            trunkHeight = 0; // reset trunk height if we hit leaves
                        } else if (BlockTypes.isLogBlock(blockState.getBlock())) {
                            trunkHeight++;
                            continue;
                        } else if (blockState.isAir()) {
                            trunkHeight = 0; // reset trunk height if we hit air
                            continue;
                        } else if (blockState.getBlock() == Blocks.SNOW) {
                            // Don't reset trunk height for snow, just continue
                            continue; // allow snow on top of a tree
                        } else {
                            break; // hit a non-log, non-leaf, non-air, non-snow block
                        }
                    }
                    if (trunkHeight >= 3) {
                        treePositions.add(new BlockPos(x, topBlockY - trunkHeight + 1, z));
                    }
                }
            }
        }

        return treePositions.toArray(new BlockPos[0]);
    }

    public static BlockPos[] getTreeLogPositions(ServerWorld world, BlockPos pos) {
        List<BlockPos> logPositions = new ArrayList<>();
        int x = pos.getX();
        int z = pos.getZ();

        // Make sure chunks are loaded before accessing blocks
        BlockPos chunkPos = new BlockPos(x, world.getBottomY() + (world.getTopY() - world.getBottomY()) / 2, z);
        if (!ChunkLoader.loadChunksInRadius(world, chunkPos, 1)) {
            return new BlockPos[0]; // Return empty array if chunk couldn't be loaded
        }

        // Include snow when finding the surface to work properly in snowy biomes
        int topBlockY = findPointSurfaceY(world, x, z, false, true, true);
        if (topBlockY == -1) {
            return new BlockPos[0]; // No suitable surface found
        }
        
        // First check if top block is snow, and if so, look below it
        if (world.getBlockState(new BlockPos(x, topBlockY, z)).getBlock() == Blocks.SNOW) {
            topBlockY--; // Move below the snow layer
        }
        
        // Search downward for logs
        for (int y = 0; y <= 30; y++) {
            BlockPos checkPos = new BlockPos(x, topBlockY - y, z);
            BlockState blockState = world.getBlockState(checkPos);
            if (BlockTypes.isLogBlock(blockState.getBlock())) {
                logPositions.add(checkPos);
                
                // Also check adjacent blocks for a more complete tree structure
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue; // Skip the center block we already added
                        BlockPos adjacentPos = new BlockPos(x + dx, topBlockY - y, z + dz);
                        BlockState adjacentState = world.getBlockState(adjacentPos);
                        if (BlockTypes.isLogBlock(adjacentState.getBlock())) {
                            logPositions.add(adjacentPos);
                        }
                    }
                }
            } else if (blockState.getBlock() == Blocks.SNOW) {
                continue; // Skip snow layers and keep looking
            } else if (!(blockState.getBlock() instanceof LeavesBlock) && !blockState.isAir()) {
                break; // Stop if we hit a non-log, non-leaf, non-air, non-snow block
            }
        }
        return logPositions.toArray(new BlockPos[0]);
    }
}