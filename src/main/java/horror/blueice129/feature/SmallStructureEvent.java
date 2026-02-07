package horror.blueice129.feature;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BedBlock;
import net.minecraft.item.Items;
import horror.blueice129.utils.StructurePlacer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.block.entity.FurnaceBlockEntity;
// import net.minecraft.client.font.MultilineText.Line;
import net.minecraft.block.entity.BlockEntity;
import horror.blueice129.utils.SurfaceFinder;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.LineOfSightUtils;
import horror.blueice129.utils.TorchPlacer;

import net.minecraft.util.math.random.Random;

public class SmallStructureEvent {
    public static final String SMALL_STRUCTURE_TIMER_KEY = "smallStructureEventTimer";
    private static final Random RANDOM = Random.create();
    // 2d list of a structure id and its weight
    public static String[][] STRUCTURE_LIST = { // not final, agro meter will change weights
            { "crafting_table", "10" },
            { "furnace", "5" },
            { "cobblestone_pillar", "3" },
            { "single_torch", "10" },
            { "torched_area", "15" },
            { "tree_mined", "17" },
            { "deforestation", "5" },
            { "flower_patch", "3" },
            // { "watchtower", "0" },
            // { "starter_base", "1" },
            // { "pitfall_trap", "0" },
            { "chunk_deletion", "0" },
            { "burning_forest", "0" }
    };

    /**
     * Selects a random structure based on weights.
     */
    private static String selectRandomStructure() {
        int totalWeight = 0;
        for (String[] structure : STRUCTURE_LIST) {
            totalWeight += Integer.parseInt(structure[1]);
        }
        // Get a random value between 0 and totalWeight - 1
        int randomWeight = RANDOM.nextInt(totalWeight);
        for (String[] structure : STRUCTURE_LIST) {
            randomWeight -= Integer.parseInt(structure[1]);
            if (randomWeight < 0) {
                return structure[0];
            }
        }
        return null;
    }

    /**
     * Triggers the small structure event for a player.
     * 
     * @param server The Minecraft server instance
     * @param player The player to trigger the event for
     * @return boolean indicating if the event was successfully triggered
     */
    public static boolean triggerEvent(MinecraftServer server) {
        var playerList = server.getPlayerManager().getPlayerList();
        if (playerList.isEmpty()) {
            return false; // No players online
        }
        ServerPlayerEntity player = playerList.get(RANDOM.nextInt(playerList.size()));
        if (player == null) {
            return false; // No player found
        }
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        // adjust weights based on player agro meter
        adjustWeightsBasedOnAgro(state);

        String structureId = selectRandomStructure();
        if (structureId == null) {
            return false;
        }
        logStructureEvent(structureId, player, true);
        boolean success = placeStructureNearPlayer(server, player, structureId);
        if (success) {
            logStructureEvent(structureId, player, false);
        }
        return success;
    }

    /**
     * Triggers the small structure event for a player with a specific structure ID.
     * 
     * @param server      The Minecraft server instance
     * @param player      The player to trigger the event for
     * @param structureId The ID of the structure to trigger
     * @return boolean indicating if the event was successfully triggered
     */
    public static boolean triggerEvent(MinecraftServer server, ServerPlayerEntity player, String structureId) {
        // Adjust weights based on player agro meter
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        adjustWeightsBasedOnAgro(state);

        // If a specific structureId is provided, use it directly
        if (structureId == null || structureId.isEmpty()) {
            structureId = selectRandomStructure();
        }

        if (structureId == null) {
            return false;
        }
        logStructureEvent(structureId, player, true);
        boolean success = placeStructureNearPlayer(server, player, structureId);
        if (success) {
            logStructureEvent(structureId, player, false);
        }
        return success;
    }

