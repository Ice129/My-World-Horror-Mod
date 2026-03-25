package horror.blueice129.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class DisclaimerScreen extends Screen {
    private static final String TITLE_TEXT = "ENGRAM - TEST 02 - PRIVATE BETA";
    private static final String[] BODY_LINES = {
        "SENSITIVE",
        "DO NOT PROCEED UNLESS AUTHORISED BY THE ENGRAMS HOST",
        "",
        "",
        "",
        "If you are authorised to access this Engram, please note:",
        "",
        "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=",
        "",
        "- Engram technology is experimental, and may be unstable in its current state",
        "",
        "- Engrams are recomended to be stored in a 30-day simulation loop for mental stability, however, the simulation period can be modified in the config if necessary",
        "",
        "- Engrams have been observed to modify the behaviour of the clients machine, experimental safeguards can be enabled in the config to mitigate certain events.",
        "",
        "",
        "",
        "By clicking 'I Agree', you acknowledge that you have read and understood this disclaimer, and that you will undertake all necessary precautions to enable the safe transfer back to a suitable host after the simulation period ends.",
        ""
    };

    private final Screen parent;

    public DisclaimerScreen(Screen parent) {
        super(Text.literal("ENGRAM Horror Mod Disclaimer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("I Agree"), button -> this.client.setScreen(this.parent))
            .dimensions(centerX - 50, this.height - 30, 100, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        int maxTextWidth = Math.max(80, this.width - 24);
        List<String> wrappedLines = getWrappedLines(maxTextWidth);

        int titleWidth = this.textRenderer.getWidth(TITLE_TEXT);
        int bodyWidth = 0;
        for (String line : wrappedLines) {
            bodyWidth = Math.max(bodyWidth, this.textRenderer.getWidth(line));
        }

        int contentWidth = Math.max(titleWidth, bodyWidth);
        int lineHeight = 12;
        int contentHeight = 24 + (wrappedLines.size() * lineHeight);

        int topMargin = 20;
        int bottomMargin = 54;
        int availableHeight = Math.max(60, this.height - topMargin - bottomMargin);
        int availableWidth = Math.max(80, this.width - 24);

        float widthScale = (float) availableWidth / (float) Math.max(1, contentWidth);
        float heightScale = (float) availableHeight / (float) Math.max(1, contentHeight);
        float scale = Math.min(1.0f, Math.min(widthScale, heightScale));

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);

        int scaledCenterX = Math.round((this.width / 2.0f) / scale);
        int scaledStartY = Math.round(topMargin / scale);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(TITLE_TEXT), scaledCenterX, scaledStartY, 0xFFFFFF);

        int lineY = scaledStartY + 24;
        for (String line : wrappedLines) {
            if (line.isEmpty()) {
                lineY += 6;
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), scaledCenterX, lineY, 0xFFFFFF);
                lineY += lineHeight;
            }
        }

        context.getMatrices().pop();
        
        super.render(context, mouseX, mouseY, delta);
    }

    private List<String> getWrappedLines(int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        for (String line : BODY_LINES) {
            if (line.isEmpty()) {
                wrapped.add("");
                continue;
            }

            wrapped.addAll(wrapLine(line, maxWidth));
        }
        return wrapped;
    }

    private List<String> wrapLine(String line, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (this.textRenderer.getWidth(line) <= maxWidth) {
            result.add(line);
            return result;
        }

        String[] words = line.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (this.textRenderer.getWidth(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                current.append(word);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}

