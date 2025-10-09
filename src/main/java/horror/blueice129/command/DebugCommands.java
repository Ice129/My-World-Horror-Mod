package horror.blueice129.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import horror.blueice129.HorrorMod129;
import horror.blueice129.feature.HomeVisitorEvent;
import horror.blueice129.feature.SmallStructureEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
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

import static net.minecraft.server.command.CommandManager.literal;

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
                                    .then(literal("place_diamond_pillars")
                                            .executes(context -> placeDiamondPillars(context.getSource())))));
            dispatcher.register(CommandManager.literal("debug_event")
                    .then(CommandManager.literal("crafting_table").executes(context -> {
                        return executeEvent(context.getSource(), "crafting_table");
                    }))
                    .then(CommandManager.literal("furnace").executes(context -> {
                        return executeEvent(context.getSource(), "furnace");
                    }))
                    .then(CommandManager.literal("cobblestone_pillar").executes(context -> {
                        return executeEvent(context.getSource(), "cobblestone_pillar");
                    }))
                    .then(CommandManager.literal("single_torch").executes(context -> {
                        return executeEvent(context.getSource(), "single_torch");
                    }))
                    .then(CommandManager.literal("torched_area").executes(context -> {
                        return executeEvent(context.getSource(), "torched_area");
                    }))
                    .then(CommandManager.literal("tree_mined").executes(context -> {
                        return executeEvent(context.getSource(), "tree_mined");
                    }))
                    .then(CommandManager.literal("deforestation").executes(context -> {
                        return executeEvent(context.getSource(), "deforestation");
                    }))
                    .then(CommandManager.literal("flower_patch").executes(context -> {
                        return executeEvent(context.getSource(), "flower_patch");
                    }))
                    .then(CommandManager.literal("watchtower").executes(context -> {
                        return executeEvent(context.getSource(), "watchtower");
                    }))
                    .then(CommandManager.literal("starter_base").executes(context -> {
                        return executeEvent(context.getSource(), "starter_base");
                    }))
                    .then(CommandManager.literal("pitfall_trap").executes(context -> {
                        return executeEvent(context.getSource(), "pitfall_trap");
                    }))
                    .then(CommandManager.literal("chunk_deletion").executes(context -> {
                        return executeEvent(context.getSource(), "chunk_deletion");
                    }))
                    .then(CommandManager.literal("burning_forest").executes(context -> {
                        return executeEvent(context.getSource(), "burning_forest");
                    })));
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
}