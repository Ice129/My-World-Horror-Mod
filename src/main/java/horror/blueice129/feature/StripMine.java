package horror.blueice129.feature;

import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;


public class StripMine {
    private static final String STRIP_MINE_LOCATIONS_ID = "strip_mine_block_locations";
    private static final Random RANDOM = Random.create();

    /**
     * generate locations for the strip mine
     * @param startX starting X coordinate
     * @param startZ starting Z coordinate
     * @param length length of the strip mine
     * @return array of coordinates for the strip mine [[x1, z1, y1, blockType1], [x2, z2, y2, blockType2], ...]
     * blockType: 0 = stairs , 1 = main tunnel entrance, 2 = main tunnel, 3 = side tunnel entrance , 4 = side tunnel main, 5 = stair support
     */
    public static int[][] generateStripMine(MinecraftServer server, int startX, int startZ, int length) {
        int[][] coordinates = new int[length][4];
        // block pos for start of strip mine
        int x = startX;
        int z = startZ;
        int stripMineYLevel = 54; // Y level for strip mine
        int sideTunnelSpacing = 2; // spacing between side tunnels
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

        return null;
    }

    private static int[][] generateStairs(MinecraftServer server, int x, int stripMineYLevel, int z, int[][] coordinates) {
        ServerWorld overworld = server.getWorld(ServerWorld.OVERWORLD);
        int topY = SurfaceFinder.findPointSurfaceY(overworld, x, z, true, true, true);
        if (topY == -1) {
            return null; // Could not find surface
        }
        int stairY = topY+1;
        int stairLength = topY - stripMineYLevel;
        int[][] stairCoordinates = new int[stairLength][4]; // x, y, z, blockType (0 = stairs)
        for (int i = 0; i < stairLength; i++) {
            BlockPos leftSide = new BlockPos(x, stairY - i, z);
            BlockPos rightSide = leftSide.east();
            coordinates = addBlockToCoordinatesArray(leftSide, 0, coordinates);
            coordinates = addBlockToCoordinatesArray(rightSide, 0, coordinates);
            for (int j = 1; j <= 4; j++) { // each plane will be 3 blocks long, y plane, 3d printer style, with 1 block as support
                BlockPos lStep = leftSide.north(j);
                BlockPos rStep = rightSide.north(j);
                int blockType = 0;
                if (j == 1){
                    blockType = 5; // support block
                }
                coordinates = addBlockToCoordinatesArray(lStep, blockType, coordinates);
                coordinates = addBlockToCoordinatesArray(rStep, blockType, coordinates); // TEST: this function
            }
        }
        return stairCoordinates; // TODO: make a tester to visualize the coordinates in game
    }

    private static int[][] generateMainTunnel(MinecraftServer server, int[][] stairBottomBlocks, int length) {
        int[][] coordinates = new int[length*4][4]; // x, z, y, blockType (1 = main tunnel entrance, 2 = main tunnel)
        BlockPos lStartPos = new BlockPos(stairBottomBlocks[0][0], stairBottomBlocks[0][2], stairBottomBlocks[0][1]).south(2);
        BlockPos rStartPos = lStartPos.east();
        for (int i = 0; i < length; i++) {

            int blockType = 2; // main tunnel
            if (i <= 5){
                blockType = 1; // main tunnel entrance
            }
            coordinates = addBlockToCoordinatesArray(lStartPos.north(i), blockType, coordinates);
            coordinates = addBlockToCoordinatesArray(lStartPos.north(i).up(), blockType, coordinates);
            coordinates = addBlockToCoordinatesArray(rStartPos.north(i), blockType, coordinates);
            coordinates = addBlockToCoordinatesArray(rStartPos.north(i).up(), blockType, coordinates);
        }
        return coordinates;
    }

    private static int[][] addBlockToCoordinatesArray(BlockPos pos, int blockType, int[][] coordinates) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int blockTypeValue = blockType;
        int[] newElement = new int[]{x, z, y, blockTypeValue};
        coordinates = addToArray(coordinates, newElement);
        return coordinates;
    }

    private static int[][] addToArray(int[][] array, int[] newElement) {
        if (array == null) {
            return new int[][]{newElement};
        }
        int[][] newArray = new int[array.length + 1][];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = newElement;
        return newArray;
    }
}
