package net.revilodev.boundless.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class QuestPanelBackgroundRenderer {
    private static final int TEXTURE_WIDTH = 147;
    private static final int TEXTURE_HEIGHT = 166;
    private static final int SLICE = 10;

    private QuestPanelBackgroundRenderer() {
    }

    public static void render(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        int centerWidth = Math.max(0, width - SLICE * 2);
        int centerHeight = Math.max(0, height - SLICE * 2);
        int sourceCenterWidth = TEXTURE_WIDTH - SLICE * 2;
        int sourceCenterHeight = TEXTURE_HEIGHT - SLICE * 2;

        graphics.blit(texture, x, y, 0, 0, SLICE, SLICE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.blit(texture, x + width - SLICE, y, TEXTURE_WIDTH - SLICE, 0, SLICE, SLICE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.blit(texture, x, y + height - SLICE, 0, TEXTURE_HEIGHT - SLICE, SLICE, SLICE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.blit(texture, x + width - SLICE, y + height - SLICE, TEXTURE_WIDTH - SLICE, TEXTURE_HEIGHT - SLICE,
                SLICE, SLICE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (centerWidth > 0) {
            graphics.blit(texture, x + SLICE, y, centerWidth, SLICE, SLICE, 0, sourceCenterWidth, SLICE,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
            graphics.blit(texture, x + SLICE, y + height - SLICE, centerWidth, SLICE, SLICE,
                    TEXTURE_HEIGHT - SLICE, sourceCenterWidth, SLICE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (centerHeight > 0) {
            graphics.blit(texture, x, y + SLICE, SLICE, centerHeight, 0, SLICE, SLICE, sourceCenterHeight,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
            graphics.blit(texture, x + width - SLICE, y + SLICE, SLICE, centerHeight, TEXTURE_WIDTH - SLICE,
                    SLICE, SLICE, sourceCenterHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (centerWidth > 0 && centerHeight > 0) {
            graphics.blit(texture, x + SLICE, y + SLICE, centerWidth, centerHeight, SLICE, SLICE,
                    sourceCenterWidth, sourceCenterHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }
}
