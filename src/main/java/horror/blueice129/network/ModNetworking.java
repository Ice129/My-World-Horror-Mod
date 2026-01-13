package horror.blueice129.network;

import horror.blueice129.HorrorMod129;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Handles packet sending for client-server communication.
 */
public class ModNetworking {
    public static final Identifier SETTINGS_TRIGGER_ID = new Identifier(HorrorMod129.MOD_ID, "settings_trigger");

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
}
