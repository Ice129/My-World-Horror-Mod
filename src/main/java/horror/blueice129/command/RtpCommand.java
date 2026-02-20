package horror.blueice129.command;

import com.mojang.brigadier.CommandDispatcher;
import horror.blueice129.HorrorMod129;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static net.minecraft.server.command.CommandManager.literal;

public class RtpCommand {

    private static final int MAX_DISTANCE = 100000;
    private static final int RTP_Y = 100;
    private static final Random RANDOM = new Random();

    public static void register() {
        CommandRegistrationCallback.EVENT.register(RtpCommand::registerCommands);
        HorrorMod129.LOGGER.info("Registered RTP command");
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        
        dispatcher.register(
            literal("rtp")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    
                    if (player == null) {
                        source.sendError(Text.literal("This command must be run by a player"));
                        return 0;
                    }

                    int x = RANDOM.nextInt(MAX_DISTANCE * 2) - MAX_DISTANCE;
                    int z = RANDOM.nextInt(MAX_DISTANCE * 2) - MAX_DISTANCE;
                    
                    BlockPos targetPos = new BlockPos(x, RTP_Y, z);
                    
                    player.teleport(
                        player.getServerWorld(),
                        targetPos.getX() + 0.5,
                        targetPos.getY(),
                        targetPos.getZ() + 0.5,
                        player.getYaw(),
                        player.getPitch()
                    );
                    
                    source.sendFeedback(
                        () -> Text.literal("Teleported to X: " + x + ", Y: " + RTP_Y + ", Z: " + z),
                        false
                    );
                    
                    return 1;
                })
        );
    }
}
