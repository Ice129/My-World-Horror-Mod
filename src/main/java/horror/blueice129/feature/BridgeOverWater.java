package horror.blueice129.feature;

import horror.blueice129.utils.StructurePlacer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class BridgeOverWater {

    public static boolean debugPlaceBridgeFromPosition(ServerWorld world, BlockPos startPos) {
        BlockPos waterLocation = findStartWater(world, startPos, 12, 4);
        if (waterLocation == null) {
            return false;
        }

        return placeBridge(world, waterLocation);
    }

    public static boolean triggerEvent(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = server.getOverworld();
        for (int i = 0; i < 300; i++) {
            BlockPos surfacePos = StructurePlacer.findSurfaceLocation(world, player.getBlockPos(), 30, 100);
            if (surfacePos == null) {
                continue;
            }

            BlockPos waterLocation = findStartWater(world, surfacePos, 8, 4);
            if (waterLocation == null) {
                continue;
            }

            if (placeBridge(world, waterLocation)) {
                return true;
            }
        }

        return false;
    }

    private static boolean placeBridge(ServerWorld world, BlockPos waterLocation) {
        int bridgeMinLength = 4;
        int bridgeMaxLength = 50;
        Block[] bridgeBlocks = { Blocks.OAK_PLANKS, Blocks.BIRCH_PLANKS, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLESTONE,
                Blocks.DIRT, Blocks.DIORITE, Blocks.GRANITE, Blocks.ANDESITE };

        BlockPos topWater = getTopWaterInColumn(world, waterLocation);
        if (topWater == null) {
            return false;
        }

        BlockPos[] bridgeEnds = findBridgeEnds(world, topWater, bridgeMinLength, bridgeMaxLength);
        if (bridgeEnds == null) {
            return false; // No suitable bridge ends found
        }

        BlockPos end1 = bridgeEnds[0];
        BlockPos end2 = bridgeEnds[1];

        int dx = end2.getX() - end1.getX();
        int dy = end2.getY() - end1.getY();
        int dz = end2.getZ() - end1.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps <= 1) {
            return false;
        }

        Block blockToPlace = bridgeBlocks[world.random.nextInt(bridgeBlocks.length)];
        for (int i = 0; i <= steps; i++) {
            int x = end1.getX() + Math.round((dx * i) / (float) steps);
            int y = end1.getY() + Math.round((dy * i) / (float) steps);
            int z = end1.getZ() + Math.round((dz * i) / (float) steps);
            world.setBlockState(new BlockPos(x, y, z), blockToPlace.getDefaultState(), 3);
        }

        return true;
    }

    private static BlockPos[] findBridgeEnds(ServerWorld world, BlockPos waterLocation, int minLength, int maxLength) {
        BlockPos end1 = getEnd(waterLocation, world, Direction.NORTH);
        BlockPos end2 = getEnd(waterLocation, world, Direction.SOUTH);
        BlockPos end3 = getEnd(waterLocation, world, Direction.EAST);
        BlockPos end4 = getEnd(waterLocation, world, Direction.WEST);

        int length12 = end1.getManhattanDistance(end2);
        int length34 = end3.getManhattanDistance(end4);
        if (length12 >= minLength && length12 <= maxLength && length12 > length34) {
            return new BlockPos[] { end1, end2 };
        } else if (length34 >= minLength && length34 <= maxLength) {
            return new BlockPos[] { end3, end4 };
        }
        return null; // No suitable bridge ends found

    }

    private static BlockPos getEnd(BlockPos start, ServerWorld world, Direction direction) {
        BlockPos currentPos = getTopWaterInColumn(world, start);
        if (currentPos == null) {
            return start;
        }

        while (true) {
            BlockPos nextPos = getTopWaterInColumn(world, currentPos.offset(direction));
            if (nextPos == null) {
                return currentPos;
            }
            currentPos = nextPos;
        }
    }

    private static BlockPos getTopWaterInColumn(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).isOf(Blocks.WATER)) {
            return null;
        }

        BlockPos currentPos = pos;
        while (world.getBlockState(currentPos.up()).isOf(Blocks.WATER)) {
            currentPos = currentPos.up();
        }
        return currentPos;
    }

    private static BlockPos findStartWater(ServerWorld world, BlockPos center, int horizontalRadius,
            int verticalRadius) {
        BlockPos topCenter = getTopWaterInColumn(world, center);
        if (isShoreWater(world, topCenter)) {
            return topCenter;
        }

        BlockPos topBelow = getTopWaterInColumn(world, center.down());
        if (isShoreWater(world, topBelow)) {
            return topBelow;
        }

        BlockPos best = null;
        int bestDistSq = Integer.MAX_VALUE;
        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    BlockPos checkPos = center.add(x, y, z);
                    BlockPos topWater = getTopWaterInColumn(world, checkPos);
                    if (!isShoreWater(world, topWater)) {
                        continue;
                    }

                    int dx = topWater.getX() - center.getX();
                    int dy = topWater.getY() - center.getY();
                    int dz = topWater.getZ() - center.getZ();
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        best = topWater;
                    }
                }
            }
        }
        return best;
    }

    private static boolean isShoreWater(ServerWorld world, BlockPos waterPos) {
        if (waterPos == null) {
            return false;
        }

        if (!world.getBlockState(waterPos.up()).isAir()) {
            return false;
        }

        return hasSolidNeighbor(world, waterPos.north())
                || hasSolidNeighbor(world, waterPos.south())
                || hasSolidNeighbor(world, waterPos.east())
                || hasSolidNeighbor(world, waterPos.west());
    }

    private static boolean hasSolidNeighbor(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.isAir() && state.isSolidBlock(world, pos);
    }
}
