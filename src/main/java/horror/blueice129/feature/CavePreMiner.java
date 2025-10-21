package horror.blueice129.feature;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Comparator;
import net.minecraft.util.math.random.Random;
import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.LineOfSightUtils;
import horror.blueice129.utils.SurfaceFinder;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.BlockTypes;

public class CavePreMiner {

    // Create a Random object for generating random values
    private static final Random random = Random.create();

    /**
     * Checks if a block is suitable for placing a torch on.
     * A block is suitable if it is:
     * - Solid on the top face
     * - cave air
     * - light level less than or equal to 4
     * - torch isnt in line of sight
     * 
     * @param world         The world to check in
     * @param pos           The position to check
     * @param caveAirBlocks list of cave air blocks to check against
     * @param player        The player to check line of sight against
     * @return True if the block is suitable, false otherwise
     */
    public static boolean isSuitableForTorch(World world, BlockPos pos, PlayerEntity player,
            java.util.List<BlockPos> caveAirBlocks) {
        // Make sure the chunk is loaded before accessing blocks
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        // Hard cutoff: no functionality above Y = 55 (except mineStairs)
        if (pos.getY() > 55)
            return false;

        // Check if the block bellow is solid on the top face
        BlockPos belowPos = pos.down();
        boolean isSolidTop = world.getBlockState(belowPos).isSideSolidFullSquare(world, belowPos,
                net.minecraft.util.math.Direction.UP);

        // Check if the block is cave air OR normal air (we now accept both)
        boolean isAirBlock = caveAirBlocks.contains(pos) || world.getBlockState(pos).isOf(Blocks.AIR);

        // Check if the light level is less than or equal to 4
        int lightLevel = world.getLightLevel(pos);
        boolean isLowLight = lightLevel <= 4;

        boolean isInLineOfSight = LineOfSightUtils.isBlockInLineOfSight(player, pos, 16 * 10); // 10 chunks

        return isSolidTop && isLowLight && !isInLineOfSight && isAirBlock;
    }

