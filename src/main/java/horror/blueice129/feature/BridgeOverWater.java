package horror.blueice129.feature;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

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
        BlockPos center = player.getBlockPos();

        BlockPos riverWater = findRiverWaterByRings(world, center, 32, 224, 16, 8);
        if (riverWater != null && placeBridge(world, riverWater)) {
            return true;
        }

        BlockPos fallbackWater = findWaterByRings(world, center, 16, 160, 16, 8);
        if (fallbackWater != null && placeBridge(world, fallbackWater)) {
            return true;
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

    private static BlockPos findRiverWaterByRings(ServerWorld world, BlockPos center, int minRadius, int maxRadius,
            int radiusStep, int edgeStep) {
        int[] radii = buildRadii(minRadius, maxRadius, radiusStep);
        shuffleIntArray(radii, world.random);

        for (int radius : radii) {
            BlockPos found = findWaterOnRing(world, center, radius, edgeStep, true);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static BlockPos findWaterByRings(ServerWorld world, BlockPos center, int minRadius, int maxRadius,
            int radiusStep, int edgeStep) {
        int[] radii = buildRadii(minRadius, maxRadius, radiusStep);
        shuffleIntArray(radii, world.random);

        for (int radius : radii) {
            BlockPos found = findWaterOnRing(world, center, radius, edgeStep, false);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static BlockPos findWaterOnRing(ServerWorld world, BlockPos center, int radius, int edgeStep,
            boolean riverOnly) {
        boolean scanNorthSouthFirst = world.random.nextBoolean();
        if (scanNorthSouthFirst) {
            BlockPos found = scanNorthSouthEdges(world, center, radius, edgeStep, riverOnly);
            if (found != null) {
                return found;
            }

            return scanWestEastEdges(world, center, radius, edgeStep, riverOnly);
        }

        BlockPos found = scanWestEastEdges(world, center, radius, edgeStep, riverOnly);
        if (found != null) {
            return found;
        }

        return scanNorthSouthEdges(world, center, radius, edgeStep, riverOnly);
    }

    private static BlockPos scanNorthSouthEdges(ServerWorld world, BlockPos center, int radius, int edgeStep,
            boolean riverOnly) {
        boolean reverse = world.random.nextBoolean();
        for (int i = 0; i <= radius * 2; i += edgeStep) {
            int x = reverse ? radius - i : -radius + i;
            BlockPos northAnchor = center.add(x, 0, -radius);
            BlockPos southAnchor = center.add(x, 0, radius);

            BlockPos found = checkAnchorPair(world, northAnchor, southAnchor, riverOnly);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static BlockPos scanWestEastEdges(ServerWorld world, BlockPos center, int radius, int edgeStep,
            boolean riverOnly) {
        boolean reverse = world.random.nextBoolean();
        for (int i = edgeStep; i <= radius * 2 - edgeStep; i += edgeStep) {
            int z = reverse ? radius - i : -radius + i;
            BlockPos westAnchor = center.add(-radius, 0, z);
            BlockPos eastAnchor = center.add(radius, 0, z);

            BlockPos found = checkAnchorPair(world, westAnchor, eastAnchor, riverOnly);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static BlockPos checkAnchorPair(ServerWorld world, BlockPos firstAnchor, BlockPos secondAnchor,
            boolean riverOnly) {
        boolean firstDirectionFirst = world.random.nextBoolean();
        if (firstDirectionFirst) {
            BlockPos firstFound = findWaterNearAnchor(world, firstAnchor, riverOnly);
            if (firstFound != null) {
                return firstFound;
            }

            return findWaterNearAnchor(world, secondAnchor, riverOnly);
        }

        BlockPos secondFound = findWaterNearAnchor(world, secondAnchor, riverOnly);
        if (secondFound != null) {
            return secondFound;
        }

        return findWaterNearAnchor(world, firstAnchor, riverOnly);
    }

    private static int[] buildRadii(int minRadius, int maxRadius, int radiusStep) {
        int count = ((maxRadius - minRadius) / radiusStep) + 1;
        int[] radii = new int[count];
        for (int i = 0; i < count; i++) {
            radii[i] = minRadius + (i * radiusStep);
        }
        return radii;
    }

    private static void shuffleIntArray(int[] values, net.minecraft.util.math.random.Random random) {
        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }

    private static BlockPos findWaterNearAnchor(ServerWorld world, BlockPos anchor, boolean riverOnly) {
        BlockPos surfacePos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, anchor);
        if (riverOnly && !isRiverBiome(world, surfacePos)) {
            return null;
        }

        return findStartWater(world, surfacePos, 10, 4);
    }

    private static boolean isRiverBiome(ServerWorld world, BlockPos pos) {
        String biomeKey = world.getBiome(pos)
                .getKey()
                .map(key -> key.getValue().toString())
                .orElse("");
        return biomeKey.contains("river");
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
