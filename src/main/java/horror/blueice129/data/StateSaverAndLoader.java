package horror.blueice129.data;

import java.util.Map;
import java.util.List;

import horror.blueice129.HorrorMod129;
import net.minecraft.world.PersistentState;
import net.minecraft.util.math.BlockPos;


public class StateSaverAndLoader extends PersistentState{
    // The unique identifier for this persistent state
    private static final String IDENTIFIER = HorrorMod129.MOD_ID + "_state";

    // Map to store integer values with string keys
    private Map<String, Integer> intValues;

    private Map<String, StripMineBlocks> stripMineBlocks;


}
