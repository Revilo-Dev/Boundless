package net.revilodev.boundless.client.editor;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

// scaled multi line edit box
public final class ScaledMultiLineEditBox extends AbstractScrollWidget {
    private static final int CURSOR_INSERT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = -2039584;
    private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
    private static final int CURSOR_BLINK_INTERVAL_MS = 300;
    private final Font font;
    private Component placeholder;
    private final ScaledTextField textField;
    private final float textScale;
    private final double lineHeight;
    private final boolean allowColorFormatting;
    private long focusedTime = Util.getMillis();
    private int textColor = TEXT_COLOR;

    public ScaledMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message, float textScale, boolean allowColorFormatting) {
        super(x, y, width, height, message);
        this.font = font;
        this.placeholder = placeholder;
        this.textScale = textScale <= 0f ? 1f : textScale;
        this.lineHeight = 9.0 * this.textScale;
        this.allowColorFormatting = allowColorFormatting;
        int fieldWidth = Math.max(20, Math.round((width - this.totalInnerPadding()) / this.textScale));
        this.textField = new ScaledTextField(font, fieldWidth, allowColorFormatting);
        this.textField.setCursorListener(this::scrollToCursor);
    }

    public void setCharacterLimit(int characterLimit) {
        this.textField.setCharacterLimit(characterLimit);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        float safeScale = this.textScale <= 0f ? 1f : this.textScale;
        this.textField.setWidth(Math.max(20, Math.round((width - this.totalInnerPadding()) / safeScale)));
        this.setScrollAmount(Mth.clamp(this.scrollAmount(), 0.0, Math.max(0.0, this.getInnerHeight() - (this.height - this.totalInnerPadding()))));
    }

    public void setValueListener(Consumer<String> valueListener) {
        this.textField.setValueListener(valueListener);
    }

    public void setValue(String fullText) {
        this.textField.setValue(fullText);
    }

    public void setPlaceholder(Component placeholder) {
        this.placeholder = placeholder == null ? Component.empty() : placeholder;
    }

    public void insertText(String text) {
        this.textField.insertText(text);
    }

    public boolean canUndo() {
        return this.textField.canUndo();
    }

    public boolean canRedo() {
        return this.textField.canRedo();
    }

    public void undo() {
        this.textField.undo();
    }

    public void redo() {
        this.textField.redo();
    }

    public void clearHistory() {
        this.textField.clearHistory();
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public String getValue() {
        return this.textField.value();
    }

    public int getCursorPosition() {
        return this.textField.cursor();
    }

    public void setCursorPosition(int cursor) {
        this.textField.seekCursor(Whence.ABSOLUTE, Mth.clamp(cursor, 0, this.textField.value().length()));
    }

    public int getLineCount() {
        return this.textField.getLineCount();
    }

    public double getLineHeight() {
        return this.lineHeight;
    }

    public void scrollToTop() {
        this.setScrollAmount(0.0);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;
        if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
            this.setFocused(true);
            this.textField.setSelecting(Screen.hasShiftDown());
            this.seekCursorScreen(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.visible || !this.active) return false;
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        } else if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
            this.textField.setSelecting(true);
            this.seekCursorScreen(mouseX, mouseY);
            this.textField.setSelecting(Screen.hasShiftDown());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.textField.keyPressed(keyCode);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.visible && this.isFocused() && StringUtil.isAllowedChatCharacter(codePoint)) {
            this.textField.insertText(Character.toString(codePoint));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY)) return false;
        double maxScroll = Math.max(0.0, this.getInnerHeight() - (this.height - this.totalInnerPadding()));
        if (maxScroll <= 0.0) return false;
        this.setScrollAmount(Mth.clamp(this.scrollAmount() - (deltaY * this.lineHeight), 0.0, maxScroll));
        return true;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        this.renderBackground(guiGraphics);
        guiGraphics.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0, -this.scrollAmount(), 0.0);
        guiGraphics.pose().scale(this.textScale, this.textScale, 1.0f);
        this.renderContents(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        this.renderDecorations(guiGraphics);
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        int border = this.isFocused() ? 0xFFFFFFFF : 0xFF8A8A8A;
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, border);
        guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, border);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, border);
        guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, border);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        String text = this.textField.value();
        float safeScale = this.textScale <= 0f ? 1f : this.textScale;
        float invScale = 1.0f / safeScale;
        int baseX = Math.round((this.getX() + this.innerPadding()) * invScale);
        int baseY = Math.round((this.getY() + this.innerPadding()) * invScale);
        int wrapWidth = Math.max(1, Math.round((this.width - this.totalInnerPadding()) * invScale));

        if (text.isEmpty() && !this.isFocused()) {
            guiGraphics.drawWordWrap(this.font, this.placeholder, baseX, baseY, wrapWidth, PLACEHOLDER_TEXT_COLOR);
            return;
        }

        int cursor = this.textField.cursor();
        boolean showCursor = this.isFocused() && (Util.getMillis() - this.focusedTime) / CURSOR_BLINK_INTERVAL_MS % 2L == 0L;
        boolean cursorInText = cursor < text.length();
        int drawX = baseX;
        int activeColor = this.textColor;
        double lineScreenY = this.getY() + this.innerPadding();
        double lastLineScreenY = lineScreenY;
        int cursorRenderX = -1;
        int cursorRenderY = -1;

        for (ScaledTextField.StringView line : this.textField.iterateLines()) {
            boolean visible = this.withinContentAreaTopBottom((int) lineScreenY, (int) (lineScreenY + this.lineHeight));
            String lineText = text.substring(line.beginIndex(), line.endIndex());
            int lineInitialColor = activeColor;
            if (visible) {
                int drawY = Math.round((float) (lineScreenY * invScale));
                long lineRender = drawFormattedString(guiGraphics, lineText, baseX, drawY, activeColor);
                drawX = unpackEndX(lineRender);
                activeColor = unpackFinalColor(lineRender);
                if (cursor >= line.beginIndex() && cursor <= line.endIndex()) {
                    cursorRenderX = cursorXForLine(text, line, cursor, baseX, lineInitialColor);
                    cursorRenderY = drawY;
                }
            } else {
                activeColor = unpackFinalColor(drawFormattedString(null, lineText, baseX, 0, activeColor));
            }

            lastLineScreenY = lineScreenY;
            lineScreenY += this.lineHeight;
        }

        if (showCursor && !cursorInText
                && this.withinContentAreaTopBottom((int) lastLineScreenY, (int) (lastLineScreenY + this.lineHeight))) {
            int drawY = Math.round((float) (lastLineScreenY * invScale));
            cursorRenderX = drawX;
            cursorRenderY = drawY;
        }

        if (this.textField.hasSelection()) {
            ScaledTextField.StringView selected = this.textField.getSelected();
            int startX = baseX;
            int maxLineWidth = Math.max(1, Math.round((this.width - this.innerPadding()) * invScale));
            double selScreenY = this.getY() + this.innerPadding();

            for (ScaledTextField.StringView line : this.textField.iterateLines()) {
                if (selected.beginIndex() > line.endIndex()) {
                    selScreenY += this.lineHeight;
                    continue;
                }
                if (line.beginIndex() > selected.endIndex()) break;

                if (this.withinContentAreaTopBottom((int) selScreenY, (int) (selScreenY + this.lineHeight))) {
                    int offset = this.font.width(
                            text.substring(line.beginIndex(), Math.max(selected.beginIndex(), line.beginIndex()))
                    );
                    int endWidth;
                    if (selected.endIndex() > line.endIndex()) {
                        endWidth = maxLineWidth;
                    } else {
                        endWidth = this.font.width(text.substring(line.beginIndex(), selected.endIndex()));
                    }
                    int drawY = Math.round((float) (selScreenY * invScale));
                    this.renderHighlight(guiGraphics, startX + offset, drawY, startX + endWidth, drawY + this.font.lineHeight);
                }
                selScreenY += this.lineHeight;
            }
        }
        if (showCursor && cursorRenderX >= 0 && cursorRenderY >= 0) {
            renderCursor(guiGraphics, cursorRenderX, cursorRenderY);
        }
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics) {
    }

    @Override
    public int getInnerHeight() {
        return Math.max(1, (int) Math.ceil(this.lineHeight * this.textField.getLineCount()));
    }

    @Override
    protected boolean scrollbarVisible() {
        return (double) this.textField.getLineCount() > this.getDisplayableLineCount()
                && this.getMaxScrollAmount() > 0;
    }

    @Override
    protected double scrollRate() {
        return this.lineHeight / 2.0;
    }

    private void renderHighlight(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY) {
        guiGraphics.fill(RenderType.guiTextHighlight(), minX, minY, maxX, maxY, -16776961);
    }

    private void renderCursor(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y - 1, x + 1, y + 1 + this.font.lineHeight, CURSOR_INSERT_COLOR);
    }

    private int cursorXForLine(String fullText, ScaledTextField.StringView line, int cursor, int baseX, int initialColor) {
        int boundedCursor = Mth.clamp(cursor, line.beginIndex(), line.endIndex());
        String beforeCursor = fullText.substring(line.beginIndex(), boundedCursor);
        return unpackEndX(drawFormattedString(null, beforeCursor, baseX, 0, initialColor));
    }

    private void scrollToCursor() {
        if (this.lineHeight <= 0.0) return;
        if (this.textField.getLineCount() <= 0) {
            this.setScrollAmount(0.0);
            return;
        }
        double scroll = this.scrollAmount();
        ScaledTextField.StringView line = this.textField.getLineView((int) (scroll / this.lineHeight));
        int cursorLine = this.textField.getLineAtCursor();
        if (cursorLine < 0) {
            this.setScrollAmount(0.0);
            return;
        }
        if (this.textField.cursor() <= line.beginIndex()) {
            scroll = (double) cursorLine * this.lineHeight;
        } else {
            ScaledTextField.StringView lastVisible = this.textField.getLineView((int) ((scroll + (double) this.height) / this.lineHeight) - 1);
            if (this.textField.cursor() > lastVisible.endIndex()) {
                scroll = (double) cursorLine * this.lineHeight - this.height + this.lineHeight + this.totalInnerPadding();
            }
        }

        this.setScrollAmount(scroll);
    }

    private double getDisplayableLineCount() {
        return this.lineHeight <= 0.0 ? 0.0 : (double) (this.height - this.totalInnerPadding()) / this.lineHeight;
    }

    private long drawFormattedString(GuiGraphics guiGraphics, String text, int x, int y, int initialColor) {
        if (!this.allowColorFormatting) {
            int drawX = x;
            if (guiGraphics != null) {
                drawX = guiGraphics.drawString(this.font, text, drawX, y, initialColor, false);
            } else {
                drawX += this.font.width(text);
            }
            return packRenderResult(drawX, initialColor);
        }
        int drawX = x;
        int color = initialColor;
        boolean bold = false;
        boolean italic = false;
        boolean encrypted = false;
        boolean highlight = false;
        StringBuilder segment = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (startsWithColorToken(text, i)) {
                if (!segment.isEmpty()) {
                    if (guiGraphics != null) {
                        drawX = drawStyledSegment(guiGraphics, segment.toString(), drawX, y, color, bold, italic, encrypted, highlight);
                    } else {
                        drawX += this.font.width(segment.toString());
                    }
                    segment.setLength(0);
                }
                String escape = text.substring(i, Math.min(i + 2, text.length()));
                if (guiGraphics != null) {
                    drawX = guiGraphics.drawString(this.font, escape, drawX, y, 0xFF9A9A9A, false);
                } else {
                    drawX += this.font.width(escape);
                }
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'l') {
                    bold = !bold;
                } else if (code == 'i') {
                    italic = !italic;
                } else if (code == 'e') {
                    encrypted = !encrypted;
                } else if (code == 'h') {
                    highlight = !highlight;
                } else if (code == 'x') {
                    color = this.textColor & 0xFFFFFF;
                    bold = false;
                    italic = false;
                    encrypted = false;
                    highlight = false;
                } else {
                    color = formatColor(code, this.textColor);
                }
                i += 1;
                continue;
            }
            segment.append(text.charAt(i));
        }
        if (!segment.isEmpty()) {
            if (guiGraphics != null) {
                drawX = drawStyledSegment(guiGraphics, segment.toString(), drawX, y, color, bold, italic, encrypted, highlight);
            } else {
                drawX += this.font.width(segment.toString());
            }
        }
        return packRenderResult(drawX, color);
    }

    private int drawStyledSegment(GuiGraphics guiGraphics, String text, int x, int y, int color,
                                  boolean bold, boolean italic, boolean encrypted, boolean highlight) {
        if (!highlight || text == null || text.isEmpty()) {
            return guiGraphics.drawString(
                    this.font,
                    Component.literal(text).withStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(italic).withObfuscated(encrypted)),
                    x,
                    y,
                    color,
                    false
            );
        }
        int drawX = x;
        int length = Math.max(1, text.length());
        float phase = (Util.getMillis() % 1800L) / 1800.0f;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float position = i / (float) length;
            float distance = Math.abs(position - phase);
            distance = Math.min(distance, 1.0f - distance);
            float shine = Mth.clamp(1.0f - distance / 0.22f, 0.0f, 1.0f);
            int shineColor = lightenColor(color, 44 + Math.round(110.0f * shine));
            drawX = guiGraphics.drawString(
                    this.font,
                    Component.literal(ch).withStyle(Style.EMPTY.withColor(shineColor).withBold(bold).withItalic(italic).withObfuscated(encrypted)),
                    drawX,
                    y,
                    shineColor,
                    false
            );
        }
        return drawX;
    }

    private int lightenColor(int color, int amount) {
        int r = Math.min(255, ((color >>> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >>> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (r << 16) | (g << 8) | b;
    }

    private boolean startsWithColorToken(String text, int index) {
        return index + 1 < text.length()
                && text.charAt(index) == '/'
                && isColorTokenCode(text.charAt(index + 1));
    }

    private int formatColor(char code, int fallback) {
        return switch (Character.toLowerCase(code)) {
            case 'w' -> 0x55FFFF;
            case 'r' -> 0xFF5555;
            case 'g' -> 0x55FF55;
            case 'b' -> 0x5555FF;
            case 'y' -> 0xFFFF55;
            case 'o' -> 0xFFAA00;
            case 'a' -> 0xAAAAAA;
            case 'p' -> 0xAA55FF;
            case 'x' -> this.textColor & 0xFFFFFF;
            default -> fallback;
        };
    }

    private boolean isColorTokenCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case 'w', 'r', 'g', 'b', 'y', 'o', 'a', 'p', 'x', 'l', 'i', 'e', 'h' -> true;
            default -> false;
        };
    }

    private long packRenderResult(int endX, int finalColor) {
        return (((long) endX) << 32) | (finalColor & 0xFFFFFFFFL);
    }

    private int unpackEndX(long packed) {
        return (int) (packed >> 32);
    }

    private int unpackFinalColor(long packed) {
        return (int) packed;
    }

    private void seekCursorScreen(double mouseX, double mouseY) {
        double safeScale = this.textScale <= 0f ? 1.0 : this.textScale;
        double d0 = (mouseX - (double) this.getX() - (double) this.innerPadding()) / safeScale;
        double d1 = (mouseY - (double) this.getY() - (double) this.innerPadding() + this.scrollAmount()) / safeScale;
        this.textField.seekCursorToPoint(d0, d1);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            this.focusedTime = Util.getMillis();
        }
    }
}

