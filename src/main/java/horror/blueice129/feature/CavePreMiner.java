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
import horror.blueice129.utils.TorchPlacer;
import horror.blueice129.data.HorrorModPersistentState;

public class CavePreMiner {

    // Create a Random object for generating random values
    private static final Random random = Random.create();

    /**
     * Checks if a position is roughly within the player's field of view cone (wider than actual FOV)
     * Uses a generous angle of ~100 degrees to catch edge cases
     * 
     * @param player The player to check against
     * @param pos The position to check
     * @return True if the position is within the wider FOV cone
     */
    private static boolean isInWideFOVCone(PlayerEntity player, BlockPos pos) {
        // Get player's look direction
        net.minecraft.util.math.Vec3d lookVec = player.getRotationVector();
        
        // Vector from player to position
        net.minecraft.util.math.Vec3d toPos = new net.minecraft.util.math.Vec3d(
            pos.getX() - player.getX(),
            pos.getY() - player.getEyeY(),
            pos.getZ() - player.getZ()
        ).normalize();
        
        // Dot product gives cosine of angle between vectors
        // cos(100°) ≈ -0.17, so we use a wider cone than normal FOV
        double dotProduct = lookVec.dotProduct(toPos);
        return dotProduct > -0.2; // Very wide cone to catch edge cases
    }

