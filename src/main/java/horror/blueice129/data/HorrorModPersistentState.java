package horror.blueice129.data;

import horror.blueice129.HorrorMod129;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO: make a way to visualise how and when data is being saved and loaded and stored persistently

/**
 * Persistent state handler for the Horror Mod.
 * Manages data that needs to persist between game sessions.
 */
public class HorrorModPersistentState extends PersistentState {
    // The unique identifier for this persistent state
    private static final String IDENTIFIER = HorrorMod129.MOD_ID + "_state";
    
    // Map to store multiple timers with their respective IDs
    private Map<String, Integer> timers;
    
    // Map to store block positions with string keys
    private Map<String, BlockPos> positions;
    
    // Map to store integer values with string keys
    private Map<String, Integer> intValues;
    
    // Map to store long values with string keys
    private Map<String, Long> longValues;
    
    // Map to store lists of block positions with string keys
    private Map<String, List<BlockPos>> positionLists;
    
    // Map to store 2D arrays of integers with string keys
    private Map<String, int[][]> int2DArrays;

    // Constructor with default values
    public HorrorModPersistentState() {
        this.timers = new HashMap<>();
        this.positions = new HashMap<>();
        this.intValues = new HashMap<>();
        this.longValues = new HashMap<>();
        this.positionLists = new HashMap<>();
        this.int2DArrays = new HashMap<>();
    }
    
    /**
     * Constructor used when loading data from NBT
     * @param timers The saved timers map
     * @param positions The saved positions map
     * @param intValues The saved integer values map
     * @param longValues The saved long values map
     * @param positionLists The saved position lists map
     */
    public HorrorModPersistentState(
            Map<String, Integer> timers, 
            Map<String, BlockPos> positions, 
            Map<String, Integer> intValues,
            Map<String, Long> longValues,
            Map<String, List<BlockPos>> positionLists,
            Map<String, int[][]> int2DArrays) {
        this.timers = timers;
        this.positions = positions;
        this.intValues = intValues;
        this.longValues = longValues;
        this.positionLists = positionLists;
        this.int2DArrays = int2DArrays;
    }
    
    /**
     * Gets a timer value by its ID
     * @param timerId The ID of the timer to retrieve
     * @return Current timer value in ticks, or 0 if the timer doesn't exist
     */
    public int getTimer(String timerId) {
        return timers.getOrDefault(timerId, 0);
    }
    
    /**
     * Sets a timer value by its ID
     * @param timerId The ID of the timer to set
     * @param value The new timer value in ticks
     */
    public void setTimer(String timerId, int value) {
        timers.put(timerId, value);
        // Mark that the state has changed and needs to be saved
        this.markDirty();
    }
    