final class ScaledTextField {
    private final Font font;
    private final List<StringView> displayLines = new ArrayList<>();
    private int width;
    private final boolean colorTokenAwareDeletion;
    private String value = "";
    private int cursor;
    private int selectCursor;
    private boolean selecting;
    private int characterLimit = Integer.MAX_VALUE;
    private Consumer<String> valueListener = ignored -> {
    };
    private Runnable cursorListener = () -> {
    };
    private final Deque<HistoryState> undoHistory = new ArrayDeque<>();
    private final Deque<HistoryState> redoHistory = new ArrayDeque<>();
    private boolean restoringHistory = false;

    ScaledTextField(Font font, int width, boolean colorTokenAwareDeletion) {
        this.font = font;
        this.width = width;
        this.colorTokenAwareDeletion = colorTokenAwareDeletion;
        this.setValue("");
    }

    public void setWidth(int width) {
        int next = Math.max(1, width);
        if (this.width == next) return;
        this.width = next;
        this.reflowDisplayLines();
        this.cursorListener.run();
    }

    public int characterLimit() {
        return this.characterLimit;
    }

    public void setCharacterLimit(int characterLimit) {
        if (characterLimit < 0) {
            throw new IllegalArgumentException("Character limit cannot be negative");
        }
        this.characterLimit = characterLimit;
    }