    /**
     * makes a list of all connected cave air blocks from startPos
     * only checks 5 blocks up from ground of cave
     * 
     * @param world    The world to check in
     * @param startPos The position to start from
     * @return A list of all connected cave air blocks
     */
    public static java.util.List<BlockPos> findConnectedCaveAirBlocks(World world, BlockPos startPos) {
        java.util.List<BlockPos> caveAirBlocks = new java.util.ArrayList<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            caveAirBlocks.add(currentPos);

            for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
                BlockPos neighborPos = currentPos.offset(direction);

                // Check if neighbor is within 200 blocks of start position
                if (neighborPos.getSquaredDistance(startPos) > 200 * 50) {
                    continue;
                }
                // Skip anything above the hard cutoff
                if (neighborPos.getY() > 55)
                    continue;

                if (!visited.contains(neighborPos)) {
                    // accept both normal air and cave air
                    BlockState neighborState = world.getBlockState(neighborPos);
                    if (!neighborState.isOf(Blocks.CAVE_AIR) && !neighborState.isOf(Blocks.AIR)) {
                        continue;
                    }
                    // Only check 10 blocks up from the ground of the cave, takes into
                    // account slope and inclines of cave floor
                    // if solid block within 10 blocks below, add to queue
                    boolean hasSolidBelow = false;
                    for (int i = 1; i <= 10; i++) {
                        BlockPos belowPos = currentPos.down(i);
                        if (belowPos.getY() > 55)
                            continue; // don't test surface blocks
                        if (world.getBlockState(belowPos).isSolidBlock(world, belowPos)) {
                            hasSolidBelow = true;
                            break;
                        }
                    }
                    if (hasSolidBelow) {
                        queue.add(neighborPos);
                        visited.add(neighborPos);
                    }
                }
            }
        }
        return caveAirBlocks;
    }

    /**
     * mines all ore veins exposed to any block in caveAirBlocks
     * 
     * @param world         The world to mine in
     * @param caveAirBlocks The list of cave air blocks to check against
     * @param player        The player to check line of sight against
     * @return The number of ore blocks mined
     */
    public static int mineExposedOres(World world, java.util.List<BlockPos> caveAirBlocks, PlayerEntity player) {
        int oresMined = 0;
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>(caveAirBlocks);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();

            for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
                BlockPos neighborPos = currentPos.offset(direction);

                if (!visited.contains(neighborPos)) {
                    BlockState state = world.getBlockState(neighborPos);
                    if (BlockTypes.isOreBlock(state)) {
                        // Mine the ore block if not in view
                        if (!LineOfSightUtils.isBlockInLineOfSight(player, neighborPos, 16 * 10)) { // 10 chunks
                            world.breakBlock(neighborPos, false);
                            oresMined++;
                        }
                        // Add neighboring blocks to the queue to check for connected ores
                        queue.add(neighborPos);
                    }
                    visited.add(neighborPos);
                }
            }
        }
        return oresMined;
    }

    /**
     * Populates the area with torches, updating lighting after each placement
     * 
     * @param world         The world to populate torches in
     * @param caveAirBlocks The list of cave air blocks to check against
     * @param player        The player to check line of sight against
     * @return The number of torches placed
     */
    public static int populateTorches(World world, java.util.List<BlockPos> caveAirBlocks, PlayerEntity player) {
        int torchesPlaced = 0;
        int minTorchDistance = 8; // Minimum distance between torches

        // Track placed torch positions
        java.util.Set<BlockPos> torchPositions = new java.util.HashSet<>();

        // Convert to list for easier shuffling and sorting
        java.util.List<BlockPos> positions = new java.util.ArrayList<>(caveAirBlocks);

        // Sort by light level (darkest first)
        positions.sort(Comparator.comparingInt(world::getLightLevel));

        for (BlockPos pos : positions) {
            // Hard cutoff: do not place torches above Y=55 (surface differentiation)
            if (pos.getY() > 55)
                continue;
            // Check if this position is still dark enough for a torch
            if (world.getLightLevel(pos) > 4) {
                continue; // Skip if the area is already lit by previously placed torches
            }

            // Check if too close to existing torches
            boolean tooClose = false;
            for (BlockPos torchPos : torchPositions) {
                if (pos.getSquaredDistance(torchPos) < minTorchDistance * minTorchDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose && isSuitableForTorch(world, pos, player, caveAirBlocks)) {
                // Place a torch
                world.setBlockState(pos, Blocks.TORCH.getDefaultState(), 3);
                torchesPlaced++;
                torchPositions.add(pos);

                // Force block update to update lighting
                world.updateNeighbors(pos, Blocks.TORCH);
            }
        }

        return torchesPlaced;
    }

    /**
     * Chance to place crafting table, furnace, or cobblestone pillar, or nothing
     * 
     * @param world         The world to place the block in
     * @param caveAirBlocks The list of cave air blocks to check against
     * @param player        The player to check line of sight against
     * @return
     */

    public static boolean placeExtraBlocks(World world, java.util.List<BlockPos> caveAirBlocks, PlayerEntity player) {
        // Chance distribution for extras: furnace 20%, crafting table 40%, pillar 30%,
        // nothing 10%
        java.util.List<BlockPos> suitablePositions = new java.util.ArrayList<>();
        for (BlockPos pos : caveAirBlocks) {
            if (pos.getY() > 55)
                continue; // never place extras on/above surface
            if (isBlockSuitableForExtraBlock(world, pos, player))
                suitablePositions.add(pos);
        }

        if (suitablePositions.isEmpty())
            return false;

        // Pick a random candidate position
        java.util.Collections.shuffle(suitablePositions);
        BlockPos target = suitablePositions.get(0);

        int roll = random.nextInt(100);
        if (roll < 20) {
            return placeFurnaceAt(world, target);
        } else if (roll < 60) {
            return placeCraftingTableAt(world, target);
        } else if (roll < 90) {
            return placeCobblestonePillarAt(world, target);
        }

        // 10% chance to do nothing
        return true;
    }

    // Helper: place a furnace at pos (on the pos itself). Returns true if placed.
    private static boolean placeFurnaceAt(World world, BlockPos pos) {
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, pos, 1))
            return false;
        if (!world.getBlockState(pos).isAir())
            return false;

        world.setBlockState(pos, Blocks.FURNACE.getDefaultState(), 3);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FurnaceBlockEntity furnace) {
            furnace.setStack(1, new ItemStack(Items.COAL, random.nextInt(64) + 1));
            ItemStack input = new ItemStack(random.nextBoolean() ? Items.RAW_IRON
                    : (random.nextBoolean() ? Items.RAW_GOLD : Items.BEEF), random.nextInt(64) + 1);
            furnace.setStack(0, input);
        }
        return true;
    }

    // Helper: place a crafting table above the supplied pos. Returns true if
    // placed.
    private static boolean placeCraftingTableAt(World world, BlockPos pos) {
        BlockPos above = pos.up();
        if (above.getY() > 55)
            return false;
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, above, 1))
            return false;
        if (!world.getBlockState(above).isAir())
            return false;
        world.setBlockState(above, Blocks.CRAFTING_TABLE.getDefaultState(), 3);
        return true;
    }

    // Helper: place a 3-block tall cobblestone pillar starting at pos.up(). Returns
    // true if at least one block placed.
    private static boolean placeCobblestonePillarAt(World world, BlockPos pos) {
        BlockPos base = pos.up();
        if (base.getY() > 55)
            return false;
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, base, 1))
            return false;

        boolean placedAny = false;
        for (int i = 0; i < 3; i++) {
            BlockPos p = base.up(i);
            if (p.getY() <= 55 && world.getBlockState(p).isAir()) {
                world.setBlockState(p, Blocks.COBBLESTONE.getDefaultState(), 3);
                placedAny = true;
            }
        }
        return placedAny;
    }

    /**
     * Checks if a block is suitable for placing extra blocks on.
     * 
     * @param world  The world to check in
     * @param pos    The position to check
     * @param player The player to check line of sight against
     * @return True if the block is suitable, false otherwise
     */
    private static boolean isBlockSuitableForExtraBlock(World world, BlockPos pos, PlayerEntity player) {
        // Make sure the chunk is loaded before accessing blocks
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        // Hard cutoff: never suitable above Y=55
        if (pos.getY() > 55)
            return false;
        BlockState state = world.getBlockState(pos);
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        // Check if there's a solid block below
        boolean hasSolidBelow = belowState.isSolidBlock(world, belowPos);

        // Check if current position is air
        boolean isAir = state.isOf(Blocks.AIR) || state.isOf(Blocks.CAVE_AIR);

        // Check if not in line of sight
        boolean isInLineOfSight = LineOfSightUtils.isBlockInLineOfSight(player, pos, 16 * 10); // 10 chunks

        return hasSolidBelow && isAir && !isInLineOfSight;
    }

    /**
     * Finds the starter block position
     * Should be within 16 chunks of player and be bellow y=48
     * Should be on ground of cave
     * 
     * @param world     The world to search in
     * @param playerPos The player's position
     * @return A suitable BlockPos, or null if none found
     */

    public static BlockPos findStarterBlock(World world, BlockPos playerPos) {
        // Define our distance ranges (in blocks)
        // 1 chunk = 16 blocks
        int maxDistance = 16 * 5; // 5 chunks
        int minY = world.getBottomY();
        int maxY = Math.min(48, world.getTopY() - 1); // Ensure we don't exceed world height

        // We'll try up to 100 times to find a suitable position
        for (int attempt = 0; attempt < 100; attempt++) {
            // Choose a random x and z offset within the max distance
            int xOffset = random.nextInt(2 * maxDistance + 1) - maxDistance;
            int zOffset = random.nextInt(2 * maxDistance + 1) - maxDistance;
            int yOffset = random.nextInt(30) - 15; // Random y offset between -15 and +15

            // Calculate the target position
            int x = playerPos.getX() + xOffset;
            int z = playerPos.getZ() + zOffset;
            int y = playerPos.getY() + yOffset;

            // Make sure the chunk at this position is loaded before accessing blocks
            BlockPos targetPos = new BlockPos(x, y, z);
            if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, targetPos, 1)) {
                continue; // Skip if chunk couldn't be loaded
            }

            // look 5 blocks down from this block for the first air block
            for (int i = 0; i <= 5; i++) {
                BlockPos checkPos = new BlockPos(x, y - i, z);
                if (checkPos.getY() < minY || checkPos.getY() > maxY) {
                    continue;
                }
                BlockState state = world.getBlockState(checkPos);
                // accept either cave air or normal air as a candidate for starter
                if (state.isOf(Blocks.CAVE_AIR) || state.isOf(Blocks.AIR)) {
                    // keep looking down until we find a solid block
                    BlockPos belowPos = checkPos.down();
                    BlockState belowState = world.getBlockState(belowPos);
                    while (belowPos.getY() >= minY && belowPos.getY() <= maxY
                            && (belowState.isOf(Blocks.CAVE_AIR) || belowState.isOf(Blocks.AIR))) {
                        belowPos = belowPos.down();
                        belowState = world.getBlockState(belowPos);
                    }
                    if (belowState.isSolidBlock(world, belowPos)) {
                        return BlockPos.ofFloored(x, belowPos.getY() + 1, z); // Return the air block above the solid
                                                                              // block
                    }
                }
            }
        }
        return null; // If we couldn't find a suitable position after all attempts
    }

    /**
     * Mines stairs from the starter block up to the surface
     * stairs go up 1 block and forward 1 block, with a height of 3 blocks
     * 
     * @param world      The world to mine in
     * @param starterPos The starting position to mine from
     * @return The length of the stairs mined
     */
    public static int mineStairs(World world, BlockPos starterPos) {
        int stairLength = 0;
        BlockPos currentPos = starterPos;
        // list of stair blocks, which we will then set the 3 blocks above to air
        java.util.List<BlockPos> stairBlocks = new java.util.ArrayList<>();
        // fill in the stairblocks list
        while (true) {
            // BlockState state = world.getBlockState(currentPos);
            // if y value is less than 100
            if (currentPos.getY() >= 130) {
                break;
            }
            stairBlocks.add(currentPos);
            currentPos = currentPos.up().north(); // Move up 1 and forward 1
            stairLength++;
        }
        // Set the 3 blocks above each stair block to air
        int torchDistance = 0;
        for (BlockPos stairPos : stairBlocks) {
            if (world.getBlockState(stairPos).isOf(Blocks.CAVE_AIR) || world.getBlockState(stairPos).isOf(Blocks.AIR)) {
                int surfaceY = SurfaceFinder.findPointSurfaceY((ServerWorld) world,
                        stairPos.getX(), stairPos.getZ(), true, false, false);
                if (stairPos.getY() < surfaceY || surfaceY != -1) {
                    // only place cobblestone if below surface
                    world.setBlockState(stairPos, Blocks.COBBLESTONE.getDefaultState());
                }
            }
            for (int i = 1; i <= 3; i++) {
                if (i == 1 && torchDistance == 8) {
                    // place torch on stair block
                    BlockPos torchPos = stairPos.up(1);
                    if (torchPos.getY() < world.getTopY()) {
                        world.setBlockState(torchPos, Blocks.TORCH.getDefaultState());
                    }
                    torchDistance = 0;
                } else {
                    BlockPos abovePos = stairPos.up(i);
                    if (abovePos.getY() < world.getTopY()) {
                        world.setBlockState(abovePos, Blocks.AIR.getDefaultState());
                    }
                }

            }
            torchDistance++;
        }
        return stairLength;
    }

    /**
     * Main method to pre-mine a cave near the player.
     * 
     * @param world     The world to mine in
     * @param playerPos The player's position
     * @param player    The player entity
     * @return True if a cave was successfully pre-mined, false otherwise
     */
    public static boolean preMineCave(World world, BlockPos playerPos, PlayerEntity player) {
        BlockPos starterPos = findStarterBlock(world, playerPos);
        if (starterPos == null) {
            return false;
        }
        horror.blueice129.HorrorMod129.LOGGER.info("Cave Pre-Miner: Found starter block at " + starterPos);
        java.util.List<BlockPos> caveAirBlocks = findConnectedCaveAirBlocks(world, starterPos);
        if (caveAirBlocks.size() < 50) {
            return false; // Not enough cave air blocks to consider this a cave
        }
        int oresMined = mineExposedOres(world, caveAirBlocks, player);
        int torchesPlaced = populateTorches(world, caveAirBlocks, player);
        boolean extraBlockPlaced = placeExtraBlocks(world, caveAirBlocks, player);
        int stairLength = mineStairs(world, starterPos);
        HorrorMod129.LOGGER.info("Cave Pre-Miner: Mined " + oresMined + " ores, placed " + torchesPlaced
                + " torches, extra block placed: " + extraBlockPlaced + ", stair length: " + stairLength);
        return true;
    }

}