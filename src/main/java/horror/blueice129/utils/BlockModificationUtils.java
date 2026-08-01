package horror.blueice129.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
// import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
// import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
// import net.minecraft.entity.player.PlayerEntity;

public class BlockModificationUtils {

    /**
     * places air block at the given position, but protects against
     * other unwanted blocks like gravel or liquids
     */
    public static boolean placeAirBlock(World world, BlockPos pos, Block replacementBlock) {
        Direction[] directions = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP,
                Direction.DOWN };
        for (Direction dir : directions) {
            BlockPos adjacentPos = pos.offset(dir);
            dealWithLiquids(world, adjacentPos, replacementBlock);
            dealWithGravel(world, adjacentPos, replacementBlock);
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        return true; // If all adjacent blocks are dealt with, return true
    }

    private static void dealWithLiquids(World world, BlockPos pos, Block replacementBlock) {
        FluidState fluidState = world.getFluidState(pos);

        if (!fluidState.isEmpty()) {
            world.setBlockState(pos, replacementBlock.getDefaultState());
        }
    }

    private static void dealWithGravel(World world, BlockPos pos, Block replacementBlock) {
        if (world.getBlockState(pos).getBlock() instanceof FallingBlock) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            dealWithLiquids(world, pos, replacementBlock);
            dealWithGravel(world, pos.up(), replacementBlock);
        }
        return;

    }

}