    public boolean hasCharacterLimit() {
        return this.characterLimit != Integer.MAX_VALUE;
    }

    public void setValueListener(Consumer<String> valueListener) {
        this.valueListener = valueListener == null ? ignored -> {
        } : valueListener;
    }

    public void setCursorListener(Runnable cursorListener) {
        this.cursorListener = cursorListener == null ? () -> {
        } : cursorListener;
    }

    public void setValue(String fullText) {
        String next = fullText == null ? "" : fullText;
        this.value = this.truncateFullText(next);
        this.cursor = this.value.length();
        this.selectCursor = this.cursor;
        this.clearHistory();
        this.onValueChange();
    }

    public String value() {
        return this.value;
    }

    public void insertText(String text) {
        if (!text.isEmpty() || this.hasSelection()) {
            pushUndoState();
            String filtered = this.truncateInsertionText(StringUtil.filterText(text, true));
            StringView selected = this.getSelected();
            int len = this.value.length();
            int begin = Mth.clamp(selected.beginIndex, 0, len);
            int end = Mth.clamp(selected.endIndex, begin, len);
            this.value = new StringBuilder(this.value).replace(begin, end, filtered).toString();
            this.cursor = begin + filtered.length();
            this.selectCursor = this.cursor;
            this.onValueChange();
        }
    }

