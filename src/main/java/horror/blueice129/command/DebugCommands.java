package horror.blueice129.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.feature.HomeVisitorEvent;
import horror.blueice129.feature.PlayerDeathItems;
import horror.blueice129.feature.SmallStructureEvent;
import horror.blueice129.feature.LedgePusher;
import horror.blueice129.feature.CavePreMiner;
import horror.blueice129.feature.RenderDistanceChanger;
import horror.blueice129.feature.MusicVolumeLocker;
import horror.blueice129.feature.BrightnessChanger;
import horror.blueice129.feature.FpsLimiter;
import horror.blueice129.debug.LineOfSightChecker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
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

            dispatcher.register(
                    literal("horror129")
                            .then(literal("debug")
                                    .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (op)
                                    .then(literal("homevisitor")
                                            .executes(DebugCommands::triggerHomeVisitor))
                                    .then(literal("smallstructure10s")
                                            .executes(context -> setSmallStructure10s(context.getSource())))
                                    .then(literal("ledgepusher20ticks")
                                            .executes(context -> setLedgePusherCooldown20Ticks(context.getSource())))
                                    .then(literal("playerdeathitems10s")
                                            .executes(context -> setPlayerDeathItems10s(context.getSource())))
                                    .then(literal("trigger_playerdeathitems")
                                            .executes(context -> triggerPlayerDeathItems(context.getSource())))
                                    .then(literal("place_diamond_pillars")
                                            .executes(context -> placeDiamondPillars(context.getSource())))
                                    .then(literal("ledgepusher")
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) {
                                                    context.getSource().sendError(Text.literal("This command must be run by a player"));
                                                    return 0;
                                                }
                                                LedgePusher ledgePusher = new LedgePusher(player, 10);
                                                if (ledgePusher.isPlayerOnLedge()) {
                                                    context.getSource().sendFeedback(() -> Text.literal("You are on a ledge!"), false);
                                                } else {
                                                    context.getSource().sendFeedback(() -> Text.literal("You are not on a ledge."), false);
                                                }
                                                return 1;
                                            }))
                                    .then(literal("push")
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) {
                                                    context.getSource().sendError(Text.literal("This command must be run by a player"));
                                                    return 0;
                                                }
                                                LedgePusher ledgePusher = new LedgePusher(player, 10);
                                                ledgePusher.pushPlayer();
                                                context.getSource().sendFeedback(() -> Text.literal("You have been pushed!"), false);
                                                return 1;
                                            })
                                    )
                                    .then(literal("spawn_fleeing_entity")
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) {
                                                    context.getSource().sendError(Text.literal("This command must be run by a player"));
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
                                                context.getSource().sendFeedback(() -> Text.literal("Spawned fleeing entity!"), false);
                                                return 1;
                                            })
                                    )
                                    .then(literal("premine_cave")
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) {
                                                    context.getSource().sendError(Text.literal("This command must be run by a player"));
                                                    return 0;
                                                }
                                                
                                                context.getSource().sendFeedback(() -> Text.literal("Attempting to pre-mine a cave..."), false);
                                                boolean success = CavePreMiner.preMineCave(
                                                    player.getWorld(), 
                                                    player.getBlockPos(), 
                                                    player
                                                );
                                                
                                                if (success) {
                                                    context.getSource().sendFeedback(() -> Text.literal("Successfully pre-mined a cave!"), false);
                                                } else {
                                                    context.getSource().sendError(Text.literal("Failed to find a suitable cave to pre-mine. Try moving to a different location."));
                                                }
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(argument("attempts", IntegerArgumentType.integer(1, 10))
                                                  .executes(context -> {
                                                      ServerPlayerEntity player = context.getSource().getPlayer();
                                                      if (player == null) {
                                                          context.getSource().sendError(Text.literal("This command must be run by a player"));
                                                          return 0;
                                                      }
                                                      
                                                      int attempts = IntegerArgumentType.getInteger(context, "attempts");
                                                      context.getSource().sendFeedback(() -> Text.literal("Attempting to pre-mine a cave with " + attempts + " attempts..."), false);
                                                      
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
                                                          context.getSource().sendFeedback(() -> Text.literal("Successfully pre-mined a cave after " + finalAttempts + " attempt(s)!"), false);
                                                      } else {
                                                          context.getSource().sendError(Text.literal("Failed to find a suitable cave to pre-mine after " + attempts + " attempts."));
                                                      }
                                                      return Command.SINGLE_SUCCESS;
                                                  })
                                            )
                                    )
                                    .then(literal("fovglass")
                                            .executes(context -> fillFieldOfViewWithGlass(context.getSource())))
                                    .then(literal("renderedblocksglass")
                                            .executes(context -> fillRenderedBlocksWithGlass(context.getSource())))
                            )
            );

            // Register event debugging commands
            dispatcher.register(literal("debug_event")
                    .then(literal("crafting_table").executes(context -> {
                        return executeEvent(context.getSource(), "crafting_table");
                    }))
                    .then(literal("furnace").executes(context -> {
                        return executeEvent(context.getSource(), "furnace");
                    }))
                    .then(literal("cobblestone_pillar").executes(context -> {
                        return executeEvent(context.getSource(), "cobblestone_pillar");
                    }))
                    .then(literal("single_torch").executes(context -> {
                        return executeEvent(context.getSource(), "single_torch");
                    }))
                    .then(literal("torched_area").executes(context -> {
                        return executeEvent(context.getSource(), "torched_area");
                    }))
                    .then(literal("tree_mined").executes(context -> {
                        return executeEvent(context.getSource(), "tree_mined");
                    }))
                    .then(literal("deforestation").executes(context -> {
                        return executeEvent(context.getSource(), "deforestation");
                    }))
                    .then(literal("flower_patch").executes(context -> {
                        return executeEvent(context.getSource(), "flower_patch");
                    }))
                    .then(literal("watchtower").executes(context -> {
                        return executeEvent(context.getSource(), "watchtower");
                    }))
                    .then(literal("starter_base").executes(context -> {
                        return executeEvent(context.getSource(), "starter_base");
                    }))
                    .then(literal("pitfall_trap").executes(context -> {
                        return executeEvent(context.getSource(), "pitfall_trap");
                    }))
                    .then(literal("chunk_deletion").executes(context -> {
                        return executeEvent(context.getSource(), "chunk_deletion");
                    }))
                    .then(literal("burning_forest").executes(context -> {
                        return executeEvent(context.getSource(), "burning_forest");
                    }))
            );
            
            // Register agro meter commands
            dispatcher.register(literal("agro")
                    .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (op)
                    .then(literal("get")
                            .executes(context -> getAgroMeter(context.getSource())))
                    .then(literal("set")
                            .then(argument("level", IntegerArgumentType.integer(0, 10))
                                    .executes(context -> setAgroMeter(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "level")))))
            );
            
            // Register render distance commands
            dispatcher.register(literal("renderdistance")
                    .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (op)
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
                                            IntegerArgumentType.getInteger(context, "amount")))))
            );
            
            // Register settings commands
            dispatcher.register(literal("settings")
                    .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (op)
                    .then(literal("music")
                            .then(literal("get")
                                    .executes(context -> getMusicVolume(context.getSource())))
                            .then(literal("lock")
                                    .executes(context -> lockMusicVolume(context.getSource()))))
                    .then(literal("brightness")
                            .then(literal("get")
                                    .executes(context -> getBrightness(context.getSource())))
                            .then(literal("setmoody")
                                    .executes(context -> setMoodyBrightness(context.getSource()))))
                    .then(literal("fps")
                            .then(literal("get")
                                    .executes(context -> getFpsLimit(context.getSource())))
                            .then(literal("cap30")
                                    .executes(context -> capFpsTo30(context.getSource())))
                            .then(literal("set")
                                    .then(argument("fps", IntegerArgumentType.integer(10, 260))
                                            .executes(context -> setFpsLimit(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "fps"))))))
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

    private static int setSmallStructure10s(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        try {
            // 10 seconds = 200 ticks
            horror.blueice129.scheduler.SmallStructureScheduler.setTimer(server, 200);
            source.sendFeedback(() -> Text.literal("Set small structure timer to 10 seconds (200 ticks)."), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set small structure timer: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int setLedgePusherCooldown20Ticks(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        try {
            // Set cooldown to 20 ticks (1 second)
            horror.blueice129.scheduler.LedgePusherScheduler.setTimer(server, 20);
            source.sendFeedback(() -> Text.literal("Set ledge pusher cooldown to 20 ticks (1 second)."), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to set ledge pusher cooldown: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Sets the player death items timer to 10 seconds
     */
    private static int setPlayerDeathItems10s(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        try {
            // 10 seconds = 200 ticks
            horror.blueice129.scheduler.PlayerDeathItemsScheduler.setTimer(server, 200);
            source.sendFeedback(() -> Text.literal("Set player death items timer to 10 seconds (200 ticks)."), false);
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
     * Cap FPS to 30
     * @param source Command source
     * @return Command success value
     */
    private static int capFpsTo30(ServerCommandSource source) {
        try {
            FpsLimiter.capFpsTo30();
            int newFps = FpsLimiter.getCurrentFpsLimit();
            source.sendFeedback(() -> Text.literal("FPS capped to 30. Current FPS limit: " + newFps), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to cap FPS: " + e.getMessage()));
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
}