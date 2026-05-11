package horror.blueice129.feature.house;

import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;

public class HousePlacer {

    public static void killPreviousPhaseCreatures(BlockPos housePos, ServerWorld world) {
        Box searchBox = new Box(housePos.getX() - 40, housePos.getY() - 40, housePos.getZ() - 40,
                               housePos.getX() + 40, housePos.getY() + 40, housePos.getZ() + 40);
        world.getEntitiesByClass(SheepEntity.class, searchBox, sheep -> true).forEach(sheep -> {
            sheep.discard();
            HorrorMod129.LOGGER.info("[HousePlacer] Removed sheep at {} before next phase", sheep.getBlockPos().toShortString());
        });
        world.getEntitiesByClass(ArmorStandEntity.class, searchBox, armorStand -> true).forEach(armorStand -> {
            armorStand.discard();
            HorrorMod129.LOGGER.info("[HousePlacer] Removed armor stand at {} before next phase", armorStand.getBlockPos().toShortString());
        });
    }

    public static void placeHouse(int stage, BlockPos startPos, ServerWorld world) {
        String woodType = getWoodType(world, startPos);
        HorrorMod129.LOGGER.info("[HousePlacer] Detected wood type: '{}', placing stage {} at {}", woodType, stage,
                startPos.toShortString());

        // persist wood type used for this stage so diff/tracker can apply same processors
        try {
            horror.blueice129.data.HorrorModPersistentState state = horror.blueice129.data.HorrorModPersistentState.getServerState(world.getServer());
            net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
            nbt.putString("woodType", woodType);
            state.setNbtCompound("houseWood_stage_" + stage, nbt);
        } catch (Exception e) {
            HorrorMod129.LOGGER.warn("[HousePlacer] Failed to persist wood type for stage {}: {}", stage, e.getMessage());
        }

        Identifier structureId = new Identifier("horror-mod-129", "entitybase/house" + stage);
        BlockPos stageOffset = getStageOffset(stage);
        BlockPos placementPos = startPos.add(stageOffset);
        HorrorMod129.LOGGER.info("[HousePlacer] Stage {} offset {} -> placement at {}",
            stage, stageOffset.toShortString(), placementPos.toShortString());

        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(structureId);
        HorrorMod129.LOGGER.info("[HousePlacer] Template size: {}", template.getSize());

        clearInventoriesInPlacementArea(world, placementPos, template.getSize());

        StructurePlacementData placementData = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(BlockRotation.NONE)
                .setIgnoreEntities(false)
                .addProcessor(new WoodTypeProcessor(woodType));

        template.place(world, placementPos, placementPos, placementData, Random.create(), Block.NOTIFY_ALL);
    }

    private static void clearInventoriesInPlacementArea(ServerWorld world, BlockPos origin, Vec3i size) {
        int clearedContainers = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.add(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (!(blockEntity instanceof Inventory inventory)) {
                        continue;
                    }

                    boolean hadItems = false;
                    for (int slot = 0; slot < inventory.size(); slot++) {
                        if (!inventory.getStack(slot).isEmpty()) {
                            hadItems = true;
                            inventory.removeStack(slot);
                        }
                    }

                    if (hadItems) {
                        blockEntity.markDirty();
                        clearedContainers++;
                    }
                }
            }
        }

        if (clearedContainers > 0) {
            HorrorMod129.LOGGER.info("[HousePlacer] Cleared {} container inventories before template placement", clearedContainers);
        }
    }

    public static BlockPos getStageOffset(int stage) {
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
