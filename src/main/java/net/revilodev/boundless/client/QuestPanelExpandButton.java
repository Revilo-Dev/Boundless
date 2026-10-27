package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

public final class QuestPanelExpandButton extends AbstractButton {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/sprites/editor/tree/button-enlarge.png");
    private final BooleanSupplier expanded;
    private final Runnable onPress;

    public QuestPanelExpandButton(int x, int y, BooleanSupplier expanded, Runnable onPress) {
        super(x, y, 7, 7, Component.empty());
        this.expanded = expanded;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        graphics.blit(TEXTURE, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
        graphics.pose().popPose();
        if (isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(expanded.getAsBoolean() ? "Collapse tree panel" : "Expand tree panel"),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
