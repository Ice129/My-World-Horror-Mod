package horror.blueice129.feature.house;

import horror.blueice129.utils.BlockTypes;
import horror.blueice129.utils.SurfaceFinder;
import horror.blueice129.utils.TorchPlacer;
// import horror.blueice129.utils.BlockTypes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class AreaClearer {

    private static final int CLEAR_RADIUS = 20;
    private static final int TORCH_RADIUS = 22;
    private static final Random random = Random.create();

    public static void clearArea(ServerWorld world, BlockPos center) {
        int y = SurfaceFinder.findPointSurfaceY(world, center.getX(), center.getZ(), true, true, true);

        BlockPos[] trees = SurfaceFinder.findTreePositions(world, center, CLEAR_RADIUS);

        for (BlockPos treeBase : trees) {
            for (BlockPos logPos : SurfaceFinder.getTreeLogPositions(world, treeBase)) {
                world.setBlockState(logPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos pos = new BlockPos(center.getX() + x, y + dy, center.getZ() + z);
                    Block block = world.getBlockState(pos).getBlock();
                    if (BlockTypes.isFoliage(block, false)) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
        }
    }

    public static void placeTorches(ServerWorld world, BlockPos center) {
        for (int x = -TORCH_RADIUS; x <= TORCH_RADIUS; x++) {
            for (int z = -TORCH_RADIUS; z <= TORCH_RADIUS; z++) {
                if (random.nextFloat() < 0.01f) {
                    int posy = SurfaceFinder.findPointSurfaceY(world, center.getX() + x, center.getZ() + z, true,
                            true, true);

                    if (posy != -1) {
                        BlockPos pos = new BlockPos(center.getX() + x, posy + 1, center.getZ() + z);
                        TorchPlacer.placeTorch(world, pos, random, null);
                    }
                }
            }
        }
    }
}
