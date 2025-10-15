package horror.blueice129.data;
// import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

public class StripMineBlocks {
    public String identifier; // e.g., "StripMine-3"
    // blockType: 0 = stairs , 1 = main tunnel entrance, 2 = main tunnel, 3
    //  *         = side tunnel entrance , 4 = side tunnel main, 5 = stair support
    private BlockPos[] stairBlocks;
    private BlockPos[] mainTunnelEntranceBlocks;
    private BlockPos[] mainTunnelBlocks;
    private BlockPos[] sideTunnelEntranceBlocks;
    private BlockPos[] sideTunnelMainBlocks;
    private BlockPos[] stairSupportBlocks;
    
    /**
     * Default constructor for empty StripMineBlocks
     */
    public StripMineBlocks() {
        this.identifier = "";
        this.stairBlocks = new BlockPos[0];
        this.mainTunnelEntranceBlocks = new BlockPos[0];
        this.mainTunnelBlocks = new BlockPos[0];
        this.sideTunnelEntranceBlocks = new BlockPos[0];
        this.sideTunnelMainBlocks = new BlockPos[0];
        this.stairSupportBlocks = new BlockPos[0];
    }

    public StripMineBlocks(String identifier, BlockPos[] stairBlocks, BlockPos[] mainTunnelEntranceBlocks,
            BlockPos[] mainTunnelBlocks, BlockPos[] sideTunnelEntranceBlocks, BlockPos[] sideTunnelMainBlocks,
            BlockPos[] stairSupportBlocks) {
        this.identifier = identifier;
        this.stairBlocks = stairBlocks;
        this.mainTunnelEntranceBlocks = mainTunnelEntranceBlocks;
        this.mainTunnelBlocks = mainTunnelBlocks;
        this.sideTunnelEntranceBlocks = sideTunnelEntranceBlocks;
        this.sideTunnelMainBlocks = sideTunnelMainBlocks;
        this.stairSupportBlocks = stairSupportBlocks;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BlockPos[] getStairBlocks() {
        return stairBlocks;
    }
    public BlockPos[] getMainTunnelEntranceBlocks() {
        return mainTunnelEntranceBlocks;
    }
    public BlockPos[] getMainTunnelBlocks() {
        return mainTunnelBlocks;
    }
    public BlockPos[] getSideTunnelEntranceBlocks() {
        return sideTunnelEntranceBlocks;
    }
    public BlockPos[] getSideTunnelMainBlocks() {
        return sideTunnelMainBlocks;
    }
    public BlockPos[] getStairSupportBlocks() {
        return stairSupportBlocks;
    }
    
    /**
     * Converts this StripMineBlocks object to NBT format
     * @param nbt The NBT compound to write to
     * @return The NBT compound with data
     */
    public NbtCompound toNbt(NbtCompound nbt) {
        nbt.putString("identifier", this.identifier);
        
        // Save each array of BlockPos
        nbt.put("stairBlocks", blockPosArrayToNbt(stairBlocks));
        nbt.put("mainTunnelEntranceBlocks", blockPosArrayToNbt(mainTunnelEntranceBlocks));
        nbt.put("mainTunnelBlocks", blockPosArrayToNbt(mainTunnelBlocks));
        nbt.put("sideTunnelEntranceBlocks", blockPosArrayToNbt(sideTunnelEntranceBlocks));
        nbt.put("sideTunnelMainBlocks", blockPosArrayToNbt(sideTunnelMainBlocks));
        nbt.put("stairSupportBlocks", blockPosArrayToNbt(stairSupportBlocks));
        
        return nbt;
    }
    
    /**
     * Helper method to convert an array of BlockPos to an NBT list
     * @param positions Array of BlockPos
     * @return NBT list of positions
     */
    private NbtList blockPosArrayToNbt(BlockPos[] positions) {
        NbtList list = new NbtList();
        if (positions != null) {
            for (BlockPos pos : positions) {
                if (pos != null) {
                    list.add(NbtHelper.fromBlockPos(pos));
                }
            }
        }
        return list;
    }
    
    /**
     * Creates a StripMineBlocks object from NBT data
     * @param nbt The NBT compound to read from
     * @return A new StripMineBlocks object
     */
    public static StripMineBlocks fromNbt(NbtCompound nbt) {
        String identifier = nbt.getString("identifier");
        
        // Load each array of BlockPos
        BlockPos[] stairBlocks = blockPosArrayFromNbt(nbt, "stairBlocks");
        BlockPos[] mainTunnelEntranceBlocks = blockPosArrayFromNbt(nbt, "mainTunnelEntranceBlocks");
        BlockPos[] mainTunnelBlocks = blockPosArrayFromNbt(nbt, "mainTunnelBlocks");
        BlockPos[] sideTunnelEntranceBlocks = blockPosArrayFromNbt(nbt, "sideTunnelEntranceBlocks");
        BlockPos[] sideTunnelMainBlocks = blockPosArrayFromNbt(nbt, "sideTunnelMainBlocks");
        BlockPos[] stairSupportBlocks = blockPosArrayFromNbt(nbt, "stairSupportBlocks");
        
        return new StripMineBlocks(
            identifier, 
            stairBlocks, 
            mainTunnelEntranceBlocks, 
            mainTunnelBlocks, 
            sideTunnelEntranceBlocks, 
            sideTunnelMainBlocks, 
            stairSupportBlocks
        );
    }
    
    /**
     * Helper method to convert an NBT list to an array of BlockPos
     * @param nbt The NBT compound containing the list
     * @param key The key for the list in the compound
     * @return Array of BlockPos
     */
    private static BlockPos[] blockPosArrayFromNbt(NbtCompound nbt, String key) {
        NbtList list = nbt.getList(key, 10); // 10 is the type ID for compound tags
        BlockPos[] positions = new BlockPos[list.size()];
        
        for (int i = 0; i < list.size(); i++) {
            positions[i] = NbtHelper.toBlockPos(list.getCompound(i));
        }
        
        return positions;
    }
}
