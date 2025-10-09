package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.HomeVisitorEvent;
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
    private static final int MIN_START_DAY = 10;
    private static final int MAX_START_DAY = 20;
    private static final String TIMER_ID = "homeEventTimer";
    private static final String LOGOUT_TIME_ID = "playerLogoutTime";
    private static final String EVENT_READY_ID = "homeEventReady";
    private static final int MIN_ABSENCE_TIME = 600; // 10 minutes in seconds

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
            // This might lead to overflow in ~2038, but that's acceptable for now
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
                HorrorMod129.LOGGER.info("HomeEventScheduler timer reached zero, event is now ready for next extended log off.");
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
            // For initial setup, use a random delay between 10 and 20 days
            return (MIN_START_DAY + random.nextInt(MAX_START_DAY - MIN_START_DAY + 1)) * 24000; // 1 day = 24000 ticks
        } else {
            // For subsequent events, use every 3-6 days
            return (3 + random.nextInt(4)) * 24000; // 3 to 6 days in ticks
        }
    }
}
