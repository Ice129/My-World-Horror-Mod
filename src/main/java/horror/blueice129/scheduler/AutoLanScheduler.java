package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.config.ConfigManager;
import horror.blueice129.config.ModConfig;
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

        int configuredPort = ConfigManager.getConfig().autoLanPort;
        if (configuredPort < ModConfig.MIN_AUTO_LAN_PORT || configuredPort > ModConfig.MAX_AUTO_LAN_PORT) {
            HorrorMod129.LOGGER.warn(
                    "Invalid auto LAN port {} in config, using default {}",
                    configuredPort,
                    ModConfig.DEFAULT_AUTO_LAN_PORT
            );
            configuredPort = ModConfig.DEFAULT_AUTO_LAN_PORT;
        }

        attemptedOpen = true;
        boolean lanOpened = server.openToLan(null, false, configuredPort);
        if (lanOpened) {
            HorrorMod129.LOGGER.info("Auto LAN enabled for current singleplayer world on port {}", configuredPort);
        } else {
            HorrorMod129.LOGGER.warn("Failed to auto-open LAN for current singleplayer world on port {}", configuredPort);
        }
    }
}