    public void deleteText(int length) {
        if (!this.hasSelection()) {
            if (this.colorTokenAwareDeletion && length < 0) {
                int tokenStart = this.findColorTokenStartBeforeCursor();
                if (tokenStart >= 0) {
                    this.selectCursor = tokenStart;
                    this.insertText("");
                    return;
                }
            } else if (this.colorTokenAwareDeletion && length > 0) {
                int tokenEnd = this.findColorTokenEndAtCursor();
                if (tokenEnd >= 0) {
                    this.selectCursor = tokenEnd;
                    this.insertText("");
                    return;
                }
            }
            this.selectCursor = Mth.clamp(this.cursor + length, 0, this.value.length());
        }
        this.insertText("");
    }

    public int cursor() {
        return this.cursor;
    }

    public boolean canUndo() {
        return !this.undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !this.redoHistory.isEmpty();
    }

    public void undo() {
        restoreFromHistory(this.undoHistory, this.redoHistory);
    }

    public void redo() {
        restoreFromHistory(this.redoHistory, this.undoHistory);
    }

    public void clearHistory() {
        this.undoHistory.clear();
        this.redoHistory.clear();
    }

    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    public StringView getSelected() {
        int len = this.value.length();
        int start = Mth.clamp(Math.min(this.selectCursor, this.cursor), 0, len);
        int end = Mth.clamp(Math.max(this.selectCursor, this.cursor), start, len);
        return new StringView(start, end);
    }

