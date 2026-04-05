package horror.blueice129.network;

import horror.blueice129.HorrorMod129;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;

/**
 * Handles packet sending for client-server communication.
 */
public class ModNetworking {
    public static final Identifier SETTINGS_TRIGGER_ID = new Identifier(HorrorMod129.MOD_ID, "settings_trigger");
    public static final Identifier ENTITY_SCREENSHOT_ID = new Identifier(HorrorMod129.MOD_ID, "entity_screenshot");
    public static final Identifier LILY_RAIN_START_ID = new Identifier(HorrorMod129.MOD_ID, "lily_rain_start");
    public static final Identifier LILY_RAIN_STOP_ID = new Identifier(HorrorMod129.MOD_ID, "lily_rain_stop");

    public static void registerPackets() {
        // No server-side registration required for simple S2C packets with Fabric.
        HorrorMod129.LOGGER.info("ModNetworking initialized (no explicit packet registration required)");
    }

    /**
     * Sends a settings trigger packet to a specific player.
     *
     * @param player The player to send the packet to
     * @param settingType The type of setting to modify
     */
    public static void sendSettingsTrigger(net.minecraft.server.network.ServerPlayerEntity player,
                                           SettingsTriggerPayload.SettingType settingType) {
        PacketByteBuf buf = SettingsTriggerPayload.write(settingType);
        ServerPlayNetworking.send(player, SETTINGS_TRIGGER_ID, buf);
        HorrorMod129.LOGGER.info("Sent settings trigger packet to " + player.getName().getString() +
                " for setting: " + settingType);
    }

    public static void sendEntityScreenshot(net.minecraft.server.network.ServerPlayerEntity player, int entityId) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(entityId);
        ServerPlayNetworking.send(player, ENTITY_SCREENSHOT_ID, buf);
    }

    public static void sendLilyRainStart(ServerPlayerEntity player, BlockPos pos) {
        ServerPlayNetworking.send(player, LILY_RAIN_START_ID, writeBlockPos(pos));
    }

    public static void sendLilyRainStop(ServerPlayerEntity player, BlockPos pos) {
        ServerPlayNetworking.send(player, LILY_RAIN_STOP_ID, writeBlockPos(pos));
    }

    public static void sendLilyRainStop(ServerWorld world, BlockPos pos) {
        for (ServerPlayerEntity player : world.getPlayers(serverPlayer -> serverPlayer.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0)) {
            sendLilyRainStop(player, pos);
        }
    }

    private static PacketByteBuf writeBlockPos(BlockPos pos) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        return buf;
    }
}
