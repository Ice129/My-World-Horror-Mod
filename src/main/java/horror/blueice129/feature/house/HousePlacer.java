package horror.blueice129.feature.house;

import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.block.Block;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;

public class HousePlacer {

    public static void placeHouse(int stage, BlockPos startPos, ServerWorld world) {
        String woodType = getWoodType(world, startPos);
        HorrorMod129.LOGGER.info("[HousePlacer] Detected wood type: '{}', placing stage {} at {}", woodType, stage,
                startPos.toShortString());

        Identifier structureId = new Identifier("horror-mod-129", "entitybase/house" + stage);
        // Always place house 1 block up
        startPos = startPos.up();

        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(structureId);
        HorrorMod129.LOGGER.info("[HousePlacer] Template size: {}", template.getSize());

        StructurePlacementData placementData = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(BlockRotation.NONE)
                .setIgnoreEntities(false)
                .addProcessor(new WoodTypeProcessor(woodType));

        template.place(world, startPos, startPos, placementData, Random.create(), Block.NOTIFY_ALL);
    }

    private static String getWoodType(ServerWorld world, BlockPos pos) {
        BlockPos[] trees = SurfaceFinder.findTreePositions(world, pos, 50);
        HorrorMod129.LOGGER.info("[HousePlacer] Found {} trees within 50 blocks", trees.length);
        if (trees.length == 0) {
            return "oak";
        }
        // dictionary to count types:
        java.util.Map<String, Integer> woodTypeCounts = new java.util.HashMap<>();
        for (BlockPos treePos : trees) {
            String block = world.getBlockState(treePos).getBlock().getTranslationKey();
            String type = block.replace("block.minecraft.", "").replace("_log", "");
            woodTypeCounts.put(type, woodTypeCounts.getOrDefault(type, 0) + 1);
        }
        HorrorMod129.LOGGER.info("[HousePlacer] Wood type counts: {}", woodTypeCounts);
        return woodTypeCounts.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).get().getKey();
    }
}