    public int getLineCount() {
        return this.displayLines.size();
    }

    public int getLineAtCursor() {
        for (int i = 0; i < this.displayLines.size(); i++) {
            StringView line = this.displayLines.get(i);
            if (this.cursor >= line.beginIndex && this.cursor <= line.endIndex) {
                return i;
            }
        }
        return -1;
    }

    public StringView getLineView(int lineNumber) {
        if (this.displayLines.isEmpty()) return StringView.EMPTY;
        return this.displayLines.get(Mth.clamp(lineNumber, 0, this.displayLines.size() - 1));
    }

    public void seekCursor(Whence whence, int position) {
        switch (whence) {
            case ABSOLUTE -> this.cursor = position;
            case RELATIVE -> this.cursor += position;
            case END -> this.cursor = this.value.length() + position;
        }

        this.cursor = Mth.clamp(this.cursor, 0, this.value.length());
        this.cursorListener.run();
        if (!this.selecting) {
            this.selectCursor = this.cursor;
        }
    }

    public void seekCursorLine(int offset) {
        if (offset != 0) {
            int width = this.font.width(this.value.substring(this.getCursorLineView().beginIndex, this.cursor)) + 2;
            StringView line = this.getCursorLineView(offset);
            int length = this.font
                    .plainSubstrByWidth(this.value.substring(line.beginIndex, line.endIndex), width)
                    .length();
            this.seekCursor(Whence.ABSOLUTE, line.beginIndex + length);
        }
    }

