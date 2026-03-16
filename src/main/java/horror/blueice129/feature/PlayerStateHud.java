package horror.blueice129.feature;

import horror.blueice129.scheduler.PlayerStateScheduler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public final class PlayerStateHud {
    private static final int PADDING = 4;

    private PlayerStateHud() {
    }

    public static void initialize() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.currentScreen instanceof HandledScreen) {
                renderStateText(context, client);
            }
        });
    }

    private static void renderStateText(DrawContext context, MinecraftClient client) {
        int screenHeight = client.getWindow().getScaledHeight();
        String text = PlayerStateScheduler.getGamemodeSum() + "" + PlayerStateScheduler.getEverOpAsInt();

        int x = PADDING;
        int y = screenHeight - client.textRenderer.fontHeight - PADDING;

        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
    }
}