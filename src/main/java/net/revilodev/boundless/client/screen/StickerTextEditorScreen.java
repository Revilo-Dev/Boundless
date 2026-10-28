package net.revilodev.boundless.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.client.editor.QuestEditorControls.TextInsertButton;
import net.revilodev.boundless.client.editor.QuestEditorEmojis;
import net.revilodev.boundless.client.editor.ScaledMultiLineEditBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class StickerTextEditorScreen extends Screen {
    private static final ResourceLocation EMOJI_GUI = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/sprites/editor/emoji-gui.png");
    private static final ResourceLocation PANEL_GUI = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/20x20-9-slice.png");
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 132;
    private static final int EMOJI_WIDTH = 81;
    private static final int EMOJI_HEIGHT = 112;
    private static final int EMOJI_COLUMNS = 4;
    private static final int EMOJI_ROWS = 5;

    private final Screen parent;
    private final String initialText;
    private final Consumer<String> onSave;
    private final List<TextInsertButton> formatButtons = new ArrayList<>();
    private ScaledMultiLineEditBox textBox;
    private int panelX;
    private int panelY;
    private int emojiPage;

    public StickerTextEditorScreen(Screen parent, String initialText, Consumer<String> onSave) {
        super(Component.literal("Edit tree text"));
        this.parent = parent;
        this.initialText = initialText == null ? "" : initialText;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        formatButtons.clear();
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        int textX = panelX + 7;
        int textWidth = 181;
        textBox = new ScaledMultiLineEditBox(font, textX, panelY + 17, textWidth, 48,
                Component.literal("Sticker text"), Component.empty(), 0.85f, true);
        textBox.setCharacterLimit(1024);
        textBox.setValue(initialText);
        textBox.setFocused(true);
        addRenderableWidget(textBox);

        int x = textX;
        int y = panelY + 70;
        x = addFormatButton(x, y, 12, "<", "__undo__", 0xFF4A4A4A, 0.75f);
        x = addFormatButton(x, y, 12, ">", "__redo__", 0xFF4A4A4A, 0.75f);
        x = addFormatButton(x, y, 12, "X", "/x", 0xFF4A4A4A, 0.75f);
        x = addFormatButton(x, y, 9, "", "/r", 0xFFFF5555, 1f);
        x = addFormatButton(x, y, 9, "", "/g", 0xFF55FF55, 1f);
        x = addFormatButton(x, y, 9, "", "/b", 0xFF5555FF, 1f);
        x = addFormatButton(x, y, 9, "", "/w", 0xFF55FFFF, 1f);
        x = addFormatButton(x, y, 9, "", "/y", 0xFFFFFF55, 1f);
        x = addFormatButton(x, y, 9, "", "/o", 0xFFFFAA00, 1f);
        x = addFormatButton(x, y, 9, "", "/a", 0xFFAAAAAA, 1f);
        x = addFormatButton(x, y, 9, "", "/p", 0xFFAA55FF, 1f);
        x = addFormatButton(x, y, 12, "B", "/l", 0xFF555555, 0.75f);
        x = addFormatButton(x, y, 12, "I", "/i", 0xFF555555, 0.75f);
        x = addFormatButton(x, y, 12, "E", "/e", 0xFF555555, 0.75f);
        addFormatButton(x, y, 12, "H", "/h", 0xFF777733, 0.75f);

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(textX, panelY + 99, 86, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(textX + 95, panelY + 99, 86, 20).build());
    }

    private int addFormatButton(int x, int y, int width, String label, String action, int color, float scale) {
        TextInsertButton button = new TextInsertButton(x, y, width, label, action, color, scale, this::applyFormat);
        formatButtons.add(button);
        addRenderableWidget(button);
        return x + width + 2;
    }

    private void applyFormat(String action) {
        if ("__undo__".equals(action)) textBox.undo();
        else if ("__redo__".equals(action)) textBox.redo();
        else textBox.wrapSelection(action, "/x");
        textBox.setFocused(true);
    }

    private void save() {
        if (onSave != null) onSave.accept(textBox.getValue());
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (parent != null) parent.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 700);
        graphics.fill(0, 0, width, height, 0xA0000000);
        renderNineSlice(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawString(font, title, panelX + 7, panelY + 5, 0xFF000000, false);
        renderEmojiPanel(graphics);
        for (TextInsertButton button : formatButtons) {
            String action = button.insertText();
            button.active = !"__undo__".equals(action) || textBox.canUndo();
            if ("__redo__".equals(action)) button.active = textBox.canRedo();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    private void renderNineSlice(GuiGraphics graphics, int x, int y, int width, int height) {
        int slice = 5;
        int center = 10;
        graphics.blit(PANEL_GUI, x, y, 0, 0, slice, slice, 20, 20);
        graphics.blit(PANEL_GUI, x + width - slice, y, 15, 0, slice, slice, 20, 20);
        graphics.blit(PANEL_GUI, x, y + height - slice, 0, 15, slice, slice, 20, 20);
        graphics.blit(PANEL_GUI, x + width - slice, y + height - slice, 15, 15, slice, slice, 20, 20);
        graphics.blit(PANEL_GUI, x + slice, y, width - slice * 2, slice, slice, 0, center, slice, 20, 20);
        graphics.blit(PANEL_GUI, x + slice, y + height - slice, width - slice * 2, slice, slice, 15, center, slice, 20, 20);
        graphics.blit(PANEL_GUI, x, y + slice, slice, height - slice * 2, 0, slice, slice, center, 20, 20);
        graphics.blit(PANEL_GUI, x + width - slice, y + slice, slice, height - slice * 2, 15, slice, slice, center, 20, 20);
        graphics.blit(PANEL_GUI, x + slice, y + slice, width - slice * 2, height - slice * 2,
                slice, slice, center, center, 20, 20);
    }

    private void renderEmojiPanel(GuiGraphics graphics) {
        int x = panelX + PANEL_WIDTH - EMOJI_WIDTH - 5;
        int y = panelY + 15;
        graphics.blit(EMOJI_GUI, x, y, 0, 0, EMOJI_WIDTH, EMOJI_HEIGHT, EMOJI_WIDTH, EMOJI_HEIGHT);
        graphics.drawCenteredString(font, Component.literal("Emojis"), x + EMOJI_WIDTH / 2, y + 1, 0xFFFFFFFF);
        List<String> emojis = QuestEditorEmojis.names();
        int start = emojiPage * EMOJI_COLUMNS * EMOJI_ROWS;
        for (int index = 0; index < EMOJI_COLUMNS * EMOJI_ROWS && start + index < emojis.size(); index++) {
            int drawX = x + 4 + index % EMOJI_COLUMNS * 18;
            int drawY = y + 10 + index / EMOJI_COLUMNS * 18;
            graphics.blit(QuestEditorEmojis.texture(emojis.get(start + index)), drawX + 1, drawY + 1,
                    0, 0, 16, 16, 16, 16);
        }
        int maxPage = Math.max(0, (emojis.size() - 1) / (EMOJI_COLUMNS * EMOJI_ROWS));
        graphics.drawString(font, "<", x + 8, y + 101, emojiPage > 0 ? 0xFF000000 : 0xFF444444, false);
        graphics.drawString(font, ">", x + EMOJI_WIDTH - 12, y + 101, emojiPage < maxPage ? 0xFF000000 : 0xFF444444, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickEmojiPanel(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickEmojiPanel(double mouseX, double mouseY) {
        int x = panelX + PANEL_WIDTH - EMOJI_WIDTH - 5;
        int y = panelY + 15;
        if (mouseX < x || mouseX >= x + EMOJI_WIDTH || mouseY < y || mouseY >= y + EMOJI_HEIGHT) return false;
        List<String> emojis = QuestEditorEmojis.names();
        int maxPage = Math.max(0, (emojis.size() - 1) / (EMOJI_COLUMNS * EMOJI_ROWS));
        if (mouseY >= y + 99) {
            if (mouseX < x + 24) emojiPage = Math.max(0, emojiPage - 1);
            else if (mouseX > x + EMOJI_WIDTH - 24) emojiPage = Math.min(maxPage, emojiPage + 1);
            return true;
        }
        if (mouseY < y + 10) return true;
        int column = (int) ((mouseX - x - 4) / 18);
        int row = (int) ((mouseY - y - 10) / 18);
        if (column < 0 || column >= EMOJI_COLUMNS || row < 0 || row >= EMOJI_ROWS) return true;
        int index = emojiPage * EMOJI_COLUMNS * EMOJI_ROWS + row * EMOJI_COLUMNS + column;
        if (index < emojis.size()) {
            textBox.insertText(QuestEditorEmojis.text(emojis.get(index)));
            textBox.setFocused(true);
        }
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
