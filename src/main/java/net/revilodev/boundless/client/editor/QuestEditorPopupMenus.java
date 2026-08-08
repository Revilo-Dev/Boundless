package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import net.revilodev.boundless.client.editor.QuestEditorModels.DropdownMenuTarget;
import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

// editor popup menus
public final class QuestEditorPopupMenus {
    private static final int ENTRY_ROW_H = 14;
    private static final int ID_SUGGESTION_BG_COLOR = 0xFF000000;
    private static final int ID_SUGGESTION_TEXT_COLOR = 0xFFFFFF00;
    private static final int ID_SUGGESTION_BORDER_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_TRACK_COLOR = 0xFF2C2C2C;
    private static final int ID_SUGGESTION_SCROLL_THUMB_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_W = 3;

    private EntryRowKind openTypeMenuKind;
    private int openTypeMenuRow = -1;
    private int openTypeMenuX;
    private int openTypeMenuY;
    private int openTypeMenuW;
    private int openTypeMenuH;
    private int openTypeMenuScroll = 0;
    private DropdownMenuTarget openDropdownTarget;
    private int openDropdownX;
    private int openDropdownY;
    private int openDropdownW;
    private int openDropdownH;
    private int openDropdownScroll = 0;
    private final Map<DropdownMenuTarget, Integer> savedDropdownScrolls = new EnumMap<>(DropdownMenuTarget.class);

    public void close() {
        openTypeMenuKind = null;
        openTypeMenuRow = -1;
        openTypeMenuScroll = 0;
        openDropdownTarget = null;
        openDropdownScroll = 0;
    }

    public void openTypeMenu(EntryRowKind kind, int row, int x, int y, int width) {
        openTypeMenuKind = kind;
        openTypeMenuRow = row;
        openTypeMenuX = x;
        openTypeMenuY = y + ENTRY_ROW_H;
        openTypeMenuW = Math.max(56, width + 10);
        openTypeMenuH = Math.min(5, QuestEditorEntryTypes.entryTypeOptions(kind).size()) * ENTRY_ROW_H;
        openTypeMenuScroll = 0;
    }

    public boolean clickTypeMenu(double mouseX, double mouseY, BiConsumer<EntryRowKind, TypeSelection> selectionHandler) {
        if (openTypeMenuKind == null || openTypeMenuRow < 0) return false;
        if (mouseX < openTypeMenuX || mouseX > openTypeMenuX + openTypeMenuW || mouseY < openTypeMenuY || mouseY > openTypeMenuY + openTypeMenuH) {
            openTypeMenuKind = null;
            openTypeMenuRow = -1;
            return false;
        }
        int index = openTypeMenuScroll + (int) ((mouseY - openTypeMenuY) / ENTRY_ROW_H);
        List<String> options = QuestEditorEntryTypes.entryTypeOptions(openTypeMenuKind);
        if (index >= 0 && index < options.size() && selectionHandler != null) {
            selectionHandler.accept(openTypeMenuKind, new TypeSelection(openTypeMenuRow, options.get(index)));
        }
        openTypeMenuKind = null;
        openTypeMenuRow = -1;
        return true;
    }

    public void renderTypeMenu(GuiGraphics gg, Font font, int mouseX, int mouseY) {
        if (openTypeMenuKind == null || openTypeMenuRow < 0) return;
        List<String> options = QuestEditorEntryTypes.entryTypeOptions(openTypeMenuKind);
        renderMenu(gg, font, mouseX, mouseY, openTypeMenuX, openTypeMenuY, openTypeMenuW, openTypeMenuH, openTypeMenuScroll, options);
    }

    public boolean scrollTypeMenu(double mouseX, double mouseY, double delta) {
        if (openTypeMenuKind == null || openTypeMenuRow < 0) return false;
        if (mouseX < openTypeMenuX || mouseX > openTypeMenuX + openTypeMenuW || mouseY < openTypeMenuY || mouseY > openTypeMenuY + openTypeMenuH) return true;
        List<String> options = QuestEditorEntryTypes.entryTypeOptions(openTypeMenuKind);
        int visible = Math.min(5, options.size());
        int maxStart = Math.max(0, options.size() - visible);
        openTypeMenuScroll = Mth.clamp(openTypeMenuScroll - (delta > 0 ? 1 : -1), 0, maxStart);
        return true;
    }

    public void openDropdownMenu(DropdownMenuTarget target, EditBox box, int panelY, int panelHeight, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (target == null || box == null || optionsProvider == null) return;
        List<String> options = optionsProvider.apply(target);
        if (options.isEmpty()) return;
        openDropdownTarget = target;
        openDropdownX = box.getX();
        openDropdownW = Math.max(70, box.getWidth());
        openDropdownH = Math.min(5, options.size()) * ENTRY_ROW_H;
        int preferredBelowY = box.getY() + box.getHeight();
        int preferredAboveY = box.getY() - openDropdownH;
        openDropdownY = preferredBelowY;
        if (preferredBelowY + openDropdownH > panelY + panelHeight && preferredAboveY >= panelY) {
            openDropdownY = preferredAboveY;
        }
        int maxStart = Math.max(0, options.size() - Math.min(5, options.size()));
        openDropdownScroll = Mth.clamp(savedDropdownScroll(target), 0, maxStart);
    }

