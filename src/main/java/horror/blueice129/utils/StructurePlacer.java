package horror.blueice129.utils;

import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;

import static horror.blueice129.utils.SurfaceFinder.findPointSurfaceY;
import static horror.blueice129.utils.LineOfSightUtils.isBlockRenderedOnScreen;

/**
 * Utility class for placing structures in the Minecraft world.
 * This class provides methods to handle the placement of various structures
 * at specified locations.
 */
public class StructurePlacer {
        private static final Random random = Random.create();

        /**
         * Finds a suitable surface location within a specified distance range from a
         * center point,
         * ensuring the location is in a specified biome type and isnt in line of sight
         * of the player.
         * 
         * @param world       The server world to search in.
         * @param center      The center position to search around.
         * @param player      The player to search around.
         * @param minDistance The minimum distance from the center to consider.
         * @param maxDistance The maximum distance from the center to consider.
         * @param treatSnowAsSurface If true, snow blocks are treated as valid surface blocks.
         *                           If false, snow is skipped like other foliage.
         * @return A BlockPos representing a suitable surface location, or null if none
         *         found.
         */
        public static BlockPos findSurfaceLocation(ServerWorld world, BlockPos center, PlayerEntity player,
                        int minDistance,
                        int maxDistance, boolean treatSnowAsSurface) {
                int attempts = 100;
                for (int i = 0; i < attempts; i++) {
                        // Choose a random radius between minDistance and maxDistance (inclusive of min)
                        int radius = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance + 1));
                        // Choose a random angle and convert to x/z offsets so sampling is symmetric
                        // around the center
                        double angle = random.nextDouble() * Math.PI * 2.0;
                        int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
                        int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);

                        // Make sure the chunk at this position is loaded before checking block states
                        BlockPos checkPos = new BlockPos(x,
                                        world.getBottomY() + (world.getTopY() - world.getBottomY()) / 2, z);
                        if (!ChunkLoader.loadChunksInRadius(world, checkPos, 1)) {
                                continue; // Skip if chunk couldn't be loaded
                        }

                        int y = findPointSurfaceY(world, x, z, true, true, treatSnowAsSurface);
                        if (y == -1) {
                                continue; // No suitable surface found
                        }
                        BlockPos pos = new BlockPos(x, y + 1, z);

                        // Check if the position is not in line of sight of the player
                        if (isBlockRenderedOnScreen(player, pos, 50))
                                continue; // Position is in line of sight, try again
                        return pos;
                }
                return null;
        }

        public static BlockPos findSurfaceLocation(ServerWorld world, BlockPos center, PlayerEntity player,
                        int minDistance, int maxDistance) {
                return findSurfaceLocation(world, center, player, minDistance, maxDistance, true);
        }

        /**
         * Finds a suitable surface location within a specified distance range from a
         * center point without checking line of sight with a player.
         * 
         * @param world       The server world to search in.
         * @param center      The center position to search around.
         * @param minDistance The minimum distance from the center to consider.
         * @param maxDistance The maximum distance from the center to consider.
         * @param treatSnowAsSurface If true, snow blocks are treated as valid surface blocks.
         *                           If false, snow is skipped like other foliage.
         * @return A BlockPos representing a suitable surface location, or null if none
         *         found.
         */
        public static BlockPos findSurfaceLocation(ServerWorld world, BlockPos center,
                        int minDistance, int maxDistance, boolean treatSnowAsSurface) {
                int attempts = 100;
                for (int i = 0; i < attempts; i++) {
                        // Choose a random radius between minDistance and maxDistance (inclusive of min)
                        int radius = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance + 1));
                        // Choose a random angle and convert to x/z offsets so sampling is symmetric
                        // around the center
                        double angle = random.nextDouble() * Math.PI * 2.0;
                        int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
                        int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);

                        // Make sure the chunk at this position is loaded before checking block states
                        BlockPos checkPos = new BlockPos(x,
                                        world.getBottomY() + (world.getTopY() - world.getBottomY()) / 2, z);
                        if (!ChunkLoader.loadChunksInRadius(world, checkPos, 1)) {
                                continue; // Skip if chunk couldn't be loaded
                        }

                        int y = findPointSurfaceY(world, x, z, true, true, treatSnowAsSurface);
                        if (y == -1)
                                continue; // No suitable surface found

                        return new BlockPos(x, y + 1, z);
                }
                return null;
        }

        public static BlockPos findSurfaceLocation(ServerWorld world, BlockPos center,
                        int minDistance, int maxDistance) {
                return findSurfaceLocation(world, center, minDistance, maxDistance, true);
        }

}
