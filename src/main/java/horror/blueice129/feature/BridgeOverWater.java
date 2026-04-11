package horror.blueice129.feature;

import horror.blueice129.utils.StructurePlacer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class BridgeOverWater {

    private static final Random RANDOM = Random.create();

    public static boolean triggerEvent(MinecraftServer server, ServerPlayerEntity player){
        BlockPos pos = player.getBlockPos();
        BlockPos waterLocation;
        for (int i = 0; i < 10; i++) {
            waterLocation = StructurePlacer.findSurfaceLocation(server.getOverworld(), pos, player, 30, 100, true);
        }
        return true;
    }
}
