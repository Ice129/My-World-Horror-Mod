package horror.blueice129.data;
import java.util.Map;

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
}
