package horror.blueice129.feature.house;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.mixin.StructureTemplateAccessorMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.structure.StructureTemplate;
// import net.minecraft.structure.StructurePlacementData;
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

    private static boolean isLeafBlock(BlockState state) {
        return state.getBlock() instanceof LeavesBlock;
    }

    /**
     * Snapshot of structure state at a specific moment (blocks, entities, containers).
     * Stored to compare against later world state to detect player interactions.
     */
    public static class StructureSnapshot {
        public final BlockPos basePos;
        public final int stage;
        public final Vec3i size;
        // Map of BlockPos to (blockId, nbt)
        public final java.util.Map<BlockPos, BlockStateSnapshot> blockSnapshots;
        // List of entity snapshots
        public final List<EntitySnapshot> entitySnapshots;

        public StructureSnapshot(BlockPos basePos, int stage, Vec3i size) {
            this.basePos = basePos;
            this.stage = stage;
            this.size = size;
            this.blockSnapshots = new java.util.HashMap<>();
            this.entitySnapshots = new ArrayList<>();
        }

        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("baseX", basePos.getX());
            nbt.putInt("baseY", basePos.getY());
            nbt.putInt("baseZ", basePos.getZ());
            nbt.putInt("stage", stage);
            nbt.putInt("sizeX", size.getX());
            nbt.putInt("sizeY", size.getY());
            nbt.putInt("sizeZ", size.getZ());
            
            NbtList blocksList = new NbtList();
            for (var entry : blockSnapshots.entrySet()) {
                NbtCompound blockNbt = new NbtCompound();
                blockNbt.putInt("x", entry.getKey().getX());
                blockNbt.putInt("y", entry.getKey().getY());
                blockNbt.putInt("z", entry.getKey().getZ());
                blockNbt.putString("blockId", entry.getValue().blockId);
                if (entry.getValue().containerNbt != null) {
                    blockNbt.put("containerData", entry.getValue().containerNbt);
                }
                blocksList.add(blockNbt);
            }
            nbt.put("blocks", blocksList);
            
            NbtList entitiesList = new NbtList();
            for (EntitySnapshot entity : entitySnapshots) {
                NbtCompound entityNbt = new NbtCompound();
                entityNbt.putInt("x", entity.pos.getX());
                entityNbt.putInt("y", entity.pos.getY());
                entityNbt.putInt("z", entity.pos.getZ());
                entityNbt.putString("type", entity.entityType);
                entitiesList.add(entityNbt);
            }
            nbt.put("entities", entitiesList);
            
            return nbt;
        }

        public static StructureSnapshot readNbt(NbtCompound nbt) {
            BlockPos basePos = new BlockPos(nbt.getInt("baseX"), nbt.getInt("baseY"), nbt.getInt("baseZ"));
            int stage = nbt.getInt("stage");
            Vec3i size = new Vec3i(nbt.getInt("sizeX"), nbt.getInt("sizeY"), nbt.getInt("sizeZ"));
            StructureSnapshot snapshot = new StructureSnapshot(basePos, stage, size);
            
            NbtList blocksList = nbt.getList("blocks", 10);
            for (int i = 0; i < blocksList.size(); i++) {
                NbtCompound blockNbt = blocksList.getCompound(i);
                BlockPos pos = new BlockPos(blockNbt.getInt("x"), blockNbt.getInt("y"), blockNbt.getInt("z"));
                String blockId = blockNbt.getString("blockId");
                NbtCompound containerData = blockNbt.contains("containerData", 10) ? blockNbt.getCompound("containerData") : null;
                snapshot.blockSnapshots.put(pos, new BlockStateSnapshot(blockId, containerData));
            }
            
            NbtList entitiesList = nbt.getList("entities", 10);
            for (int i = 0; i < entitiesList.size(); i++) {
                NbtCompound entityNbt = entitiesList.getCompound(i);
                BlockPos pos = new BlockPos(entityNbt.getInt("x"), entityNbt.getInt("y"), entityNbt.getInt("z"));
                String entityType = entityNbt.getString("type");
                snapshot.entitySnapshots.add(new EntitySnapshot(pos, entityType));
            }
            
            return snapshot;
        }
    }

    public static class BlockStateSnapshot {
        public final String blockId;
        public final NbtCompound containerNbt;

        public BlockStateSnapshot(String blockId, NbtCompound containerNbt) {
            this.blockId = blockId;
            this.containerNbt = containerNbt;
        }
    }

    public static class EntitySnapshot {
        public final BlockPos pos;
        public final String entityType;

        public EntitySnapshot(BlockPos pos, String entityType) {
            this.pos = pos;
            this.entityType = entityType;
        }
    }

    /**
     * Captures a snapshot of the structure state after placement.
     * Stores all blocks, entities, and container contents for later comparison.
     *
     * @param world The server world
     * @param housePos The base position of the house
     * @param stage The stage being placed
     * @return StructureSnapshot with current state of the structure
     */
    public static StructureSnapshot captureStructureSnapshot(ServerWorld world, BlockPos housePos, int stage) {
        Identifier stageId = new Identifier("horror-mod-129", "entitybase/house" + stage);
        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(stageId);
        Vec3i size = template.getSize();
        List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = ((StructureTemplateAccessorMixin) (Object) template).horror$getBlockInfoLists();
        List<StructureTemplate.StructureEntityInfo> entityInfos = ((StructureTemplateAccessorMixin) (Object) template).horror$getEntities();

        BlockPos placementPos = housePos.add(HousePlacer.getStageOffset(stage));
        StructureSnapshot snapshot = new StructureSnapshot(housePos, stage, size);

        // Capture blocks
        for (StructureTemplate.PalettedBlockInfoList palettedBlockInfoList : blockInfoLists) {
            for (StructureTemplate.StructureBlockInfo info : palettedBlockInfoList.getAll()) {
                BlockPos worldPos = placementPos.add(info.pos());
                BlockState blockState = world.getBlockState(worldPos);
                if (isLeafBlock(blockState)) {
                    continue;
                }
                if (blockState.getBlock() instanceof DoorBlock && blockState.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                    continue;
                }
                String blockId = getNormalizedBlockId(blockState);
                
                NbtCompound containerData = null;
                BlockEntity blockEntity = world.getBlockEntity(worldPos);
                if (blockEntity instanceof Inventory && shouldCompareContainerContents(blockEntity.createNbt())) {
                    containerData = blockEntity.createNbt();
                }
                
                snapshot.blockSnapshots.put(worldPos, new BlockStateSnapshot(blockId, containerData));
            }
        }

        // Capture entities
        for (StructureTemplate.StructureEntityInfo entityInfo : entityInfos) {
            BlockPos entityPos = placementPos.add(entityInfo.blockPos);
            if (hasMatchingArmorStand(world, placementPos, entityInfo)) {
                snapshot.entitySnapshots.add(new EntitySnapshot(entityPos, "armor_stand"));
            }
        }

        HorrorMod129.LOGGER.info("Captured structure snapshot for stage {}: {} blocks, {} entities",
                stage, snapshot.blockSnapshots.size(), snapshot.entitySnapshots.size());

        return snapshot;
    }

    /**
     * Builds a diff between a stored snapshot and current world state.
     * Detects all changes: broken blocks, replacements, container changes, missing entities.
     *
     * @param world The server world
     * @param snapshot The previously captured structure snapshot
     * @return InteractionRecord with all detected interactions
     */
    public static InteractionRecord buildDiffFromSnapshot(ServerWorld world, StructureSnapshot snapshot) {
        InteractionRecord record = new InteractionRecord();

        HorrorMod129.LOGGER.info("Building diff from snapshot for stage {}: comparing {} block positions",
                snapshot.stage, snapshot.blockSnapshots.size());

        // Compare blocks
        for (var entry : snapshot.blockSnapshots.entrySet()) {
            BlockPos worldPos = entry.getKey();
            BlockStateSnapshot expected = entry.getValue();
            BlockState actualState = world.getBlockState(worldPos);
            if (isLeafBlock(actualState) || expected.blockId.contains("leaves") || expected.blockId.contains("leaf")) {
                continue;
            }
            String actualId = getNormalizedBlockId(actualState);

            // Detect broken blocks (expected non-air, actual air)
            if (!expected.blockId.equals("minecraft:air") && actualState.isAir()) {
                recordBlockInteraction(record, worldPos,
                        expected.blockId,
                        "minecraft:air",
                        snapshot.stage,
                        InteractionType.BLOCK_BROKEN);
                continue;
            }

            // Detect replacements
            if (!expected.blockId.equals(actualId)) {
                recordBlockInteraction(record, worldPos,
                        expected.blockId,
                        actualId,
                        snapshot.stage,
                        InteractionType.BLOCK_REPLACED);
            }

            // Detect container changes
            if (expected.containerNbt != null) {
                BlockEntity blockEntity = world.getBlockEntity(worldPos);
                if (blockEntity instanceof Inventory inventory) {
                    List<ItemStack> expectedItems = extractItemsFromNbt(expected.containerNbt, inventory.size());
                    List<ItemStack> actualItems = extractItemsFromInventory(inventory);
                    if (!itemsMatch(expectedItems, actualItems)) {
                        recordContainerInteraction(record, worldPos, snapshot.stage, expectedItems, actualItems);
                    }
                }
            }
        }

        // Compare entities
        for (EntitySnapshot expectedEntity : snapshot.entitySnapshots) {
            if (!hasArmorStandAt(world, expectedEntity.pos)) {
                recordEntityInteraction(record, expectedEntity.pos, snapshot.stage,
                        "armor_stand", "missing", "entity was removed");
            }
        }

        return record;
    }

    private static boolean hasArmorStandAt(ServerWorld world, BlockPos pos) {
        Box box = new Box(pos).expand(0.5);
        return !world.getEntitiesByClass(ArmorStandEntity.class, box, e -> true).isEmpty();
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

    private static boolean shouldCompareContainerContents(NbtCompound nbt) {
        return nbt != null && nbt.contains("Items", 9) && !nbt.contains("LootTable");
    }

    private static List<ItemStack> extractItemsFromNbt(NbtCompound nbt, int inventorySize) {
        List<ItemStack> items = new ArrayList<>(inventorySize);
        for (int slot = 0; slot < inventorySize; slot++) {
            items.add(ItemStack.EMPTY);
        }

        if (nbt == null || !nbt.contains("Items", 9)) {
            return items;
        }

        NbtList itemsList = nbt.getList("Items", 10);
        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound itemNbt = itemsList.getCompound(i);
            ItemStack stack = ItemStack.fromNbt(itemNbt);
            if (stack.isEmpty()) {
                continue;
            }

            int slot = itemNbt.contains("Slot", 99) ? itemNbt.getByte("Slot") & 255 : i;
            if (slot >= 0 && slot < items.size()) {
                items.set(slot, stack);
            }
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

    private static boolean hasMatchingArmorStand(ServerWorld world, BlockPos placementPos,
                                                 StructureTemplate.StructureEntityInfo entityInfo) {
        Vec3d expectedPos = new Vec3d(placementPos.getX(), placementPos.getY(), placementPos.getZ()).add(entityInfo.pos);
        BlockPos expectedBlockPos = placementPos.add(entityInfo.blockPos);
        Box searchBox = new Box(
                expectedPos.x - 1.0D, expectedPos.y - 1.0D, expectedPos.z - 1.0D,
                expectedPos.x + 1.0D, expectedPos.y + 1.0D, expectedPos.z + 1.0D);

        for (ArmorStandEntity armorStand : world.getEntitiesByClass(ArmorStandEntity.class, searchBox, entity -> true)) {
            if (armorStand.getBlockPos().equals(expectedBlockPos)) {
                return true;
            }

            if (armorStand.getPos().squaredDistanceTo(expectedPos) <= 1.0D) {
                return true;
            }
        }

        return false;
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
    private static final String SNAPSHOT_KEY_PREFIX = "houseSnapshot_stage_";

    /**
     * Saves structure snapshot to persistent state for a specific stage.
     * @param server The Minecraft server
     * @param stage The stage number
     * @param snapshot The snapshot to save
     */
    public static void saveStructureSnapshot(MinecraftServer server, int stage, StructureSnapshot snapshot) {
        try {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            state.setNbtCompound(SNAPSHOT_KEY_PREFIX + stage, snapshot.writeNbt());
            HorrorMod129.LOGGER.info("Saved structure snapshot for stage {}", stage);
        } catch (Exception e) {
            HorrorMod129.LOGGER.warn("Failed to save snapshot for stage {}: {}", stage, e.getMessage());
        }
    }

    /**
     * Loads structure snapshot from persistent state for a specific stage.
     * @param server The Minecraft server
     * @param stage The stage number
     * @return The snapshot, or null if not found
     */
    public static StructureSnapshot loadStructureSnapshot(MinecraftServer server, int stage) {
        try {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            String key = SNAPSHOT_KEY_PREFIX + stage;
            if (state.hasNbtCompound(key)) {
                StructureSnapshot snapshot = StructureSnapshot.readNbt(state.getNbtCompound(key));
                HorrorMod129.LOGGER.info("Loaded structure snapshot for stage {}: {} blocks, {} entities",
                        stage, snapshot.blockSnapshots.size(), snapshot.entitySnapshots.size());
                return snapshot;
            }
        } catch (Exception e) {
            HorrorMod129.LOGGER.warn("Failed to load snapshot for stage {}: {}", stage, e.getMessage());
        }
        return null;
    }

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
