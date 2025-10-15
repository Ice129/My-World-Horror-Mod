package horror.blueice129.data;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import horror.blueice129.HorrorMod129;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class StateSaverAndLoader extends PersistentState {
    // The unique identifier for this persistent state
    private static final String IDENTIFIER = HorrorMod129.MOD_ID + "_state";

    // Map to store integer values with string keys
    private Map<String, Integer> intValues;

    private Map<String, StripMineBlocks> stripMineBlocks;

    private Map<String, BlockPos> blockPositions;

    // Constructor
    public StateSaverAndLoader() {
        // Initialize the map
        this.intValues = new HashMap<>();
        this.stripMineBlocks = new HashMap<>();
        this.blockPositions = new HashMap<>();
    }

    public StateSaverAndLoader(
            Map<String, Integer> intValues,
            Map<String, StripMineBlocks> stripMineBlocks,
            Map<String, BlockPos> blockPositions) {
        this.intValues = intValues;
        this.stripMineBlocks = stripMineBlocks;
        this.blockPositions = blockPositions;
    }

    /**
     * Saves the state to NBT
     * @return NBT compound containing the state data
     */
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Save integer values
        NbtCompound intValuesNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : intValues.entrySet()) {
            intValuesNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("intValues", intValuesNbt);

        // Save StripMineBlocks
        NbtCompound stripMineBlocksNbt = new NbtCompound();
        for (Map.Entry<String, StripMineBlocks> entry : stripMineBlocks.entrySet()) {
            NbtCompound blockNbt = new NbtCompound();
            // Use the toNbt method we implemented in StripMineBlocks
            entry.getValue().toNbt(blockNbt);
            stripMineBlocksNbt.put(entry.getKey(), blockNbt);
        }

        nbt.put("stripMineBlocks", stripMineBlocksNbt);
        // Save BlockPos values
        NbtCompound blockPositionsNbt = new NbtCompound();
        for (Map.Entry<String, BlockPos> entry : blockPositions.entrySet()) {
            NbtCompound posNbt = new NbtCompound();
            posNbt.putInt("x", entry.getValue().getX());
            posNbt.putInt("y", entry.getValue().getY());
            posNbt.putInt("z", entry.getValue().getZ());
            blockPositionsNbt.put(entry.getKey(), posNbt);
        }
        nbt.put("blockPositions", blockPositionsNbt);
        return nbt;
    }
    
    /**
     * Creates a StateSaverAndLoader instance from NBT data
     * @param nbt The NBT data to load from
     * @return A new StateSaverAndLoader instance
     */
    public static StateSaverAndLoader createFromNbt(NbtCompound nbt) {
        // Load integer values
        Map<String, Integer> intValues = new HashMap<>();
        NbtCompound intValuesNbt = nbt.getCompound("intValues");
        for (String key : intValuesNbt.getKeys()) {
            intValues.put(key, intValuesNbt.getInt(key));
        }
        
        // Load StripMineBlocks
        Map<String, StripMineBlocks> stripMineBlocks = new HashMap<>();
        NbtCompound stripMineBlocksNbt = nbt.getCompound("stripMineBlocks");
        for (String key : stripMineBlocksNbt.getKeys()) {
            NbtCompound blockNbt = stripMineBlocksNbt.getCompound(key);
            StripMineBlocks blocks = StripMineBlocks.fromNbt(blockNbt);
            stripMineBlocks.put(key, blocks);
        }
        
        // Load BlockPos values
        Map<String, BlockPos> blockPositions = new HashMap<>();
        NbtCompound blockPositionsNbt = nbt.getCompound("blockPositions");
        for (String key : blockPositionsNbt.getKeys()) {
            NbtCompound posNbt = blockPositionsNbt.getCompound(key);
            int x = posNbt.getInt("x");
            int y = posNbt.getInt("y");
            int z = posNbt.getInt("z");
            blockPositions.put(key, new BlockPos(x, y, z));
        }
        
        return new StateSaverAndLoader(intValues, stripMineBlocks, blockPositions);
    }
    
    /**
     * Gets the persistent state from the server, creating it if it doesn't exist
     * @param server The Minecraft server instance
     * @return The persistent state instance
     */
    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        // Get the persistent state manager for the overworld
        PersistentStateManager persistentStateManager = server.getWorld(net.minecraft.world.World.OVERWORLD).getPersistentStateManager();
        
        // Get or create the state
        StateSaverAndLoader state = persistentStateManager.getOrCreate(
            StateSaverAndLoader::createFromNbt,
            StateSaverAndLoader::new,
            IDENTIFIER
        );
        
        // Mark as dirty to ensure it's saved
        state.markDirty();
        
        return state;
    }
    
    /**
     * Gets a StripMineBlocks object by ID
     * @param id The ID of the StripMineBlocks to retrieve
     * @return The StripMineBlocks object, or null if not found
     */
    public StripMineBlocks getStripMineBlocks(String id) {
        return stripMineBlocks.get(id);
    }
    
    /**
     * Sets a StripMineBlocks object by ID
     * @param id The ID to use
     * @param blocks The StripMineBlocks object
     */
    public void setStripMineBlocks(String id, StripMineBlocks blocks) {
        stripMineBlocks.put(id, blocks);
        this.markDirty(); // Mark as dirty to ensure it's saved
    }
    
    /**
     * Removes a StripMineBlocks object
     * @param id The ID of the StripMineBlocks to remove
     */
    public void removeStripMineBlocks(String id) {
        if (stripMineBlocks.containsKey(id)) {
            stripMineBlocks.remove(id);
            this.markDirty(); // Mark as dirty to ensure it's saved
        }
    }
    
    /**
     * Checks if a StripMineBlocks object exists
     * @param id The ID to check
     * @return True if the StripMineBlocks exists
     */
    public boolean hasStripMineBlocks(String id) {
        return stripMineBlocks.containsKey(id);
    }
    
    /**
     * Gets all StripMineBlocks IDs
     * @return Set of all StripMineBlocks IDs
     */
    public Set<String> getStripMineBlocksIds() {
        return stripMineBlocks.keySet();
    }

}
