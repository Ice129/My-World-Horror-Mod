package horror.blueice129.feature.house;

import horror.blueice129.feature.house.EntityHouseInteractionTracker.Interaction;
import horror.blueice129.feature.house.EntityHouseInteractionTracker.InteractionRecord;
import horror.blueice129.feature.house.EntityHouseInteractionTracker.InteractionType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class HouseModificationPlanner {

    public enum ModificationType {
        REINFORCE_DOOR,
        ADD_HIDDEN_CHEST,
        REPLACE_BLOCK
        // TODO: add more response types
    }

    public record HouseModification(ModificationType type, BlockPos targetPos, Block newBlock) {}

    public static List<HouseModification> planModifications(InteractionRecord diff) {
        List<HouseModification> mods = new ArrayList<>();

        // Process broken blocks
        for (Interaction interaction : diff.getInteractionsByType(InteractionType.BLOCK_BROKEN)) {
            if (interaction.expectedState.toLowerCase().contains("door")) {
                mods.add(new HouseModification(ModificationType.REINFORCE_DOOR, interaction.pos, Blocks.IRON_DOOR));
            }
            // TODO: add responses for other broken block types
        }

        // Process container changes
        for (Interaction interaction : diff.getInteractionsByType(InteractionType.CONTAINER_CHANGED)) {
            mods.add(new HouseModification(ModificationType.ADD_HIDDEN_CHEST, interaction.pos, Blocks.CHEST));
        }

        // TODO: handle BLOCK_REPLACED, ENTITY_CHANGED, SIGN_TEXT_CHANGED

        return mods;
    }

    public static void applyModifications(ServerWorld world, List<HouseModification> mods) {
        for (HouseModification mod : mods) {
            switch (mod.type()) {
                case REINFORCE_DOOR -> {
                    // TODO: place iron door correctly (lower + upper half with matching properties)
                }
                case ADD_HIDDEN_CHEST -> {
                    // TODO: find a hidden location near targetPos and place a stocked chest
                }
                case REPLACE_BLOCK -> world.setBlockState(mod.targetPos(), mod.newBlock().getDefaultState());
            }
        }
    }
}
