package horror.blueice129.utils;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkLoader {
    // TEST: this function
    

    /**
     * Loads all chunks in a given radius around a block position and ensures they are ready for modification.
     * <p>
     * If the radius is less than or equal to zero, no chunks will be loaded and the method returns false.
     * If the radius is greater than 5, an IllegalArgumentException is thrown.
     *
     * @param world  the server world where the chunks are located
     * @param center the center block position
     * @param radius the radius (in chunks) to load around the center; must be between 1 and 5 (inclusive)
     * @return {@code true} if all chunks are loaded and ready, {@code false} otherwise
     * @throws IllegalArgumentException if the radius is greater than 5
     */
    public static boolean loadChunksInRadius(ServerWorld world, BlockPos center, int radius) {
        ChunkManager chunkManager = world.getChunkManager();
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        // Return false if radius is less than or equal to zero
        if (radius <= 0) {
            return false;
        }

        // Limit the radius to avoid server lag
        if (radius > 5) {
            throw new IllegalArgumentException("Radius too large! Maximum allowed is 5.");
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;

                // Retrieve the chunk from the chunk manager
                Chunk chunk = chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

                // Ensure the chunk is a fully loaded, modifiable WorldChunk with FULL status.
                if (!(chunk instanceof WorldChunk)) {
                    // If the chunk is not a WorldChunk, return false
                    return false;
                }
                
                if (chunk.getStatus() != ChunkStatus.FULL) {
                    // If the chunk is not fully loaded, return false
                    return false;
                }
            }
        }

        return true; // All chunks are loaded and ready
    }
}
