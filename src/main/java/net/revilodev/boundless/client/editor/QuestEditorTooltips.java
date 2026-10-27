package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.revilodev.boundless.Config;

import java.util.ArrayList;
import java.util.List;

public final class QuestEditorTooltips {
    private QuestEditorTooltips() {
    }

    public static void render(GuiGraphics graphics, Font font, Component text, int mouseX, int mouseY) {
        if (text == null) return;
        renderFormatted(graphics, font, List.of(text.getVisualOrderText()), mouseX, mouseY);
    }

    public static void renderComponents(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;
        renderFormatted(graphics, font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
    }

    public static void renderFormatted(GuiGraphics graphics, Font font, List<? extends FormattedCharSequence> lines,
                                       int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;
        float scale = Config.editorScale();
        ClientTooltipPositioner positioner = (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) ->
                DefaultTooltipPositioner.INSTANCE.positionTooltip(
                        Math.max(1, Math.round(screenWidth / scale)),
                        Math.max(1, Math.round(screenHeight / scale)),
                        x, y, tooltipWidth, tooltipHeight);
        graphics.renderTooltip(font, new ArrayList<>(lines), positioner, mouseX, mouseY);
    }
}