    /**
     * Saves the state to NBT
     * @return NBT compound containing the state data
     */
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Save timers
        NbtCompound timersNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : timers.entrySet()) {
            timersNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("timers", timersNbt);
        
        // Save positions
        NbtCompound positionsNbt = new NbtCompound();
        for (Map.Entry<String, BlockPos> entry : positions.entrySet()) {
            BlockPos pos = entry.getValue();
            NbtCompound posNbt = new NbtCompound();
            posNbt.putInt("x", pos.getX());
            posNbt.putInt("y", pos.getY());
            posNbt.putInt("z", pos.getZ());
            positionsNbt.put(entry.getKey(), posNbt);
        }
        nbt.put("positions", positionsNbt);
        
        // Save integer values
        NbtCompound intValuesNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : intValues.entrySet()) {
            intValuesNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("intValues", intValuesNbt);
        
        // Save long values
        NbtCompound longValuesNbt = new NbtCompound();
        for (Map.Entry<String, Long> entry : longValues.entrySet()) {
            longValuesNbt.putLong(entry.getKey(), entry.getValue());
        }
        nbt.put("longValues", longValuesNbt);
        
        // Save position lists
        NbtCompound posListsNbt = new NbtCompound();
        for (Map.Entry<String, List<BlockPos>> entry : positionLists.entrySet()) {
            NbtList posList = new NbtList();
            for (BlockPos pos : entry.getValue()) {
                NbtCompound posNbt = new NbtCompound();
                posNbt.putInt("x", pos.getX());
                posNbt.putInt("y", pos.getY());
                posNbt.putInt("z", pos.getZ());
                posList.add(posNbt);
            }
            posListsNbt.put(entry.getKey(), posList);
        }
        nbt.put("positionLists", posListsNbt);

        // Save 2D integer arrays
        NbtCompound int2DArraysNbt = new NbtCompound();
        for (Map.Entry<String, int[][]> entry : int2DArrays.entrySet()) {
            NbtList arrayList = new NbtList();
            for (int[] row : entry.getValue()) {
                NbtList rowList = new NbtList();
                for (int value : row) {
                    rowList.add(NbtInt.of(value));
                }
                arrayList.add(rowList);
            }
            int2DArraysNbt.put(entry.getKey(), arrayList);
        }
        nbt.put("int2DArrays", int2DArraysNbt);

        return nbt;
    }
    
    /**
     * Creates a persistent state instance from NBT data
     * @param nbt The NBT data to read from
     * @return A new persistent state instance
     */
    public static HorrorModPersistentState createFromNbt(NbtCompound nbt) {
        Map<String, Integer> loadedTimers = new HashMap<>();
        Map<String, BlockPos> loadedPositions = new HashMap<>();
        Map<String, Integer> loadedIntValues = new HashMap<>();
        Map<String, Long> loadedLongValues = new HashMap<>();
        Map<String, List<BlockPos>> loadedPositionLists = new HashMap<>();
        Map<String, int[][]> loadedInt2DArrays = new HashMap<>();
        
        // Read timers
        if (nbt.contains("timers")) {
            NbtCompound timersNbt = nbt.getCompound("timers");
            for (String key : timersNbt.getKeys()) {
                loadedTimers.put(key, timersNbt.getInt(key));
            }
        }
        
        // Read positions
        if (nbt.contains("positions")) {
            NbtCompound positionsNbt = nbt.getCompound("positions");
            for (String key : positionsNbt.getKeys()) {
                NbtCompound posNbt = positionsNbt.getCompound(key);
                int x = posNbt.getInt("x");
                int y = posNbt.getInt("y");
                int z = posNbt.getInt("z");
                loadedPositions.put(key, new BlockPos(x, y, z));
            }
        }
        
        // Read integer values
        if (nbt.contains("intValues")) {
            NbtCompound intValuesNbt = nbt.getCompound("intValues");
            for (String key : intValuesNbt.getKeys()) {
                loadedIntValues.put(key, intValuesNbt.getInt(key));
            }
        }
        
        // Read long values
        if (nbt.contains("longValues")) {
            NbtCompound longValuesNbt = nbt.getCompound("longValues");
            for (String key : longValuesNbt.getKeys()) {
                loadedLongValues.put(key, longValuesNbt.getLong(key));
            }
        }
        
        // Read position lists
        if (nbt.contains("positionLists")) {
            NbtCompound posListsNbt = nbt.getCompound("positionLists");
            for (String key : posListsNbt.getKeys()) {
                List<BlockPos> positions = new ArrayList<>();
                NbtList posList = (NbtList) posListsNbt.get(key);
                
                for (int i = 0; i < posList.size(); i++) {
                    NbtCompound posNbt = posList.getCompound(i);
                    int x = posNbt.getInt("x");
                    int y = posNbt.getInt("y");
                    int z = posNbt.getInt("z");
                    positions.add(new BlockPos(x, y, z));
                }
                
                loadedPositionLists.put(key, positions);
            }
        }

        // Read 2D integer arrays
        if (nbt.contains("int2DArrays")) {
            NbtCompound int2DArraysNbt = nbt.getCompound("int2DArrays");
            for (String key : int2DArraysNbt.getKeys()) {
                NbtList arrayList = (NbtList) int2DArraysNbt.get(key);
                int[][] array = new int[arrayList.size()][];
                for (int i = 0; i < arrayList.size(); i++) {
                    NbtList rowList = (NbtList) arrayList.get(i);
                    int[] row = new int[rowList.size()];
                    for (int j = 0; j < rowList.size(); j++) {
                        row[j] = rowList.getInt(j);
                    }
                    array[i] = row;
                }
                loadedInt2DArrays.put(key, array);
            }
        }
        
        return new HorrorModPersistentState(loadedTimers, loadedPositions, loadedIntValues, loadedLongValues, loadedPositionLists, loadedInt2DArrays);
    }
    
    /**
     * Gets the persistent state from the server, creating it if it doesn't exist
     * @param server The Minecraft server instance
     * @return The persistent state instance
     */
    public static HorrorModPersistentState getServerState(MinecraftServer server) {
        // Get the persistent state manager for the overworld
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        
        // Get or create the persistent state
        return persistentStateManager.getOrCreate(
            HorrorModPersistentState::createFromNbt,
            HorrorModPersistentState::new,
            IDENTIFIER
        );
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
        return timers.containsKey(timerId);
    }
    
    /**
     * Removes a timer
     * @param timerId The ID of the timer to remove
     */
    public void removeTimer(String timerId) {
        if (timers.containsKey(timerId)) {
            timers.remove(timerId);
            this.markDirty();
        }
    }
    
    /**
     * Gets all timer IDs
     * @return Set of all timer IDs
     */
    public Set<String> getTimerIds() {
        return timers.keySet();
    }
    
    // === BLOCK POSITION METHODS ===
    
    /**
     * Gets a block position by its ID
     * @param id The ID of the position to retrieve
     * @return The block position, or null if not found
     */
    public BlockPos getPosition(String id) {
        return positions.get(id);
    }
    
    /**
     * Sets a block position by its ID
     * @param id The ID of the position to set
     * @param pos The block position value
     */
    public void setPosition(String id, BlockPos pos) {
        positions.put(id, pos);
        this.markDirty();
    }
    
    /**
     * Removes a block position
     * @param id The ID of the position to remove
     */
    public void removePosition(String id) {
        if (positions.containsKey(id)) {
            positions.remove(id);
            this.markDirty();
        }
    }
    
    /**
     * Checks if a position exists
     * @param id The ID to check
     * @return True if the position exists
     */
    public boolean hasPosition(String id) {
        return positions.containsKey(id);
    }
    
    /**
     * Gets all position IDs
     * @return Set of all position IDs
     */
    public Set<String> getPositionIds() {
        return positions.keySet();
    }
    
    // === INTEGER VALUE METHODS ===
    
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
    
    // === LONG VALUE METHODS ===
    
    /**
     * Gets a long value by its ID
     * @param id The ID of the value to retrieve
     * @param defaultValue The default value to return if not found
     * @return The long value, or the default value if not found
     */
    public long getLongValue(String id, long defaultValue) {
        return longValues.getOrDefault(id, defaultValue);
    }
    
    /**
     * Sets a long value by its ID
     * @param id The ID of the value to set
     * @param value The long value
     */
    public void setLongValue(String id, long value) {
        longValues.put(id, value);
        this.markDirty();
    }
    
    /**
     * Removes a long value
     * @param id The ID of the value to remove
     */
    public void removeLongValue(String id) {
        if (longValues.containsKey(id)) {
            longValues.remove(id);
            this.markDirty();
        }
    }
    
    /**
     * Checks if a long value exists
     * @param id The ID to check
     * @return True if the long value exists
     */
    public boolean hasLongValue(String id) {
        return longValues.containsKey(id);
    }
    
    /**
     * Gets all long value IDs
     * @return Set of all long value IDs
     */
    public Set<String> getLongValueIds() {
        return longValues.keySet();
    }
    
    // === POSITION LIST METHODS ===
    
    /**
     * Gets a list of positions by its ID
     * @param id The ID of the position list to retrieve
     * @return The list of positions, or an empty list if not found
     */
    public List<BlockPos> getPositionList(String id) {
        return positionLists.getOrDefault(id, new ArrayList<>());
    }
    
    /**
     * Sets a list of positions by its ID
     * @param id The ID of the position list to set
     * @param positions The list of positions
     */
    public void setPositionList(String id, List<BlockPos> positions) {
        positionLists.put(id, new ArrayList<>(positions));
        this.markDirty();
    }
    
    /**
     * Adds a position to a list
     * @param id The ID of the position list
     * @param pos The position to add
     */
    public void addPositionToList(String id, BlockPos pos) {
        List<BlockPos> list = getPositionList(id);
        list.add(pos);
        positionLists.put(id, list);
        this.markDirty();
    }
    
    /**
     * Removes a position from a list
     * @param id The ID of the position list
     * @param pos The position to remove
     * @return True if the position was removed
     */
    public boolean removePositionFromList(String id, BlockPos pos) {
        if (!positionLists.containsKey(id)) {
            return false;
        }
        
        List<BlockPos> list = positionLists.get(id);
        boolean removed = list.remove(pos);
        
        if (removed) {
            this.markDirty();
        }
        
        return removed;
    }
    
    /**
     * Removes a position list
     * @param id The ID of the position list to remove
     */
    public void removePositionList(String id) {
        if (positionLists.containsKey(id)) {
            positionLists.remove(id);
            this.markDirty();
        }
    }
    
    /**
     * Gets all position list IDs
     * @return Set of all position list IDs
     */
    public Set<String> getPositionListIds() {
        return positionLists.keySet();
    }

    // === 2D INTEGER ARRAY METHODS === // TODO: make several arrays for the strip mine, with sub strip mine id's


    /**
     * Gets a 2D array of integers by its ID
     * @param id The ID of the array to retrieve
     * @return The 2D array of integers, or an empty array if not found
     */
    public int[][] getInt2DArray(String id) {
        return int2DArrays.getOrDefault(id, new int[0][0]);
    }

    /**
     * Sets a 2D array of integers by its ID
     * @param id The ID of the array to set
     * @param array The 2D array of integers
     */
    public void setInt2DArray(String id, int[][] array) {
        int2DArrays.put(id, array);
        this.markDirty();
    }

    /**
     * Checks if a 2D integer array exists
     * @param id The ID to check
     * @return True if the 2D integer array exists
     */
    public boolean hasInt2DArray(String id) {
        return int2DArrays.containsKey(id);
    }

    /**
     * Removes a 2D integer array
     * @param id The ID of the array to remove
     */
    public void removeInt2DArray(String id) {
        if (int2DArrays.containsKey(id)) {
            int2DArrays.remove(id);
            this.markDirty();
        }
    }

    /**
     * Gets all 2D integer array IDs
     * @return Set of all 2D integer array IDs
     */
    public Set<String> getInt2DArrayIds() {
        return int2DArrays.keySet();
    }
}