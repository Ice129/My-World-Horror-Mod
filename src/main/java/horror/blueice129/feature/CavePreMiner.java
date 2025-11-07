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
import horror.blueice129.utils.StructurePlacer;

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

        boolean isInLineOfSight = LineOfSightUtils.isBlockRenderedOnScreen(player, pos, 16 * 10); // 10 chunks

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
        
        // Cache for solid block checks to avoid repeated world access
        java.util.Map<BlockPos, Boolean> solidBelowCache = new java.util.HashMap<>();
        
        // Define directions once to avoid recreating the array in each iteration
        net.minecraft.util.math.Direction[] DIRECTIONS = net.minecraft.util.math.Direction.values();
        
        // Define max distance squared for faster distance checks (no square root calculation)
        int maxDistanceSquared = 50 * 50;
        
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            caveAirBlocks.add(currentPos);
            
            // Check and cache if current position has solid below
            boolean currentHasSolidBelow = checkAndCacheSolidBelow(world, currentPos, solidBelowCache);
            if (!currentHasSolidBelow) continue; // Skip neighbors if current has no solid below

            for (net.minecraft.util.math.Direction direction : DIRECTIONS) {
                BlockPos neighborPos = currentPos.offset(direction);

                if (visited.contains(neighborPos)) continue;
                
                // Check if neighbor is within 50 blocks of start position using squared distance
                if (neighborPos.getSquaredDistance(startPos) > maxDistanceSquared) continue;
                
                // Skip anything above the hard cutoff
                if (neighborPos.getY() > 55) continue;

                // accept both normal air and cave air
                BlockState neighborState = world.getBlockState(neighborPos);
                if (!neighborState.isOf(Blocks.CAVE_AIR) && !neighborState.isOf(Blocks.AIR)) continue;
                
                // We already know current position has solid below, so this neighbor is valid
                queue.add(neighborPos);
                visited.add(neighborPos);
            }
        }
        return caveAirBlocks;
    }
    
    /**
     * Helper method to check and cache if a position has solid blocks below
     */
    private static boolean checkAndCacheSolidBelow(World world, BlockPos pos, java.util.Map<BlockPos, Boolean> cache) {
        // Check cache first to avoid world access
        if (cache.containsKey(pos)) {
            return cache.get(pos);
        }
        
        // Check for solid blocks below
        boolean hasSolidBelow = false;
        for (int i = 1; i <= 8; i++) {
            BlockPos belowPos = pos.down(i);
            if (belowPos.getY() > 55) continue; // don't test surface blocks
            
            // Check if we already know this position is solid
            if (cache.containsKey(belowPos) && cache.get(belowPos)) {
                hasSolidBelow = true;
                break;
            }
            
            BlockState state = world.getBlockState(belowPos);
            if (state.isSolidBlock(world, belowPos)) {
                hasSolidBelow = true;
                // Cache this solid block to speed up future checks
                cache.put(belowPos, true);
                break;
            }
        }
        
        // Cache the result
        cache.put(pos, hasSolidBelow);
        return hasSolidBelow;
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
        net.minecraft.util.math.Direction[] DIRECTIONS = net.minecraft.util.math.Direction.values();
        
        // Use a more efficient approach: find ore blocks adjacent to cave air first
        java.util.Set<BlockPos> oreBlocksToCheck = new java.util.HashSet<>();
        
        // First pass: identify only the ore blocks directly adjacent to cave air
        for (BlockPos airPos : caveAirBlocks) {
            for (net.minecraft.util.math.Direction direction : DIRECTIONS) {
                BlockPos neighborPos = airPos.offset(direction);
                
                if (!visited.contains(neighborPos)) {
                    visited.add(neighborPos);
                    BlockState state = world.getBlockState(neighborPos);
                    if (BlockTypes.isOreBlock(state)) {
                        oreBlocksToCheck.add(neighborPos);
                    }
                }
            }
        }
        
        // Second pass: process only ore blocks and their connections
        visited.clear(); // Reset visited set for the second pass
        java.util.Queue<BlockPos> oreQueue = new java.util.LinkedList<>(oreBlocksToCheck);
        
        while (!oreQueue.isEmpty()) {
            BlockPos currentPos = oreQueue.poll();
            
            if (visited.contains(currentPos)) continue;
            visited.add(currentPos);
            
            BlockState state = world.getBlockState(currentPos);
            if (BlockTypes.isOreBlock(state)) {
                // Mine the ore block if not in view
                if (!LineOfSightUtils.isBlockRenderedOnScreen(player, currentPos, 16 * 10)) { // 10 chunks
                    world.breakBlock(currentPos, false);
                    oresMined++;
                }
                
                // Check neighboring blocks for connected ores
                for (net.minecraft.util.math.Direction direction : DIRECTIONS) {
                    BlockPos neighborPos = currentPos.offset(direction);
                    if (!visited.contains(neighborPos)) {
                        BlockState neighborState = world.getBlockState(neighborPos);
                        if (BlockTypes.isOreBlock(neighborState)) {
                            oreQueue.add(neighborPos);
                        }
                    }
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
        int gridSize = minTorchDistance; // Grid cell size matches minimum torch distance

        // Use a spatial grid for faster distance checks - O(1) lookup instead of O(n)
        java.util.Map<Long, BlockPos> torchGrid = new java.util.HashMap<>();
        
        // Helper function to get grid key
        java.util.function.Function<BlockPos, Long> getGridKey = (BlockPos p) -> {
            int gridX = Math.floorDiv(p.getX(), gridSize);
            int gridY = Math.floorDiv(p.getY(), gridSize);
            int gridZ = Math.floorDiv(p.getZ(), gridSize);
            return (((long)gridX) << 40) | (((long)gridY & 0xFFFFL) << 20) | (gridZ & 0xFFFFL);
        };
        
        // Convert to list for easier shuffling and sorting
        java.util.List<BlockPos> positions = new java.util.ArrayList<>(caveAirBlocks);

        // Sort by light level (darkest first) - keep this for quality results
        positions.sort(Comparator.comparingInt(world::getLightLevel));

        boolean firstPos = true;

        for (BlockPos pos : positions) {
            if (firstPos) {
                firstPos = false;
                continue; // Skip the first position to avoid placing where the stairs are
            }
            // Hard cutoff: do not place torches above Y=55 (surface differentiation)
            if (pos.getY() > 55)
                continue;
            // Check if this position is still dark enough for a torch
            if (world.getLightLevel(pos) > 4) {
                continue; // Skip if the area is already lit by previously placed torches
            }

            // Check nearby grid cells for existing torches - O(1) instead of O(n)
            boolean tooClose = false;
            long posKey = getGridKey.apply(pos);
            int checkRadius = 1; // Check surrounding grid cells
            
            for (int dx = -checkRadius; dx <= checkRadius && !tooClose; dx++) {
                for (int dy = -checkRadius; dy <= checkRadius && !tooClose; dy++) {
                    for (int dz = -checkRadius; dz <= checkRadius && !tooClose; dz++) {
                        long neighborKey = posKey + (((long)dx) << 40) | (((long)dy & 0xFFFFL) << 20) | (dz & 0xFFFFL);
                        BlockPos existing = torchGrid.get(neighborKey);
                        if (existing != null && pos.getSquaredDistance(existing) < minTorchDistance * minTorchDistance) {
                            tooClose = true;
                        }
                    }
                }
            }

            if (!tooClose && isSuitableForTorch(world, pos, player, caveAirBlocks)) {
                // Place a torch
                world.setBlockState(pos, Blocks.TORCH.getDefaultState(), 3);
                torchesPlaced++;
                torchGrid.put(getGridKey.apply(pos), pos);

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
        boolean isInLineOfSight = LineOfSightUtils.isBlockRenderedOnScreen(player, pos, 16 * 10); // 10 chunks

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
        
        // Start with player's Y level and then try in expanding rings
        int playerY = playerPos.getY();
        int idealY = Math.max(minY + 5, Math.min(maxY - 5, playerY - 10)); // Prefer caves below player
        
        // Use a more structured search pattern
        // Try a spiral pattern for x,z coordinates starting close to player
        int[] searchDistances = {16, 32, 48, 64, 80}; // 1, 2, 3, 4, 5 chunks
        int[] yOffsets = {0, -5, -10, -15, 5, 10, 15}; // Try different Y levels
        
        // Create a cache of already checked positions to avoid redundant checks
        java.util.Set<BlockPos> checkedPositions = new java.util.HashSet<>();
        
        for (int distance : searchDistances) {
            // Try 8 points around the circle at this distance
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0; // 0, 45, 90, 135, 180, 225, 270, 315 degrees
                int xOffset = (int) (Math.cos(angle) * distance);
                int zOffset = (int) (Math.sin(angle) * distance);
                
                // Try different Y levels at this x,z coordinate
                for (int yOffset : yOffsets) {
                    int x = playerPos.getX() + xOffset;
                    int z = playerPos.getZ() + zOffset;
                    int y = idealY + yOffset;
                    
                    if (y < minY || y > maxY) continue;
                    
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (checkedPositions.contains(checkPos)) continue;
                    checkedPositions.add(checkPos);
                    
                    // Make sure the chunk is loaded before accessing blocks
                    if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, checkPos, 1)) {
                        continue;
                    }
                    
                    // Check if this is a suitable air block with solid below
                    BlockState state = world.getBlockState(checkPos);
                    if ((state.isOf(Blocks.CAVE_AIR) || state.isOf(Blocks.AIR))) {
                        // Find solid ground below
                        BlockPos solidPos = findSolidBlockBelow(world, checkPos, minY, maxY);
                        if (solidPos != null) {
                            return solidPos.up(); // Return the air block above the solid block
                        }
                    } else {
                        // Try looking down from here for air
                        BlockPos airPos = findAirBlockBelow(world, checkPos, minY, maxY);
                        if (airPos != null) {
                            BlockPos solidPos = findSolidBlockBelow(world, airPos, minY, maxY);
                            if (solidPos != null) {
                                return solidPos.up(); // Return air above solid
                            }
                        }
                    }
                }
            }
        }
        
        // Fallback to a few random attempts if structured search fails
        for (int attempt = 0; attempt < 20; attempt++) {
            int xOffset = random.nextInt(2 * maxDistance + 1) - maxDistance;
            int zOffset = random.nextInt(2 * maxDistance + 1) - maxDistance;
            int yOffset = random.nextInt(30) - 15;
            
            int x = playerPos.getX() + xOffset;
            int z = playerPos.getZ() + zOffset;
            int y = playerPos.getY() + yOffset;
            
            BlockPos targetPos = new BlockPos(x, y, z);
            if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, targetPos, 1)) continue;
            
            // Find air and solid ground
            BlockPos airPos = findAirBlockBelow(world, targetPos, minY, maxY);
            if (airPos != null) {
                BlockPos solidPos = findSolidBlockBelow(world, airPos, minY, maxY);
                if (solidPos != null) {
                    return solidPos.up();
                }
            }
        }
        
        return null; // If we couldn't find a suitable position
    }
    
    /**
     * Helper method to find a solid block below the given position
     */
    private static BlockPos findSolidBlockBelow(World world, BlockPos startPos, int minY, int maxY) {
        BlockPos pos = startPos;
        while (pos.getY() >= minY) {
            BlockState state = world.getBlockState(pos);
            if (state.isSolidBlock(world, pos)) {
                return pos;
            }
            pos = pos.down();
        }
        return null;
    }
    
    /**
     * Helper method to find an air block below the given position
     */
    private static BlockPos findAirBlockBelow(World world, BlockPos startPos, int minY, int maxY) {
        // Look up to 5 blocks down
        for (int i = 0; i <= 5; i++) {
            BlockPos checkPos = startPos.down(i);
            if (checkPos.getY() < minY || checkPos.getY() > maxY) continue;
            
            BlockState state = world.getBlockState(checkPos);
            if (state.isOf(Blocks.CAVE_AIR) || state.isOf(Blocks.AIR)) {
                return checkPos;
            }
        }
        return null;
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
            // if y value is getting too high, stop
            if (currentPos.getY() >= 130) {
                break;
            }
            stairBlocks.add(currentPos);
            currentPos = currentPos.up().north(); // Move up 1 and forward 1
            stairLength++;
        }
        
        // Find surface entrance position (last position at or above surface)
        BlockPos entrancePos = null;
        for (BlockPos pos : stairBlocks) {
            int surfaceY = SurfaceFinder.findPointSurfaceY((ServerWorld) world,
                    pos.getX(), pos.getZ(), true, false, true);
            if (surfaceY != -1 && pos.getY() >= surfaceY) {
                entrancePos = pos;
                break;
            }
        }
        
        // Set the 3 blocks above each stair block to air
        int torchDistance = 0;
        for (BlockPos stairPos : stairBlocks) {
            // Check if we're at or above surface
            int surfaceY = SurfaceFinder.findPointSurfaceY((ServerWorld) world,
                    stairPos.getX(), stairPos.getZ(), true, false, true);
            boolean isAtOrAboveSurface = (surfaceY != -1 && stairPos.getY() >= surfaceY);
            
            // Only place cobblestone if below surface and the block is air
            if ((world.getBlockState(stairPos).isOf(Blocks.CAVE_AIR) || world.getBlockState(stairPos).isOf(Blocks.AIR)) 
                    && !isAtOrAboveSurface) {
                world.setBlockState(stairPos, Blocks.COBBLESTONE.getDefaultState());
                world.setBlockState(stairPos.north(), Blocks.COBBLESTONE.getDefaultState());
            }

            // break if is above surface
            if (isAtOrAboveSurface) {
                break;
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
        
        // Add torches around entrance if we found one
        if (entrancePos != null) {
            placeEntranceTorches(world, entrancePos);
        }
        
        return stairLength;
    }
    
    /**
     * Places torches randomly around the staircase entrance to make it more visible
     * 
     * @param world       The world to place torches in
     * @param entrancePos The position of the entrance
     */
    private static void placeEntranceTorches(World world, BlockPos entrancePos) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Determine the number of torches to place (5-8)
        int torchCount = 5 + random.nextInt(4);
        int torchesPlaced = 0;
        int attempts = 0;
        
        // Track placed positions to avoid placing torches too close together
        java.util.Set<BlockPos> placedPositions = new java.util.HashSet<>();
        
        // Try to place the desired number of torches with a maximum number of attempts
        while (torchesPlaced < torchCount && attempts < 30) {
            attempts++;
            
            // Find a random location on the surface near the entrance
            BlockPos surfacePos = StructurePlacer.findSurfaceLocation(serverWorld, entrancePos, 2, 6);
            
            if (surfacePos != null) {
                // Check if this position is too close to existing torches
                boolean tooClose = false;
                for (BlockPos existingPos : placedPositions) {
                    if (surfacePos.getSquaredDistance(existingPos) < 4) {
                        tooClose = true;
                        break;
                    }
                }
                
                if (!tooClose) {
                    // Place a torch at this position
                    world.setBlockState(surfacePos.up(), Blocks.TORCH.getDefaultState(), 3);
                    placedPositions.add(surfacePos);
                    torchesPlaced++;
                }
            }
        }
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