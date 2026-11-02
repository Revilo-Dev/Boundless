package net.revilodev.boundless.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

public final class QuestTextFormatting {
    private QuestTextFormatting() {
    }

    public static Component component(String raw, int defaultColor) {
        String text = raw == null ? "" : raw;
        MutableComponent result = Component.empty();
        StringBuilder segment = new StringBuilder();
        int color = defaultColor & 0xFFFFFF;
        boolean bold = false;
        boolean italic = false;
        boolean encrypted = false;
        boolean highlight = false;

        for (int index = 0; index < text.length(); index++) {
            if (!startsWithToken(text, index)) {
                segment.append(text.charAt(index));
                continue;
            }
            append(result, segment, color, bold, italic, encrypted, highlight);
            char code = Character.toLowerCase(text.charAt(index + 1));
            if (code == 'l') bold = !bold;
            else if (code == 'i') italic = !italic;
            else if (code == 'e') encrypted = !encrypted;
            else if (code == 'h') highlight = !highlight;
            else if (code == 'x') {
                color = defaultColor & 0xFFFFFF;
                bold = false;
                italic = false;
                encrypted = false;
                highlight = false;
            } else {
                color = color(code, defaultColor);
            }
            index++;
        }
        append(result, segment, color, bold, italic, encrypted, highlight);
        return result;
    }

    public static String strip(String raw) {
        String text = raw == null ? "" : raw;
        StringBuilder result = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            if (startsWithToken(text, index)) {
                index++;
                continue;
            }
            if (text.charAt(index) == '\u00a7' && index + 1 < text.length()) {
                index++;
                continue;
            }
            result.append(text.charAt(index));
        }
        return result.toString();
    }

    public static Component componentByWidth(String raw, int defaultColor, Font font, int maxWidth) {
        Component full = component(raw, defaultColor);
        if (font == null || font.width(full) <= maxWidth) return full;
        if (maxWidth <= 0) return Component.empty();
        String text = raw == null ? "" : raw;
        int lastEnd = 0;
        for (int index = 0; index < text.length(); index++) {
            if (startsWithToken(text, index)) {
                index++;
                continue;
            }
            int end = index + 1;
            if (font.width(component(text.substring(0, end) + "...", defaultColor)) > maxWidth) break;
            lastEnd = end;
        }
        return component(text.substring(0, lastEnd) + "...", defaultColor);
    }

    private static void append(MutableComponent result, StringBuilder segment, int color,
                               boolean bold, boolean italic, boolean encrypted, boolean highlight) {
        if (segment.isEmpty()) return;
        int displayColor = highlight ? highlight(color) : color;
        result.append(Component.literal(segment.toString()).withStyle(Style.EMPTY
                .withColor(displayColor)
                .withBold(bold)
                .withItalic(italic)
                .withObfuscated(encrypted)));
        segment.setLength(0);
    }

    private static boolean startsWithToken(String text, int index) {
        return index + 1 < text.length()
                && text.charAt(index) == '/'
                && switch (Character.toLowerCase(text.charAt(index + 1))) {
                    case 'w', 'r', 'g', 'b', 'y', 'o', 'a', 'p', 'x', 'l', 'i', 'e', 'h' -> true;
                    default -> false;
                };
    }

    private static int color(char code, int fallback) {
        return switch (Character.toLowerCase(code)) {
            case 'w' -> 0x55FFFF;
            case 'r' -> 0xFF5555;
            case 'g' -> 0x55FF55;
            case 'b' -> 0x5555FF;
            case 'y' -> 0xFFFF55;
            case 'o' -> 0xFFAA00;
            case 'a' -> 0xAAAAAA;
            case 'p' -> 0xAA55FF;
            default -> fallback & 0xFFFFFF;
        };
    }

    private static int highlight(int color) {
        float pulse = (float) ((Math.sin(Util.getMillis() / 220.0) + 1.0) * 0.5);
        int amount = 38 + Math.round(72.0f * pulse);
        int red = Mth.clamp(((color >>> 16) & 0xFF) + amount, 0, 255);
        int green = Mth.clamp(((color >>> 8) & 0xFF) + amount, 0, 255);
        int blue = Mth.clamp((color & 0xFF) + amount, 0, 255);
        return red << 16 | green << 8 | blue;
    }
}
