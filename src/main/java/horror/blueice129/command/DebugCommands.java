package horror.blueice129.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.HomeVisitorEvent;
import horror.blueice129.feature.PlayerDeathItems;
import horror.blueice129.feature.SmallStructureEvent;
import horror.blueice129.feature.LedgePusher;
import horror.blueice129.feature.CavePreMiner;
import horror.blueice129.feature.EntityHouse;
import horror.blueice129.feature.RenderDistanceChanger;
import horror.blueice129.feature.MusicVolumeLocker;
import horror.blueice129.feature.BrightnessChanger;
import horror.blueice129.feature.FpsLimiter;
import horror.blueice129.feature.MouseSensitivityChanger;
import horror.blueice129.feature.SmoothLightingChanger;
import horror.blueice129.debug.LineOfSightChecker;
import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.scheduler.Blueice129SpawnScheduler;
import net.minecraft.entity.Entity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
// import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
// import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.Blocks;
import com.mojang.brigadier.Command;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.server.MinecraftServer;

public class DebugCommands {

    /**
     * Register all debug commands
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register(DebugCommands::registerCommands);

        HorrorMod129.LOGGER.info("Registered debug commands");
    }

    /**
     * Register the commands with the command dispatcher
     */
    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        // Only register in development environment or if explicitly enabled
        if (environment == CommandManager.RegistrationEnvironment.DEDICATED
                || environment == CommandManager.RegistrationEnvironment.INTEGRATED) {

            // Single base command with all subgroups
            dispatcher.register(
                literal("horror")
                    .requires(source -> source.hasPermissionLevel(2)) // Require op permission
                    
                    // === EVENT TRIGGERS ===
                    .then(literal("event")
                        .then(literal("homevisitor")
                            .executes(DebugCommands::triggerHomeVisitor))
                        .then(literal("playerdeathitems")
                            .executes(context -> triggerPlayerDeathItems(context.getSource())))
                        .then(literal("structure")
                            .then(literal("crafting_table")
                                .executes(context -> executeEvent(context.getSource(), "crafting_table")))
                            .then(literal("furnace")
                                .executes(context -> executeEvent(context.getSource(), "furnace")))
                            .then(literal("cobblestone_pillar")
                                .executes(context -> executeEvent(context.getSource(), "cobblestone_pillar")))
                            .then(literal("single_torch")
                                .executes(context -> executeEvent(context.getSource(), "single_torch")))
                            .then(literal("torched_area")
                                .executes(context -> executeEvent(context.getSource(), "torched_area")))
                            .then(literal("tree_mined")
                                .executes(context -> executeEvent(context.getSource(), "tree_mined")))
                            .then(literal("deforestation")
                                .executes(context -> executeEvent(context.getSource(), "deforestation")))
                            .then(literal("flower_patch")
                                .executes(context -> executeEvent(context.getSource(), "flower_patch")))
                            .then(literal("watchtower")
                                .executes(context -> executeEvent(context.getSource(), "watchtower")))
                            .then(literal("starter_base")
                                .executes(context -> executeEvent(context.getSource(), "starter_base")))
                            .then(literal("pitfall_trap")
                                .executes(context -> executeEvent(context.getSource(), "pitfall_trap")))
                            .then(literal("chunk_deletion")
                                .executes(context -> executeEvent(context.getSource(), "chunk_deletion")))
                            .then(literal("burning_forest")
                                .executes(context -> executeEvent(context.getSource(), "burning_forest")))))
                    
                    // === TIMER MANAGEMENT ===
                    .then(literal("timer")
                        .then(literal("get")
                            .then(argument("timerId", StringArgumentType.word())
                                .executes(context -> getTimer(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "timerId")))))
                        .then(literal("set")
                            .then(argument("timerId", StringArgumentType.word())
                                .then(argument("ticks", IntegerArgumentType.integer(0))
                                    .executes(context -> setTimer(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "timerId"),
                                        IntegerArgumentType.getInteger(context, "ticks"))))))
                        .then(literal("smallstructure")
                            .then(argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(context -> setSmallStructureTimer(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "seconds")))))
                        .then(literal("ledgepusher")
                            .then(argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(context -> setLedgePusherTimer(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "seconds")))))
                        .then(literal("playerdeathitems")
                            .then(argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(context -> setPlayerDeathItemsTimer(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "seconds"))))))
                    
                    // === AGGRO METER ===
                    .then(literal("agro")
                        .then(literal("get")
                            .executes(context -> getAgroMeter(context.getSource())))
                        .then(literal("set")
                            .then(argument("level", IntegerArgumentType.integer(0, 10))
                                .executes(context -> setAgroMeter(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "level"))))))
                    
                    // === CLIENT SETTINGS ===
                    .then(literal("settings")
                        .then(literal("render")
                            .then(literal("get")
                                .executes(context -> getRenderDistance(context.getSource())))
                            .then(literal("set")
                                .then(argument("distance", IntegerArgumentType.integer(2, 128))
                                    .executes(context -> setRenderDistance(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "distance")))))
                            .then(literal("increase")
                                .then(argument("amount", IntegerArgumentType.integer(1, 32))
                                    .executes(context -> increaseRenderDistance(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount")))))
                            .then(literal("decrease")
                                .then(argument("amount", IntegerArgumentType.integer(1, 32))
                                    .executes(context -> decreaseRenderDistance(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(literal("music")
                            .then(literal("get")
                                .executes(context -> getMusicVolume(context.getSource())))
                            .then(literal("lock")
                                .executes(context -> lockMusicVolume(context.getSource()))))
                        .then(literal("brightness")
                            .then(literal("get")
                                .executes(context -> getBrightness(context.getSource())))
                            .then(literal("moody")
                                .executes(context -> setMoodyBrightness(context.getSource()))))
                        .then(literal("fps")
                            .then(literal("get")
                                .executes(context -> getFpsLimit(context.getSource())))
                            .then(literal("set")
                                .then(argument("fps", IntegerArgumentType.integer(10, 260))
                                    .executes(context -> setFpsLimit(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "fps"))))))
                        .then(literal("sensitivity")
                            .then(literal("get")
                                .executes(context -> getMouseSensitivity(context.getSource())))
                            .then(literal("set")
                                .then(argument("value", IntegerArgumentType.integer(0, 100))
                                    .executes(context -> setMouseSensitivity(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "value"))))))
                        .then(literal("smoothlighting")
                            .then(literal("get")
                                .executes(context -> getSmoothLighting(context.getSource())))
                            .then(literal("enable")
                                .executes(context -> enableSmoothLighting(context.getSource())))
                            .then(literal("disable")
                                .executes(context -> disableSmoothLighting(context.getSource())))
                            .then(literal("toggle")
                                .executes(context -> toggleSmoothLighting(context.getSource())))))
                    
                    // === DEBUG TOOLS ===
                    .then(literal("tool")
                        .then(literal("ledge")
                            .then(literal("check")
                                .executes(context -> checkLedge(context.getSource())))
                            .then(literal("push")
                                .executes(context -> pushPlayer(context.getSource())))
                            .then(literal("spawn_fleeing")
                                .executes(context -> spawnFleeingEntity(context.getSource()))))
                        .then(literal("cave")
                            .then(literal("premine")
                                .executes(context -> premineCave(context.getSource(), 1))
                                .then(argument("attempts", IntegerArgumentType.integer(1, 10))
                                    .executes(context -> premineCave(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "attempts"))))))
                        .then(literal("flatness")
                            .executes(context -> checkFlatness(context.getSource())))
                        .then(literal("visualize")
                            .then(literal("fov")
                                .executes(context -> fillFieldOfViewWithGlass(context.getSource())))
                            .then(literal("rendered")
                                .executes(context -> fillRenderedBlocksWithGlass(context.getSource())))
                            .then(literal("notvisible")
                                .executes(context -> fillNotVisibleBlocksWithConcrete(context.getSource())))
                            .then(literal("trees")
                                .executes(context -> placeDiamondPillars(context.getSource())))))
                    
                    // === PERSISTENT STATE ===
                    .then(literal("state")
                        .then(literal("list")
                            .executes(context -> listPersistentStateKeys(context.getSource()))))
            );
            
            // Register entity state commands
            dispatcher.register(literal("blueice129")
                    .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (op)
                    .then(literal("state")
                            .then(literal("passive")
                                    .executes(context -> setEntityState(context.getSource(), "PASSIVE")))
                            .then(literal("paniced")
                                    .executes(context -> setEntityState(context.getSource(), "PANICED")))
                            .then(literal("surface_hiding")
                                    .executes(context -> setEntityState(context.getSource(), "SURFACE_HIDING")))
                            .then(literal("underground_burrowing")
                                    .executes(context -> setEntityState(context.getSource(), "UNDERGROUND_BURROWING")))
                            .then(literal("in_menus")
                                    .executes(context -> setEntityState(context.getSource(), "IN_MENUS")))
                            .then(literal("investigating")
                                    .executes(context -> setEntityState(context.getSource(), "INVESTIGATING")))
                            .then(literal("upgrading_house")
                                    .executes(context -> setEntityState(context.getSource(), "UPGRADING_HOUSE")))
                            .then(literal("get")
                                    .executes(context -> getEntityState(context.getSource()))))
                    .then(literal("spawn")
                            .then(literal("force")
                                    .executes(context -> forceSpawnEntity(context.getSource())))
                            .then(literal("timer")
                                    .then(argument("ticks", IntegerArgumentType.integer(1))
                                            .executes(context -> setSpawnTimer(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "ticks")))))
                            .then(literal("chance")
                                    .executes(context -> getSpawnChance(context.getSource()))))
            );
        }
    }

    /**
     * Executes the homevisitor debug command
     */
    private static int triggerHomeVisitor(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player != null) {
            BlockPos bedPos = player.getSpawnPointPosition();

            // If player doesn't have a spawn point, use their current position
            if (bedPos == null) {
                bedPos = player.getBlockPos();
                source.sendFeedback(() -> Text.literal("No spawn point found, using current position"), false);
            }

            // Trigger the home visitor event
            source.sendFeedback(() -> Text.literal("Triggering HomeVisitorEvent..."), true);
            HomeVisitorEvent.triggerEvent(source.getServer(), player, bedPos);
            source.sendFeedback(() -> Text.literal("HomeVisitorEvent triggered successfully!"), true);

            return 1;
        } else {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
    }

    private static int placeDiamondPillars(ServerCommandSource source) {
        try {
            ServerWorld world = source.getWorld();
            BlockPos playerPos = source.getPlayer().getBlockPos();
            BlockPos[] treePositions = SurfaceFinder.findTreePositions(world, playerPos, 15);

            for (BlockPos treePos : treePositions) {
                for (int y = 0; y < 10; y++) {
                    BlockPos pillarPos = treePos.up(y);
                    world.setBlockState(pillarPos, Blocks.DIAMOND_BLOCK.getDefaultState());
                }
            }

            source.sendFeedback(() -> Text.of("Diamond pillars placed at tree locations."), false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.of("An error occurred while placing diamond pillars."));
            return 0;
        }
    }

    private static int executeEvent(ServerCommandSource source, String eventId) {
        MinecraftServer server = source.getServer();
        ServerPlayerEntity player = source.getPlayer();
        boolean success = SmallStructureEvent.triggerEvent(server, player, eventId);
        if (success) {
            source.sendFeedback(() -> Text.literal("Successfully triggered event: " + eventId), false);
        } else {
            source.sendError(Text.literal("Failed to trigger event: " + eventId));
        }
        return success ? 1 : 0;
    }

    /**
     * Gets a timer value dynamically by its ID from persistent state
     * @param source Command source
     * @param timerId The ID of the timer to retrieve
     * @return Command success value
     */
    private static int getTimer(ServerCommandSource source, String timerId) {
        MinecraftServer server = source.getServer();
        try {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            int ticks = state.getTimer(timerId);
            double seconds = ticks / 20.0;
            double minutes = seconds / 60.0;
            
            source.sendFeedback(() -> Text.literal(
                String.format("Timer '%s': %d ticks (%.1f seconds, %.2f minutes)", 
                    timerId, ticks, seconds, minutes)
            ), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get timer '" + timerId + "': " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Sets a timer value dynamically by its ID in persistent state
     * @param source Command source
     * @param timerId The ID of the timer to set
     * @param ticks The timer value in ticks
     * @return Command success value
     */
    private static int setTimer(ServerCommandSource source, String timerId, int ticks) {
        MinecraftServer server = source.getServer();
        try {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            state.setTimer(timerId, ticks);
            double seconds = ticks / 20.0;
            double minutes = seconds / 60.0;
            
            source.sendFeedback(() -> Text.literal(
                String.format("Timer '%s' set to %d ticks (%.1f seconds, %.2f minutes)", 
                    timerId, ticks, seconds, minutes)
            ), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set timer '" + timerId + "': " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Sets the small structure timer
     * @param source Command source
     * @param seconds Timer duration in seconds
     * @return Command success value
     */
    private static int setSmallStructureTimer(ServerCommandSource source, int seconds) {
        MinecraftServer server = source.getServer();
        try {
            int ticks = seconds * 20;
            horror.blueice129.scheduler.SmallStructureScheduler.setTimer(server, ticks);
            source.sendFeedback(() -> Text.literal("Small structure timer set to " + seconds + " seconds (" + ticks + " ticks)"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set small structure timer: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Sets the ledge pusher cooldown timer
     * @param source Command source
     * @param seconds Timer duration in seconds
     * @return Command success value
     */
    private static int setLedgePusherTimer(ServerCommandSource source, int seconds) {
        MinecraftServer server = source.getServer();
        try {
            int ticks = seconds * 20;
            horror.blueice129.scheduler.LedgePusherScheduler.setTimer(server, ticks);
            source.sendFeedback(() -> Text.literal("Ledge pusher timer set to " + seconds + " seconds (" + ticks + " ticks)"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set ledge pusher timer: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Sets the player death items timer
     * @param source Command source
     * @param seconds Timer duration in seconds
     * @return Command success value
     */
    private static int setPlayerDeathItemsTimer(ServerCommandSource source, int seconds) {
        MinecraftServer server = source.getServer();
        try {
            int ticks = seconds * 20;
            horror.blueice129.scheduler.PlayerDeathItemsScheduler.setTimer(server, ticks);
            source.sendFeedback(() -> Text.literal("Player death items timer set to " + seconds + " seconds (" + ticks + " ticks)"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set player death items timer: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Immediately triggers a player death items event
     */
    private static int triggerPlayerDeathItems(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        try {
            // Try to get the player who executed the command
            ServerPlayerEntity player;
            String playerName;
            try {
                player = source.getPlayer();
                playerName = player.getName().getString();
            } catch (Exception e) {
                // If command wasn't executed by a player, try to find any player
                if (server.getPlayerManager().getPlayerList().isEmpty()) {
                    source.sendError(Text.literal("No players online to trigger player death items event."));
                    return 0;
                }
                player = server.getPlayerManager().getPlayerList().get(0);
                playerName = player.getName().getString();
            }
            
            // Store result of the event
            final boolean success = PlayerDeathItems.triggerEvent(server, player);
            final String finalPlayerName = playerName;
            
            if (success) {
                source.sendFeedback(() -> Text.literal("Successfully triggered player death items event around player " + finalPlayerName + "."), false);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to trigger player death items event."));
                return 0;
            }
        } catch (Exception e) {
            source.sendError(Text.literal("Error triggering player death items event: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Get the current agro meter value
     * @param source Command source
     * @return Command success value
     */
    private static int getAgroMeter(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        try {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            int agroMeter = state.getIntValue("agroMeter", 0);
            source.sendFeedback(() -> Text.literal("Current agro meter level: " + agroMeter + "/10"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get agro meter level: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set the agro meter to a specific value
     * @param source Command source
     * @param level New agro meter level (0-10)
     * @return Command success value
     */
    private static int setAgroMeter(ServerCommandSource source, int level) {
        MinecraftServer server = source.getServer();
        try {
            // Ensure level is within valid range
            final int finalLevel = Math.max(0, Math.min(10, level));
            
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            state.setIntValue("agroMeter", finalLevel);
            source.sendFeedback(() -> Text.literal("Agro meter level set to: " + finalLevel + "/10"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set agro meter level: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Fills all non-air blocks within the player's field of view with blue stained glass
     * @param source Command source
     * @return Command success value
     */
    private static int fillFieldOfViewWithGlass(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
        
        try {
            source.sendFeedback(() -> Text.literal("Filling field of view with glass (64 block range)..."), false);
            LineOfSightChecker.fillFieldOfViewWithGlass(player, 64.0);
            source.sendFeedback(() -> Text.literal("Completed field of view visualization!"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error while filling field of view: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Fills all blocks rendered on the player's screen with yellow stained glass
     * @param source Command source
     * @return Command success value
     */
    private static int fillRenderedBlocksWithGlass(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
        
        try {
            source.sendFeedback(() -> Text.literal("Filling rendered blocks with glass (64 block range)..."), false);
            LineOfSightChecker.fillRenderedBlocksWithGlass(player, 64.0);
            source.sendFeedback(() -> Text.literal("Completed rendered blocks visualization!"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error while filling rendered blocks: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Fills all blocks NOT visible to the player with lime concrete (inverse of fillRenderedBlocksWithGlass)
     * @param source Command source
     * @return Command success value
     */
    private static int fillNotVisibleBlocksWithConcrete(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
        
        try {
            source.sendFeedback(() -> Text.literal("Filling NOT visible blocks with lime concrete (64 block range, FOV cone)..."), false);
            LineOfSightChecker.fillNotVisibleBlocksWithConcrete(player, 64.0);
            source.sendFeedback(() -> Text.literal("Completed not-visible blocks visualization! Green = hidden from view"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error while filling not-visible blocks: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Get the current render distance
     * @param source Command source
     * @return Command success value
     */
    private static int getRenderDistance(ServerCommandSource source) {
        try {
            int distance = RenderDistanceChanger.getRenderDistance();
            source.sendFeedback(() -> Text.literal("Current render distance: " + distance + " chunks"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get render distance: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set the render distance to a specific value
     * @param source Command source
     * @param distance New render distance (2-128 chunks)
     * @return Command success value
     */
    private static int setRenderDistance(ServerCommandSource source, int distance) {
        try {
            RenderDistanceChanger.setRenderDistance(distance);
            final int finalDistance = Math.max(2, Math.min(128, distance));
            source.sendFeedback(() -> Text.literal("Render distance set to: " + finalDistance + " chunks"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set render distance: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Increase the render distance by a specified amount
     * @param source Command source
     * @param amount Amount to increase by (1-32 chunks)
     * @return Command success value
     */
    private static int increaseRenderDistance(ServerCommandSource source, int amount) {
        try {
            int oldDistance = RenderDistanceChanger.getRenderDistance();
            RenderDistanceChanger.increaseRenderDistance(amount);
            int newDistance = RenderDistanceChanger.getRenderDistance();
            source.sendFeedback(() -> Text.literal("Render distance increased from " + oldDistance + " to " + newDistance + " chunks"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to increase render distance: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Decrease the render distance by a specified amount
     * @param source Command source
     * @param amount Amount to decrease by (1-32 chunks)
     * @return Command success value
     */
    private static int decreaseRenderDistance(ServerCommandSource source, int amount) {
        try {
            int oldDistance = RenderDistanceChanger.getRenderDistance();
            RenderDistanceChanger.decreaseRenderDistance(amount);
            int newDistance = RenderDistanceChanger.getRenderDistance();
            source.sendFeedback(() -> Text.literal("Render distance decreased from " + oldDistance + " to " + newDistance + " chunks"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to decrease render distance: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Get the current music volume
     * @param source Command source
     * @return Command success value
     */
    private static int getMusicVolume(ServerCommandSource source) {
        try {
            double volume = MusicVolumeLocker.getMusicVolume();
            double minVolume = MusicVolumeLocker.getMinimumMusicVolume();
            source.sendFeedback(() -> Text.literal("Current music volume: " + (volume * 100) + "% (minimum: " + (minVolume * 100) + "%)"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get music volume: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Lock music volume to minimum 50%
     * @param source Command source
     * @return Command success value
     */
    private static int lockMusicVolume(ServerCommandSource source) {
        try {
            MusicVolumeLocker.enforceMinimumMusicVolume();
            double newVolume = MusicVolumeLocker.getMusicVolume();
            source.sendFeedback(() -> Text.literal("Music volume locked to minimum. Current volume: " + (newVolume * 100) + "%"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to lock music volume: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Get the current brightness level
     * @param source Command source
     * @return Command success value
     */
    private static int getBrightness(ServerCommandSource source) {
        try {
            double brightness = BrightnessChanger.getBrightness();
            String description = brightness == 0.0 ? " (moody)" : brightness == 1.0 ? " (bright)" : "";
            source.sendFeedback(() -> Text.literal("Current brightness: " + (brightness * 100) + "%" + description), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get brightness: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set brightness to moody (minimum)
     * @param source Command source
     * @return Command success value
     */
    private static int setMoodyBrightness(ServerCommandSource source) {
        try {
            BrightnessChanger.setToMoodyBrightness();
            double newBrightness = BrightnessChanger.getBrightness();
            source.sendFeedback(() -> Text.literal("Brightness set to moody (minimum). Current brightness: " + (newBrightness * 100) + "%"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set brightness: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Get the current FPS limit
     * @param source Command source
     * @return Command success value
     */
    private static int getFpsLimit(ServerCommandSource source) {
        try {
            int fpsLimit = FpsLimiter.getCurrentFpsLimit();
            source.sendFeedback(() -> Text.literal("Current FPS limit: " + fpsLimit), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get FPS limit: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set a custom FPS limit
     * @param source Command source
     * @param fps The desired FPS limit (10-260)
     * @return Command success value
     */
    private static int setFpsLimit(ServerCommandSource source, int fps) {
        try {
            FpsLimiter.setFpsLimit(fps);
            int newFps = FpsLimiter.getCurrentFpsLimit();
            source.sendFeedback(() -> Text.literal("FPS limit set to: " + newFps), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set FPS limit: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Get the current mouse sensitivity
     * @param source Command source
     * @return Command success value
     */
    private static int getMouseSensitivity(ServerCommandSource source) {
        try {
            double sensitivity = MouseSensitivityChanger.getMouseSensitivity();
            source.sendFeedback(() -> Text.literal("Current mouse sensitivity: " + (sensitivity * 100) + "%"), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get mouse sensitivity: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set mouse sensitivity to a custom value (0-100)
     * @param source Command source
     * @param value The desired sensitivity as a percentage (0-100)
     * @return Command success value
     */
    private static int setMouseSensitivity(ServerCommandSource source, int value) {
        try {
            double sensitivity = value / 100.0;
            MouseSensitivityChanger.setMouseSensitivity(sensitivity);
            double newSensitivity = MouseSensitivityChanger.getMouseSensitivity();
            source.sendFeedback(() -> Text.literal("Mouse sensitivity set to: " + (newSensitivity * 100) + "%"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set mouse sensitivity: " + e.getMessage()));
            return 0;
        }
    }
    
    // ===== DEBUG TOOL METHODS =====
    
    /**
     * Check if the player is on a ledge
     * @param source Command source
     * @return Command success value
     */
    private static int checkLedge(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
        
        LedgePusher ledgePusher = new LedgePusher(player, 10);
        if (ledgePusher.isPlayerOnLedge()) {
            source.sendFeedback(() -> Text.literal("You are on a ledge!"), false);
        } else {
            source.sendFeedback(() -> Text.literal("You are not on a ledge"), false);
        }
        return 1;
    }
    
    /**
     * Push the player off a ledge
     * @param source Command source
     * @return Command success value
     */
    private static int pushPlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
        
        LedgePusher ledgePusher = new LedgePusher(player, 10);
        ledgePusher.pushPlayer();
        source.sendFeedback(() -> Text.literal("Player pushed!"), true);
        return 1;
    }
    
    /**
     * Spawn a fleeing entity in front of the player
     * @param source Command source
     * @return Command success value
     */
    private static int spawnFleeingEntity(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
        
        ServerWorld world = (ServerWorld) player.getWorld();
        String direction = horror.blueice129.utils.PlayerUtils.getPlayerCompassDirection(player);
        double[] directionVector = horror.blueice129.utils.PlayerUtils.getDirectionVector(direction);
        
        // Spawn fleeing entity 3 blocks in front of player
        net.minecraft.util.math.Vec3d spawnPos = new net.minecraft.util.math.Vec3d(
            player.getX() + directionVector[0] * 3,
            player.getY(),
            player.getZ() + directionVector[1] * 3
        );
        
        LedgePusher.spawnFleeingEntityStatic(world, spawnPos, directionVector);
        source.sendFeedback(() -> Text.literal("Spawned fleeing entity!"), false);
        return 1;
    }
    
    /**
     * Pre-mine a cave near the player
     * @param source Command source
     * @param attempts Number of attempts to find a suitable cave
     * @return Command success value
     */
    private static int premineCave(ServerCommandSource source, int attempts) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }
        
        if (attempts == 1) {
            source.sendFeedback(() -> Text.literal("Attempting to pre-mine a cave..."), false);
            boolean success = CavePreMiner.preMineCave(
                player.getWorld(),
                player.getBlockPos(),
                player
            );
            
            if (success) {
                source.sendFeedback(() -> Text.literal("Successfully pre-mined a cave!"), false);
                return Command.SINGLE_SUCCESS;
            } else {
                source.sendError(Text.literal("Failed to find a suitable cave. Try a different location."));
                return 0;
            }
        } else {
            source.sendFeedback(() -> Text.literal("Attempting to pre-mine a cave with " + attempts + " attempts..."), false);
            
            // Use a mutable wrapper class
            class MutableResult {
                public boolean success = false;
                public int attempts = 0;
            }
            final MutableResult result = new MutableResult();
            
            for (int i = 0; i < attempts && !result.success; i++) {
                result.attempts++;
                result.success = CavePreMiner.preMineCave(
                    player.getWorld(),
                    player.getBlockPos(),
                    player
                );
            }
            
            if (result.success) {
                final int finalAttempts = result.attempts;
                source.sendFeedback(() -> Text.literal("Successfully pre-mined a cave after " + finalAttempts + " attempt(s)!"), false);
                return Command.SINGLE_SUCCESS;
            } else {
                source.sendError(Text.literal("Failed to find a suitable cave after " + attempts + " attempts"));
                return 0;
            }
        }
    }

    private static int checkFlatness(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player"));
            return 0;
        }

        ServerWorld world = (ServerWorld) player.getWorld();
        int x = player.getBlockX();
        int z = player.getBlockZ();

        int surfaceY = SurfaceFinder.findPointSurfaceY(world, x, z, true, false, true);
        
        if (surfaceY == -1) {
            source.sendError(Text.literal("Failed to find surface at your position"));
            return 0;
        }

        BlockPos surfacePos = new BlockPos(x, surfaceY, z);
        int flatnessScore = EntityHouse.debugForEvaluateFlatness(world, surfacePos);

        source.sendFeedback(() -> Text.literal("Flatness score at surface: " + flatnessScore + " (lower is flatter)"), false);
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Get the current smooth lighting state
     * @param source Command source
     * @return Command success value
     */
    private static int getSmoothLighting(ServerCommandSource source) {
        try {
            boolean enabled = SmoothLightingChanger.isSmoothLightingEnabled();
            source.sendFeedback(() -> Text.literal("Smooth lighting is " + (enabled ? "enabled" : "disabled")), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get smooth lighting state: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Enable smooth lighting
     * @param source Command source
     * @return Command success value
     */
    private static int enableSmoothLighting(ServerCommandSource source) {
        try {
            SmoothLightingChanger.enableSmoothLighting();
            source.sendFeedback(() -> Text.literal("Smooth lighting enabled"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to enable smooth lighting: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Disable smooth lighting
     * @param source Command source
     * @return Command success value
     */
    private static int disableSmoothLighting(ServerCommandSource source) {
        try {
            SmoothLightingChanger.disableSmoothLighting();
            source.sendFeedback(() -> Text.literal("Smooth lighting disabled"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to disable smooth lighting: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Set the state of the nearest Blueice129 entity
     * @param source Command source
     * @param stateName Name of the state to set
     * @return Command success value
     */
    private static int setEntityState(ServerCommandSource source, String stateName) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("This command must be run by a player"));
                return 0;
            }
            
            ServerWorld world = (ServerWorld) player.getWorld();
            
            // Find the nearest Blueice129 entity within 64 blocks
            Blueice129Entity nearestEntity = null;
            double nearestDistance = 64.0 * 64.0; // Squared distance for efficiency
            
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof Blueice129Entity) {
                    double distSquared = player.squaredDistanceTo(entity);
                    if (distSquared < nearestDistance) {
                        nearestDistance = distSquared;
                        nearestEntity = (Blueice129Entity) entity;
                    }
                }
            }
            
            if (nearestEntity == null) {
                source.sendError(Text.literal("No Blueice129 entity found within 64 blocks"));
                return 0;
            }
            
            // Convert state name to enum
            Blueice129Entity.EntityState newState;
            try {
                newState = Blueice129Entity.EntityState.valueOf(stateName);
            } catch (IllegalArgumentException e) {
                source.sendError(Text.literal("Invalid state name: " + stateName));
                return 0;
            }
            
            // Set the state
            nearestEntity.setState(newState);
            
            final String finalStateName = stateName.toLowerCase().replace('_', ' ');
            final double finalDistance = Math.sqrt(nearestDistance);
            source.sendFeedback(() -> Text.literal(
                String.format("Set Blueice129 entity state to '%s' (%.1f blocks away)", 
                    finalStateName, finalDistance)
            ), true);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error setting entity state: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Toggle smooth lighting on/off
     * @param source Command source
     * @return Command success value
     */
    private static int toggleSmoothLighting(ServerCommandSource source) {
        try {
            boolean wasEnabled = SmoothLightingChanger.isSmoothLightingEnabled();
            SmoothLightingChanger.toggleSmoothLighting();
            boolean isEnabled = SmoothLightingChanger.isSmoothLightingEnabled();
            source.sendFeedback(() -> Text.literal("Smooth lighting toggled from " + (wasEnabled ? "enabled" : "disabled") + " to " + (isEnabled ? "enabled" : "disabled")), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to toggle smooth lighting: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Get the current state of the nearest Blueice129 entity
     * @param source Command source
     * @return Command success value
     */
    private static int getEntityState(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("This command must be run by a player"));
                return 0;
            }
            
            ServerWorld world = (ServerWorld) player.getWorld();
            
            // Find the nearest Blueice129 entity within 64 blocks
            Blueice129Entity nearestEntity = null;
            double nearestDistance = 64.0 * 64.0; // Squared distance for efficiency
            
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof Blueice129Entity) {
                    double distSquared = player.squaredDistanceTo(entity);
                    if (distSquared < nearestDistance) {
                        nearestDistance = distSquared;
                        nearestEntity = (Blueice129Entity) entity;
                    }
                }
            }
            
            if (nearestEntity == null) {
                source.sendError(Text.literal("No Blueice129 entity found within 64 blocks"));
                return 0;
            }
            
            // Get the current state
            Blueice129Entity.EntityState currentState = nearestEntity.getState();
            final String stateName = currentState.toString().toLowerCase().replace('_', ' ');
            final double finalDistance = Math.sqrt(nearestDistance);
            
            source.sendFeedback(() -> Text.literal(
                String.format("Blueice129 entity state: '%s' (%.1f blocks away)", 
                    stateName, finalDistance)
            ), false);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error getting entity state: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Forces an immediate spawn attempt of Blueice129 entity near the player
     * @param source Command source
     * @return Command success value
     */
    private static int forceSpawnEntity(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            MinecraftServer server = source.getServer();
            // ServerWorld world = server.getOverworld();
            
            BlockPos spawnPos = Blueice129SpawnScheduler.forceSpawn(server, player);
            
            if (spawnPos != null) {
                // Create clickable chat message with coordinates
                final int x = spawnPos.getX();
                final int y = spawnPos.getY();
                final int z = spawnPos.getZ();
                
                MutableText message = Text.literal("Blueice129 spawned at ")
                    .styled(style -> style.withColor(0x55FF55)); // Green
                
                MutableText coordinates = Text.literal("[" + x + ", " + y + ", " + z + "]")
                    .styled(style -> style
                        .withColor(0xFFAA00) // Gold/Orange
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/tp @s " + x + " " + y + " " + z
                        ))
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Text.literal("Click to teleport")
                        ))
                        .withUnderline(true));
                
                MutableText suffix = Text.literal(" (click to teleport)")
                    .styled(style -> style.withColor(0xAAAAAA)); // Gray
                
                message.append(coordinates).append(suffix);
                
                source.sendFeedback(() -> message, true);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to spawn Blueice129 entity. Check that: 1) No entity already exists, 2) You're near a forest biome"));
                return 0;
            }
        } catch (Exception e) {
            source.sendError(Text.literal("Error forcing spawn: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Sets the spawn scheduler timer
     * @param source Command source
     * @param ticks Timer value in ticks
     * @return Command success value
     */
    private static int setSpawnTimer(ServerCommandSource source, int ticks) {
        try {
            MinecraftServer server = source.getServer();
            Blueice129SpawnScheduler.setTimer(server, ticks);
            
            final int finalTicks = ticks;
            source.sendFeedback(() -> Text.literal(
                String.format("Spawn timer set to %d ticks (%.1f seconds)", 
                    finalTicks, finalTicks / 20.0)
            ), true);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error setting spawn timer: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets the current spawn chance based on agro meter
     * @param source Command source
     * @return Command success value
     */
    private static int getSpawnChance(ServerCommandSource source) {
        try {
            MinecraftServer server = source.getServer();
            String chanceString = Blueice129SpawnScheduler.getSpawnChanceString(server);
            
            source.sendFeedback(() -> Text.literal(
                "Current Blueice129 spawn chance: " + chanceString
            ), false);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error getting spawn chance: " + e.getMessage()));
            HorrorMod129.LOGGER.error("Error getting spawn chance", e);
            return 0;
        }
    }

    /**
     * Lists all keys currently stored in the persistent state
     * @param source Command source
     * @return Command success value
     */
    private static int listPersistentStateKeys(ServerCommandSource source) {
        try {
            MinecraftServer server = source.getServer();
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
            
            // Build a comprehensive message with all keys organized by type
            StringBuilder message = new StringBuilder("§6=== Persistent State Keys ===\n");
            
            // Timers
            var timerIds = state.getTimerIds();
            message.append("§e[Timers] (").append(timerIds.size()).append("):\n");
            if (timerIds.isEmpty()) {
                message.append("  §7(none)\n");
            } else {
                for (String id : timerIds) {
                    int value = state.getTimer(id);
                    message.append("  §f").append(id).append(" = ").append(value).append(" ticks\n");
                }
            }
            
            // Integer Values
            var intValueIds = state.getIntValueIds();
            message.append("§e[Integer Values] (").append(intValueIds.size()).append("):\n");
            if (intValueIds.isEmpty()) {
                message.append("  §7(none)\n");
            } else {
                for (String id : intValueIds) {
                    int value = state.getIntValue(id, 0);
                    message.append("  §f").append(id).append(" = ").append(value).append("\n");
                }
            }
            
            // Long Values
            var longValueIds = state.getLongValueIds();
            message.append("§e[Long Values] (").append(longValueIds.size()).append("):\n");
            if (longValueIds.isEmpty()) {
                message.append("  §7(none)\n");
            } else {
                for (String id : longValueIds) {
                    long value = state.getLongValue(id, 0L);
                    message.append("  §f").append(id).append(" = ").append(value).append("\n");
                }
            }
            
            // Block Positions
            var positionIds = state.getPositionIds();
            message.append("§e[Block Positions] (").append(positionIds.size()).append("):\n");
            if (positionIds.isEmpty()) {
                message.append("  §7(none)\n");
            } else {
                for (String id : positionIds) {
                    BlockPos pos = state.getPosition(id);
                    message.append("  §f").append(id).append(" = ")
                        .append(pos.getX()).append(", ")
                        .append(pos.getY()).append(", ")
                        .append(pos.getZ()).append("\n");
                }
            }
            
            // Position Lists
            var positionListIds = state.getPositionListIds();
            message.append("§e[Position Lists] (").append(positionListIds.size()).append("):\n");
            if (positionListIds.isEmpty()) {
                message.append("  §7(none)\n");
            } else {
                for (String id : positionListIds) {
                    var posList = state.getPositionList(id);
                    message.append("  §f").append(id).append(" (").append(posList.size()).append(" positions)\n");
                }
            }
            
            // 2D Integer Arrays
            var int2DArrayIds = state.getInt2DArrayIds();
            message.append("§e[2D Integer Arrays] (").append(int2DArrayIds.size()).append("):\n");
            if (int2DArrayIds.isEmpty()) {
                message.append("  §7(none)\n");
            } else {
                for (String id : int2DArrayIds) {
                    int[][] array = state.getInt2DArray(id);
                    message.append("  §f").append(id).append(" (")
                        .append(array.length).append("x")
                        .append(array.length > 0 ? array[0].length : 0)
                        .append(")\n");
                }
            }
            
            // Calculate total keys
            int totalKeys = timerIds.size() + intValueIds.size() + longValueIds.size() + 
                           positionIds.size() + positionListIds.size() + int2DArrayIds.size();
            message.append("§6Total Keys: ").append(totalKeys);
            
            // Send the message
            String finalMessage = message.toString();
            source.sendFeedback(() -> Text.literal(finalMessage), false);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error listing persistent state keys: " + e.getMessage()));
            HorrorMod129.LOGGER.error("Error listing persistent state keys", e);
            return 0;
        }
    }
}