    public void seekCursorToPoint(double x, double y) {
        if (this.displayLines.isEmpty()) {
            this.seekCursor(Whence.ABSOLUTE, 0);
            return;
        }
        int xi = Mth.floor(x);
        int yi = Mth.floor(y / 9.0);
        StringView line = this.displayLines.get(Mth.clamp(yi, 0, this.displayLines.size() - 1));
        int length = this.font
                .plainSubstrByWidth(this.value.substring(line.beginIndex, line.endIndex), xi)
                .length();
        this.seekCursor(Whence.ABSOLUTE, line.beginIndex + length);
    }

    public boolean keyPressed(int keyCode) {
        this.selecting = Screen.hasShiftDown();
        if (Screen.isSelectAll(keyCode)) {
            this.cursor = this.value.length();
            this.selectCursor = 0;
            return true;
        } else if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            this.undo();
            return true;
        } else if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
            this.redo();
            return true;
        } else if (Screen.isCopy(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
            return true;
        } else if (Screen.isPaste(keyCode)) {
            this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        } else if (Screen.isCut(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
            this.insertText("");
            return true;
        } else {
            switch (keyCode) {
                case 257, 335 -> {
                    this.insertText("\n");
                    return true;
                }
                case 259 -> {
                    if (Screen.hasControlDown()) {
                        StringView word = this.getPreviousWord();
                        this.deleteText(word.beginIndex - this.cursor);
                    } else {
                        this.deleteText(-1);
                    }
                    return true;
                }
                case 261 -> {
                    if (Screen.hasControlDown()) {
                        StringView word = this.getNextWord();
                        this.deleteText(word.beginIndex - this.cursor);
                    } else {
                        this.deleteText(1);
                    }
                    return true;
                }
                case 262 -> {
                    if (Screen.hasControlDown()) {
                        StringView word = this.getNextWord();
                        this.seekCursor(Whence.ABSOLUTE, word.beginIndex);
                    } else {
                        this.seekCursor(Whence.RELATIVE, 1);
                    }
                    return true;
                }
                case 263 -> {
                    if (Screen.hasControlDown()) {
                        StringView word = this.getPreviousWord();
                        this.seekCursor(Whence.ABSOLUTE, word.beginIndex);
                    } else {
                        this.seekCursor(Whence.RELATIVE, -1);
                    }
                    return true;
                }
                case 264 -> {
                    if (!Screen.hasControlDown()) {
                        this.seekCursorLine(1);
                    }
                    return true;
                }
                case 265 -> {
                    if (!Screen.hasControlDown()) {
                        this.seekCursorLine(-1);
                    }
                    return true;
                }
                case 266 -> {
                    this.seekCursor(Whence.ABSOLUTE, 0);
                    return true;
                }
                case 267 -> {
                    this.seekCursor(Whence.END, 0);
                    return true;
                }
                case 268 -> {
                    if (Screen.hasControlDown()) {
                        this.seekCursor(Whence.ABSOLUTE, 0);
                    } else {
                        this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
                    }
                    return true;
                }
                case 269 -> {
                    if (Screen.hasControlDown()) {
                        this.seekCursor(Whence.END, 0);
                    } else {
                        this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
                    }
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }
    }

    public Iterable<StringView> iterateLines() {
        return this.displayLines;
    }

    public boolean hasSelection() {
        return this.selectCursor != this.cursor;
    }

    private String getSelectedText() {
        StringView selected = this.getSelected();
        return this.value.substring(selected.beginIndex, selected.endIndex);
    }

    private StringView getCursorLineView() {
        return this.getCursorLineView(0);
    }

    private StringView getCursorLineView(int offset) {
        if (this.displayLines.isEmpty()) {
            return StringView.EMPTY;
        }
        int line = this.getLineAtCursor();
        if (line < 0) {
            throw new IllegalStateException("Cursor is not within text (cursor = " + this.cursor + ", length = " + this.value.length() + ")");
        }
        return this.displayLines.get(Mth.clamp(line + offset, 0, this.displayLines.size() - 1));
    }

    private StringView getPreviousWord() {
        if (this.value.isEmpty()) {
            return StringView.EMPTY;
        }
        int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);
        while (i > 0 && Character.isWhitespace(this.value.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && !Character.isWhitespace(this.value.charAt(i - 1))) {
            i--;
        }
        return new StringView(i, this.getWordEndPosition(i));
    }

    private StringView getNextWord() {
        if (this.value.isEmpty()) {
            return StringView.EMPTY;
        }
        int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);
        while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
            i++;
        }
        while (i < this.value.length() && Character.isWhitespace(this.value.charAt(i))) {
            i++;
        }
        return new StringView(i, this.getWordEndPosition(i));
    }

    private int getWordEndPosition(int cursor) {
        int i = cursor;
        while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
            i++;
        }
        return i;
    }

    private int findColorTokenStartBeforeCursor() {
        if (this.cursor < 2 || this.cursor > this.value.length()) return -1;
        int start = this.cursor - 2;
        return isColorTokenAt(start) && this.cursor == start + 2 ? start : -1;
    }

    private int findColorTokenEndAtCursor() {
        if (this.cursor < 0 || this.cursor + 2 > this.value.length()) return -1;
        return isColorTokenAt(this.cursor) ? this.cursor + 2 : -1;
    }

    private boolean isColorTokenAt(int index) {
        if (index < 0 || index + 1 >= this.value.length()) return false;
        if (this.value.charAt(index) != '/') return false;
        char code = Character.toLowerCase(this.value.charAt(index + 1));
        return code == 'w' || code == 'r' || code == 'g' || code == 'b'
                || code == 'y' || code == 'o' || code == 'a' || code == 'p' || code == 'x'
                || code == 'l' || code == 'i' || code == 'e';
    }

    private void onValueChange() {
        this.reflowDisplayLines();
        this.valueListener.accept(this.value);
        this.cursorListener.run();
    }

    private void pushUndoState() {
        if (this.restoringHistory) return;
        HistoryState current = snapshot();
        if (!this.undoHistory.isEmpty() && this.undoHistory.peekLast().sameAs(current)) return;
        this.undoHistory.addLast(current);
        while (this.undoHistory.size() > 100) {
            this.undoHistory.removeFirst();
        }
        this.redoHistory.clear();
    }

    private void restoreFromHistory(Deque<HistoryState> source, Deque<HistoryState> target) {
        if (source.isEmpty()) return;
        HistoryState current = snapshot();
        target.addLast(current);
        HistoryState next = source.removeLast();
        this.restoringHistory = true;
        this.value = next.value;
        this.cursor = Mth.clamp(next.cursor, 0, this.value.length());
        this.selectCursor = Mth.clamp(next.selectCursor, 0, this.value.length());
        this.restoringHistory = false;
        this.onValueChange();
    }

    private HistoryState snapshot() {
        return new HistoryState(this.value, this.cursor, this.selectCursor);
    }

    private void reflowDisplayLines() {
        this.displayLines.clear();
        if (this.value.isEmpty()) {
            this.displayLines.add(StringView.EMPTY);
        } else {
            int safeWidth = Math.max(1, this.width);
            this.font.getSplitter().splitLines(
                    this.value,
                    safeWidth,
                    Style.EMPTY,
                    false,
                    (style, begin, end) -> this.displayLines.add(new StringView(begin, end))
            );
            if (this.value.charAt(this.value.length() - 1) == '\n') {
                this.displayLines.add(new StringView(this.value.length(), this.value.length()));
            }
            if (this.displayLines.isEmpty()) {
                this.displayLines.add(new StringView(0, this.value.length()));
            }
        }
    }

    private String truncateFullText(String fullText) {
        return this.hasCharacterLimit() ? StringUtil.truncateStringIfNecessary(fullText, this.characterLimit, false) : fullText;
    }

    private String truncateInsertionText(String text) {
        if (this.hasCharacterLimit()) {
            int allowed = this.characterLimit - this.value.length();
            return StringUtil.truncateStringIfNecessary(text, allowed, false);
        }
        return text;
    }

    static record StringView(int beginIndex, int endIndex) {
        static final StringView EMPTY = new StringView(0, 0);
    }

    private static record HistoryState(String value, int cursor, int selectCursor) {
        boolean sameAs(HistoryState other) {
            return other != null
                    && Objects.equals(this.value, other.value)
                    && this.cursor == other.cursor
                    && this.selectCursor == other.selectCursor;
        }
    }
}
