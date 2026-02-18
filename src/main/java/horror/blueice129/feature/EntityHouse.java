package horror.blueice129.feature;

import horror.blueice129.utils.StructurePlacer;
import horror.blueice129.utils.SurfaceFinder;
import horror.blueice129.utils.BlockTypes;

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

        // using findSerfaceAt function to get the surface height at each point
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                // BlockPos checkPos = pos.add(x, 0, z);
                int surfaceY = SurfaceFinder.findPointSurfaceY(world, x, z, true, true, false);
                if (surfaceY == -1) {
                    flatnessScore += 3;
                } else {
                    BlockPos checkPos = new BlockPos(x, surfaceY, z);
                    // while loop to get true surface, in case of trees, foliage, or air
                    while (BlockTypes.isLogBlock(world.getBlockState(checkPos.down()).getBlock())
                            || BlockTypes.isFoliage(world.getBlockState(checkPos.down()).getBlock(), false)
                            || world.getBlockState(checkPos.down()).isAir()) {
                        checkPos = checkPos.down();
                    }
                }
            }
        }
        return flatnessScore;
    }

    public static int debugForEvaluateFlatness(ServerWorld world, BlockPos pos) {
        return evaluateFlatness(world, pos);
    }
}