    public boolean clickDropdownMenu(double mouseX, double mouseY, Function<DropdownMenuTarget, List<String>> optionsProvider, BiConsumer<DropdownMenuTarget, String> selectionHandler) {
        if (openDropdownTarget == null) return false;
        if (mouseX < openDropdownX || mouseX > openDropdownX + openDropdownW || mouseY < openDropdownY || mouseY > openDropdownY + openDropdownH) {
            rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
            openDropdownTarget = null;
            return false;
        }
        List<String> options = optionsProvider.apply(openDropdownTarget);
        int index = openDropdownScroll + (int) ((mouseY - openDropdownY) / ENTRY_ROW_H);
        if (index >= 0 && index < options.size() && selectionHandler != null) {
            selectionHandler.accept(openDropdownTarget, options.get(index));
        }
        rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
        openDropdownTarget = null;
        return true;
    }

    public void renderDropdownMenu(GuiGraphics gg, Font font, int mouseX, int mouseY, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (openDropdownTarget == null || optionsProvider == null) return;
        List<String> options = optionsProvider.apply(openDropdownTarget);
        renderMenu(gg, font, mouseX, mouseY, openDropdownX, openDropdownY, openDropdownW, openDropdownH, openDropdownScroll, options);
    }

    public boolean scrollDropdownMenu(double mouseX, double mouseY, double delta, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (openDropdownTarget == null) return false;
        if (mouseX < openDropdownX || mouseX > openDropdownX + openDropdownW || mouseY < openDropdownY || mouseY > openDropdownY + openDropdownH) return true;
        List<String> options = optionsProvider.apply(openDropdownTarget);
        int visible = Math.min(5, options.size());
        int maxStart = Math.max(0, options.size() - visible);
        openDropdownScroll = Mth.clamp(openDropdownScroll - (delta > 0 ? 1 : -1), 0, maxStart);
        rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
        return true;
    }

    public boolean isOpenDropdownField(EditBox box, EditBox subCategoryBox, EditBox questCategoryBox, EditBox questSubCategoryBox) {
        if (box == null || openDropdownTarget == null) return false;
        return switch (openDropdownTarget) {
            case SUBCATEGORY_PARENT -> box == subCategoryBox;
            case QUEST_CATEGORY -> box == questCategoryBox;
            case QUEST_SUBCATEGORY -> box == questSubCategoryBox;
        };
    }

    private void renderMenu(GuiGraphics gg, Font font, int mouseX, int mouseY, int x, int y, int width, int height, int scroll, List<String> options) {
        gg.pose().pushPose();
        gg.pose().translate(0.0f, 0.0f, 650.0f);
        gg.fill(x, y, x + width, y + height, ID_SUGGESTION_BG_COLOR);
        gg.fill(x, y, x + width, y + 1, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(x, y + height - 1, x + width, y + height, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(x, y, x + 1, y + height, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(x + width - 1, y, x + width, y + height, ID_SUGGESTION_BORDER_COLOR);
        int visible = Math.min(5, options.size());
        int start = Mth.clamp(scroll, 0, Math.max(0, options.size() - visible));
        int end = Math.min(options.size(), start + visible);
        for (int i = start; i < end; i++) {
            int row = i - start;
            int rowY = y + row * ENTRY_ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY < rowY + ENTRY_ROW_H;
            if (hovered) gg.fill(x + 1, rowY, x + width - 1, rowY + ENTRY_ROW_H, 0xFF2D2D2D);
            gg.drawString(font, options.get(i), x + 3, rowY + 2, ID_SUGGESTION_TEXT_COLOR, false);
        }
        if (options.size() > visible) renderScrollThumb(gg, x, y, width, height, scroll, visible, options.size());
        gg.pose().popPose();
    }

    private void renderScrollThumb(GuiGraphics gg, int x, int y, int width, int height, int scroll, int visible, int total) {
        int trackX0 = x + width - ID_SUGGESTION_SCROLL_W - 1;
        int trackY0 = y + 1;
        int trackH = height - 2;
        gg.fill(trackX0, trackY0, trackX0 + ID_SUGGESTION_SCROLL_W, trackY0 + trackH, ID_SUGGESTION_SCROLL_TRACK_COLOR);
        int maxStart = total - visible;
        int thumbH = Math.max(6, (trackH * visible) / total);
        int thumbTravel = Math.max(0, trackH - thumbH);
        int thumbY = trackY0 + (maxStart <= 0 ? 0 : (scroll * thumbTravel) / maxStart);
        gg.fill(trackX0, thumbY, trackX0 + ID_SUGGESTION_SCROLL_W, thumbY + thumbH, ID_SUGGESTION_SCROLL_THUMB_COLOR);
    }

    private int savedDropdownScroll(DropdownMenuTarget target) {
        return savedDropdownScrolls.getOrDefault(target, 0);
    }

    private void rememberDropdownScroll(DropdownMenuTarget target, int scroll) {
        if (target != null) savedDropdownScrolls.put(target, Math.max(0, scroll));
    }

    // type selection
    public record TypeSelection(int row, String type) {}
}
