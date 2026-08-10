package net.revilodev.boundless.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// quest search field with its icon inside the normal input border
public final class QuestSearchBox extends EditBox {
    private static final ResourceLocation SEARCH_ICON = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/sprites/icons/search.png");
    private static final ResourceLocation FIELD = ResourceLocation.withDefaultNamespace("widget/text_field");
    private static final ResourceLocation FIELD_FOCUSED = ResourceLocation.withDefaultNamespace("widget/text_field_highlighted");
    private static final int TEXT_INSET = 21;

    public QuestSearchBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        int x = getX();
        int y = getY();
        int width = getWidth();
        LegacyGuiSprites.drawButton(graphics, isFocused() ? FIELD_FOCUSED : FIELD, x, y, width, getHeight());
        graphics.blit(SEARCH_ICON, x + 2, y, 0, 0, 16, 16, 16, 16);

        setX(x + TEXT_INSET);
        setY(y + 4);
        setWidth(Math.max(1, width - TEXT_INSET - 4));
        setBordered(false);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        setBordered(true);
        setWidth(width);
        setY(y);
        setX(x);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int x = getX();
        int width = getWidth();
        int textX = x + TEXT_INSET;
        setX(textX);
        setWidth(Math.max(1, width - TEXT_INSET - 4));
        setBordered(false);
        super.onClick(Math.max(mouseX, textX), mouseY);
        setBordered(true);
        setWidth(width);
        setX(x);
    }
}
