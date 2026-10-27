package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class QuestStickerTextRenderer {
    private QuestStickerTextRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font, String value, int x, int y) {
        walk(graphics, font, value, x, y, true);
    }

    public static int width(Font font, String value) {
        return walk(null, font, value, 0, 0, false).width;
    }

    public static int height(Font font, String value) {
        return walk(null, font, value, 0, 0, false).height;
    }

    private static Metrics walk(GuiGraphics graphics, Font font, String raw, int startX, int startY, boolean draw) {
        String text = raw == null ? "" : raw;
        FormatState state = new FormatState();
        int x = startX;
        int y = startY;
        int maxWidth = 0;
        int lines = 1;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '\n') {
                maxWidth = Math.max(maxWidth, x - startX);
                x = startX;
                y += font.lineHeight;
                lines++;
                continue;
            }
            int emojiEnd = QuestEditorEmojis.tokenEnd(text, index);
            if (emojiEnd >= 0) {
                if (draw) {
                    String emoji = text.substring(index + 1, emojiEnd);
                    int size = QuestEditorEmojis.glyphSize();
                    graphics.blit(QuestEditorEmojis.texture(emoji), x, y, 0, 0, size, size, 16, 16);
                }
                x += QuestEditorEmojis.glyphSize();
                index = emojiEnd;
                continue;
            }
            if (index + 1 < text.length() && current == '/' && state.applyCustom(text.charAt(index + 1))) {
                index++;
                continue;
            }
            if (index + 1 < text.length() && (current == '§' || current == '&')
                    && state.applyVanilla(text.charAt(index + 1))) {
                index++;
                continue;
            }
            Component glyph = Component.literal(String.valueOf(current)).withStyle(state.style());
            if (draw) graphics.drawString(font, glyph, x, y, state.color, true);
            x += font.width(glyph);
        }
        maxWidth = Math.max(maxWidth, x - startX);
        return new Metrics(maxWidth, Math.max(font.lineHeight, lines * font.lineHeight));
    }

    private static final class FormatState {
        private int color = 0xFFFFFF;
        private boolean bold;
        private boolean italic;
        private boolean obfuscated;
        private boolean underlined;
        private boolean strikethrough;

        private Style style() {
            return Style.EMPTY.withColor(color).withBold(bold).withItalic(italic).withObfuscated(obfuscated)
                    .withUnderlined(underlined).withStrikethrough(strikethrough);
        }

        private boolean applyCustom(char raw) {
            char code = Character.toLowerCase(raw);
            Integer nextColor = customColor(code);
            if (nextColor != null) color = nextColor;
            else if (code == 'l') bold = !bold;
            else if (code == 'i') italic = !italic;
            else if (code == 'e') obfuscated = !obfuscated;
            else if (code == 'h') color = 0xFFFF55;
            else if (code == 'x') reset();
            else return false;
            return true;
        }

        private boolean applyVanilla(char raw) {
            char code = Character.toLowerCase(raw);
            Integer nextColor = vanillaColor(code);
            if (nextColor != null) {
                resetStyles();
                color = nextColor;
            } else if (code == 'k') obfuscated = true;
            else if (code == 'l') bold = true;
            else if (code == 'm') strikethrough = true;
            else if (code == 'n') underlined = true;
            else if (code == 'o') italic = true;
            else if (code == 'r') reset();
            else return false;
            return true;
        }

        private void reset() {
            color = 0xFFFFFF;
            resetStyles();
        }

        private void resetStyles() {
            bold = false;
            italic = false;
            obfuscated = false;
            underlined = false;
            strikethrough = false;
        }
    }

    private static Integer customColor(char code) {
        return switch (code) {
            case 'w' -> 0x55FFFF;
            case 'r' -> 0xFF5555;
            case 'g' -> 0x55FF55;
            case 'b' -> 0x5555FF;
            case 'y' -> 0xFFFF55;
            case 'o' -> 0xFFAA00;
            case 'a' -> 0xAAAAAA;
            case 'p' -> 0xAA55FF;
            default -> null;
        };
    }

    private static Integer vanillaColor(char code) {
        return switch (code) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> null;
        };
    }

    private record Metrics(int width, int height) {
    }
}
