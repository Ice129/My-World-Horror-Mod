package horror.blueice129.feature;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
// import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Comparator;
import net.minecraft.util.math.random.Random;
import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.LineOfSightUtils;
import horror.blueice129.utils.ChunkLoader;

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
     * @param world The world to check in
     * @param pos The position to check
     * @param caveAirBlocks list of cave air blocks to check against
     * @param player The player to check line of sight against
     * @return True if the block is suitable, false otherwise
     */
    public static boolean isSuitableForTorch(World world, BlockPos pos, PlayerEntity player, java.util.List<BlockPos> caveAirBlocks) {
        // Make sure the chunk is loaded before accessing blocks
        if (!ChunkLoader.loadChunksInRadius((net.minecraft.server.world.ServerWorld)world, pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        
        // Check if the block bellow is solid on the top face
        BlockPos belowPos = pos.down();
        boolean isSolidTop = world.getBlockState(belowPos).isSideSolidFullSquare(world, belowPos, net.minecraft.util.math.Direction.UP);
        
        // Check if the block is cave air
        boolean isCaveAir = caveAirBlocks.contains(pos);

        // Check if the light level is less than or equal to 4
        int lightLevel = world.getLightLevel(pos);
        boolean isLowLight = lightLevel <= 4;

        boolean isInLineOfSight = LineOfSightUtils.isBlockInLineOfSight(player, pos, 16 * 10); // 10 chunks

        return isSolidTop && isLowLight && !isInLineOfSight && isCaveAir;
    }

    /**
     * makes a list of all connected cave air blocks from startPos
     * only checks 5 blocks up from ground of cave
     * @param world The world to check in
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
                
                // Check if neighbor is within 50 blocks of start position
                if (neighborPos.getSquaredDistance(startPos) > 2500) { // 50^2 = 2500
                    continue;
                }
                
                if (!visited.contains(neighborPos) && world.getBlockState(neighborPos).isOf(Blocks.CAVE_AIR)) {
                    // Only check 5 blocks up from the ground of the cave, takes into
                    // account slope and inclines of cave floor
                    // if solid block within 5 blocks below, add to queue
                    boolean hasSolidBelow = false;
                    for (int i = 1; i <= 5; i++) {
                        BlockPos belowPos = currentPos.down(i);
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
     * @param world The world to mine in
     * @param caveAirBlocks The list of cave air blocks to check against
     * @param player The player to check line of sight against
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
                    if (isOreBlock(state)) {
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
     * @param world The world to populate torches in
     * @param caveAirBlocks The list of cave air blocks to check against
     * @param player The player to check line of sight against
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
     * @param world The world to place the block in
     * @param caveAirBlocks The list of cave air blocks to check against
     * @param player The player to check line of sight against 
     * @return
     */

    public static boolean placeExtraBlocks(World world, java.util.List<BlockPos> caveAirBlocks, PlayerEntity player) {
        // 20% chance to place a furnace, 40% chance to place a crafting table, 30% chance to place a cobblestone pillar, 10% chance to do nothing
        // Only place if block is suitable (on top of a solid block, not in line of sight, cave air)
        java.util.List<BlockPos> suitablePositions = new java.util.ArrayList<>();
        for (BlockPos pos : caveAirBlocks) {
            if (isBlockSuitableForExtraBlock(world, pos, player)) {
                suitablePositions.add(pos);
            }
        }
        java.util.Collections.shuffle(suitablePositions);
        if (suitablePositions.isEmpty()) {
            return false;
        }
        int chance = random.nextInt(100);

        // TODO: improve this method to make look and read better

        if (chance < 20) {
            // Place a furnace with a random amount of coal and either iron, gold, or beef
            BlockPos furnacePos = suitablePositions.get(0);
            // Make sure the chunk is loaded before modifying blocks
            if (!ChunkLoader.loadChunksInRadius((net.minecraft.server.world.ServerWorld)world, furnacePos, 1)) {
                return false; // Chunk couldn't be loaded
            }
            world.setBlockState(furnacePos, Blocks.FURNACE.getDefaultState(), 3);
            BlockEntity blockEntity = world.getBlockEntity(furnacePos);
            if (blockEntity instanceof FurnaceBlockEntity furnace) {
                furnace.setStack(1, new ItemStack(Items.COAL, random.nextInt(64) + 1));
                furnace.setStack(0, new ItemStack(random.nextBoolean() ? Items.RAW_IRON : (random.nextBoolean() ? Items.RAW_GOLD : Items.BEEF), random.nextInt(64) + 1));
            }
        } else if (chance < 60) {
            // Place a crafting table, but first check if the spot above is air
            BlockPos craftingTablePos = suitablePositions.get(0).up();
            // Make sure the chunk is loaded before accessing and modifying blocks
            if (!ChunkLoader.loadChunksInRadius((net.minecraft.server.world.ServerWorld)world, craftingTablePos, 1)) {
                return false; // Chunk couldn't be loaded
            }
            if (world.getBlockState(craftingTablePos).isAir()) {
                world.setBlockState(craftingTablePos, Blocks.CRAFTING_TABLE.getDefaultState(), 3);
            }
        } else if (chance < 90) {
            // Place a cobblestone pillar 3 blocks tall, checking each position
            BlockPos pillarPos = suitablePositions.get(0).up();
            // Make sure the chunk is loaded before accessing and modifying blocks
            if (!ChunkLoader.loadChunksInRadius((net.minecraft.server.world.ServerWorld)world, pillarPos, 1)) {
                return false; // Chunk couldn't be loaded
            }
            if (world.getBlockState(pillarPos).isAir()) {
                world.setBlockState(pillarPos, Blocks.COBBLESTONE.getDefaultState(), 3);
                
                BlockPos pillarPos2 = pillarPos.up();
                // No need to check chunks again for pillarPos2 and pillarPos3 since they're in the same chunk as pillarPos
                if (world.getBlockState(pillarPos2).isAir()) {
                    world.setBlockState(pillarPos2, Blocks.COBBLESTONE.getDefaultState(), 3);
                    
                    BlockPos pillarPos3 = pillarPos2.up();
                    if (world.getBlockState(pillarPos3).isAir()) {
                        world.setBlockState(pillarPos3, Blocks.COBBLESTONE.getDefaultState(), 3);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks if a block is suitable for placing extra blocks on.
     * @param world The world to check in
     * @param pos The position to check
     * @param player The player to check line of sight against
     * @return True if the block is suitable, false otherwise
     */
    private static boolean isBlockSuitableForExtraBlock(World world, BlockPos pos, PlayerEntity player) {
        // Make sure the chunk is loaded before accessing blocks
        if (!ChunkLoader.loadChunksInRadius((net.minecraft.server.world.ServerWorld)world, pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        
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
     * Checks if a block state is an ore block.
     * @param state The block state to check
     * @return True if the block is an ore, false otherwise
     */
    private static boolean isOreBlock(BlockState state) { // TODO: update to use BlockTypes.isOre
        return state.isOf(Blocks.COAL_ORE) ||
               state.isOf(Blocks.IRON_ORE) ||
               state.isOf(Blocks.GOLD_ORE) ||
               state.isOf(Blocks.DIAMOND_ORE) ||
               state.isOf(Blocks.EMERALD_ORE) ||
               state.isOf(Blocks.REDSTONE_ORE) ||
               state.isOf(Blocks.LAPIS_ORE) ||
               state.isOf(Blocks.COPPER_ORE) ||
               state.isOf(Blocks.DEEPSLATE_COAL_ORE) ||
               state.isOf(Blocks.DEEPSLATE_IRON_ORE) ||
               state.isOf(Blocks.DEEPSLATE_GOLD_ORE) ||
               state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE) ||
               state.isOf(Blocks.DEEPSLATE_EMERALD_ORE) ||
               state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE) ||
               state.isOf(Blocks.DEEPSLATE_LAPIS_ORE) ||
               state.isOf(Blocks.DEEPSLATE_COPPER_ORE) ||
                state.isOf(Blocks.RAW_COPPER_BLOCK) ||
                state.isOf(Blocks.RAW_GOLD_BLOCK) ||
                state.isOf(Blocks.RAW_IRON_BLOCK);
    }

    /**
     * Finds the starter block position
     * Should be within 16 chunks of player and be bellow y=48
     * Should be on ground of cave
     * @param world The world to search in
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
            if (!ChunkLoader.loadChunksInRadius((net.minecraft.server.world.ServerWorld)world, targetPos, 1)) {
                continue; // Skip if chunk couldn't be loaded
            }

            // look 5 blocks down from this block for the first air block
            for (int i = 0; i <= 5; i++) {
                BlockPos checkPos = new BlockPos(x, y - i, z);
                if (checkPos.getY() < minY || checkPos.getY() > maxY) {
                    continue;
                }
                BlockState state = world.getBlockState(checkPos);
                if (state.isOf(Blocks.CAVE_AIR)) {
                    // keep looking down until we find a solid block
                    BlockPos belowPos = checkPos.down();
                    BlockState belowState = world.getBlockState(belowPos);
                    while (belowPos.getY() >= minY && belowPos.getY() <= maxY && belowState.isOf(Blocks.CAVE_AIR)) {
                        belowPos = belowPos.down();
                        belowState = world.getBlockState(belowPos);
                    }
                    if (belowState.isSolidBlock(world, belowPos)) {
                        return BlockPos.ofFloored(x, belowPos.getY() + 1, z); // Return the air block above the solid block
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
     * @param world The world to mine in
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
            if (currentPos.getY() >= 100) {
                break;
            }
            stairBlocks.add(currentPos);
            currentPos = currentPos.up().north(); // Move up 1 and forward 1
            stairLength++;
        }
        // Set the 3 blocks above each stair block to air
        for (BlockPos stairPos : stairBlocks) {
            if(world.getBlockState(stairPos).isOf(Blocks.CAVE_AIR)) {
                world.setBlockState(stairPos, Blocks.COBBLESTONE.getDefaultState());
            }
            for (int i = 1; i <= 3; i++) {
                BlockPos abovePos = stairPos.up(i);
                if (abovePos.getY() < world.getTopY()) {
                    world.setBlockState(abovePos, Blocks.AIR.getDefaultState());
                }
            }
        }
        return stairLength;
    }



    /**
     * Main method to pre-mine a cave near the player.
     * @param world The world to mine in
     * @param playerPos The player's position
     * @param player The player entity
     * @return True if a cave was successfully pre-mined, false otherwise
     */
    public static boolean preMineCave(World world, BlockPos playerPos, PlayerEntity player) {
        BlockPos starterPos = findStarterBlock(world, playerPos); //FIXME: only finds mineshafts due to cave_air, make more general / look into cave spawning specifics
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
        HorrorMod129.LOGGER.info("Cave Pre-Miner: Mined " + oresMined + " ores, placed " + torchesPlaced + " torches, extra block placed: " + extraBlockPlaced + ", stair length: " + stairLength);
        return true;
    }
        
}