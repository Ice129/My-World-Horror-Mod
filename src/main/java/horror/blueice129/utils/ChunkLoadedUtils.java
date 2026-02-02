package horror.blueice129.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

public class ChunkLoadedUtils {

    /**
     * Checks if the chunk at the given position is loaded in the specified world.
     */
    public static boolean isChunkLoadedAt(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false; // Only server worlds have loaded chunk information
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        return serverWorld.isChunkLoaded(chunkPos.x, chunkPos.z);
    }
}
