package horror.blueice129.feature;

import horror.blueice129.data.HorrorModPersistentState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import horror.blueice129.utils.StructurePlacer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import horror.blueice129.utils.SurfaceFinder;
import horror.blueice129.utils.ChunkLoader;

import net.minecraft.util.math.random.Random;

public class SmallStructureEvent {
    public static final String SMALL_STRUCTURE_TIMER_KEY = "smallStructureEventTimer";
    private static final Random random = Random.create();
    // 2d list of a structure id and its weight
    public static String[][] STRUCTURE_LIST = { // not final, agro meter will change weights
            { "crafting_table", "10" },
            { "furnace", "5" },
            { "cobblestone_pillar", "7" },
            { "single_torch", "10" },
            { "torched_area", "15" },
            { "tree_mined", "14" },
            // { "deforestation", "6" },
            { "flower_patch", "3" },
            { "watchtower", "0" },
            { "starter_base", "1" },
            { "pitfall_trap", "0" },
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
        int randomWeight = random.nextInt(totalWeight);
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
        ServerPlayerEntity player = server.getPlayerManager().getPlayerList()
                .get(random.nextInt(server.getPlayerManager().getPlayerList().size()));
        if (player == null) {
            return false; // No player found
        }
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        // adjust weights based on player agro meter
        adjustWeightsBasedOnAgro(state);

        String structureId = selectRandomStructure();
        if (structureId == null) {
            return false; // No structure selected
        }
        System.out.println("[SmallStructureEvent] Attempting structure '" + structureId + "' for player "
                + player.getName().getString() + " at " + player.getBlockPos());
        boolean success = placeStructureNearPlayer(server, player, structureId);
        System.out.println("[SmallStructureEvent] Structure '" + structureId + "' for player "
                + player.getName().getString() + " completed: " + success);

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
            return false; // No structure selected
        }
        System.out.println("[SmallStructureEvent] Attempting structure '" + structureId + "' for player "
                + player.getName().getString() + " at " + player.getBlockPos());
        boolean success = placeStructureNearPlayer(server, player, structureId);
        System.out.println("[SmallStructureEvent] Structure '" + structureId + "' for player "
                + player.getName().getString() + " completed: " + success);
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

