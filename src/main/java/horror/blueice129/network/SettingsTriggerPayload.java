package horror.blueice129.network;

import horror.blueice129.HorrorMod129;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Packet payload for triggering client-side settings changes.
 * Sent from server to specific client when settings modification timer expires.
 */
public record SettingsTriggerPayload(SettingType settingType) implements CustomPayload {
    
    public static final CustomPayload.Id<SettingsTriggerPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(HorrorMod129.MOD_ID, "settings_trigger"));
    
    public static final PacketCodec<PacketByteBuf, SettingsTriggerPayload> CODEC = 
        PacketCodec.of(SettingsTriggerPayload::write, SettingsTriggerPayload::read);

    public enum SettingType {
        RENDER_DISTANCE,
        BRIGHTNESS,
        FPS,
        MOUSE_SENSITIVITY,
        SMOOTH_LIGHTING
    }

    private static void write(PacketByteBuf buf, SettingsTriggerPayload payload) {
        buf.writeEnumConstant(payload.settingType);
    }

    private static SettingsTriggerPayload read(PacketByteBuf buf) {
        return new SettingsTriggerPayload(buf.readEnumConstant(SettingType.class));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