    /**
     * Enhanced visibility check that also checks nearby blocks
     * Only used for torches within the FOV cone for performance
     * 
     * @param player The player to check against
     * @param pos The torch position
     * @return True if torch or nearby blocks are visible
     */
    private static boolean isEnhancedVisible(PlayerEntity player, BlockPos pos) {
        // Check the torch position itself
        if (LineOfSightUtils.isBlockRenderedOnScreen(player, pos, 16 * 10)) {
            return true;
        }
        
        // Check blocks 1-2 blocks up
        for (int dy = 1; dy <= 2; dy++) {
            BlockPos upPos = pos.up(dy);
            if (LineOfSightUtils.isBlockRenderedOnScreen(player, upPos, 16 * 10)) {
                return true;
            }
        }
        
        // Check within 4 block radius at ground level and 1 layer up
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue; // Already checked
                    if (dx * dx + dz * dz > 16) continue; // Keep within 4 block radius
                    
                    BlockPos nearPos = pos.add(dx, dy, dz);
                    if (LineOfSightUtils.isBlockRenderedOnScreen(player, nearPos, 16 * 10)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

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
            java.util.Set<BlockPos> caveAirSet) {
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
        boolean isAirBlock = caveAirSet.contains(pos) || world.getBlockState(pos).isOf(Blocks.AIR);

        // Check if the light level is less than or equal to 4
        int lightLevel = world.getLightLevel(pos);
        boolean isLowLight = lightLevel <= 4;

        // Use enhanced visibility check for torches in FOV cone, simple check otherwise
        boolean isInLineOfSight;
        if (isInWideFOVCone(player, pos)) {
            // Enhanced check for torches in FOV - also checks nearby blocks
            isInLineOfSight = isEnhancedVisible(player, pos);
        } else {
            // Simple check for torches outside FOV
            isInLineOfSight = LineOfSightUtils.isBlockRenderedOnScreen(player, pos, 16 * 10);
        }

        return isSolidTop && isLowLight && !isInLineOfSight && isAirBlock;
    }

    /**
     * Result container for cave exploration that includes both cave air blocks and ore count
     */
    public static class CaveExplorationResult {
        public final java.util.List<BlockPos> caveAirBlocks;
        public final int oresMined;

        public CaveExplorationResult(java.util.List<BlockPos> caveAirBlocks, int oresMined) {
            this.caveAirBlocks = caveAirBlocks;
            this.oresMined = oresMined;
        }
    }

    /**
     * Finds all connected cave air blocks from startPos AND mines exposed ores in a single pass
     * Fills up to 4 blocks above ground, searches 8 blocks down to find ground
     * 
     * @param world    The world to check in
     * @param startPos The position to start from
     * @param player   The player to check line of sight against for ore mining
     * @return CaveExplorationResult containing the list of cave air blocks and count of ores mined
     */
    public static CaveExplorationResult findCaveAirAndMineOres(World world, BlockPos startPos, PlayerEntity player) {
        java.util.List<BlockPos> caveAirBlocks = new java.util.ArrayList<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        int oresMined = 0;

        // Cache for solid block checks to avoid repeated world access
        java.util.Map<BlockPos, Boolean> solidBelowCache = new java.util.HashMap<>();
        // Cache for ground level (Y coordinate of solid block below)
        java.util.Map<BlockPos, Integer> groundLevelCache = new java.util.HashMap<>();

        // Track ore blocks we've already processed to avoid mining the same vein multiple times
        java.util.Set<BlockPos> processedOres = new java.util.HashSet<>();

        // Define directions once to avoid recreating the array in each iteration
        net.minecraft.util.math.Direction[] DIRECTIONS = net.minecraft.util.math.Direction.values();

        // Define max distance squared for faster distance checks (no square root calculation)
        int maxDistanceSquared = 50 * 50;

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            caveAirBlocks.add(currentPos);

            // Check and cache if current position has solid below (searches down 8 blocks)
            Integer groundLevel = findAndCacheGroundLevel(world, currentPos, solidBelowCache, groundLevelCache);
            if (groundLevel == null)
                continue; // Skip neighbors if current has no solid below

            // While exploring cave air, also check adjacent blocks for exposed ores
            for (net.minecraft.util.math.Direction direction : DIRECTIONS) {
                BlockPos neighborPos = currentPos.offset(direction);

                // Skip anything above the hard cutoff (cheap check - do first)
                if (neighborPos.getY() > 55)
                    continue;

                // Check if neighbor is within 50 blocks of start position using squared distance
                if (neighborPos.getSquaredDistance(startPos) > maxDistanceSquared)
                    continue;

                // Check if already visited as cave air OR already processed as ore
                if (visited.contains(neighborPos) || processedOres.contains(neighborPos))
                    continue;

                // Get neighbor state once for efficiency (expensive operation - do last)
                BlockState neighborState = world.getBlockState(neighborPos);

                // Check if neighbor is air (continue cave exploration)
                if (neighborState.isOf(Blocks.CAVE_AIR) || neighborState.isOf(Blocks.AIR)) {
                    if (direction == net.minecraft.util.math.Direction.UP) {
                        // Limit upward expansion to 4 blocks above ground
                        if (neighborPos.getY() - groundLevel > 4)
                            continue;
                    }

                    queue.add(neighborPos);
                    visited.add(neighborPos);
                }
                // Check if neighbor is an exposed ore block
                else if (BlockTypes.isOreBlock(neighborState)) {
                    oresMined += mineOreVein(world, neighborPos, player, processedOres);
                }
            }
        }
        
        return new CaveExplorationResult(caveAirBlocks, oresMined);
    }

