package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.HomeVisitorEvent;
import horror.blueice129.utils.ChunkLoadedUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraft.util.math.random.Random;

public class HomeEventScheduler {
    private static final Random random = Random.create();
    private static final int MIN_START_DAY = 2;
    private static final int MAX_START_DAY = 3;
    private static final String TIMER_ID = "homeEventTimer";
    private static final String LOGOUT_TIME_ID = "playerLogoutTime";
    private static final String EVENT_READY_ID = "homeEventReady";
    private static final String HOME_CHUNK_UNLOAD_TIME = "homeChunkUnloadTime_";
    private static final String HOME_CHUNK_WAS_LOADED = "homeChunkWasLoaded_";
    private static final String HOME_TRIGGER_COUNTDOWN = "homeTriggerCountdown_";
    private static final int MIN_ABSENCE_TIME = 600; // 10 minutes in seconds
    private static final int MIN_HOME_UNLOAD_TIME = 600; // 10 minutes in seconds
    private static final int TRIGGER_DELAY_TICKS = 20 * 3; // 3 seconds in ticks
    private static final int CHECK_INTERVAL_TICKS = 20*30; // Check every 30 seconds

    /**
     * Registers the tick event to handle the home event scheduling.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HomeEventScheduler::onServerTick);

        // Register server world loading event to initialize timer if needed
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
                
                // If the timer is not set, initialize it
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay(true));
                    state.setIntValue(EVENT_READY_ID, 0);
                    HorrorMod129.LOGGER.info("HomeEventScheduler initialized with timer: " + state.getTimer(TIMER_ID) + " ticks");
                }
            }
        });
        
        // Track player logout time
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            long currentTime = System.currentTimeMillis() / 1000L;
            // Store as integer to track logout time (in seconds since epoch)
            // if mod is still being played in 2038, fix this
            state.setIntValue(LOGOUT_TIME_ID + player.getUuidAsString(), (int)currentTime);
            HorrorMod129.LOGGER.info("Player " + player.getName().getString() + " disconnected, time recorded: " + currentTime);
        });
        
        // Check time difference when player logs in
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            long currentTime = System.currentTimeMillis() / 1000L;
            String playerKey = LOGOUT_TIME_ID + player.getUuidAsString();
            
            // Check if timer has reached zero (event is ready)
            boolean eventReady = state.getIntValue(EVENT_READY_ID, 0) == 1;
            
            // Get the logout time as int (safe until ~2038)
            int logoutTime = state.getIntValue(playerKey, 0);
            if (logoutTime > 0) {
                long timeDifference = currentTime - logoutTime;
                
                // Only trigger if event is ready AND player has been gone long enough
                if (eventReady && timeDifference > MIN_ABSENCE_TIME) {
                    HorrorMod129.LOGGER.info("Player " + player.getName().getString() + 
                        " reconnected after " + timeDifference + " seconds, triggering home event");
                    
                    // get respawn point
                    if (player.getSpawnPointPosition() != null) {
                        BlockPos bedPos = player.getSpawnPointPosition();
                        triggerHomeEvent(server, player, bedPos);
                        state.setIntValue(EVENT_READY_ID, 0); // Reset ready flag
                    } else {
                        HorrorMod129.LOGGER.warn("Player " + player.getName().getString() + 
                            " has no spawn point set, cannot trigger home event");
                        // Reset timer anyway if no spawn point
                        state.setTimer(TIMER_ID, getRandomDelay(false));
                    }
                }
            } else {
                // First time player connection, record current time
                state.setIntValue(playerKey, (int)currentTime);
                HorrorMod129.LOGGER.info("First login recorded for player: " + player.getName().getString());
            }
        });

        HorrorMod129.LOGGER.info("Registered HomeEventScheduler");
    }

    private static void onServerTick(MinecraftServer server) {
        // Get the persistent state
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int currentTimer = state.getTimer(TIMER_ID);
        
        // Only decrement if timer is greater than 0
        if (currentTimer > 0) {
            currentTimer = state.decrementTimer(TIMER_ID, 1); // Decrement
            
            if (currentTimer == 0) {
                // Mark that the event is ready to trigger on next login after absence
                state.setIntValue(EVENT_READY_ID, 1);
                HorrorMod129.LOGGER.info("HomeEventScheduler timer reached zero, event is now ready for next extended log off or away period.");
            }
        }

        // Handle countdown timers for all players with pending home events
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String playerUuid = player.getUuidAsString();
            String countdownKey = HOME_TRIGGER_COUNTDOWN + playerUuid;
            
            if (state.hasTimer(countdownKey)) {
                int countdown = state.decrementTimer(countdownKey, 1);
                
                if (countdown <= 0) {
                    // Trigger the home event
                    BlockPos bedPos = player.getSpawnPointPosition();
                    if (bedPos != null) {
                        HorrorMod129.LOGGER.info("Triggering home event for player " + player.getName().getString() + 
                            " after 3 second delay");
                        triggerHomeEvent(server, player, bedPos);
                        
                        // Clean up per-player tracking state
                        state.removeTimer(countdownKey);
                        state.removeLongValue(HOME_CHUNK_UNLOAD_TIME + playerUuid);
                        state.removeIntValue(HOME_CHUNK_WAS_LOADED + playerUuid);
                        state.setIntValue(EVENT_READY_ID, 0); // Reset ready flag
                    } else {
                        // No spawn point, just clean up
                        state.removeTimer(countdownKey);
                        state.removeLongValue(HOME_CHUNK_UNLOAD_TIME + playerUuid);
                        state.removeIntValue(HOME_CHUNK_WAS_LOADED + playerUuid);
                    }
                }
            }
        }

        // Periodic check for home chunk load/unload state
        if (server.getTicks() % CHECK_INTERVAL_TICKS == 0) {
            boolean eventReady = state.getIntValue(EVENT_READY_ID, 0) == 1;
            
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getSpawnPointPosition() != null) {
                    BlockPos bedPos = player.getSpawnPointPosition();
                    String playerUuid = player.getUuidAsString();
                    String unloadTimeKey = HOME_CHUNK_UNLOAD_TIME + playerUuid;
                    String wasLoadedKey = HOME_CHUNK_WAS_LOADED + playerUuid;
                    String countdownKey = HOME_TRIGGER_COUNTDOWN + playerUuid;
                    
                    boolean isChunkLoaded = ChunkLoadedUtils.isChunkLoadedAt(server.getWorld(World.OVERWORLD), bedPos);
                    boolean wasLoaded = state.getIntValue(wasLoadedKey, 1) == 1; // Default to loaded
                    
                    if (!isChunkLoaded && wasLoaded) {
                        // Chunk just became unloaded, record timestamp
                        long currentTime = System.currentTimeMillis() / 1000L;
                        state.setLongValue(unloadTimeKey, currentTime);
                        state.setIntValue(wasLoadedKey, 0);
                        HorrorMod129.LOGGER.info("Player " + player.getName().getString() + "'s home chunk unloaded at " + currentTime);
                    } else if (isChunkLoaded && !wasLoaded) {
                        // Chunk just became loaded again
                        long unloadTime = state.getLongValue(unloadTimeKey, 0L);
                        
                        if (unloadTime > 0) {
                            long currentTime = System.currentTimeMillis() / 1000L;
                            long unloadDuration = currentTime - unloadTime;
                            
                            // Check if chunk was unloaded long enough AND event is ready
                            if (unloadDuration >= MIN_HOME_UNLOAD_TIME && eventReady) {
                                // Start the 3-second countdown
                                if (!state.hasTimer(countdownKey)) {
                                    state.setTimer(countdownKey, TRIGGER_DELAY_TICKS);
                                    HorrorMod129.LOGGER.info("Player " + player.getName().getString() + 
                                        "'s home chunk reloaded after " + unloadDuration + " seconds. Starting 3 second countdown.");
                                }
                            } else {
                                HorrorMod129.LOGGER.info("Player " + player.getName().getString() + 
                                    "'s home chunk reloaded, but conditions not met (duration: " + unloadDuration + 
                                    "s, eventReady: " + eventReady + ")");
                            }
                            
                            // Reset the unload time regardless
                            state.removeLongValue(unloadTimeKey);
                        }
                        
                        state.setIntValue(wasLoadedKey, 1);
                    }
                }
            }
        }
    }
    
    /**
     * Triggers a home event for the player who just reconnected after being away.
     * 
     * @param server The Minecraft server instance
     * @param player The player who reconnected
     */
    private static void triggerHomeEvent(MinecraftServer server, ServerPlayerEntity player, BlockPos bedPos) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

        HomeVisitorEvent.triggerEvent(server, player, bedPos);


        HorrorMod129.LOGGER.info("Home event triggered for player: " + player.getName().getString());
        
        // Reset the timer with a new random delay
        state.setTimer(TIMER_ID, getRandomDelay(false));
    }

    /**
     * Generates a random delay for the home event.
     * The delay is between MIN_START_DAY and MAX_START_DAY days in ticks.
     *
     * @return Random delay in ticks
     */
    private static int getRandomDelay(boolean isInitial) {
        if (isInitial) {
            // For initial setup, use a random delay between 2 and 3 days
            return (MIN_START_DAY + random.nextInt(MAX_START_DAY - MIN_START_DAY + 1)) * 24000; // 1 day = 24000 ticks
        } else {
            // For subsequent events, use every 1-2 days
            return (1 + random.nextInt(2)) * 24000; // 1 to 2 days in ticks
        }
    }
}
