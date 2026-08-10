package net.revilodev.boundless.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class LegacyGuiSprites {
    private static final ResourceLocation WIDGETS_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/widgets.png");

    private LegacyGuiSprites() {}

    public static void drawButton(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        String path = sprite == null ? "" : sprite.getPath();
        if (path.contains("text_field")) {
            drawTextField(graphics, path.contains("highlighted") || path.contains("focused"), x, y, width, height);
            return;
        }
        boolean disabled = path.contains("disabled");
        boolean highlighted = path.contains("highlighted") || path.contains("focused");
        int textureY = disabled ? 46 : highlighted ? 86 : 66;
        if (width < 3 || height < 3) {
            graphics.blit(WIDGETS_TEXTURE, x, y, width, height, 0, textureY, 200, 20, 256, 256);
            return;
        }
        int horizontalBorder = Math.min(20, (width - 1) / 2);
        int verticalBorder = Math.min(4, (height - 1) / 2);
        graphics.blitNineSliced(WIDGETS_TEXTURE, x, y, width, height,
                horizontalBorder, verticalBorder, horizontalBorder, verticalBorder,
                200, 20, 0, textureY);
    }

    private static void drawTextField(GuiGraphics graphics, boolean focused, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, focused ? 0xFFFFFFFF : 0xFFA0A0A0);
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
    }
}