    /**
     * Mines an entire ore vein starting from the given ore block
     * 
     * @param world         The world to mine in
     * @param startOre      The starting ore block position
     * @param player        The player to check line of sight against
     * @param processedOres Set of already processed ore positions to update
     * @return The number of ore blocks mined in this vein
     */
    private static int mineOreVein(World world, BlockPos startOre, PlayerEntity player, 
                                   java.util.Set<BlockPos> processedOres) {
        int mined = 0;
        java.util.Queue<BlockPos> oreQueue = new java.util.LinkedList<>();
        net.minecraft.util.math.Direction[] DIRECTIONS = net.minecraft.util.math.Direction.values();

        oreQueue.add(startOre);
        processedOres.add(startOre);

        while (!oreQueue.isEmpty()) {
            BlockPos currentOre = oreQueue.poll();
            BlockState state = world.getBlockState(currentOre);

            if (BlockTypes.isOreBlock(state)) {
                // Mine the ore block if not in view
                if (!LineOfSightUtils.isBlockRenderedOnScreen(player, currentOre, 16 * 10)) {
                    world.breakBlock(currentOre, false);
                    mined++;                    
                    // 25% chance to break an adjacent non-ore block
                    if (random.nextInt(100) < 25) {
                        java.util.List<net.minecraft.util.math.Direction> shuffledDirs = java.util.Arrays.asList(DIRECTIONS);
                        java.util.Collections.shuffle(shuffledDirs);
                        for (net.minecraft.util.math.Direction dir : shuffledDirs) {
                            BlockPos adjacentPos = currentOre.offset(dir);
                            BlockState adjacentState = world.getBlockState(adjacentPos);
                            if (!BlockTypes.isOreBlock(adjacentState) && 
                                !adjacentState.isAir() && 
                                !LineOfSightUtils.isBlockRenderedOnScreen(player, adjacentPos, 16 * 10)) {
                                world.breakBlock(adjacentPos, false);
                                break; // Only break one adjacent block
                            }
                        }
                    }                }

                // Check neighboring blocks for connected ores
                for (net.minecraft.util.math.Direction direction : DIRECTIONS) {
                    BlockPos neighborOre = currentOre.offset(direction);
                    if (!processedOres.contains(neighborOre)) {
                        BlockState neighborState = world.getBlockState(neighborOre);
                        if (BlockTypes.isOreBlock(neighborState)) {
                            oreQueue.add(neighborOre);
                            processedOres.add(neighborOre);
                        }
                    }
                }
            }
        }

        return mined;
    }

