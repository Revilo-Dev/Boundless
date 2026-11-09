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

    public static void drawJoint(GuiGraphics graphics, int x, int y, int color) {
        drawJoint(graphics, x, y, OUTER_WIDTH, 0xFF000000);
        drawJoint(graphics, x, y, INNER_WIDTH, color);
    }

    private static void drawSegment(GuiGraphics graphics, int startX, int startY, int endX, int endY,
                                    int thickness, int color) {
        int dx = endX - startX;
        int dy = endY - startY;
        int half = thickness / 2;
        if (dy == 0) {
            graphics.fill(Math.min(startX, endX), startY - half,
                    Math.max(startX, endX) + 1, startY + thickness - half, color);
            return;
        }
        if (dx == 0) {
            graphics.fill(startX - half, Math.min(startY, endY),
                    startX + thickness - half, Math.max(startY, endY) + 1, color);
            return;
        }
        int length = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy)));
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        graphics.pose().pushPose();
        graphics.pose().translate(startX, startY, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.fill(0, -half, length + 1, thickness - half, color);
        graphics.pose().popPose();
    }

    private static void drawJoint(GuiGraphics graphics, int x, int y, int thickness, int color) {
        int half = thickness / 2;
        graphics.fill(x - half, y - half, x + thickness - half, y + thickness - half, color);
    }
}
