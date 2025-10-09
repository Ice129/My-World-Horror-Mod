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

import net.minecraft.util.math.random.Random;
import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.ChunkLoader;
//TODO: delete file, as its unused and redundant
/**
 * This class handles random block placement near players.
 * It places various types of blocks at a distance between 3-5 chunks from the
 * player.
 */
public class RandomBlockPlacer {
    // Create a Random object for generating random values
    private static final Random random = Random.create();

    // Enum to represent the different types of blocks we can place
    public enum BlockType {
        TORCH,
        CRAFTING_TABLE,
        COBBLESTONE_PILLAR,
        FURNACE
    }

    /**
     * Places a random block somewhere around the player.
     * 
     * @param player The player to place blocks around
     * @param world  The world to place blocks in
     */
    public static void placeRandomBlockNearPlayer(PlayerEntity player, ServerWorld world) {
        // Log that we're attempting to place a block
        HorrorMod129.LOGGER.info("Attempting to place a random block near player: " + player.getName().getString());

        // Get the player's position
        BlockPos playerPos = player.getBlockPos();

        // Calculate a random position 3-5 chunks away from the player
        BlockPos targetPos = findSuitablePosition(world, playerPos);

        // If we couldn't find a suitable position, return without placing anything
        if (targetPos == null) {
            HorrorMod129.LOGGER.warn("Could not find suitable position for block placement");
            return;
        }

        // Choose a random block type
        BlockType blockType = getRandomBlockType();

        // Place the chosen block type at the target position
        placeBlock(world, targetPos, blockType);

        // Log that we successfully placed a block
        HorrorMod129.LOGGER.info(
                "Placed " + blockType + " at " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ());
    }

    /**
     * Finds a suitable position to place a block.
     * The position will be 3-5 chunks away from the player and on the surface.
     * 
     * @param world     The world to search in
     * @param playerPos The player's position
     * @return A suitable BlockPos, or null if none found
     */
    private static BlockPos findSuitablePosition(ServerWorld world, BlockPos playerPos) {
        // Define our distance ranges (in blocks)
        // 1 chunk = 16 blocks, so 3 chunks = 48 blocks, 5 chunks = 80 blocks
        int minDistance = 48; // 3 chunks
        int maxDistance = 80; // 5 chunks

        // We'll try up to 50 times to find a suitable position
        for (int attempt = 0; attempt < 50; attempt++) {
            // Choose a random angle
            double angle = random.nextDouble() * 2 * Math.PI;

            // Choose a random distance between min and max
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

            // Calculate the X and Z offsets using trigonometry
            int xOffset = (int) (Math.sin(angle) * distance);
            int zOffset = (int) (Math.cos(angle) * distance);

            // Calculate the target position
            int x = playerPos.getX() + xOffset;
            int z = playerPos.getZ() + zOffset;

            // Make sure the chunk at this position is loaded before checking blocks
            BlockPos checkPos = new BlockPos(x, world.getBottomY() + (world.getTopY() - world.getBottomY()) / 2, z);
            if (!ChunkLoader.loadChunksInRadius(world, checkPos, 1)) {
                continue; // Skip if chunk couldn't be loaded
            }

            // Find the top solid block at this X,Z position
            int y = findSurfaceY(world, x, z);

            // If we found a valid Y position
            if (y > 0) {
                BlockPos pos = new BlockPos(x, y + 1, z);
                // Check if the block is air (so we can place something there)
                if (world.isAir(pos)) {
                    return pos;
                }
            }
        }

        // If we couldn't find a suitable position after all attempts
        return null;
    }

    /**
     * Finds the Y coordinate of the surface at the given X,Z coordinates.
     * 
     * @param world The world to search in
     * @param x     The X coordinate
     * @param z     The Z coordinate
     * @return The Y coordinate of the surface, or -1 if not found
     */
    private static int findSurfaceY(ServerWorld world, int x, int z) {
        // Start from a high position and move down
        // 319 is typically the max build height in 1.20.1
        int y = world.getTopY();
        BlockPos.Mutable pos = new BlockPos.Mutable(x, y, z);

        // Make sure the chunk is loaded before accessing blocks
        // (This is redundant since we already check in findSuitablePosition, but it's
        // good practice)
        if (!ChunkLoader.loadChunksInRadius(world,
                new BlockPos(x, world.getBottomY() + (world.getTopY() - world.getBottomY()) / 2, z), 1)) {
            return -1; // Return -1 if chunk couldn't be loaded
        }

        // Move down until we find a non-air block
        while (y > world.getBottomY()) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);

            // If we found a non-air block that's not foliage
            if (!state.isAir() &&
                    state.getBlock() != Blocks.GRASS &&
                    state.getBlock() != Blocks.TALL_GRASS &&
                    state.getBlock() != Blocks.SNOW &&
                    state.getBlock() != Blocks.VINE &&
                    !state.getBlock().getTranslationKey().contains("leaves") &&
                    state.getBlock() != Blocks.WATER) {
                return y;
            }

            y--;
        }

        // If we couldn't find a suitable surface
        return -1;
    }

    /**
     * Randomly selects one of the block types we can place.
     * 
     * @return A random BlockType
     */
    private static BlockType getRandomBlockType() {
        BlockType[] types = BlockType.values();
        return types[random.nextInt(types.length)];
    }

    /**
     * Places the specified block type at the given position.
     * 
     * @param world     The world to place the block in
     * @param pos       The position to place the block at
     * @param blockType The type of block to place
     */
    private static void placeBlock(World world, BlockPos pos, BlockType blockType) {
        // Make sure the chunk is loaded before modifying blocks
        if (!ChunkLoader.loadChunksInRadius((ServerWorld) world, pos, 1)) {
            return; // Don't place the block if the chunk couldn't be loaded
        }

        switch (blockType) {
            case TORCH:
                // Place a torch
                world.setBlockState(pos, Blocks.TORCH.getDefaultState());
                break;

            case CRAFTING_TABLE:
                // Place a crafting table
                world.setBlockState(pos, Blocks.CRAFTING_TABLE.getDefaultState());
                break;

            case COBBLESTONE_PILLAR:
                // Place a 3-block tall cobblestone pillar
                for (int i = 0; i < 3; i++) {
                    BlockPos pillarPos = pos.up(i);
                    world.setBlockState(pillarPos, Blocks.COBBLESTONE.getDefaultState());
                }
                break;

            case FURNACE:
                // Place a furnace
                world.setBlockState(pos, Blocks.FURNACE.getDefaultState());

                // Get the furnace block entity to add items
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof FurnaceBlockEntity furnace) {
                    // Add coal to fuel slot (slot 1)
                    furnace.setStack(1, new ItemStack(Items.COAL, 1));

                    // Add a stack of raw chicken to input slot (slot 0)
                    furnace.setStack(0, new ItemStack(Items.CHICKEN, 64));
                }
                break;
        }
    }
}