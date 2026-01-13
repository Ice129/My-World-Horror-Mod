package horror.blueice129.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;

/**
 * Simple payload helper for settings trigger packets.
 * Uses a PacketByteBuf to write/read the enum value.
 */
public final class SettingsTriggerPayload {
    private SettingsTriggerPayload() {}

    public enum SettingType {
        RENDER_DISTANCE,
        BRIGHTNESS,
        FPS,
        MOUSE_SENSITIVITY,
        SMOOTH_LIGHTING
    }

    public static PacketByteBuf write(SettingType type) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeEnumConstant(type);
        return buf;
    }

    public static SettingType read(PacketByteBuf buf) {
        return buf.readEnumConstant(SettingType.class);
    }
}
