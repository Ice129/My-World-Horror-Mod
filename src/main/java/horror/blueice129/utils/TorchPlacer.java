package horror.blueice129.utils;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.entity.player.PlayerEntity;

public class TorchPlacer {

    public static boolean placeTorch(World world, BlockPos pos, Random random, PlayerEntity player){
        if (player != null && LineOfSightUtils.hasLineOfSight(player, pos, 200)) {
            return false;
        }
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
        Direction[] validDirections = new Direction[5];
        int validCount = 0;

        for (Direction dir : directions) {
            BlockPos adjacentPos = pos.offset(dir);
            Direction attachmentFace = dir.getOpposite();
            if (world.getBlockState(adjacentPos).isSideSolidFullSquare(world, adjacentPos, attachmentFace)) {
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
