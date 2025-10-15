package horror.blueice129.scheduler;

import horror.blueice129.feature.LedgePusher;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.HorrorMod129;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
// import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import horror.blueice129.utils.PlayerUtils;

public class LedgePusherScheduler {
    // check every tick if the player is on a ledge
    private static final Random random = Random.create();

    private final static String cooldownTimerKey = "ledgePusherCooldown";
    private final static int MIN_DELAY = 20 * 60 * 10; // 10 minutes
    private static int ticksSinceLastPush;
    private static LedgePusher ledgePusher;
    private static HorrorModPersistentState state;
    private static final int PUSH_CHANCE = 20 * 60; // chance is 1 in PUSH_CHANCE every tick, so for 1 minute on an edge,
                                                    // you can be expected to be pushed once

    public static void register() {

        ServerTickEvents.END_SERVER_TICK.register(LedgePusherScheduler::onServerTick);
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient())
                return;
            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                // Initialize any necessary state here
                state = HorrorModPersistentState.getServerState(server);
                if (!state.hasTimer(cooldownTimerKey)) {
                    state.setTimer(cooldownTimerKey, MIN_DELAY);
                }
            }
        });
    }

    /**
     * Debug helper to set the ledge pusher cooldown timer directly (in ticks).
     */
    public static void setTimer(MinecraftServer server, int ticks) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setTimer(cooldownTimerKey, Math.max(ticks, 1));
        HorrorMod129.LOGGER.info("LedgePusherScheduler cooldown set to " + state.getTimer(cooldownTimerKey) + " ticks via debug command");
    }
    
    private static void onServerTick(MinecraftServer server) {
        // get the first player in the server
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }
        PlayerEntity player = server.getPlayerManager().getPlayerList().get(0);
        if (ledgePusher == null) {
            ledgePusher = new LedgePusher(player, 10);
        }

        ticksSinceLastPush++;
        
        if (ticksSinceLastPush == 10) { // 0.5 seconds
            if (ledgePusher.didPlayerFall()) {
                // make entity flee
                LedgePusher.spawnFleeingEntityStatic((ServerWorld) player.getWorld(), player.getPos(), PlayerUtils.getDirectionVector(PlayerUtils.getPlayerCompassDirection(player)));
            }
        }

        int cooldown = state.getTimer(cooldownTimerKey);
        if (cooldown > 0) {
            state.setTimer(cooldownTimerKey, cooldown - 1);
            HorrorMod129.LOGGER.info("LedgePusherScheduler, cooldown is: " + state.getTimer(cooldownTimerKey));
            return;
        }
        else if (ledgePusher.isPlayerOnLedge()) {
            // player is on a ledge and the cooldown has expired, roll to see if they get pushed
            HorrorMod129.LOGGER.info("Player is on a ledge, rolling to see if they get pushed");
            if (random.nextInt(PUSH_CHANCE) == 0) {
                // push the player
                ledgePusher.pushPlayer();
                ticksSinceLastPush = 0;
                state.setTimer(cooldownTimerKey, MIN_DELAY); // Reset cooldown timer
                HorrorMod129.LOGGER.info("Player pushed off ledge");
            }
        }
    }
}