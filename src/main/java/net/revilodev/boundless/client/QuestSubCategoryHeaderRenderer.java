package net.revilodev.boundless.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class QuestSubCategoryHeaderRenderer {
    private static final int BACKGROUND_COLOR = 0xFF373737;
    private static final int OUTLINE_COLOR = 0xFF000000;

    private QuestSubCategoryHeaderRenderer() {
    }

    public static void renderBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
    }

    public static void drawOutlinedText(GuiGraphics graphics, Font font, String text, float scale,
                                        int x, int y, int color) {
        if (text == null || text.isEmpty() || scale <= 0f) return;
        float inverseScale = 1f / scale;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1f);
        float drawX = x * inverseScale;
        float drawY = y * inverseScale;
        float outline = inverseScale;
        graphics.drawString(font, text, drawX - outline, drawY, OUTLINE_COLOR, false);
        graphics.drawString(font, text, drawX + outline, drawY, OUTLINE_COLOR, false);
        graphics.drawString(font, text, drawX, drawY - outline, OUTLINE_COLOR, false);
        graphics.drawString(font, text, drawX, drawY + outline, OUTLINE_COLOR, false);
        graphics.drawString(font, text, drawX, drawY, color, false);
        graphics.pose().popPose();
    }
}