    /**
     * Helper method to find and cache the ground level (Y coordinate) below a
     * position
     * Searches down 8 blocks to find solid ground
     * 
     * @return The Y coordinate of the ground, or null if no ground found within 8
     *         blocks
     */
    private static Integer findAndCacheGroundLevel(World world, BlockPos pos,
            java.util.Map<BlockPos, Boolean> solidCache, java.util.Map<BlockPos, Integer> groundCache) {
        // Check cache first
        if (groundCache.containsKey(pos)) {
            return groundCache.get(pos);
        }

        // Search down up to 8 blocks for solid ground
        for (int i = 1; i <= 8; i++) {
            BlockPos belowPos = pos.down(i);
            if (belowPos.getY() > 55)
                continue; // don't test surface blocks

            // Check if we already know this position is solid
            boolean isSolid = false;
            if (solidCache.containsKey(belowPos) && solidCache.get(belowPos)) {
                isSolid = true;
            } else {
                BlockState state = world.getBlockState(belowPos);
                if (state.isSolidBlock(world, belowPos)) {
                    isSolid = true;
                    solidCache.put(belowPos, true);
                }
            }

            if (isSolid) {
                int groundY = belowPos.getY();
                groundCache.put(pos, groundY);
                return groundY;
            }
        }

        // No ground found
        return null;
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
        int minTorchDistance = 10;
        int gridSize = minTorchDistance;
        int minTorchDistSquared = minTorchDistance * minTorchDistance;

        java.util.Map<Long, BlockPos> torchGrid = new java.util.HashMap<>();
        
        java.util.Set<BlockPos> caveAirSet = new java.util.HashSet<>(caveAirBlocks);

        // Cache light levels to avoid querying world multiple times for same position
        java.util.Map<BlockPos, Integer> lightLevelCache = new java.util.HashMap<>();
        for (BlockPos pos : caveAirBlocks) {
            lightLevelCache.put(pos, world.getLightLevel(pos));
        }

        // Convert to list and sort by cached light level (darkest first)
        java.util.List<BlockPos> positions = new java.util.ArrayList<>(caveAirBlocks);
        positions.sort(Comparator.comparingInt(p -> lightLevelCache.getOrDefault(p, 15)));

        boolean firstPos = true;

        for (BlockPos pos : positions) {
            if (firstPos) {
                firstPos = false;
                continue; // Skip the first position to avoid placing where the stairs are
            }
            // Hard cutoff: do not place torches above Y=55 (surface)
            if (pos.getY() > 55)
                continue;
            // Check if this position is still dark enough for a torch (use cached value)
            if (lightLevelCache.getOrDefault(pos, 15) > 2) {
                continue; // Skip if the area is already lit by previously placed torches
            }

            // Simplified grid key calculation
            int gridX = Math.floorDiv(pos.getX(), gridSize);
            int gridY = Math.floorDiv(pos.getY(), gridSize);
            int gridZ = Math.floorDiv(pos.getZ(), gridSize);

            // Check nearby grid cells for existing torches
            boolean tooClose = false;
            int checkRadius = 1;

            for (int dx = -checkRadius; dx <= checkRadius && !tooClose; dx++) {
                for (int dy = -checkRadius; dy <= checkRadius && !tooClose; dy++) {
                    for (int dz = -checkRadius; dz <= checkRadius && !tooClose; dz++) {
                        long neighborKey = (((long)(gridX + dx)) << 40) | 
                                         (((long)(gridY + dy) & 0xFFFFL) << 20) | 
                                         ((gridZ + dz) & 0xFFFFL);
                        BlockPos existing = torchGrid.get(neighborKey);
                        if (existing != null && pos.getSquaredDistance(existing) < minTorchDistSquared) {
                            tooClose = true;
                        }
                    }
                }
            }

            if (!tooClose && isSuitableForTorch(world, pos, player, caveAirSet)) {
                // 20% chance to skip placing this torch
                if (random.nextInt(100) < 20) {
                    continue;
                }
                
                // Randomize position within 3 block radius to break up grid patterns
                BlockPos torchPos = pos;
                int attempts = 0;
                boolean placed = false;
                
                while (attempts < 10 && !placed) {
                    int offsetX = random.nextInt(7) - 3;
                    int offsetZ = random.nextInt(7) - 3;
                    BlockPos horizontalPos = new BlockPos(pos.getX() + offsetX, pos.getY(), pos.getZ() + offsetZ);
                    
                    BlockPos groundPos = null;
                    
                    for (int dy = 0; dy <= 8; dy++) {
                        BlockPos checkBelow = horizontalPos.down(dy);
                        if (checkBelow.getY() < world.getBottomY() || checkBelow.getY() > 55) {
                            continue;
                        }
                        if (world.getBlockState(checkBelow).isSideSolidFullSquare(world, checkBelow, net.minecraft.util.math.Direction.UP)) {
                            groundPos = checkBelow.up();
                            break;
                        }
                    }
                    
                    if (groundPos == null) {
                        for (int dy = 1; dy <= 4; dy++) {
                            BlockPos checkAbove = horizontalPos.up(dy);
                            if (checkAbove.getY() > 55) {
                                break;
                            }
                            BlockPos belowCheckAbove = checkAbove.down();
                            if (world.getBlockState(belowCheckAbove).isSideSolidFullSquare(world, belowCheckAbove, net.minecraft.util.math.Direction.UP)) {
                                groundPos = checkAbove;
                                break;
                            }
                        }
                    }
                    
                    if (groundPos != null && groundPos.getY() <= 55 && isSuitableForTorch(world, groundPos, player, caveAirSet)) {
                        torchPos = groundPos;
                        placed = TorchPlacer.placeTorch(world, torchPos, random, player);
                        break;
                    }
                    attempts++;
                }
                
                if (!placed) {
                    placed = TorchPlacer.placeTorch(world, pos, random, player);
                    torchPos = pos;
                }
                
                if (placed) {
                    torchesPlaced++;
                    long gridKey = (((long)gridX) << 40) | (((long)gridY & 0xFFFFL) << 20) | (gridZ & 0xFFFFL);
                    torchGrid.put(gridKey, torchPos);
                }

                // Force block update to update lighting and update cache
                world.updateNeighbors(torchPos, Blocks.TORCH);
                // Update light level cache for nearby positions affected by the new torch
                for (int dx = -15; dx <= 15; dx++) {
                    for (int dy = -15; dy <= 15; dy++) {
                        for (int dz = -15; dz <= 15; dz++) {
                            BlockPos nearPos = pos.add(dx, dy, dz);
                            if (lightLevelCache.containsKey(nearPos)) {
                                lightLevelCache.put(nearPos, world.getLightLevel(nearPos));
                            }
                        }
                    }
                }
            }
        }

        return torchesPlaced;
    }

