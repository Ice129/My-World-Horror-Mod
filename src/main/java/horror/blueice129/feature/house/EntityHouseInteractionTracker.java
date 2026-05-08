package horror.blueice129.feature.house;

import net.minecraft.block.Block;
// import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
// import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class EntityHouseInteractionTracker {

    public record ChangedBlock(BlockPos pos, Block expected, Block actual) {}
    public record ChangedContainer(BlockPos pos) {}

    public static class InteractionRecord {
        public final List<ChangedBlock> brokenBlocks = new ArrayList<>();
        public final List<ChangedBlock> replacedBlocks = new ArrayList<>();
        public final List<BlockPos> playerPlacedBlocks = new ArrayList<>();
        public final List<ChangedContainer> changedContainers = new ArrayList<>();
        public final List<String> signTexts = new ArrayList<>();
        // entity diff requires Fabric event hooks — to be tracked separately
    }

    public static InteractionRecord buildDiff(ServerWorld world, BlockPos housePos, int currentStage) {
        InteractionRecord record = new InteractionRecord();

        Identifier stageId = new Identifier("horror-mod-129", "entitybase/house" + currentStage);
        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(stageId);
        Vec3i size = template.getSize();

        // Iterate every position within the template's bounding box.
        // For each local position, compare the world block against what the template placed.
        // NOTE: reading per-block template data requires access to StructureTemplate.blockInfoLists
        // (private field). Add a mixin accessor before filling in this logic.
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    // BlockPos worldPos = housePos.add(x, y, z);
                    // BlockState worldState = world.getBlockState(worldPos);

                    // TODO: get templateState at BlockPos(x, y, z) from the template
                    // BlockState templateState = ...;
                    // compare templateState to worldState:
                    //   - template non-air, world air/different block -> brokenBlocks or replacedBlocks
                    //   - template air, world non-air -> playerPlacedBlocks
                    //   - template is a container -> compare BlockEntity NBT inventories -> changedContainers
                    //   - template is a sign -> read live SignBlockEntity text -> signTexts
                }
            }
        }

        return record;
    }
}
