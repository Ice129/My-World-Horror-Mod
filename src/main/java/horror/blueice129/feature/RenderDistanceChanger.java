package horror.blueice129.feature;
import net.minecraft.client.MinecraftClient;

/**
 * can modify and get the clients render distance
 */
public class RenderDistanceChanger {
    public static int getRenderDistance() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 12; // Default render distance
        }
        return client.options.getViewDistance().getValue();
    }
    
    public static void setRenderDistance(int distance) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        // Clamp distance between 2 and 128 chunks
        int clampedDistance = Math.max(2, Math.min(128, distance));
        client.options.getViewDistance().setValue(clampedDistance);
        client.options.write();
    }
    
    public static void increaseRenderDistance(int amount) {
        setRenderDistance(getRenderDistance() + amount);
    }

    public static void decreaseRenderDistance(int amount) {
        setRenderDistance(getRenderDistance() - amount);
    }
}
