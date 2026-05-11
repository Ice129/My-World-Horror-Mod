package horror.blueice129.feature.house;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.mixin.StructureTemplateAccessorMixin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructurePlacementData;
import horror.blueice129.feature.house.WoodTypeProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class EntityHouseInteractionTracker {

    public enum InteractionType {
        BLOCK_BROKEN("block_broken"),
        BLOCK_REPLACED("block_replaced"),
        CONTAINER_CHANGED("container_changed"),
        ENTITY_CHANGED("entity_changed"),
        SIGN_TEXT_CHANGED("sign_text_changed");

        public final String id;

        InteractionType(String id) {
            this.id = id;
        }

        public static InteractionType fromId(String id) {
            for (InteractionType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return BLOCK_BROKEN;
        }
    }

    /**
     * Represents a single player interaction with the house.
     * Stores what was expected vs what actually exists.
     */
    public static class Interaction {
        public final BlockPos pos;
        public final InteractionType type;
        public final int stageDetected;
        public final String expectedState;
        public final String actualState;
        public final NbtCompound extraData;

        public Interaction(BlockPos pos, InteractionType type, int stageDetected, 
                          String expectedState, String actualState, NbtCompound extraData) {
            this.pos = pos;
            this.type = type;
            this.stageDetected = stageDetected;
            this.expectedState = expectedState;
            this.actualState = actualState;
            this.extraData = extraData;
        }

        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());
            nbt.putString("type", type.id);
            nbt.putInt("stage", stageDetected);
            nbt.putString("expected", expectedState);
            nbt.putString("actual", actualState);
            if (!extraData.isEmpty()) {
                nbt.put("extra", extraData);
            }
            return nbt;
        }

        public static Interaction readNbt(NbtCompound nbt) {
            BlockPos pos = new BlockPos(
                nbt.getInt("x"),
                nbt.getInt("y"),
                nbt.getInt("z")
            );
            InteractionType type = InteractionType.fromId(nbt.getString("type"));
            int stage = nbt.getInt("stage");
            String expected = nbt.getString("expected");
            String actual = nbt.getString("actual");
            NbtCompound extra = nbt.contains("extra") ? nbt.getCompound("extra") : new NbtCompound();
            return new Interaction(pos, type, stage, expected, actual, extra);
        }
    }

    /**
     * Container for all interactions for a specific house, organized by position.
     */
    public static class InteractionRecord {
        private final List<Interaction> interactions;

        public InteractionRecord() {
            this.interactions = new ArrayList<>();
        }

        public void addInteraction(Interaction interaction) {
            interactions.add(interaction);
        }

        public List<Interaction> getInteractionsAt(BlockPos pos) {
            List<Interaction> result = new ArrayList<>();
            for (Interaction i : interactions) {
                if (i.pos.equals(pos)) {
                    result.add(i);
                }
            }
            return result;
        }

        public List<Interaction> getInteractionsByType(InteractionType type) {
            List<Interaction> result = new ArrayList<>();
            for (Interaction i : interactions) {
                if (i.type == type) {
                    result.add(i);
                }
            }
            return result;
        }

        public List<Interaction> getInteractionsByStage(int stage) {
            List<Interaction> result = new ArrayList<>();
            for (Interaction i : interactions) {
                if (i.stageDetected == stage) {
                    result.add(i);
                }
            }
            return result;
        }

        public List<Interaction> getAllInteractions() {
            return new ArrayList<>(interactions);
        }

        public NbtList writeNbtList() {
            NbtList list = new NbtList();
            for (Interaction interaction : interactions) {
                list.add(interaction.writeNbt());
            }
            return list;
        }

        public static InteractionRecord readNbtList(NbtList list) {
            InteractionRecord record = new InteractionRecord();
            for (int i = 0; i < list.size(); i++) {
                NbtCompound nbt = list.getCompound(i);
                record.addInteraction(Interaction.readNbt(nbt));
            }
            return record;
        }
    }

    /**
     * Gets the normalized block ID. For wood-type variants (logs, planks, etc),
     * returns the actual wood type from the world. For template expectations,
     * uses the actual world state to determine the correct wood type.
     */
    private static String getNormalizedBlockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    /**
     * Builds a diff of all interactions between the template structure (with reactions applied)
     * and the current world state. Only records unexpected changes (deviations from the template).
     * If in updating phase, use isUpdatingPhase=true to skip recording interactions.
     *
     * @param world The server world
     * @param housePos The base position of the house
     * @param currentStage The stage that was just completed (to compare against)
     * @return InteractionRecord with all detected unexpected interactions
     */
    public static InteractionRecord buildDiff(ServerWorld world, BlockPos housePos, int currentStage) {
        return buildDiff(world, housePos, currentStage, false);
    }

    /**
     * Builds a diff of all interactions between the template structure (with reactions applied)
     * and the current world state. Detects broken blocks, replacements, container changes, etc.
     *
     * @param world The server world
     * @param housePos The base position of the house
     * @param currentStage The stage that was just completed (to compare against)
     * @param isUpdatingPhase If true, skips recording interactions (changes are expected during update)
     * @return InteractionRecord with all detected unexpected interactions
     */
    public static InteractionRecord buildDiff(ServerWorld world, BlockPos housePos, int currentStage, boolean isUpdatingPhase) {
        InteractionRecord record = new InteractionRecord();

        Identifier stageId = new Identifier("horror-mod-129", "entitybase/house" + currentStage);
        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(stageId);
        Vec3i size = template.getSize();
        List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = ((StructureTemplateAccessorMixin) (Object) template).horror$getBlockInfoLists();
        List<StructureTemplate.StructureEntityInfo> entityInfos = ((StructureTemplateAccessorMixin) (Object) template).horror$getEntities();

        // load wood type used during placement for this stage so we can apply same processor
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(world.getServer());
        String woodKey = "houseWood_stage_" + currentStage;
        String woodType = "birch"; // default (no-op in WoodTypeProcessor)
        if (state.hasNbtCompound(woodKey)) {
            try {
                woodType = state.getNbtCompound(woodKey).getString("woodType");
            } catch (Exception ignored) {}
        }
        WoodTypeProcessor woodProcessor = new WoodTypeProcessor(woodType);
        StructurePlacementData placementData = new StructurePlacementData().addProcessor(woodProcessor);

        HorrorMod129.LOGGER.info("Building interaction diff for stage {}: template size {}", currentStage, size);

        if (isUpdatingPhase) {
            return record;
        }

        BlockPos placementPos = housePos.add(HousePlacer.getStageOffset(currentStage));

        for (StructureTemplate.PalettedBlockInfoList palettedBlockInfoList : blockInfoLists) {
            for (StructureTemplate.StructureBlockInfo info : palettedBlockInfoList.getAll()) {
                BlockPos worldPos = placementPos.add(info.pos());
                // compute expected state after same placement processors applied
                StructureTemplate.StructureBlockInfo processed = woodProcessor.process(world,
                    worldPos, BlockPos.ORIGIN, info, info, placementData);
                BlockState expectedState = processed.state();
                BlockState actualState = world.getBlockState(worldPos);

                if (expectedState.isAir() && !actualState.isAir()) {
                    recordBlockInteraction(record, worldPos,
                            "minecraft:air",
                            getNormalizedBlockId(actualState),
                            currentStage,
                            InteractionType.BLOCK_REPLACED);
                    continue;
                }

                if (!expectedState.isAir() && actualState.isAir()) {
                    recordBlockInteraction(record, worldPos,
                            getNormalizedBlockId(expectedState),
                            "minecraft:air",
                            currentStage,
                            InteractionType.BLOCK_BROKEN);
                    continue;
                }

                if (!expectedState.isAir() && expectedState.getBlock() != actualState.getBlock()) {
                    recordBlockInteraction(record, worldPos,
                            getNormalizedBlockId(expectedState),
                            getNormalizedBlockId(actualState),
                            currentStage,
                            InteractionType.BLOCK_REPLACED);
                }

                BlockEntity blockEntity = world.getBlockEntity(worldPos);
                if (info.nbt() != null && blockEntity instanceof Inventory inventory) {
                    List<ItemStack> expectedItems = extractItemsFromNbt(info.nbt());
                    List<ItemStack> actualItems = extractItemsFromInventory(inventory);
                    if (!itemsMatch(expectedItems, actualItems)) {
                        recordContainerInteraction(record, worldPos, currentStage, expectedItems, actualItems);
                    }
                }
            }
        }

        for (StructureTemplate.StructureEntityInfo entityInfo : entityInfos) {
            BlockPos entityPos = placementPos.add(entityInfo.blockPos);
            if (world.getEntitiesByClass(net.minecraft.entity.decoration.ArmorStandEntity.class,
                    Box.of(Vec3d.ofCenter(entityPos), 1.0, 1.0, 1.0), entity -> true).isEmpty()) {
                recordEntityInteraction(record, entityPos, currentStage, "armor_stand", "missing", "template entity not present");
            }
        }

        return record;
    }

    /**
     * Records a block interaction (broken or replaced).
     */
    public static void recordBlockInteraction(InteractionRecord record, BlockPos pos, String expectedBlock, 
                                               String actualBlock, int stage, InteractionType type) {
        Interaction interaction = new Interaction(pos, type, stage, expectedBlock, actualBlock, new NbtCompound());
        record.addInteraction(interaction);
    }

    /**
     * Records container contents changes.
     */
    public static void recordContainerInteraction(InteractionRecord record, BlockPos pos, int stage,
                                                   List<ItemStack> expectedItems, List<ItemStack> actualItems) {
        NbtCompound extra = new NbtCompound();
        
        // Store expected items
        NbtList expectedList = new NbtList();
        for (ItemStack item : expectedItems) {
            NbtCompound itemNbt = new NbtCompound();
            item.writeNbt(itemNbt);
            expectedList.add(itemNbt);
        }
        extra.put("expectedItems", expectedList);
        
        // Store actual items
        NbtList actualList = new NbtList();
        for (ItemStack item : actualItems) {
            NbtCompound itemNbt = new NbtCompound();
            item.writeNbt(itemNbt);
            actualList.add(itemNbt);
        }
        extra.put("actualItems", actualList);

        Interaction interaction = new Interaction(pos, InteractionType.CONTAINER_CHANGED, stage, 
                                                   "container", "modified", extra);
        record.addInteraction(interaction);
    }

    /**
     * Records entity changes (killed sheep, missing armor stands, armor taken, etc).
     */
    public static void recordEntityInteraction(InteractionRecord record, BlockPos pos, int stage,
                                                String entityType, String change, String detail) {
        NbtCompound extra = new NbtCompound();
        extra.putString("detail", detail);

        Interaction interaction = new Interaction(pos, InteractionType.ENTITY_CHANGED, stage, 
                                                   entityType, change, extra);
        record.addInteraction(interaction);
    }

    /**
     * Records sign text changes.
     */
    public static void recordSignInteraction(InteractionRecord record, BlockPos pos, int stage,
                                              String[] expectedLines, String[] actualLines) {
        NbtCompound extra = new NbtCompound();
        
        // Store expected lines
        NbtList expectedList = new NbtList();
        for (String line : expectedLines) {
            expectedList.add(NbtString.of(line));
        }
        extra.put("expectedLines", expectedList);
        
        // Store actual lines
        NbtList actualList = new NbtList();
        for (String line : actualLines) {
            actualList.add(NbtString.of(line));
        }
        extra.put("actualLines", actualList);

        Interaction interaction = new Interaction(pos, InteractionType.SIGN_TEXT_CHANGED, stage,
                                                   "sign", "text_modified", extra);
        record.addInteraction(interaction);
    }

    private static List<ItemStack> extractItemsFromNbt(NbtCompound nbt) {
        List<ItemStack> items = new ArrayList<>();
        if (nbt == null || !nbt.contains("Items")) {
            return items;
        }

        NbtList itemsList = nbt.getList("Items", 10);
        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound itemNbt = itemsList.getCompound(i);
            items.add(ItemStack.fromNbt(itemNbt));
        }

        return items;
    }

    private static List<ItemStack> extractItemsFromInventory(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            items.add(inventory.getStack(slot).copy());
        }
        return items;
    }

    private static boolean itemsMatch(List<ItemStack> expectedItems, List<ItemStack> actualItems) {
        if (expectedItems.size() != actualItems.size()) {
            return false;
        }

        for (int slot = 0; slot < expectedItems.size(); slot++) {
            ItemStack expected = expectedItems.get(slot);
            ItemStack actual = actualItems.get(slot);

            if (expected.isEmpty() != actual.isEmpty()) {
                return false;
            }

            if (!expected.isEmpty() && !ItemStack.areEqual(expected, actual)) {
                return false;
            }
        }

        return true;
    }

    // === PERSISTENCE METHODS ===

    private static final String INTERACTIONS_KEY_PREFIX = "houseInteractions_stage_";

    /**
     * Saves interaction record to persistent state for a specific house stage.
     * @param server The Minecraft server
     * @param stage The stage number
     * @param record The interaction record to save
     */
    public static void saveInteractionsForStage(MinecraftServer server, int stage, InteractionRecord record) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        String key = INTERACTIONS_KEY_PREFIX + stage;
        
        NbtCompound compound = new NbtCompound();
        compound.put("interactions", record.writeNbtList());
        state.setNbtCompound(key, compound);
        
        HorrorMod129.LOGGER.info("[InteractionTracker] Saved {} interactions for stage {}", 
            record.getAllInteractions().size(), stage);
    }

    /**
     * Loads interaction record from persistent state for a specific house stage.
     * @param server The Minecraft server
     * @param stage The stage number
     * @return The interaction record, or empty record if not found
     */
    public static InteractionRecord loadInteractionsForStage(MinecraftServer server, int stage) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        String key = INTERACTIONS_KEY_PREFIX + stage;
        
        if (!state.hasNbtCompound(key)) {
            return new InteractionRecord();
        }
        
        NbtCompound compound = state.getNbtCompound(key);
        if (compound.contains("interactions")) {
            NbtList list = (NbtList) compound.get("interactions");
            return InteractionRecord.readNbtList(list);
        }
        
        return new InteractionRecord();
    }

    /**
     * Loads all accumulated interactions for stages up to and including the given stage.
     * @param server The Minecraft server
     * @param upToStage The maximum stage to include
     * @return Combined interaction record with all interactions from all stages
     */
    public static InteractionRecord loadAllInteractionsUpToStage(MinecraftServer server, int upToStage) {
        InteractionRecord combined = new InteractionRecord();
        
        for (int stage = 1; stage <= upToStage; stage++) {
            InteractionRecord stageRecord = loadInteractionsForStage(server, stage);
            for (Interaction interaction : stageRecord.getAllInteractions()) {
                combined.addInteraction(interaction);
            }
        }
        
        return combined;
    }

    /**
     * Clears all stored interactions for a specific stage.
     * @param server The Minecraft server
     * @param stage The stage number
     */
    public static void clearInteractionsForStage(MinecraftServer server, int stage) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        String key = INTERACTIONS_KEY_PREFIX + stage;
        state.removeNbtCompound(key);
    }

    /**
     * Clears all stored interaction records for the house.
     * @param server The Minecraft server
     */
    public static void clearAllInteractions(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        List<String> compoundIds = new ArrayList<>(state.getNbtCompoundIds());

        for (String id : compoundIds) {
            if (id.startsWith(INTERACTIONS_KEY_PREFIX)) {
                state.removeNbtCompound(id);
            }
        }
    }
}
