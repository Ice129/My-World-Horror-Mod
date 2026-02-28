package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.sounds.FakeFootsteps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Random;

public class FakeFootstepScheduler {
    private static final Random random = new Random();
    private static final String TIMER_ID = "fakeFootstepTimer";
    private static final int MIN_DELAY = 20 * 60 * 20; // 20 minutes in ticks
    private static final int MAX_DELAY = 20 * 60 * 45; // 45 minutes in ticks
    private static final int MIN_AGRO = 7;


    /**
     * Registers the tick event to handle fake footstep scheduling.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FakeFootstepScheduler::onServerTick);

        // Initialize timer when world loads
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

                // Initialize main timer if not set
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay());
                    HorrorMod129.LOGGER.info(
                            "FakeFootstepScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered FakeFootstepScheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

        if (state.getIntValue("agroMeter", 0) < MIN_AGRO) {
            return; // Don't run footstep logic if agro level is too low
        }

        // Handle active footstep playback
        int playbackActive = state.getIntValue("fakeFootstepActive", 0);
        if (playbackActive == 1) {
            int playbackTimer = state.getIntValue("fakeFootstepPlaybackTimer", 0);
            if (playbackTimer > 0) {
                state.setIntValue("fakeFootstepPlaybackTimer", playbackTimer - 1);
            } else {
                FakeFootsteps.tickFootstepPlayback(server);
            }
            return; // Don't process main timer while playback is active
        }

        // Handle main trigger timer
        int currentTimer = state.getTimer(TIMER_ID);
        if (currentTimer > 0) {
            currentTimer = state.decrementTimer(TIMER_ID, 1);

            if (currentTimer == 0) {
                // Timer expired, try to trigger footsteps
                boolean triggered = tryTriggerFootsteps(server);

                if (triggered) {
                    state.setTimer(TIMER_ID, getRandomDelay());
                } else {
                    state.setTimer(TIMER_ID, 20 * 60 * 3); // Retry in 5 minutes if conditions not met
                }
            }
        }
    }

    /**
     * Attempts to trigger fake footsteps for a random player.
     * Only triggers if environmental conditions are met.
     */
    private static boolean tryTriggerFootsteps(MinecraftServer server) {
        // Get all players in overworld
        var players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            HorrorMod129.LOGGER.info("No players online, skipping footstep trigger");
            return false;
        }

        // Pick a random player
        ServerPlayerEntity targetPlayer = players.get(random.nextInt(players.size()));

        // Check environmental conditions
        if (!FakeFootsteps.shouldTriggerFootsteps(targetPlayer.getServerWorld(), targetPlayer)) {
            HorrorMod129.LOGGER.info("Environmental conditions not met for footsteps (need: dark + covered)");
            return false;
        }

        // Generate and play footstep path
        HorrorMod129.LOGGER.info("Triggering fake footsteps for player: " + targetPlayer.getName().getString());
        FakeFootsteps.SoundPath path = FakeFootsteps.getFootstepLocation(targetPlayer.getServerWorld(), targetPlayer);

        if (path != null) {
            FakeFootsteps.playAmbientCue(targetPlayer);
            FakeFootsteps.playSoundPath(server, path);
            return true;
        } else {
            HorrorMod129.LOGGER.warn("Failed to generate valid footstep path");
            return false;
        }
    }

    /**
     * Generates a random delay between MIN_DELAY and MAX_DELAY.
     * 
     * @return Random delay in ticks
     */
    private static int getRandomDelay() {
        return MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY + 1);
    }
}
