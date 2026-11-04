package net.revilodev.boundless.client.editor;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;

// keeps dependency line styling identical in editable and playable trees
public final class QuestTreeConnectorRenderer {
    public static final int NORMAL_COLOR = 0xFF686868;
    public static final int INCOMPATIBLE_COLOR = 0xFFFF3030;
    public static final int COMPLETED_COLOR = 0xFF528C67;
    public static final int HIGHLIGHTED_COLOR = 0xFFFFFFFF;
    private static final int OUTER_WIDTH = 5;
    private static final int INNER_WIDTH = 3;

    private QuestTreeConnectorRenderer() {
    }

    public static void drawSegment(GuiGraphics graphics, int startX, int startY, int endX, int endY, int color) {
        drawSegment(graphics, startX, startY, endX, endY, OUTER_WIDTH, 0xFF000000);
        drawSegment(graphics, startX, startY, endX, endY, INNER_WIDTH, color);
    }

    private static void drawSegment(GuiGraphics graphics, int startX, int startY, int endX, int endY,
                                    int thickness, int color) {
        int dx = endX - startX;
        int dy = endY - startY;
        int length = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy)));
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        int half = thickness / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(startX, startY, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.fill(0, -half, length + 1, thickness - half, color);
        graphics.pose().popPose();
    }
}
