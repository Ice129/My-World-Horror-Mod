package horror.blueice129.feature;

import horror.blueice129.utils.StructurePlacer;
import horror.blueice129.utils.SurfaceFinder;
import horror.blueice129.utils.BlockTypes;

// import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

public class EntityHouse {

    public static class FlatnessResult {
        public final BlockPos pos;
        public final int flatness;

        public FlatnessResult(BlockPos pos, int flatness) {
            this.pos = pos;
            this.flatness = flatness;
        }
    }

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
        int checkDistance = 15; // how far out to check in each direction
        int flatnessScore = 0; // the closer to 0, the flatter it is.

        // using findSerfaceAt function to get the surface height at each point
        for (int x = -checkDistance; x <= checkDistance; x++) {
            for (int z = -checkDistance; z <= checkDistance; z++) {
                int worldX = pos.getX() + x;
                int worldZ = pos.getZ() + z;
                int surfaceY = SurfaceFinder.findPointSurfaceY(world, worldX, worldZ, true, true, false);
                if (surfaceY == -1) {
                    flatnessScore += 3;
                } else {
                    BlockPos checkPos = new BlockPos(worldX, surfaceY, worldZ);

                    // while loop to get true surface, in case of trees, foliage, or air
                    while (BlockTypes.isLogBlock(world.getBlockState(checkPos.down()).getBlock())
                            || BlockTypes.isFoliage(world.getBlockState(checkPos.down()).getBlock(), false)
                            || world.getBlockState(checkPos.down()).isAir()) {
                        checkPos = checkPos.down();
                    }
                    if (checkPos.getY() == pos.getY()) {
                        flatnessScore += 0;
                    } else {
                        int change = Math.abs(checkPos.getY() - pos.getY());
                        flatnessScore += change;
                    }
                }
            }
        }
        return flatnessScore;
    }

    public static FlatnessResult getBestLocalFlatness(ServerWorld world, BlockPos firstPos) {
        int gridStride = 15;
        int gridPointsPerAxis = 15;
        int checkDistance = (gridPointsPerAxis - 1) * gridStride / 2;
        int gridSize = gridPointsPerAxis * gridPointsPerAxis;
        BlockPos[] toCheckGrid = new BlockPos[gridSize];
        int[] flatnessScores = new int[gridSize];
        
        int index = 0;
        for (int x = -checkDistance; x <= checkDistance; x += gridStride) {
            for (int z = -checkDistance; z <= checkDistance; z += gridStride) {
                int worldX = firstPos.getX() + x;
                int worldZ = firstPos.getZ() + z;
                int surfaceY = SurfaceFinder.findPointSurfaceY(world, worldX, worldZ, true, true, false);
                if (surfaceY == -1) {
                    surfaceY = firstPos.getY();
                }
                toCheckGrid[index++] = new BlockPos(worldX, surfaceY, worldZ);
            }
        }
        
        int bestFlatness = Integer.MAX_VALUE;
        BlockPos bestPos = firstPos;
        
        for (int i = 0; i < toCheckGrid.length; i++) {
            int flatness = evaluateFlatness(world, toCheckGrid[i]);
            flatnessScores[i] = flatness;
            if (flatness < bestFlatness) {
                bestFlatness = flatness;
                bestPos = toCheckGrid[i];
            }
        }
        

        // Debug visualization - place blocks and signs showing flatness scores
        // for (int i = 0; i < toCheckGrid.length; i++) {
        //     BlockPos checkPos = toCheckGrid[i];
        //     int flatness = flatnessScores[i];
            
        //     world.setBlockState(checkPos, Blocks.DIAMOND_BLOCK.getDefaultState());
            
        //     BlockPos signPos = new BlockPos(checkPos.getX(), 100, checkPos.getZ());
        //     world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState());
        //     if (world.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
        //         sign.changeText(signText -> {
        //             return signText.withMessage(0, Text.literal("Flatness:"))
        //                     .withMessage(1, Text.literal(String.valueOf(flatness)));
        //         }, true);
        //         sign.markDirty();
        //     }
        // }
        return new FlatnessResult(bestPos, bestFlatness);
    }

    public static int debugForEvaluateFlatness(ServerWorld world, BlockPos pos) {
        // return evaluateFlatness(world, pos);
        FlatnessResult result = getBestLocalFlatness(world, pos);
        for (int x = 0; x < 30; x++) {
            world.setBlockState(result.pos.add(0,x,0), Blocks.REDSTONE_BLOCK.getDefaultState());
        }
        return result.flatness;
    }
}