    /**
     * Adjusts structure weights based on the player's agro meter.
     * Higher agro increases the likelihood of more intrusive structures.
     * 
     * @param player The player whose agro meter is used for adjustment
     */
    private static void adjustWeightsBasedOnAgro(HorrorModPersistentState state) {
        int agroMeter = state.getIntValue("agroMeter", 0);

        for (String[] structure : STRUCTURE_LIST) {
            String id = structure[0];
            int baseWeight = Integer.parseInt(structure[1]);
            int adjustedWeight = baseWeight;

            // Increase weights for more intrusive structures based on agro meter
            if (id.equals("watchtower")) {
                adjustedWeight += agroMeter / 4;
            } else if (id.equals("pitfall_trap")) {
                adjustedWeight += agroMeter / 2;
            } else if (id.equals("chunk_deletion")) {
                adjustedWeight += agroMeter / 10;
            } else if (id.equals("deforestation")) {
                adjustedWeight += agroMeter / 5;
            } else if (id.equals("burning_forest")) {
                adjustedWeight += agroMeter / 3;
            } else if (id.equals("cobblestone_pillar")) {
                adjustedWeight += agroMeter / 8;
            }
            structure[1] = Integer.toString(adjustedWeight);
        }
    }

    /**
     * Places the specified structure near the player.
     * 
     * @param server      The Minecraft server instance
     * @param player      The player to place the structure for
     * @param structureId The ID of the structure to place
     * @return boolean indicating if the structure was successfully placed
     */
    private static boolean placeStructureNearPlayer(MinecraftServer server, ServerPlayerEntity player,
            String structureId) {
        boolean success = false;
        switch (structureId) {
            case "crafting_table":
                success = craftingTableEvent(server, player);
                break;
            case "furnace":
                success = furnaceEvent(server, player);
                break;
            case "cobblestone_pillar":
                success = cobblestonePillarEvent(server, player);
                break;
            case "single_torch":
                success = singleTorchEvent(server, player);
                break;
            case "torched_area":
                success = torchedAreaEvent(server, player);
                break;
            case "tree_mined":
                success = treeMinedEvent(server, player);
                break;
            case "deforestation":
                success = deforestationEvent(server, player);
                break;
            case "flower_patch":
                success = flowerPatchEvent(server, player);
                break;
            case "watchtower":
                success = watchtowerEvent(server, player);
                break;
            case "pitfall_trap":
                success = pitfallTrapEvent(server, player);
                break;
            case "chunk_deletion":
                success = chunkDeletionEvent(server, player);
                break;
            case "burning_forest":
                success = burningForestEvent(server, player);
                break;
            default:
                break;
        }

        return success;
    }

