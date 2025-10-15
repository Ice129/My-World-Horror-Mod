package horror.blueice129.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    
    /**
     * Gets an integer value by its ID
     * @param id The ID of the value to retrieve
     * @param defaultValue The default value to return if not found
     * @return The integer value, or the default value if not found
     */
    public int getIntValue(String id, int defaultValue) {
        return intValues.getOrDefault(id, defaultValue);
    }
    
    /**
     * Sets an integer value by its ID
     * @param id The ID of the value to set
     * @param value The integer value
     */
    public void setIntValue(String id, int value) {
        intValues.put(id, value);
        this.markDirty();
    }
    
    /**
     * Removes an integer value
     * @param id The ID of the value to remove
     */
    public void removeIntValue(String id) {
        if (intValues.containsKey(id)) {
            intValues.remove(id);
            this.markDirty();
        }
    }
    
    /**
     * Checks if an integer value exists
     * @param id The ID to check
     * @return True if the integer value exists
     */
    public boolean hasIntValue(String id) {
        return intValues.containsKey(id);
    }
    
    /**
     * Gets all integer value IDs
     * @return Set of all integer value IDs
     */
    public Set<String> getIntValueIds() {
        return intValues.keySet();
    }
    
    /**
     * Gets a block position by its ID
     * @param id The ID of the position to retrieve
     * @return The block position, or null if not found
     */
    public BlockPos getPosition(String id) {
        return blockPositions.get(id);
    }
    
    /**
     * Sets a block position by its ID
     * @param id The ID of the position to set
     * @param pos The block position value
     */
    public void setPosition(String id, BlockPos pos) {
        blockPositions.put(id, pos);
        this.markDirty();
    }
    
    /**
     * Removes a block position
     * @param id The ID of the position to remove
     */
    public void removePosition(String id) {
        if (blockPositions.containsKey(id)) {
            blockPositions.remove(id);
            this.markDirty();
        }
    }
    
    /**
     * Checks if a position exists
     * @param id The ID to check
     * @return True if the position exists
     */
    public boolean hasPosition(String id) {
        return blockPositions.containsKey(id);
    }
    
    /**
     * Gets all position IDs
     * @return Set of all position IDs
     */
    public Set<String> getPositionIds() {
        return blockPositions.keySet();
    }
    
    // Timer methods using intValues with "timer_" prefix
    
    /**
     * Gets a timer value by its ID
     * @param timerId The ID of the timer to retrieve
     * @return Current timer value in ticks, or 0 if the timer doesn't exist
     */
    public int getTimer(String timerId) {
        return getIntValue("timer_" + timerId, 0);
    }
    
    /**
     * Sets a timer value by its ID
     * @param timerId The ID of the timer to set
     * @param value The new timer value in ticks
     */
    public void setTimer(String timerId, int value) {
        setIntValue("timer_" + timerId, value);
    }
    
    /**
     * Increments a timer by the specified amount
     * @param timerId The ID of the timer to increment
     * @param amount The amount to increment the timer by
     * @return The new timer value
     */
    public int incrementTimer(String timerId, int amount) {
        int currentValue = getTimer(timerId);
        int newValue = currentValue + amount;
        setTimer(timerId, newValue);
        return newValue;
    }
    
    /**
     * Decrements a timer by the specified amount (won't go below 0)
     * @param timerId The ID of the timer to decrement
     * @param amount The amount to decrement the timer by
     * @return The new timer value
     */
    public int decrementTimer(String timerId, int amount) {
        int currentValue = getTimer(timerId);
        int newValue = Math.max(0, currentValue - amount);
        setTimer(timerId, newValue);
        return newValue;
    }
    
    /**
     * Checks if a timer exists
     * @param timerId The ID of the timer to check
     * @return True if the timer exists, false otherwise
     */
    public boolean hasTimer(String timerId) {
        return hasIntValue("timer_" + timerId);
    }
    
    /**
     * Removes a timer
     * @param timerId The ID of the timer to remove
     */
    public void removeTimer(String timerId) {
        removeIntValue("timer_" + timerId);
    }
    
    /**
     * Gets all timer IDs
     * @return Set of all timer IDs
     */
    public Set<String> getTimerIds() {
        Set<String> timerKeys = new HashSet<>();
        for (String key : getIntValueIds()) {
            if (key.startsWith("timer_")) {
                timerKeys.add(key.substring(6)); // Remove "timer_" prefix
            }
        }
        return timerKeys;
    }
}
