package horror.blueice129.feature.house;

import horror.blueice129.feature.house.EntityHouseInteractionTracker.Interaction;
import horror.blueice129.feature.house.EntityHouseInteractionTracker.InteractionRecord;
import horror.blueice129.feature.house.EntityHouseInteractionTracker.InteractionType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;

import java.util.ArrayList;
import java.util.List;

public class HouseModificationPlanner {

    public enum ModificationType {
        REINFORCE_DOOR,
        REPLACE_CONTAINER,
        SEAL_OPENING,
        HIDE_VALUABLES,
        TRIGGER_TRAP
    }

    public record HouseModification(ModificationType type, BlockPos targetPos, Block newBlock) {}

    /**
     * Plans house modifications based on detected player interactions.
     * Each interaction type triggers specific house responses to react to player tampering.
     *
     * @param diff The interaction record detecting player changes
     * @return List of modifications to apply to the structure
     */
    public static List<HouseModification> planModifications(InteractionRecord diff) {
        List<HouseModification> mods = new ArrayList<>();

        // Response to broken blocks: reinforce or seal the breach
        for (Interaction interaction : diff.getInteractionsByType(InteractionType.BLOCK_BROKEN)) {
            if (interaction.expectedState.toLowerCase().contains("door")) {
                // Reinforce broken doors with iron doors
                mods.add(new HouseModification(ModificationType.REINFORCE_DOOR, interaction.pos, Blocks.IRON_DOOR));
            } else if (interaction.expectedState.toLowerCase().contains("wall") || 
                       interaction.expectedState.toLowerCase().contains("wood")) {
                // Seal openings in walls with harder material
                mods.add(new HouseModification(ModificationType.SEAL_OPENING, interaction.pos, Blocks.OBSIDIAN));
            }
        }

        // Response to block replacements: undo or reinforce against future changes
        for (Interaction interaction : diff.getInteractionsByType(InteractionType.BLOCK_REPLACED)) {
            // If original was a door and player replaced it, upgrade to iron door
            if (interaction.expectedState.toLowerCase().contains("door")) {
                mods.add(new HouseModification(ModificationType.REINFORCE_DOOR, interaction.pos, Blocks.IRON_DOOR));
            }
        }

        // Response to container changes: move valuables to new hidden location
        for (Interaction interaction : diff.getInteractionsByType(InteractionType.CONTAINER_CHANGED)) {
            mods.add(new HouseModification(ModificationType.HIDE_VALUABLES, interaction.pos, Blocks.CHEST));
        }

        // Response to entity changes (killed sheep, missing armor stands): spawn replacement defenders
        for (Interaction interaction : diff.getInteractionsByType(InteractionType.ENTITY_CHANGED)) {
            String change = interaction.actualState;
            if (change.equals("missing") || change.equals("killed")) {
                // TODO: trigger entity spawning or trap mechanism
                // For now, mark positions where defenses were disabled
            }
        }

        // Response to sign changes: would indicate player discovered something
        for (Interaction interaction : diff.getInteractionsByType(InteractionType.SIGN_TEXT_CHANGED)) {
            // Player read the signs - house could escalate haunting
            // TODO: add escalation logic
        }

        return mods;
    }

    /**
     * Applies all planned modifications to the world.
     * Each modification type has its own application logic.
     *
     * @param world The server world
     * @param mods List of modifications to apply
     */
    public static void applyModifications(ServerWorld world, List<HouseModification> mods) {
        for (HouseModification mod : mods) {
            switch (mod.type()) {
                case REINFORCE_DOOR -> {
                    // Change door type to iron door, but keep original orientation
                    BlockState originalState = world.getBlockState(mod.targetPos());
                    BlockState newState = mod.newBlock().getDefaultState()
                            .with(DoorBlock.FACING, originalState.get(DoorBlock.FACING))
                            .with(DoorBlock.HALF, originalState.get(DoorBlock.HALF))
                            .with(DoorBlock.OPEN, false);
                    world.setBlockState(mod.targetPos(), newState);
                }
                case SEAL_OPENING -> {
                    // Place obsidian to seal breaches
                    world.setBlockState(mod.targetPos(), mod.newBlock().getDefaultState());
                }
                case REPLACE_CONTAINER -> {
                    // Replace container with new one
                    world.setBlockState(mod.targetPos(), mod.newBlock().getDefaultState());
                }
                case HIDE_VALUABLES -> {
                    // TODO: find hidden location nearby and relocate valuable items
                    // For now, just mark the position
                }
                case TRIGGER_TRAP -> {
                    // TODO: activate or spawn trap mechanism
                }
            }
        }
    }
}
