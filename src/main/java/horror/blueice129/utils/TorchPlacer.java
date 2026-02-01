package horror.blueice129.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class TorchPlacer {

    public static boolean placeTorch(World world, BlockPos pos, Random random){
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
        Direction[] validDirections = new Direction[5];
        int validCount = 0;

        for (Direction dir : directions) {
            BlockPos adjacentPos = pos.offset(dir);
            if (world.getBlockState(adjacentPos).isSolidBlock(world, adjacentPos)) {
                validDirections[validCount++] = dir;
            }
        }

        if (validCount == 0) return false;

        Direction selectedDir = validDirections[random.nextInt(validCount)];
        
        if (selectedDir == Direction.DOWN) {
            world.setBlockState(pos, Blocks.TORCH.getDefaultState());
        } else {
            world.setBlockState(pos, Blocks.WALL_TORCH.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, selectedDir.getOpposite()));
        }
        return true;
    }
}