    /**
     * Chance to place crafting table, furnace, or cobblestone pillar, or nothing
     * Can place up to 2 extra blocks
     * 
     * @param world         The world to place the block in
     * @param caveAirBlocks The list of cave air blocks to check against
     * @param player        The player to check line of sight against
     * @return Number of extra blocks placed
     */

    public static int placeExtraBlocks(World world, java.util.List<BlockPos> caveAirBlocks, PlayerEntity player) {
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
            return 0;

        // Shuffle positions for randomness
        java.util.Collections.shuffle(suitablePositions);

        int blocksPlaced = 0;
        int maxBlocks = random.nextInt(3); // 0, 1, or 2 blocks

        for (int i = 0; i < maxBlocks && i < suitablePositions.size(); i++) {
            BlockPos target = suitablePositions.get(i);

            int roll = random.nextInt(100);
            boolean placed = false;
            
            if (roll < 20) {
                placed = placeFurnaceAt(world, target);
            } else if (roll < 60) {
                placed = placeCraftingTableAt(world, target);
            } else if (roll < 90) {
                placed = placeCobblestonePillarAt(world, target);
            }

            if (placed) {
                blocksPlaced++;
            }
        }

        return blocksPlaced;
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
        BlockPos above = pos;
        if (above.getY() > 55)
            return false;
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, above, 1))
            return false;
        if (!world.getBlockState(above).isAir())
            return false;
        world.setBlockState(above, Blocks.CRAFTING_TABLE.getDefaultState(), 3);
        return true;
    }

    // Helper: place a 3-block tall cobblestone pillar starting at pos. Returns
    // true if at least one block placed.
    private static boolean placeCobblestonePillarAt(World world, BlockPos pos) {
        BlockPos base = pos;
        if (base.getY() > 55)
            return false;
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, base, 1))
            return false;

        boolean placedAny = false;
        for (int i = 0; i < 4; i++) {
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
     * Finds the starter block position by searching from far to near
     * Finds the furthest suitable cave within loaded chunks
     * Should be below y=48 and on ground of cave
     * 
     * @param world     The world to search in
     * @param playerPos The player's position
     * @return A suitable BlockPos, or null if none found
     */
    public static BlockPos findStarterBlock(World world, BlockPos playerPos) {
        int chunkRadius = 20;
        int maxDistance = chunkRadius * 16;
        int minY = world.getBottomY();
        int maxY = Math.min(48, world.getTopY() - 1);
        int playerY = playerPos.getY();
        int idealY = Math.max(minY + 5, Math.min(maxY - 5, playerY - 10));
        
        int stepSize = 16;
        int angularSteps = 8;
        int ySearchRange = 30;
        int yStepSize = 5;
        
        int[] yOffsets = new int[ySearchRange / yStepSize * 2 + 1];
        for (int i = 0; i < yOffsets.length; i++) {
            yOffsets[i] = (i - yOffsets.length / 2) * yStepSize;
        }
        
        java.util.Set<BlockPos> checkedPositions = new java.util.HashSet<>();
        
        for (int distance = maxDistance; distance >= stepSize; distance -= stepSize) {
            for (int i = 0; i < angularSteps; i++) {
                double angle = (2 * Math.PI * i) / angularSteps;
                int xOffset = (int) (distance * Math.cos(angle));
                int zOffset = (int) (distance * Math.sin(angle));
                
                for (int yOffset : yOffsets) {
                    int x = playerPos.getX() + xOffset;
                    int z = playerPos.getZ() + zOffset;
                    int y = idealY + yOffset;
                    
                    if (y < minY || y > maxY)
                        continue;
                    
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (checkedPositions.contains(checkPos))
                        continue;
                    checkedPositions.add(checkPos);
                    
                    if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, checkPos, 1)) {
                        continue;
                    }
                    
                    BlockState state = world.getBlockState(checkPos);
                    if ((state.isOf(Blocks.CAVE_AIR) || state.isOf(Blocks.AIR))) {
                        BlockPos solidPos = findSolidBlockBelow(world, checkPos, minY, maxY);
                        if (solidPos != null) {
                            return solidPos.up();
                        }
                    } else {
                        BlockPos airPos = findAirBlockBelow(world, checkPos, minY, maxY);
                        if (airPos != null) {
                            BlockPos solidPos = findSolidBlockBelow(world, airPos, minY, maxY);
                            if (solidPos != null) {
                                return solidPos.up();
                            }
                        }
                    }
                }
            }
        }
        
        return null;
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
            if (checkPos.getY() < minY || checkPos.getY() > maxY)
                continue;

            BlockState state = world.getBlockState(checkPos);
            if (state.isOf(Blocks.CAVE_AIR) || state.isOf(Blocks.AIR)) {
                return checkPos;
            }
        }
        return null;
    }

    /**
     * Moves a BlockPos up 1 and forward 1 in the specified cardinal direction
     * 
     * @param pos       The current position
     * @param direction The cardinal direction ("N", "E", "S", "W")
     * @return The new position moved up 1 and forward 1 in the direction
     */
    private static BlockPos moveInDirection(BlockPos pos, String direction) {
        switch(direction) {
            case "N":
                return pos.up().north();
            case "E":
                return pos.up().east();
            case "S":
                return pos.up().south();
            case "W":
                return pos.up().west();
            default:
                return pos.up().north();
        }
    }

    /**
     * Rotates a cardinal direction 90 degrees left or right
     * 
     * @param direction The current cardinal direction ("N", "E", "S", "W")
     * @param left      True to rotate left (counterclockwise), false for right (clockwise)
     * @return The new direction after rotation
     */
    private static String rotate90Degrees(String direction, boolean left) {
        String[] cardinals = {"N", "E", "S", "W"};
        int index = java.util.Arrays.asList(cardinals).indexOf(direction);
        if (left) {
            index = (index - 1 + 4) % 4;
        } else {
            index = (index + 1) % 4;
        }
        return cardinals[index];
    }

    /**
     * Gets the offset BlockPos in a cardinal direction (without moving up)
     * 
     * @param pos       The current position
     * @param direction The cardinal direction ("N", "E", "S", "W")
     * @return The position offset by 1 in the direction
     */
    private static BlockPos getOffsetInDirection(BlockPos pos, String direction) {
        switch(direction) {
            case "N":
                return pos.north();
            case "E":
                return pos.east();
            case "S":
                return pos.south();
            case "W":
                return pos.west();
            default:
                return pos.north();
        }
    }

    /**
     * Mines stairs from the starter block up to the surface
     * Stairs go up 1 block and forward 1 block in a random cardinal direction
     * Can make up to 5 random 90-degree turns with 10% chance per valid opportunity
     * 
     * @param world      The world to mine in
     * @param starterPos The starting position to mine from
     * @param player     The player to check line of sight against
     * @return The length of the stairs mined
     */
    public static int mineStairs(World world, BlockPos starterPos, PlayerEntity player) {
        int stairLength = 0;
        BlockPos currentPos = starterPos;
        java.util.List<BlockPos> stairBlocks = new java.util.ArrayList<>();
        
        // Random initial direction
        String[] cardinals = {"N", "E", "S", "W"};
        String currentDirection = cardinals[random.nextInt(4)];
        
        // Turn tracking
        int turnsMade = 0;
        int maxTurns = 5;
        int blocksSinceLastTurn = 0;
        int minBlocksBetweenTurns = 6;
        
        // Cache surface Y values to avoid repeated expensive calculations
        java.util.Map<Long, Integer> surfaceYCache = new java.util.HashMap<>();
        
        // Helper to get cached surface Y
        java.util.function.BiFunction<Integer, Integer, Integer> getSurfaceY = (x, z) -> {
            long key = ((long)x << 32) | (z & 0xFFFFFFFFL);
            return surfaceYCache.computeIfAbsent(key, k -> 
                SurfaceFinder.findPointSurfaceY((ServerWorld) world, x, z, true, false, true)
            );
        };
        
        // fill in the stairblocks list, checking surface level as we go
        while (true) {
            // if y value is getting too high, stop
            if (currentPos.getY() >= 130) {
                break;
            }
            
            // Check if we've reached the surface
            int surfaceY = getSurfaceY.apply(currentPos.getX(), currentPos.getZ());
            if (surfaceY != -1 && currentPos.getY() >= surfaceY) {
                stairBlocks.add(currentPos);
                stairLength++;
                break;
            }
            
            if (surfaceY == -1 && currentPos.getY() >= 60) {
                return stairLength;
            }
            
            stairBlocks.add(currentPos);
            
            // Check if we should make a turn
            if (blocksSinceLastTurn >= minBlocksBetweenTurns && 
                turnsMade < maxTurns && 
                random.nextInt(100) < 10) {
                
                boolean turnLeft = random.nextBoolean();
                currentDirection = rotate90Degrees(currentDirection, turnLeft);
                
                turnsMade++;
                blocksSinceLastTurn = 0;
            }
            
            currentPos = moveInDirection(currentPos, currentDirection);
            stairLength++;
            blocksSinceLastTurn++;
        }

        // Find surface entrance position (last position at or above surface)
        BlockPos entrancePos = null;
        for (BlockPos pos : stairBlocks) {
            int surfaceY = getSurfaceY.apply(pos.getX(), pos.getZ());
            if (surfaceY != -1 && pos.getY() >= surfaceY) {
                entrancePos = pos;
                break;
            }
        }

        // Set the 3 blocks above each stair block to air
        int torchDistance = 0;
        String stairDirection = cardinals[random.nextInt(4)];
        
        for (int i = 0; i < stairBlocks.size(); i++) {
            BlockPos stairPos = stairBlocks.get(i);
            
            // Detect direction changes for corner fill
            if (i > 0) {
                BlockPos prevPos = stairBlocks.get(i - 1);
                int dx = stairPos.getX() - prevPos.getX();
                int dz = stairPos.getZ() - prevPos.getZ();
                
                String detectedDirection;
                if (dz < 0) detectedDirection = "N";
                else if (dz > 0) detectedDirection = "S";
                else if (dx > 0) detectedDirection = "E";
                else detectedDirection = "W";
                
                if (!detectedDirection.equals(stairDirection)) {
                    String oldDirection = stairDirection;
                    stairDirection = detectedDirection;
                    
                    BlockPos cornerFill = getOffsetInDirection(stairPos, oldDirection);
                    if (world.getBlockState(cornerFill).isAir() || world.getBlockState(cornerFill).isOf(Blocks.CAVE_AIR)) {
                        world.setBlockState(cornerFill, Blocks.COBBLESTONE.getDefaultState());
                    }
                }
            }
            
            // Check if we're at or above surface (using cache)
            int surfaceY = getSurfaceY.apply(stairPos.getX(), stairPos.getZ());
            boolean isAtOrAboveSurface = (surfaceY != -1 && stairPos.getY() >= surfaceY);

            // Only place cobblestone if below surface and the block is air
            if ((world.getBlockState(stairPos).isOf(Blocks.CAVE_AIR) || world.getBlockState(stairPos).isOf(Blocks.AIR))
                    && !isAtOrAboveSurface) {
                world.setBlockState(stairPos, Blocks.COBBLESTONE.getDefaultState());
                BlockPos adjacent = getOffsetInDirection(stairPos, stairDirection);
                world.setBlockState(adjacent, Blocks.COBBLESTONE.getDefaultState());
            }

            // break if is above surface
            if (isAtOrAboveSurface) {
                break;
            }

            for (int height = 1; height <= 3; height++) {
                if (height == 1 && torchDistance == 8) {
                    BlockPos torchPos = stairPos.up(1);
                    if (torchPos.getY() < world.getTopY()) {
                        TorchPlacer.placeTorch(world, torchPos, random, player);
                    }
                    torchDistance = 0;
                } else {
                    BlockPos abovePos = stairPos.up(height);
                    if (abovePos.getY() < world.getTopY()) {
                        world.setBlockState(abovePos, Blocks.AIR.getDefaultState());
                    }
                }
            }
            torchDistance++;
        }

        // Add torches around entrance if we found one
        if (entrancePos != null) {
            placeEntranceTorches(world, entrancePos, player);
        }

        return stairLength;
    }

    /**
     * Places torches randomly around the staircase entrance to make it more visible
     * 
     * @param world       The world to place torches in
     * @param entrancePos The position of the entrance
     * @param player      The player to check line of sight against
     */
    private static void placeEntranceTorches(World world, BlockPos entrancePos, PlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) world;

        // number of torches (5-8)
        int torchCount = 5 + random.nextInt(4);
        int torchesPlaced = 0;
        int attempts = 0;

        // Track placed positions to avoid placing torches too close together
        java.util.Set<BlockPos> placedPositions = new java.util.HashSet<>();

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
                    BlockState currentState = world.getBlockState(surfacePos);
                    BlockState belowState = world.getBlockState(surfacePos.down());
                    
                    if (currentState.isOf(Blocks.SNOW) || currentState.isReplaceable()) {
                        world.setBlockState(surfacePos, Blocks.AIR.getDefaultState(), 3);
                    }
                    
                    BlockPos targetPos = surfacePos;
                    if (belowState.isOf(Blocks.SNOW)) {
                        world.setBlockState(surfacePos.down(), Blocks.AIR.getDefaultState(), 3);
                        targetPos = surfacePos.down();
                    }
                    
                    if (TorchPlacer.placeTorch(world, targetPos, random, player)) {
                        placedPositions.add(targetPos);
                        torchesPlaced++;
                    }
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
        
        // Check if too close to existing pre-mined caves
        ServerWorld serverWorld = (ServerWorld) world;
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(serverWorld.getServer());
        java.util.List<BlockPos> existingCaves = state.getPositionList("preminedCaveLocations");
        
        final int MIN_CAVE_DISTANCE_SQUARED = 60 * 60;
        for (BlockPos existingCave : existingCaves) {
            if (starterPos.getSquaredDistance(existingCave) < MIN_CAVE_DISTANCE_SQUARED) {
                return false;
            }
        }
        
        horror.blueice129.HorrorMod129.LOGGER.info("Cave Pre-Miner: Found starter block at " + starterPos);
        
        // Combined cave exploration and ore mining in a single pass
        CaveExplorationResult result = findCaveAirAndMineOres(world, starterPos, player);
        java.util.List<BlockPos> caveAirBlocks = result.caveAirBlocks;
        int oresMined = result.oresMined;
        
        if (caveAirBlocks.size() < 50) {
            return false; // Not enough cave air blocks to consider this a cave
        }
        
        int torchesPlaced = populateTorches(world, caveAirBlocks, player);
        int extraBlocksPlaced = placeExtraBlocks(world, caveAirBlocks, player);
        int stairLength = mineStairs(world, starterPos, player);
        
        // Store this cave location to prevent future caves from being too close
        state.addPositionToList("preminedCaveLocations", starterPos);
        
        HorrorMod129.LOGGER.info("Cave Pre-Miner: Mined " + oresMined + " ores, placed " + torchesPlaced
                + " torches, extra blocks placed: " + extraBlocksPlaced + ", stair length: " + stairLength);
        return true;
    }

}