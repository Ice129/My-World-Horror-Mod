package horror.blueice129.feature;

import horror.blueice129.data.StateSaverAndLoader;
import horror.blueice129.data.StripMineBlocks;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.SurfaceFinder;
import horror.blueice129.utils.BlockTypes;
// import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StripMine {
    private static final Random RANDOM = Random.create();

    /**
     * generate locations for the strip mine
     * 
     * @param startX starting X coordinate
     * @param startZ starting Z coordinate
     * @param length length of the strip mine
     * @return array of coordinates for the strip mine [[x1, z1, y1, blockType1],
     *         [x2, z2, y2, blockType2], ...]
     *         blockType: 0 = stairs , 1 = main tunnel entrance, 2 = main tunnel, 3
     *         = side tunnel entrance , 4 = side tunnel main, 5 = stair support
     */
    public static String generateStripMine(MinecraftServer server, int startX, int startZ, int length) {
        int[][] coordinates = new int[0][4];
        // block pos for start of strip mine
        int x = startX;
        int z = startZ;
        int stripMineYLevel = 54; // Y level for strip mine
        int sideTunnelMinLength = 30;
        int sideTunnelMaxLength = 70;

        int[][] stairBlocks = generateStairs(server, x, stripMineYLevel, z, coordinates);
        if (stairBlocks == null) {
            return null; // Could not generate stairs
        }
        int[][] stairBottomeBlocks = new int[2][4];
        // get the bottom two blocks of the stairs
        stairBottomeBlocks[0] = stairBlocks[stairBlocks.length - 1];
        stairBottomeBlocks[1] = stairBlocks[stairBlocks.length - 2];
        int[][] generatedMainTunnel = generateMainTunnel(server, stairBottomeBlocks, length);

        int[][] sideTunnels = generateSideTunnels(server, sideTunnelMinLength, sideTunnelMaxLength, length,
                stairBottomeBlocks);

        // combine all coordinates into one array
        for (int[] block : stairBlocks) {
            coordinates = addBlockToCoordinatesArray(
                    new BlockPos(block[0], block[2], block[1]), block[3], coordinates);
        }
        for (int[] block : generatedMainTunnel) {
            coordinates = addBlockToCoordinatesArray(
                    new BlockPos(block[0], block[2], block[1]), block[3], coordinates);
        }
        for (int[] block : sideTunnels) {
            coordinates = addBlockToCoordinatesArray(
                    new BlockPos(block[0], block[2], block[1]), block[3], coordinates);
        }

        // Create arrays for each type of block
        BlockPos[] stairBlocksArray = new BlockPos[coordinates.length];
        BlockPos[] mainTunnelEntranceBlocksArray = new BlockPos[coordinates.length];
        BlockPos[] mainTunnelBlocksArray = new BlockPos[coordinates.length];
        BlockPos[] sideTunnelEntranceBlocksArray = new BlockPos[coordinates.length];
        BlockPos[] sideTunnelMainBlocksArray = new BlockPos[coordinates.length];
        BlockPos[] stairSupportBlocksArray = new BlockPos[coordinates.length];

        // Counter for each array
        int stairCount = 0;
        int mainTunnelEntranceCount = 0;
        int mainTunnelCount = 0;
        int sideTunnelEntranceCount = 0;
        int sideTunnelMainCount = 0;
        int stairSupportCount = 0;

        // Sort blocks by type
        for (int[] coord : coordinates) {
            BlockPos pos = new BlockPos(coord[0], coord[2], coord[1]);
            int blockType = coord[3];

            switch (blockType) {
                case 0:
                    stairBlocksArray[stairCount++] = pos;
                    break;
                case 1:
                    mainTunnelEntranceBlocksArray[mainTunnelEntranceCount++] = pos;
                    break;
                case 2:
                    mainTunnelBlocksArray[mainTunnelCount++] = pos;
                    break;
                case 3:
                    sideTunnelEntranceBlocksArray[sideTunnelEntranceCount++] = pos;
                    break;
                case 4:
                    sideTunnelMainBlocksArray[sideTunnelMainCount++] = pos;
                    break;
                case 5:
                    stairSupportBlocksArray[stairSupportCount++] = pos;
                    break;
            }
        }

        // Create trimmed arrays with the correct size
        BlockPos[] finalStairBlocks = new BlockPos[stairCount];
        BlockPos[] finalMainTunnelEntranceBlocks = new BlockPos[mainTunnelEntranceCount];
        BlockPos[] finalMainTunnelBlocks = new BlockPos[mainTunnelCount];
        BlockPos[] finalSideTunnelEntranceBlocks = new BlockPos[sideTunnelEntranceCount];
        BlockPos[] finalSideTunnelMainBlocks = new BlockPos[sideTunnelMainCount];
        BlockPos[] finalStairSupportBlocks = new BlockPos[stairSupportCount];

        // Copy data to the trimmed arrays
        System.arraycopy(stairBlocksArray, 0, finalStairBlocks, 0, stairCount);
        System.arraycopy(mainTunnelEntranceBlocksArray, 0, finalMainTunnelEntranceBlocks, 0, mainTunnelEntranceCount);
        System.arraycopy(mainTunnelBlocksArray, 0, finalMainTunnelBlocks, 0, mainTunnelCount);
        System.arraycopy(sideTunnelEntranceBlocksArray, 0, finalSideTunnelEntranceBlocks, 0, sideTunnelEntranceCount);
        System.arraycopy(sideTunnelMainBlocksArray, 0, finalSideTunnelMainBlocks, 0, sideTunnelMainCount);
        System.arraycopy(stairSupportBlocksArray, 0, finalStairSupportBlocks, 0, stairSupportCount);

        // Generate unique ID for this strip mine
        String mineID = "StripMine-" + startX + "-" + startZ;

        // Create StripMineBlocks object
        StripMineBlocks stripMineBlocks = new StripMineBlocks(
                mineID,
                finalStairBlocks,
                finalMainTunnelEntranceBlocks,
                finalMainTunnelBlocks,
                finalSideTunnelEntranceBlocks,
                finalSideTunnelMainBlocks,
                finalStairSupportBlocks);

        // Store the StripMineBlocks in the persistent state
        StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);
        state.setStripMineBlocks(mineID, stripMineBlocks);

        return mineID;
    }

    private static int[][] generateStairs(MinecraftServer server, int x, int stripMineYLevel, int z,
            int[][] coordinates) {
        ServerWorld overworld = server.getWorld(ServerWorld.OVERWORLD);
        int topY = SurfaceFinder.findPointSurfaceY(overworld, x, z, true, true, true);
        if (topY == -1) {
            return null; // Could not find surface
        }
        int stairY = topY + 1;
        int stairLength = topY - stripMineYLevel;
        int[][] stairCoordinates = new int[stairLength][4]; // x, y, z, blockType (0 = stairs)
        for (int i = 0; i < stairLength; i++) {
            BlockPos leftSide = new BlockPos(x, stairY - i, z);
            BlockPos rightSide = leftSide.east();
            coordinates = addBlockToCoordinatesArray(leftSide, 0, coordinates);
            coordinates = addBlockToCoordinatesArray(rightSide, 0, coordinates);
            for (int j = 1; j <= 4; j++) { // each plane will be 3 blocks long, y plane, 3d printer style, with 1 block
                                           // as support
                BlockPos lStep = leftSide.north(j);
                BlockPos rStep = rightSide.north(j);
                int blockType = 0;
                if (j == 1) {
                    blockType = 5; // support block
                }
                coordinates = addBlockToCoordinatesArray(lStep, blockType, coordinates);
                coordinates = addBlockToCoordinatesArray(rStep, blockType, coordinates); // TEST: this function
            }
        }
        return stairCoordinates; // TODO: make a tester to visualize the coordinates in game
    }

    private static int[][] generateMainTunnel(MinecraftServer server, int[][] stairBottomBlocks, int length) {
        int[][] coordinates = new int[length * 4][4]; // x, z, y, blockType (1 = main tunnel entrance, 2 = main tunnel)
        BlockPos lStartPos = new BlockPos(stairBottomBlocks[0][0], stairBottomBlocks[0][2], stairBottomBlocks[0][1])
                .south(2);
        BlockPos rStartPos = lStartPos.east();
        for (int i = 0; i < length; i++) {

            int blockType = 2; // main tunnel
            if (i <= 5) {
                blockType = 1; // main tunnel entrance
            }
            coordinates = addBlockToCoordinatesArray(lStartPos.north(i), blockType, coordinates);
            coordinates = addBlockToCoordinatesArray(lStartPos.north(i).up(), blockType, coordinates);
            coordinates = addBlockToCoordinatesArray(rStartPos.north(i), blockType, coordinates);
            coordinates = addBlockToCoordinatesArray(rStartPos.north(i).up(), blockType, coordinates);
        }
        return coordinates;
    }

    private static int[][] generateSideTunnels(MinecraftServer server, int sideTunnelMinLength, int sideTunnelMaxLength,
            int mainTunnelLength, int[][] stairBottomBlocks) {
        int[][] sideTunnels = new int[(mainTunnelLength / 3) * 2][4];

        for (int i = 0; i <= mainTunnelLength; i++) {
            if (i % 3 == 0 && i != 0) { // every 3 blocks, starting at block 3
                for (int j = 0; j < 2; j++) {
                    int sideTunnelLength = RANDOM.nextBetween(sideTunnelMinLength, sideTunnelMaxLength);
                    boolean toLeft;
                    if (j == 0) {
                        toLeft = true;
                    } else {
                        toLeft = false;
                    }
                    BlockPos entrancePos;
                    if (toLeft) {
                        entrancePos = new BlockPos(stairBottomBlocks[0][0], stairBottomBlocks[0][2],
                                stairBottomBlocks[0][1]).south(2).north(i).west();
                    } else {
                        entrancePos = new BlockPos(stairBottomBlocks[0][0], stairBottomBlocks[0][2],
                                stairBottomBlocks[0][1]).south(2).north(i).east(2);
                    }
                    // entrance
                    sideTunnels = addBlockToCoordinatesArray(entrancePos, 3, sideTunnels);
                    sideTunnels = addBlockToCoordinatesArray(entrancePos.up(), 3, sideTunnels);
                    // main side tunnel
                    for (int t = 1; t <= sideTunnelLength; t++) {
                        BlockPos tunnelPos;
                        if (toLeft) {
                            tunnelPos = entrancePos.west(t);
                        } else {
                            tunnelPos = entrancePos.east(t);
                        }
                        sideTunnels = addBlockToCoordinatesArray(tunnelPos, 4, sideTunnels);
                        sideTunnels = addBlockToCoordinatesArray(tunnelPos.up(), 4, sideTunnels);
                    }

                }
            }
        }
        return sideTunnels;
    }

    private static int[][] addBlockToCoordinatesArray(BlockPos pos, int blockType, int[][] coordinates) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int blockTypeValue = blockType;
        int[] newElement = new int[] { x, z, y, blockTypeValue };
        coordinates = addToArray(coordinates, newElement);
        return coordinates;
    }

    private static int[][] addToArray(int[][] array, int[] newElement) {
        if (array == null) {
            return new int[][] { newElement };
        }
        int[][] newArray = new int[array.length + 1][];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = newElement;
        return newArray;
    }

    public static boolean placePossibleBlocks(MinecraftServer server, String mineID) {
        String notPlacedBlocksID = mineID + "_placed";
        StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);
        StripMineBlocks allBlocks = state.getStripMineBlocks(mineID);
        StripMineBlocks notPlacedBlocks = state.getStripMineBlocks(placedBlocksID);

        // If we don't have the strip mine data, return false
        if (allBlocks == null) {
            return false;
        }

        // If placedBlocks doesn't exist yet, create a new empty one
        if (placedBlocks == null) {
            placedBlocks = new StripMineBlocks(placedBlocksID,
                    new BlockPos[0], new BlockPos[0], new BlockPos[0],
                    new BlockPos[0], new BlockPos[0], new BlockPos[0]);
            
            // Save the new empty placedBlocks object to the state
            state.setStripMineBlocks(placedBlocksID, placedBlocks);
        }

        // Track if any blocks were placed during this operation
        boolean anyBlocksPlaced = false;
        ServerWorld world = server.getWorld(ServerWorld.OVERWORLD);
        
        
        
        
        // Save the updated placedBlocks if any blocks were placed
        if (anyBlocksPlaced) {
            // 
            state.setStripMineBlocks(placedBlocksID, updatedPlacedBlocks);
        }

        return anyBlocksPlaced;
    }

    /**
     * Processes a specific block type for the strip mine
     * 
     * @param world           The server world to place blocks in
     * @param allBlocks       All blocks of this type that need to be placed
     * @param placedBlocks    Blocks of this type that have already been placed
     * @param blockType       The type of block (0=stairs, 1=main tunnel entrance,
     *                        etc.)
     * @param placedBlocksObj The StripMineBlocks object to update with newly placed
     *                        blocks
     * @return true if any blocks were placed, false otherwise
     */
    private static BlockPos processBlockType(ServerWorld world, BlockPos[] allBlocks, BlockPos[] placedBlocks,
            int blockType, StripMineBlocks placedBlocksObj) {
        if (allBlocks == null || allBlocks.length == 0) {
            return null;
        }

        // Convert existing placedBlocks to a List for easier checking
        List<BlockPos> placedBlocksList = new ArrayList<>();
        if (placedBlocks != null) {
            placedBlocksList.addAll(Arrays.asList(placedBlocks));
        }

        List<BlockPos> newlyPlacedBlocks = new ArrayList<>();
        boolean anyBlocksPlaced = false;

        // Process each block position
        for (BlockPos pos : allBlocks) {
            // Skip if this block has already been placed
            if (isBlockAlreadyPlaced(pos, placedBlocksList)) {
                continue;
            }

            // Check if the chunk containing this block is loaded
            if (!ChunkLoader.loadChunksInRadius(world, pos, 1)) {
                continue; // Skip if chunk is not loaded
            }

            // Place the block based on its type
            if (placeBlockByType(world, pos, blockType)) {
                newlyPlacedBlocks.add(pos);
                anyBlocksPlaced = true;
            }
        }


        if (anyBlocksPlaced) {


        }

        return null;
    }

    /**
     * Checks if a block position is already in the list of placed blocks
     * 
     * @param pos          The block position to check
     * @param placedBlocks List of already placed blocks
     * @return true if the block is already placed, false otherwise
     */
    private static boolean isBlockAlreadyPlaced(BlockPos pos, List<BlockPos> placedBlocks) {
        // We need to check each position since BlockPos equals() checks exact object,
        // not just coordinates
        for (BlockPos placedPos : placedBlocks) {
            if (pos.getX() == placedPos.getX() &&
                    pos.getY() == placedPos.getY() &&
                    pos.getZ() == placedPos.getZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places a block based on its type
     * 
     * @param world     The server world to place blocks in
     * @param pos       The position to place the block at
     * @param blockType The type of block to place (0=stairs, 1=main tunnel
     *                  entrance, etc.)
     * @return true if the block was successfully placed, false otherwise
     */
    private static boolean placeBlockByType(ServerWorld world, BlockPos pos, int blockType) {
        // Different block types for different parts of the mine
        BlockState blockState;

        // Select block state based on block type
        switch (blockType) {
            case 0: // stairs
                // Create an air block for the stairs (carve out space)
                blockState = Blocks.AIR.getDefaultState();

                // break any ore blocks in the side tunnel
                superMineOreVeins(world, pos);

                // Randomly place torches on stair blocks
                torchPlacer(world, pos);

                break;
            case 1: // main tunnel entrance
                // Create an air block for the main tunnel entrance
                blockState = Blocks.AIR.getDefaultState();

                // break any ore blocks in the side tunnel
                superMineOreVeins(world, pos);

                torchPlacer(world, pos);

                break;
            case 2: // main tunnel
                // Create an air block for the main tunnel
                blockState = Blocks.AIR.getDefaultState();

                // break any ore blocks in the side tunnel
                superMineOreVeins(world, pos);
                torchPlacer(world, pos);
                break;
            case 3: // side tunnel entrance
                blockState = Blocks.AIR.getDefaultState();

                // break any ore blocks in the side tunnel
                superMineOreVeins(world, pos);
                torchPlacer(world, pos);
                break;
            case 4: // side tunnel main
                // Create an air block for the side tunnel
                blockState = Blocks.AIR.getDefaultState();

                superMineOreVeins(world, pos);
                torchPlacer(world, pos);
                break;
            case 5: // stair support
                // Place wooden supports for the stairs
                if (world.getBlockState(pos).isAir()) {
                    blockState = Blocks.COBBLESTONE.getDefaultState();
                } else {
                    return false; // Cannot place support if space is not air
                }
                break;

            default:
                return false; // Unknown block type
        }

        // Set the block in the world
        world.setBlockState(pos, blockState);

        return true;

    }

    /**
     * check id plaacing torch is ok position
     * check if adjacent torches are in invalid locations, and if so, delete them
     * 
     * @param world
     * @param pos
     */

    private static void torchPlacer(ServerWorld world, BlockPos pos) {

        boolean isBlockBellowSolid = world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
        if (RANDOM.nextInt(10) == 0 && isBlockBellowSolid) {
            // Place a torch if the block below is solid and the random check passes
            world.setBlockState(pos, Blocks.TORCH.getDefaultState());
        }

        // get rid of torches that are in invalid locations
        for (BlockPos adjacentPos : new BlockPos[] {
                pos.north(), pos.south(), pos.east(), pos.west(), pos.up(), pos.down() }) {

            BlockState adjacentState = world.getBlockState(adjacentPos);
            if (adjacentState.getBlock() == Blocks.TORCH) {
                boolean isBelowSolid = world.getBlockState(adjacentPos.down()).isSolidBlock(world, adjacentPos.down());
                if (!isBelowSolid) {
                    // Remove the torch if the block below is not solid
                    world.setBlockState(adjacentPos, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void superMineOreVeins(ServerWorld world, BlockPos pos) {
        mineOreVeins(world, pos.down());
        mineOreVeins(world, pos.up());
        mineOreVeins(world, pos.north());
        mineOreVeins(world, pos.south());
        mineOreVeins(world, pos.east());
        mineOreVeins(world, pos.west());
    }

    /**
     * Mines ore veigns, checks if sourrounding blocks are an instance of an ore
     * block, then mines recursivley any blocks
     * 
     * @param world
     * @param pos
     */

    private static void mineOreVeins(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) {
            return;
        }
        BlockState targetState = world.getBlockState(pos);
        if (BlockTypes.isOreBlock(targetState)) {
            // Mine the ore block (set to air)
            world.setBlockState(pos, Blocks.AIR.getDefaultState());

            // Recursively check adjacent blocks in all 6 directions
            mineOreVeins(world, pos.north());
            mineOreVeins(world, pos.south());
            mineOreVeins(world, pos.east());
            mineOreVeins(world, pos.west());
            mineOreVeins(world, pos.up());
            mineOreVeins(world, pos.down());
        }
    }
