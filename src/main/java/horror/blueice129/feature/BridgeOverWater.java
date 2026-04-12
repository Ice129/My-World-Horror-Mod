package horror.blueice129.feature;

import horror.blueice129.utils.StructurePlacer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;

public class BridgeOverWater {

    public static boolean triggerEvent(MinecraftServer server, ServerPlayerEntity player){
        BlockPos pos = player.getBlockPos();
        BlockPos waterLocation;
        ServerWorld world = server.getOverworld();

        for (int i = 0; i < 10; i++) {
            waterLocation = StructurePlacer.findSurfaceLocation(world, pos, player, 30, 100, true);
            if (world.getBiome(waterLocation).isIn(BiomeTags.IS_RIVER)) {
                BlockPos nearbyWater = findNearbyWaterBlock(world, waterLocation, 8, 4);
                if (nearbyWater != null) {
                    break;
                }
            }

        }
        if (waterLocation == null) {
            return false; // No suitable water location found
        }

        boolean success = placeBridge(world, waterLocation);
        if (!success) {
            return false; // Failed to place the bridge
        }
        return true;
    }

    private static BlockPos findNearbyWaterBlock(ServerWorld world, BlockPos center, int horizontalRadius, int verticalRadius) {
        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    BlockPos checkPos = center.add(x, y, z);
                    if (world.getBlockState(checkPos).isOf(Blocks.WATER)) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
}
