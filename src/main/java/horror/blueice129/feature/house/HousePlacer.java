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
        BlockPos stageOffset = getStageOffset(stage);
        BlockPos placementPos = startPos.add(stageOffset);
        HorrorMod129.LOGGER.info("[HousePlacer] Stage {} offset {} -> placement at {}",
            stage, stageOffset.toShortString(), placementPos.toShortString());

        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(structureId);
        HorrorMod129.LOGGER.info("[HousePlacer] Template size: {}", template.getSize());

        StructurePlacementData placementData = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(BlockRotation.NONE)
                .setIgnoreEntities(false)
                .addProcessor(new WoodTypeProcessor(woodType));

        template.place(world, placementPos, placementPos, placementData, Random.create(), Block.NOTIFY_ALL);
    }

    private static BlockPos getStageOffset(int stage) {
        return switch (stage) {
            case 1 -> new BlockPos(0, 1, 0);
            case 2 -> new BlockPos(-1, 0, -1);
            case 3 -> new BlockPos(-7, -3, -1);
            default -> BlockPos.ORIGIN;
        };
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
            if ("block.minecraft.air".equals(block)) {
                continue;
            }
            String type = block.replace("block.minecraft.", "").replace("_log", "");
            woodTypeCounts.put(type, woodTypeCounts.getOrDefault(type, 0) + 1);
        }
        HorrorMod129.LOGGER.info("[HousePlacer] Wood type counts: {}", woodTypeCounts);
        if (woodTypeCounts.isEmpty()) {
            return "oak";
        }

        int highestCount = woodTypeCounts.values().stream().max(Integer::compareTo).orElse(0);
        java.util.List<String> tiedTypes = woodTypeCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == highestCount)
                .map(java.util.Map.Entry::getKey)
                .toList();

        return tiedTypes.get(world.getRandom().nextInt(tiedTypes.size()));
    }
}
