package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.revilodev.boundless.client.editor.QuestEditorModels.SuggestionBounds;

import java.util.List;

// id suggestion popup rendering
public final class QuestEditorSuggestionRenderer {
    private static final int ID_SUGGESTION_ROW_H = 10;
    private static final int ID_SUGGESTION_VISIBLE_ROWS = 6;
    private static final int ID_SUGGESTION_TEXT_TOP_PADDING = 2;
    private static final float ID_SUGGESTION_TEXT_SCALE = 0.5f;
    private static final int ID_SUGGESTION_BG_COLOR = 0xFF000000;
    private static final int ID_SUGGESTION_TEXT_COLOR = 0xFFFFFF00;
    private static final int ID_SUGGESTION_BORDER_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_TRACK_COLOR = 0xFF2C2C2C;
    private static final int ID_SUGGESTION_SCROLL_THUMB_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_W = 3;

    private QuestEditorSuggestionRenderer() {
    }

    // render
    public static void render(RenderRequest request) {
        if (request == null || request.suggestions == null || request.suggestions.isEmpty()) return;
        SuggestionBounds bounds = request.bounds;
        if (bounds == null || bounds.w <= 0 || bounds.h <= 0) return;
        GuiGraphics gg = request.gg;
        Font font = request.font;
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 500);
        gg.enableScissor(request.scissorLeft, request.scissorTop, request.scissorRight, request.scissorBottom);
        int scrollArea = request.suggestions.size() > ID_SUGGESTION_VISIBLE_ROWS ? (ID_SUGGESTION_SCROLL_W + 3) : 0;
        int textRightPadding = 6 + scrollArea;
        int end = Math.min(request.suggestions.size(), request.scroll + ID_SUGGESTION_VISIBLE_ROWS);
        for (int i = request.scroll; i < end; i++) {
            int row = i - request.scroll;
            int top = bounds.y + (row * ID_SUGGESTION_ROW_H);
            int bottom = top + ID_SUGGESTION_ROW_H;
            gg.fill(bounds.x, top, bounds.x + bounds.w, bottom, ID_SUGGESTION_BG_COLOR);
            float inv = 1f / ID_SUGGESTION_TEXT_SCALE;
            int maxWidth = Math.max(4, (int) ((bounds.w - textRightPadding) * inv));
            String text = font.plainSubstrByWidth(request.suggestions.get(i), maxWidth);
            gg.pose().pushPose();
            gg.pose().scale(ID_SUGGESTION_TEXT_SCALE, ID_SUGGESTION_TEXT_SCALE, 1f);
            gg.drawString(font, text, (int) ((bounds.x + 4) * inv), (int) ((top + ID_SUGGESTION_TEXT_TOP_PADDING) * inv), ID_SUGGESTION_TEXT_COLOR, false);
            gg.pose().popPose();
        }
        if (request.suggestions.size() > ID_SUGGESTION_VISIBLE_ROWS) {
            renderScrollThumb(gg, bounds, request.suggestions.size(), request.scroll);
        }
        drawBorder(gg, bounds);
        gg.disableScissor();
        gg.pose().popPose();
    }

    private static void renderScrollThumb(GuiGraphics gg, SuggestionBounds bounds, int total, int scroll) {
        int trackX0 = bounds.x + bounds.w - ID_SUGGESTION_SCROLL_W - 2;
        int trackX1 = bounds.x + bounds.w - 2;
        int trackY0 = bounds.y + 1;
        int trackY1 = bounds.y + bounds.h - 1;
        gg.fill(trackX0, trackY0, trackX1, trackY1, ID_SUGGESTION_SCROLL_TRACK_COLOR);
        int max = Math.max(1, total - ID_SUGGESTION_VISIBLE_ROWS);
        float ratio = (float) ID_SUGGESTION_VISIBLE_ROWS / (float) total;
        int thumbH = Math.max(8, Math.round((trackY1 - trackY0) * ratio));
        float scrollRatio = (float) scroll / (float) max;
        int thumbTop = trackY0 + Math.round((trackY1 - trackY0 - thumbH) * scrollRatio);
        gg.fill(trackX0, thumbTop, trackX1, thumbTop + thumbH, ID_SUGGESTION_SCROLL_THUMB_COLOR);
    }

    private static void drawBorder(GuiGraphics gg, SuggestionBounds bounds) {
        if (bounds == null || bounds.w <= 1 || bounds.h <= 1) return;
        int left = bounds.x;
        int top = bounds.y;
        int right = bounds.x + bounds.w;
        int bottom = bounds.y + bounds.h;
        gg.fill(left, top, right, top + 1, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(left, bottom - 1, right, bottom, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(left, top, left + 1, bottom, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(right - 1, top, right, bottom, ID_SUGGESTION_BORDER_COLOR);
    }

    // render request
    public static final class RenderRequest {
        public final GuiGraphics gg;
        public final Font font;
        public final List<String> suggestions;
        public final int scroll;
        public final SuggestionBounds bounds;
        public final int scissorLeft;
        public final int scissorTop;
        public final int scissorRight;
        public final int scissorBottom;

        public RenderRequest(GuiGraphics gg, Font font, List<String> suggestions, int scroll, SuggestionBounds bounds,
                             int scissorLeft, int scissorTop, int scissorRight, int scissorBottom) {
            this.gg = gg;
            this.font = font;
            this.suggestions = suggestions;
            this.scroll = scroll;
            this.bounds = bounds;
            this.scissorLeft = scissorLeft;
            this.scissorTop = scissorTop;
            this.scissorRight = scissorRight;
            this.scissorBottom = scissorBottom;
        }
    }
}
