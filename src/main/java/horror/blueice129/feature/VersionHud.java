package horror.blueice129.feature;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class VersionHud {
    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer("horror-mod-129")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
    private static final int PADDING = 4;
    
    public static void initialize() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            
            if (client.currentScreen instanceof HandledScreen) {
                renderVersionText(context, client);
            }
        });
    }
    
    private static void renderVersionText(DrawContext context, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        String text = "ENGRAM " + VERSION;
        int textWidth = client.textRenderer.getWidth(text);
        
        int x = screenWidth - textWidth - PADDING;
        int y = screenHeight - client.textRenderer.fontHeight - PADDING;
        
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
    }
}
