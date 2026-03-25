package horror.blueice129.scheduler;

import horror.blueice129.feature.LilyDamage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class LilyDamageScheduler {

    private static final int CHECK_INTERVAL = 20 * 2;
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LilyDamageScheduler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (++tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return;

        for (ServerPlayerEntity player : overworld.getPlayers()) {
            LilyDamage.applyToPlayer(overworld, player);
        }
    }
}
