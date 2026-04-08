package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.chat.EntityChat;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.SignBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
// import net.minecraft.world.World;

/**
 * Scheduler that detects when a player places or modifies a sign block
 * and triggers EntityChat responses accordingly.
 */
public class EntityChatScheduler {

    /**
     * Registers the hook to check every time a player interacts with a sign.
     * This should be called during mod initialization.
     */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Only process on server side
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
            
            // Check if the block is a sign
            if (block instanceof SignBlock) {
                ServerWorld serverWorld = (ServerWorld) world;
                MinecraftServer server = serverWorld.getServer();
                
                if (server != null) {
                    // Call EntityChat.respondToSigns with the world and all players
                    EntityChat.respondToSigns(serverWorld, server.getPlayerManager().getPlayerList());
                    HorrorMod129.LOGGER.debug("Sign interaction detected at " + hitResult.getBlockPos());
                }
            }

            return ActionResult.PASS;
        });
    }
}
