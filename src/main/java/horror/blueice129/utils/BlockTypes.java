package horror.blueice129.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.LeavesBlock;

public class BlockTypes {
    /**
     * Checks if a block state is an ore block.
     * 
     * @param state The block state to check
     * @return True if the block is an ore, false otherwise
     */
    public static boolean isOreBlock(BlockState state) {
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

    public static boolean isFoliage(Block block, boolean includeSnow) {
        if (block == Blocks.GRASS
                || block == Blocks.TALL_GRASS
                || (includeSnow && block == Blocks.SNOW)
                || block == Blocks.VINE
                || block == Blocks.DEAD_BUSH
                || block == Blocks.FERN
                || block == Blocks.LARGE_FERN
                || block == Blocks.SUGAR_CANE) {
            return true;
        }
        // leaves
        if (block instanceof LeavesBlock) {
            return true;
        }
        // flowers
        if (block instanceof FlowerBlock) {
            return true;
        }

        return false;
    }

    /**
     * returns true if the block is water
     * 
     * @param block The block to check
     * @return boolean indicating if the block is water
     */
    public static boolean isWater(Block block) {
        if (block == Blocks.WATER) {
            return true;
        }
        if (block == Blocks.BUBBLE_COLUMN) {
            return true;
        }
        if (block instanceof net.minecraft.block.FluidBlock) {
            return true;
        }

        return false;
    }

    public static boolean isLogBlock(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG
                || block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG;
    }
}
