package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class AutoLanScheduler {
    private static final int OPEN_DELAY_TICKS = 20;

    private static boolean attemptedOpen = false;
    private static int worldJoinDelay = 0;

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoLanScheduler::onClientTick);
        HorrorMod129.LOGGER.info("AutoLanScheduler initialized");
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.world == null || client.player == null || !client.isInSingleplayer()) {
            attemptedOpen = false;
            worldJoinDelay = 0;
            return;
        }

        if (attemptedOpen) {
            return;
        }

        if (worldJoinDelay < OPEN_DELAY_TICKS) {
            worldJoinDelay++;
            return;
        }

        var server = client.getServer();
        if (server == null) {
            return;
        }

        attemptedOpen = true;
        boolean lanOpened = server.openToLan(null, false, 0);
        if (lanOpened) {
            HorrorMod129.LOGGER.info("Auto LAN enabled for current singleplayer world");
        } else {
            HorrorMod129.LOGGER.warn("Failed to auto-open LAN for current singleplayer world");
        }
    }
}