    private static boolean craftingTableEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = findAndLoadSurfaceLocation(server, player, 20, 50);
        if (pos == null) {
            return false;
        }
        if (LineOfSightUtils.hasLineOfSight(player, pos, 200)) {
            return false;
        }
        server.getOverworld().setBlockState(pos, Blocks.CRAFTING_TABLE.getDefaultState());
        return true;
    }

    private static boolean furnaceEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = findAndLoadSurfaceLocation(server, player, 20, 50);
        if (pos == null) {
            return false;
        }
        if (LineOfSightUtils.hasLineOfSight(player, pos, 200)) {
            return false;
        }
        server.getOverworld().setBlockState(pos, Blocks.FURNACE.getDefaultState());

        int coalAmount = 1 + RANDOM.nextInt(60);
        int itemAmount = 1 + RANDOM.nextInt(20);

        BlockEntity blockEntity = server.getOverworld().getBlockEntity(pos);
        if (blockEntity instanceof FurnaceBlockEntity) {
            FurnaceBlockEntity furnace = (FurnaceBlockEntity) blockEntity;
            furnace.setStack(0, new ItemStack(Items.BEEF, itemAmount));
            furnace.setStack(1, new ItemStack(Items.COAL, coalAmount));
        }
        return true;
    }

    private static boolean cobblestonePillarEvent(MinecraftServer server, ServerPlayerEntity player) {

        BlockPos pos = intrestingAreaFinder(server, player);
        if (pos == null) {
            return false; // No suitable location found
        }

        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        // place 3 block tall cobblestone pillar
        int height = 20 + RANDOM.nextInt(40);
        if (pos.getY() < 46) {
            height += 30; // If the pillar is in a cave, make it taller to reach the surface
        }
        for (int i = 0; i < height; i++) {
            BlockPos pillarPos = pos.up(i);
            if (!LineOfSightUtils.hasLineOfSight(player, pillarPos, 200)) {
                server.getOverworld().setBlockState(pillarPos, Blocks.COBBLESTONE.getDefaultState());
            }
        }
        // server.getOverworld().setBlockState(pos.up(height), Blocks.TORCH.getDefaultState());
        // use torchutil instead
        BlockPos torchPos = pos.up(height).down();
        TorchPlacer.placeTorch(server.getOverworld(), torchPos.up(), RANDOM);
        TorchPlacer.placeTorch(server.getOverworld(), torchPos.north(), RANDOM);
        TorchPlacer.placeTorch(server.getOverworld(), torchPos.east(), RANDOM);
        TorchPlacer.placeTorch(server.getOverworld(), torchPos.south(), RANDOM);
        TorchPlacer.placeTorch(server.getOverworld(), torchPos.west(), RANDOM);


        
        return true;
    }

    private static BlockPos intrestingAreaFinder(MinecraftServer server, ServerPlayerEntity player) {
        int tries = 200;
        // list of block types that are considered intresting
        Block[] intrestingBlocks = { Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.TORCH,
                Blocks.LANTERN, Blocks.HOPPER, Blocks.OBSIDIAN, Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK,
                Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.LILY_OF_THE_VALLEY,
                Blocks.HAY_BLOCK, Blocks.MOSS_BLOCK, Blocks.LAVA, Blocks.FIRE, Blocks.EMERALD_ORE, Blocks.DIAMOND_ORE, Blocks.SPAWNER
            };

        String[] intrestingPartialBlockNames = { "door", "bed", "stairs", "plank", "glass", "rail", "path"};
        for (int i = 0; i < tries; i++) {
            BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 80,
                    200);

            if (pos != null) {

                int searchRadius = 10;
                int searchHeight = 3;
                for (int x = -searchRadius; x <= searchRadius; x++) {
                    for (int z = -searchRadius; z <= searchRadius; z++) {
                        for (int y = -1; y <= searchHeight - 1; y++) {

                            BlockPos checkPos = pos.add(x, y, z);
                            Block blockAtPos = server.getOverworld().getBlockState(checkPos).getBlock();
                            if (checkPos.getY() <= 45) {
                                HorrorMod129.LOGGER.info("returned cave pos: " + checkPos);
                                return pos; // Found a deep hole/cave, return this position
                            }
                            if (blockAtPos != Blocks.AIR || blockAtPos != Blocks.GRASS_BLOCK
                                    || blockAtPos != Blocks.DIRT || blockAtPos != Blocks.SAND
                                    || blockAtPos != Blocks.RED_SAND) {

                                if (java.util.Arrays.asList(intrestingBlocks)
                                        .contains(blockAtPos)) {
                                    HorrorMod129.LOGGER.info("returned intresting block pos: " + checkPos);
                                    return pos; // Found an intresting block, return this position
                                } else {
                                    String blockName = blockAtPos.getTranslationKey();
                                    for (String partialName : intrestingPartialBlockNames) {
                                        if (blockName.contains(partialName)) {
                                            HorrorMod129.LOGGER.info("returned intresting partial block pos: " + checkPos);
                                            HorrorMod129.LOGGER.info("block name: " + blockName);
                                            return pos; // Found a block with an intresting structure attached
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null; // No suitable location found after max tries
    }

    private static boolean singleTorchEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = findAndLoadSurfaceLocation(server, player, 20, 50);
        if (pos == null) {
            return false;
        }
        // place torch
        TorchPlacer.placeTorch(server.getOverworld(), pos, RANDOM);
        return true;
    }

    private static boolean torchedAreaEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = findAndLoadSurfaceLocation(server, player, 30, 50);
        if (pos == null) {
            return false;
        }
        int torchCount = 5 + random.nextInt(11);
            return false; // No suitable location found
        }
        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false;
        }

        // find 5-15 random positions within 15 block radius and place torches
        int torchCount = 5 + RANDOM.nextInt(11);
        for (int i = 0; i < torchCount; i++) {
            BlockPos torchPos = StructurePlacer.findSurfaceLocation(server.getOverworld(), pos, player, 1, 15);
            if (torchPos != null) {
                if (ChunkLoader.loadChunksInRadius(server.getOverworld(), torchPos, 1)) {
                    TorchPlacer.placeTorch(server.getOverworld(), torchPos, RANDOM);
                }
            }
        }
        return true;
    }

    private static BlockPos findAndLoadSurfaceLocation(MinecraftServer server, ServerPlayerEntity player,
            int minDistance, int maxDistance) {
        return findAndLoadSurfaceLocation(server, player.getBlockPos(), player, minDistance, maxDistance, false);
    }

    private static BlockPos findAndLoadSurfaceLocation(MinecraftServer server, BlockPos origin,
            ServerPlayerEntity player, int minDistance, int maxDistance, boolean includeSnow) {
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), origin, player, minDistance,
                maxDistance, includeSnow);
        if (pos == null || !ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return null;
        }
        return pos;
    }

    private static void logStructureEvent(String structureId, ServerPlayerEntity player, boolean attempting) {
        String action = attempting ? "Attempting" : "completed";
        String statusInfo = attempting ? " at " + player.getBlockPos() : ": " + true;
        System.out.println("[SmallStructureEvent] " + action + " structure '" + structureId + "' for player "
                + player.getName().getString() + statusInfo);
    }

    private static void clearSnowIfPresent(MinecraftServer server, ServerPlayerEntity player, BlockPos pos) {
        if (server.getOverworld().getBlockState(pos).getBlock() == Blocks.SNOW) {
            if (!LineOfSightUtils.hasLineOfSight(player, pos, 200)) {
                server.getOverworld().setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }
    }

    private static boolean mineTree(MinecraftServer server, ServerPlayerEntity player, BlockPos treePos) {
        BlockPos[] treeLogs = SurfaceFinder.getTreeLogPositions(server.getOverworld(), treePos);
        for (BlockPos logPos : treeLogs) {
            if (ChunkLoader.loadChunksInRadius(server.getOverworld(), logPos, 1)) {
                if (!LineOfSightUtils.hasLineOfSight(player, logPos, 200)) {
                    int chance = random.nextInt(10);

                    if (chance == 0) {
                        server.getOverworld().breakBlock(logPos, true, null);
                    } else {
                        server.getOverworld().setBlockState(logPos, Blocks.AIR.getDefaultState());
                    }
                        
                } 
            }
        }
        return true;
    }

    private static boolean treeMinedEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos[] treePositions = SurfaceFinder.findTreePositions(server.getOverworld(), player.getBlockPos(), 50);
        // if no trees found, return false
        if (treePositions.length == 0) {
            return false;
        }
        // sort tree positions by distance to player
        java.util.Arrays.sort(treePositions, (a, b) -> {
            double distA = a.getSquaredDistance(player.getBlockPos());
            double distB = b.getSquaredDistance(player.getBlockPos());
            return Double.compare(distA, distB);
        });

        BlockPos treePos = treePositions[treePositions.length - 1]; // Get the farthest tree
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), treePos, 1)) {
            return false;
        }
        return mineTree(server, player, treePos);
    }

    private static boolean deforestationEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = findAndLoadSurfaceLocation(server, player, 80, 100);
        
        BlockPos[] treePositions = SurfaceFinder.findTreePositions(server.getOverworld(), pos, 40);
        if (treePositions.length == 0) {
            return false;
        }
        boolean removedAnyTrees = false;
        for (BlockPos treePos : treePositions) {
            if (random.nextInt(100) < 90) {
                if (ChunkLoader.loadChunksInRadius(server.getOverworld(), treePos, 1)) {
                    mineTree(server, player, treePos);
                    removedAnyTrees = true;
                }
            }
        }
        return removedAnyTrees;
    }

    private static boolean flowerPatchEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = findAndLoadSurfaceLocation(server, player, 30, 50);
        if (pos == null) {
            return false;
        }
        if (!LineOfSightUtils.hasLineOfSight(player, pos.up(10), 200)) {
            server.getOverworld().setBlockState(pos.up(10), Blocks.DIAMOND_BLOCK.getDefaultState());
        }

        int flowerCount = 5 + RANDOM.nextInt(20);
        for (int i = 0; i < flowerCount; i++) {
            BlockPos flowerPos = StructurePlacer.findSurfaceLocation(server.getOverworld(), pos, player, 1, 10);

            if (flowerPos == null) {
                continue;
            }

            var blockBelow = server.getOverworld().getBlockState(flowerPos.down()).getBlock();
            if (blockBelow.equals(Blocks.GRASS_BLOCK) || blockBelow.equals(Blocks.FARMLAND) ||
                    blockBelow.equals(Blocks.PODZOL) || blockBelow.equals(Blocks.MYCELIUM)
                    || blockBelow.equals(Blocks.DIRT) || blockBelow.equals(Blocks.COARSE_DIRT)) {
                if (ChunkLoader.loadChunksInRadius(server.getOverworld(), flowerPos, 1)) {
                    if (!LineOfSightUtils.hasLineOfSight(player, flowerPos, 200)) {
                        server.getOverworld().setBlockState(flowerPos, Blocks.LILY_OF_THE_VALLEY.getDefaultState());
                    }
                    if (!LineOfSightUtils.hasLineOfSight(player, flowerPos.up(10), 200)) {
                        server.getOverworld().setBlockState(flowerPos.up(10), Blocks.GOLD_BLOCK.getDefaultState());
                    }
                }
            }
        }
        return true;
    }

    private static boolean watchtowerEvent(MinecraftServer server, ServerPlayerEntity player) {
        return false; // TODO implement
    }

    private static boolean pitfallTrapEvent(MinecraftServer server, ServerPlayerEntity player) {
        return false; // TODO implement
    }

    private static boolean chunkDeletionEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 100,
                200);
        if (pos == null) {
            return false;
        }
        if (LineOfSightUtils.hasLineOfSight(player, pos, 200)) {
            return false;
        }
        // get chunk bounds
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int endX = startX + 15;
        int endZ = startZ + 15;
        int worldBottomY = server.getOverworld().getBottomY();
        int worldTopY = server.getOverworld().getHeight();

        // Make sure the chunk is loaded before deleting blocks
        BlockPos chunkPos = new BlockPos(startX, worldBottomY + (worldTopY - worldBottomY) / 2, startZ);
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), chunkPos, 1)) {
            return false; // Chunk couldn't be loaded
        }

        boolean hasStorageOrSpawnPoint = false;
        // Check for chests, beds, or spawn points in the chunk
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = worldBottomY; y < worldTopY; y++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    // Check for beds or storage blocks
                    if (server.getOverworld().getBlockState(blockPos)
                            .getBlock() instanceof net.minecraft.block.ChestBlock
                            || server.getOverworld().getBlockState(blockPos)
                                    .getBlock() instanceof net.minecraft.block.TrappedChestBlock
                            || server.getOverworld().getBlockState(blockPos)
                                    .getBlock() instanceof net.minecraft.block.BarrelBlock
                            || server.getOverworld().getBlockState(blockPos).getBlock() instanceof BedBlock) {
                        hasStorageOrSpawnPoint = true;
                        break;
                    }
                    // Check if this is the player's spawn point
                    if (blockPos.equals(player.getSpawnPointPosition())) {
                        hasStorageOrSpawnPoint = true;
                        break;
                    }
                }
                if (hasStorageOrSpawnPoint) {
                    break;
                }
            }
            if (hasStorageOrSpawnPoint) {
                break;
            }
        }

        if (hasStorageOrSpawnPoint) {
            return false; // Abort deletion if any storage blocks or spawn points are found
        }

        // return false if chunk is within 3 chunks of players spawn point
        BlockPos spawnPos = player.getSpawnPointPosition();
        if (spawnPos != null) {
            int spawnChunkX = spawnPos.getX() >> 4;
            int spawnChunkZ = spawnPos.getZ() >> 4;
            if (chunkX >= spawnChunkX - 3 && chunkX <= spawnChunkX + 3 &&
                    chunkZ >= spawnChunkZ - 3 && chunkZ <= spawnChunkZ + 3) {
                return false;
            }
        }

        // delete all blocks in chunk
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = worldBottomY; y < worldTopY; y++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    server.getOverworld().setBlockState(blockPos, Blocks.AIR.getDefaultState());
                }
            }
        }
        return true;
    }

    private static boolean burningForestEvent(MinecraftServer server, ServerPlayerEntity player) {
        BlockPos pos = findAndLoadSurfaceLocation(server, player.getBlockPos(), player, 50, 100, true);
        if (pos == null) {
            return false;
        }
        BlockPos[] treePositions = SurfaceFinder.findTreePositions(server.getOverworld(), pos, 13);
        if (treePositions.length == 0) {
            return false; // No trees found
        }

        int numberOfTreesToBurn = Math.max(1, Math.min(3, treePositions.length)); // Limit to max 3 trees
        for (int x = 0; x < numberOfTreesToBurn; x++) {
            BlockPos treePos = treePositions[RANDOM.nextInt(treePositions.length)];

            // Get the actual log positions of this tree for more targeted fire placement
            BlockPos[] treeLogPositions = SurfaceFinder.getTreeLogPositions(server.getOverworld(), treePos);
            if (treeLogPositions.length == 0) {
                continue; // Skip if no log positions found
            }

            // Place fire on and around logs
            for (BlockPos logPos : treeLogPositions) {
                // Try to place fire directly on the log sometimes (logs themselves are
                // flammable)
                if (RANDOM.nextInt(100) < 40) { // 40% chance to place fire on log
                    // Clear snow if present and place fire
                    BlockPos fireOnLogPos = logPos.add(0, 1, 0);
                    clearSnowIfPresent(server, player, fireOnLogPos);
                    if (server.getOverworld().getBlockState(fireOnLogPos).isAir()) {
                        if (!LineOfSightUtils.hasLineOfSight(player, fireOnLogPos, 200)) {
                            server.getOverworld().setBlockState(fireOnLogPos, Blocks.FIRE.getDefaultState());
                        }
                    }
                }

                int fireAroundLogCount = 3 + random.nextInt(4);
                for (int i = 0; i < fireAroundLogCount; i++) {
                    BlockPos firePos = logPos.add(random.nextInt(3) - 1, random.nextInt(2), random.nextInt(3) - 1);
                    clearSnowIfPresent(server, player, firePos);
                    if (server.getOverworld().getBlockState(firePos).isAir()) {
                        if (!LineOfSightUtils.hasLineOfSight(player, firePos, 200)) {
                            server.getOverworld().setBlockState(firePos, Blocks.FIRE.getDefaultState());
                        }
                    }
                }
            }
        }
        return true;
    }

}