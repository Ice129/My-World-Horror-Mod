package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.GameMode;

public final class PlayerStateScheduler {
    private static final int CHECK_INTERVAL_TICKS = 600; // 30 seconds

    private static boolean initialized = false;
    private static int ticksUntilCheck = CHECK_INTERVAL_TICKS;

    // Persistent "ever seen" values for this game session.
    private static int creativeSeenValue = 0;
    private static int spectatorSeenValue = 0;
    private static boolean everOp = false;

    private PlayerStateScheduler() {
    }

    public static void initialize() {
        if (initialized) {
            HorrorMod129.LOGGER.warn("PlayerStateScheduler already initialized, skipping...");
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                return;
            }

            if (ticksUntilCheck > 0) {
                ticksUntilCheck--;
                return;
            }

            runCheck(client);
            ticksUntilCheck = CHECK_INTERVAL_TICKS;
        });

        initialized = true;
        // HorrorMod129.LOGGER.info("PlayerStateScheduler initialized - checking gamemode/op every 30 seconds");
    }

    private static void runCheck(MinecraftClient client) {
        GameMode gameMode = client.interactionManager == null ? null : client.interactionManager.getCurrentGameMode();

        if (gameMode == GameMode.CREATIVE) {
            creativeSeenValue = 1;
        } else if (gameMode == GameMode.SPECTATOR) {
            spectatorSeenValue = 2;
        }

        if (!everOp && client.player.hasPermissionLevel(2)) {
            everOp = true;
        }
    }

    public static int getGamemodeSum() {
        return creativeSeenValue + spectatorSeenValue;
    }

    public static int getEverOpAsInt() {
        return everOp ? 1 : 0;
    }
}