            if (id.equals("watchtower")) {
                adjustedWeight += agroMeter / 4; // increase watchtower weight with agro
            } else if (id.equals("pitfall_trap")) {
                adjustedWeight += agroMeter / 2; // increase pitfall trap weight with agro
            } else if (id.equals("chunk_deletion")) {
                adjustedWeight += agroMeter / 10; // increase chunk deletion weight with agro
            } else if (id.equals("deforestation")) {
                adjustedWeight += agroMeter / 5; // increase deforestation weight with agro
            } else if (id.equals("burning_forest")) {
                adjustedWeight += agroMeter / 3; // increase burning forest weight with agro
            } else if (id.equals("cobblestone_pillar")) {
                adjustedWeight += agroMeter / 8; // increase cobblestone pillar weight with agro
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
            // case "deforestation":
            // success = deforestationEvent(server, player);
            // break;
            case "flower_patch":
                success = flowerPatchEvent(server, player);
                break;
            case "watchtower":
                success = watchtowerEvent(server, player);
                break;
            case "starter_base":
                success = starterBaseEvent(server, player);
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
        // get random location near player
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 20, 50);
        if (pos == null) {
            return false; // No suitable location found
        }
        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        // place crafting table
        server.getOverworld().setBlockState(pos, Blocks.CRAFTING_TABLE.getDefaultState());
        return true;
    }

    private static boolean furnaceEvent(MinecraftServer server, ServerPlayerEntity player) {
        // get random location near player
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 20, 50);
        if (pos == null) {
            return false; // No suitable location found
        }
        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        // place furnace
        server.getOverworld().setBlockState(pos, Blocks.FURNACE.getDefaultState());

        int coalAmount = 1 + random.nextInt(60);
        int itemAmount = 1 + random.nextInt(20);

        BlockEntity blockEntity = server.getOverworld().getBlockEntity(pos);
        if (blockEntity instanceof FurnaceBlockEntity) {
            FurnaceBlockEntity furnace = (FurnaceBlockEntity) blockEntity;
            furnace.setStack(0, new ItemStack(Items.COAL, coalAmount));
            furnace.setStack(1, new ItemStack(Items.BEEF, itemAmount));
        }
        return true;
    }

    private static boolean cobblestonePillarEvent(MinecraftServer server, ServerPlayerEntity player) {
        // get random location near player
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 20, 50);
        if (pos == null) {
            return false; // No suitable location found
        }
        // Make sure the chunk is loaded before modifying blocks
        // Using radius 1 to ensure we have the chunk and immediate neighbors loaded
        // for taller pillars that might extend across chunk boundaries
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        // place 3 block tall cobblestone pillar
        int height = 3 + random.nextInt(30);
        for (int i = 0; i < height; i++) {
            BlockPos pillarPos = pos.up(i);
            server.getOverworld().setBlockState(pillarPos, Blocks.COBBLESTONE.getDefaultState());
        }
        server.getOverworld().setBlockState(pos.up(height), Blocks.TORCH.getDefaultState());
        return true;
    }

    private static boolean singleTorchEvent(MinecraftServer server, ServerPlayerEntity player) {
        // get random location near player
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 20, 50);
        if (pos == null) {
            return false; // No suitable location found
        }
        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false; // Chunk couldn't be loaded
        }
        // place torch
        server.getOverworld().setBlockState(pos, Blocks.TORCH.getDefaultState());
        return true;
    }

    private static boolean torchedAreaEvent(MinecraftServer server, ServerPlayerEntity player) {
        // get random location near player
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 30, 50);
        if (pos == null) {
            return false; // No suitable location found
        }
        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false; // Chunk couldn't be loaded
        }

        // find 5-15 random positions within 15 block radius and place torches
        int torchCount = 5 + random.nextInt(11);
        for (int i = 0; i < torchCount; i++) {
            BlockPos torchPos = StructurePlacer.findSurfaceLocation(server.getOverworld(), pos, player, 15, 15);
            if (torchPos != null) {
                // Make sure the chunk for each torch is loaded
                if (ChunkLoader.loadChunksInRadius(server.getOverworld(), torchPos, 1)) {
                    server.getOverworld().setBlockState(torchPos, Blocks.TORCH.getDefaultState());
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
        // mine the furthest tree
        BlockPos treePos = treePositions[treePositions.length - 1];
        // Make sure the chunks for the tree are loaded before breaking blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), treePos, 1)) {
            return false; // Chunks couldn't be loaded
        }
        BlockPos[] treeLogs = SurfaceFinder.getTreeLogPositions(server.getOverworld(), treePos);
        for (BlockPos logPos : treeLogs) {
            // Ensure each log position's chunk is loaded (trees can cross chunk boundaries)
            if (ChunkLoader.loadChunksInRadius(server.getOverworld(), logPos, 1)) {
                server.getOverworld().breakBlock(logPos, false, null);
            }
        }
        return true;
    }

    // private static boolean deforestationEvent(MinecraftServer server,
    // ServerPlayerEntity player) {
    // // get random location near player
    // BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(),
    // player.getBlockPos(), player, 30, 50);
    // if (pos == null) {
    // return false; // No suitable location found
    // }
    // BlockPos[] treePositions =
    // SurfaceFinder.findTreePositions(server.getOverworld(), pos, 13);
    // if (treePositions.length == 0) {
    // return false; // No trees found
    // }
    // for (BlockPos treePos : treePositions) {
    // if (random.nextInt(100) < 30) { // 30% chance to skip a tree
    // continue;
    // }
    // BlockPos[] treeLogs =
    // SurfaceFinder.getTreeLogPositions(server.getOverworld(), treePos);
    // for (BlockPos logPos : treeLogs) {
    // server.getOverworld().breakBlock(logPos, false, null);
    // }
    // }
    // return true;
    // }

    private static boolean flowerPatchEvent(MinecraftServer server, ServerPlayerEntity player) {
        // get random location near player
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 30, 50);
        if (pos == null) {
            return false; // No suitable location found
        }
        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius(server.getOverworld(), pos, 1)) {
            return false; // Chunk couldn't be loaded
        }

        int flowerCount = 5 + random.nextInt(20);
        for (int i = 0; i < flowerCount; i++) {
            BlockPos flowerPos = StructurePlacer.findSurfaceLocation(server.getOverworld(), pos, player, 1, 10);
            if (flowerPos != null) {
                // Make sure the chunk for each flower position is loaded
                if (ChunkLoader.loadChunksInRadius(server.getOverworld(), flowerPos, 1)) {
                    server.getOverworld().setBlockState(flowerPos, Blocks.LILY_OF_THE_VALLEY.getDefaultState());
                }
            }
        }
        return true;
    }

    private static boolean watchtowerEvent(MinecraftServer server, ServerPlayerEntity player) {
        return false; // TODO implement
    }

    private static boolean starterBaseEvent(MinecraftServer server, ServerPlayerEntity player) {
        return false; // TODO implement
    }

    private static boolean pitfallTrapEvent(MinecraftServer server, ServerPlayerEntity player) {
        return false; // TODO implement
    }

    private static boolean chunkDeletionEvent(MinecraftServer server, ServerPlayerEntity player) {
        // find a far away chunk, 100-200 blocks away
        // FIXME: MAKE SURE CHUNK CANT DELETE IF IT HAS CHESTS OR BEDS OR OTHER STORAGE
        // BLOCKS
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 100,
                200);
        if (pos == null) {
            return false; // No suitable location found
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
        // find random tree location 50-100 blocks away
        // We need to include snow when finding a surface location to work with snowy areas
        BlockPos pos = StructurePlacer.findSurfaceLocation(server.getOverworld(), player.getBlockPos(), player, 50,
                100, true); // Changed to include snow
        if (pos == null) {
            return false; // No suitable location found
        }
        
        // Find trees, properly handling snow-covered ones
        BlockPos[] treePositions = SurfaceFinder.findTreePositions(server.getOverworld(), pos, 13);
        if (treePositions.length == 0) {
            return false; // No trees found
        }
        
        int numberOfTreesToBurn = Math.max(1, Math.min(3, treePositions.length)); // Limit to max 3 trees
        for (int x = 0; x < numberOfTreesToBurn; x++) {
            BlockPos treePos = treePositions[random.nextInt(treePositions.length)];
            
            // Get the actual log positions of this tree for more targeted fire placement
            BlockPos[] treeLogPositions = SurfaceFinder.getTreeLogPositions(server.getOverworld(), treePos);
            if (treeLogPositions.length == 0) {
                continue; // Skip if no log positions found
            }
            
            // Place fire on and around logs
            for (BlockPos logPos : treeLogPositions) {
                // Try to place fire directly on the log sometimes (logs themselves are flammable)
                if (random.nextInt(100) < 40) { // 40% chance to place fire on log
                    // Clear snow if present and place fire
                    BlockPos fireOnLogPos = logPos.add(0, 1, 0);
                    if (server.getOverworld().getBlockState(fireOnLogPos).getBlock() == Blocks.SNOW) {
                        server.getOverworld().setBlockState(fireOnLogPos, Blocks.AIR.getDefaultState());
                    }
                    if (server.getOverworld().getBlockState(fireOnLogPos).isAir()) {
                        server.getOverworld().setBlockState(fireOnLogPos, Blocks.FIRE.getDefaultState());
                    }
                }
                
                // Place fire around logs, clearing snow if needed
                int fireAroundLogCount = 3 + random.nextInt(4); // 3-6 fires around each log
                for (int i = 0; i < fireAroundLogCount; i++) {
                    BlockPos firePos = logPos.add(random.nextInt(3) - 1, random.nextInt(2), random.nextInt(3) - 1);
                    
                    // Clear snow if present
                    if (server.getOverworld().getBlockState(firePos).getBlock() == Blocks.SNOW) {
                        server.getOverworld().setBlockState(firePos, Blocks.AIR.getDefaultState());
                    }
                    
                    // Place fire on air or replace snow layers
                    if (server.getOverworld().getBlockState(firePos).isAir()) {
                        server.getOverworld().setBlockState(firePos, Blocks.FIRE.getDefaultState());
                    }
                }
            }
        }
        return true;
    }

}