package horror.blueice129.feature;

import horror.blueice129.utils.StructurePlacer;
// import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;

public class EntityHouse {

    // functions planned for this event
    //
    // find house start location
    // - takes environment into account, like a flat ish forrest area, or plains
    //
    // calculate house stage
    // - based off aggro
    //
    // place house blocks
    // - takes player location and chunk loading and player view into account
    //
    // update loot
    // - loot in chests and containers is updated
    //
    // prepare environment
    // - clear trees and grass and level the area more
    //
    // calculate wood
    // - gets the nearest average tree type, and uses that type of wood for the
    // house
    //
    // calculate player interaction
    // - if the player has taken items, broken blocks, or otherwise interacted with
    // the house, log it, as it will be used later
    //
    // make path
    // - makes path from entity house to player house, in steps. kinda wandery but
    // alays in the player bases direction
    //
    // is player in base
    // - checks if the player is in the base, will be used for events

    Random RANDOM = Random.create();

    private static BlockPos houseStartPos;
    public static BlockPos[] possibleHouseStartLocations;
    private static int houseStage;

    public static void findHouseStartLocation() {
        return;

    }

    public static void findSuitableHouseStartLocation(PlayerEntity player, ServerWorld world) {

        boolean foundLocation = false;
        int attempts = 0;
        while (!foundLocation && attempts < 100) {
            BlockPos candidatePos = StructurePlacer.findSurfaceLocation(world, player.getBlockPos(), player, 20, 100,
                    true);
            if (candidatePos != null) {
                int flatnessRating = evaluateFlatness(world, candidatePos);
            }
        }
    }

    private static int evaluateFlatness(ServerWorld world, BlockPos pos) {
        int maxSquaredDistance = 15 * 15; // 15 block radius
        int flatnessScore = 0; // the closer to 0, the flatter it is. 
        // boolean isCurrentAir = true; // used with helping with raises

        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        Direction[] AIR_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};

        // using the same sort of flood fill method from cave miner
        queue.add(pos);
        visited.add(pos);
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            if (currentPos.getSquaredDistance(pos) > maxSquaredDistance) {
                continue; // skip positions outside the radius
            }

            boolean isAir = world.isAir(currentPos);
            if (isAir) {
                for (Direction dir : AIR_DIRECTIONS) {

                    BlockPos neighborPos = currentPos.offset(dir);
                    
                    if (!visited.contains(neighborPos)) {
                        visited.add(neighborPos);
                        queue.add(neighborPos);
                        if (world.isAir(neighborPos) && dir == Direction.DOWN) {
                            flatnessScore += 1; // air below means a hole, which is bad for flatness
                        }
                    }
                }
            } else {
                BlockPos neighborPos = currentPos.offset(Direction.UP);
                if (!visited.contains(neighborPos)) {
                    visited.add(neighborPos);
                    queue.add(neighborPos);
                    flatnessScore += 1; // block above means a raise, which is bad for flatness
                }
            }
        }

        for (BlockPos visitedPos : visited) {
            world.setBlockState(visitedPos, Blocks.RED_WOOL.getDefaultState());
        }

        return flatnessScore;
    }

    public static int debugForEvaluateFlatness(ServerWorld world, BlockPos pos) {
        return evaluateFlatness(world, pos);
    }
}
