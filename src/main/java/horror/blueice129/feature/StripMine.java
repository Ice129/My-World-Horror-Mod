package horror.blueice129.feature;

import horror.blueice129.data.StateSaverAndLoader;
import horror.blueice129.data.StripMineBlocks;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

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

        int[][] sideTunnels = generateSideTunnels(server, sideTunnelMinLength, sideTunnelMaxLength, length, stairBottomeBlocks);

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
            finalStairSupportBlocks
        );
        
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

    private static boolean placePossibleBlocks(MinecraftServer server, String mineID) {
        String placedBlocksID = mineID + "_placed";
        StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);
        StripMineBlocks allBlocks = state.getStripMineBlocks(mineID);
        StripMineBlocks placedBlocks = state.getStripMineBlocks(placedBlocksID);
        
}
