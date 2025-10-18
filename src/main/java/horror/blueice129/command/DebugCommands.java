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
}