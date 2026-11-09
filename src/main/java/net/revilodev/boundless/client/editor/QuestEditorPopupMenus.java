package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import net.revilodev.boundless.client.editor.QuestEditorModels.DropdownMenuTarget;
import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

// editor popup menus
// shared dropdown and row type menus for the quest editor
public final class QuestEditorPopupMenus {
    private static final int ENTRY_ROW_H = 14;
    private static final int ID_SUGGESTION_BG_COLOR = 0xFF000000;
    private static final int ID_SUGGESTION_TEXT_COLOR = 0xFFFFFF00;
    private static final int ID_SUGGESTION_BORDER_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_TRACK_COLOR = 0xFF2C2C2C;
    private static final int ID_SUGGESTION_SCROLL_THUMB_COLOR = 0xFFFFFFFF;
    private static final int ID_SUGGESTION_SCROLL_W = 1;

    private EntryRowKind openTypeMenuKind;
    private int openTypeMenuRow = -1;
    private int openTypeMenuX;
    private int openTypeMenuY;
    private int openTypeMenuW;
    private int openTypeMenuH;
    private int openTypeMenuScroll = 0;
    private EntryRowKind openValueMenuKind;
    private int openValueMenuRow = -1;
    private int openValueMenuX;
    private int openValueMenuY;
    private int openValueMenuW;
    private int openValueMenuH;
    private List<String> openValueMenuOptions = List.of();
    private DropdownMenuTarget openDropdownTarget;
    private int openDropdownX;
    private int openDropdownY;
    private int openDropdownW;
    private int openDropdownH;
    private int openDropdownScroll = 0;
    private String openDropdownQuery = "";
    private final Map<DropdownMenuTarget, Integer> savedDropdownScrolls = new EnumMap<>(DropdownMenuTarget.class);

    public void close() {
        openTypeMenuKind = null;
        openTypeMenuRow = -1;
        openTypeMenuScroll = 0;
        openValueMenuKind = null;
        openValueMenuRow = -1;
        openValueMenuOptions = List.of();
        openDropdownTarget = null;
        openDropdownScroll = 0;
        openDropdownQuery = "";
    }

    public boolean hasOpenMenu() {
        return (openTypeMenuKind != null && openTypeMenuRow >= 0)
                || (openValueMenuKind != null && openValueMenuRow >= 0)
                || openDropdownTarget != null;
    }

    public void openTypeMenu(EntryRowKind kind, int row, int x, int y, int width) {
        openValueMenuKind = null;
        openValueMenuRow = -1;
        openValueMenuOptions = List.of();
        openTypeMenuKind = kind;
        openTypeMenuRow = row;
        openTypeMenuX = x;
        openTypeMenuY = y + ENTRY_ROW_H;
        openTypeMenuW = Math.max(56, width + 10);
        openTypeMenuH = menuHeight(QuestEditorEntryTypes.entryTypeOptions(kind).size());
        openTypeMenuScroll = 0;
    }

    public boolean clickTypeMenu(double mouseX, double mouseY, BiConsumer<EntryRowKind, TypeSelection> selectionHandler) {
        if (openTypeMenuKind == null || openTypeMenuRow < 0) return false;
        if (mouseX < openTypeMenuX + 1 || mouseX >= openTypeMenuX + openTypeMenuW - 1
                || mouseY < openTypeMenuY + 1 || mouseY >= openTypeMenuY + openTypeMenuH - 1) {
            openTypeMenuKind = null;
            openTypeMenuRow = -1;
            return false;
        }
        int index = openTypeMenuScroll + rowAt(mouseY, openTypeMenuY);
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

    public void openValueMenu(EntryRowKind kind, int row, int x, int y, int width, List<String> options) {
        if (kind == null || row < 0 || options == null || options.isEmpty()) return;
        openTypeMenuKind = null;
        openTypeMenuRow = -1;
        openValueMenuKind = kind;
        openValueMenuRow = row;
        openValueMenuX = x;
        openValueMenuY = y + ENTRY_ROW_H;
        openValueMenuW = Math.max(56, width + 10);
        openValueMenuOptions = List.copyOf(options);
        openValueMenuH = menuHeight(openValueMenuOptions.size());
    }

    public boolean clickValueMenu(double mouseX, double mouseY,
                                  BiConsumer<EntryRowKind, TypeSelection> selectionHandler) {
        if (openValueMenuKind == null || openValueMenuRow < 0) return false;
        if (mouseX < openValueMenuX + 1 || mouseX >= openValueMenuX + openValueMenuW - 1
                || mouseY < openValueMenuY + 1 || mouseY >= openValueMenuY + openValueMenuH - 1) {
            openValueMenuKind = null;
            openValueMenuRow = -1;
            openValueMenuOptions = List.of();
            return false;
        }
        int index = rowAt(mouseY, openValueMenuY);
        if (index >= 0 && index < openValueMenuOptions.size() && selectionHandler != null) {
            selectionHandler.accept(openValueMenuKind,
                    new TypeSelection(openValueMenuRow, openValueMenuOptions.get(index)));
        }
        openValueMenuKind = null;
        openValueMenuRow = -1;
        openValueMenuOptions = List.of();
        return true;
    }

    public void renderValueMenu(GuiGraphics gg, Font font, int mouseX, int mouseY,
                                Function<String, String> labelProvider) {
        if (openValueMenuKind == null || openValueMenuRow < 0) return;
        renderMenu(gg, font, mouseX, mouseY, openValueMenuX, openValueMenuY,
                openValueMenuW, openValueMenuH, 0, openValueMenuOptions, labelProvider);
    }

    public boolean scrollValueMenu(double mouseX, double mouseY) {
        if (openValueMenuKind == null || openValueMenuRow < 0) return false;
        return true;
    }

    public void openDropdownMenu(DropdownMenuTarget target, AbstractWidget box, int panelY, int panelHeight, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (target == null || box == null || optionsProvider == null) return;
        List<String> options = optionsProvider.apply(target);
        if (options.isEmpty()) return;
        openTypeMenuKind = null;
        openTypeMenuRow = -1;
        openValueMenuKind = null;
        openValueMenuRow = -1;
        openValueMenuOptions = List.of();
        openDropdownTarget = target;
        openDropdownQuery = "";
        openDropdownX = box.getX();
        openDropdownW = Math.max(70, box.getWidth());
        openDropdownH = menuHeight(options.size());
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
        if (mouseX < openDropdownX + 1 || mouseX >= openDropdownX + openDropdownW - 1
                || mouseY < openDropdownY + 1 || mouseY >= openDropdownY + openDropdownH - 1) {
            rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
            openDropdownTarget = null;
            openDropdownQuery = "";
            return false;
        }
        List<String> options = filteredDropdownOptions(optionsProvider);
        int index = openDropdownScroll + rowAt(mouseY, openDropdownY);
        if (index >= 0 && index < options.size() && selectionHandler != null) {
            selectionHandler.accept(openDropdownTarget, options.get(index));
        }
        rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
        openDropdownTarget = null;
        openDropdownQuery = "";
        return true;
    }

    public void renderDropdownMenu(GuiGraphics gg, Font font, int mouseX, int mouseY, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (openDropdownTarget == null || optionsProvider == null) return;
        List<String> options = filteredDropdownOptions(optionsProvider);
        openDropdownH = menuHeight(options.size());
        openDropdownScroll = Mth.clamp(openDropdownScroll, 0,
                Math.max(0, options.size() - visibleRows(options.size())));
        renderMenu(gg, font, mouseX, mouseY, openDropdownX, openDropdownY, openDropdownW, openDropdownH, openDropdownScroll, options);
    }

    public boolean scrollDropdownMenu(double mouseX, double mouseY, double delta, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (openDropdownTarget == null) return false;
        if (mouseX < openDropdownX || mouseX > openDropdownX + openDropdownW || mouseY < openDropdownY || mouseY > openDropdownY + openDropdownH) return true;
        List<String> options = filteredDropdownOptions(optionsProvider);
        int visible = Math.min(5, options.size());
        int maxStart = Math.max(0, options.size() - visible);
        openDropdownScroll = Mth.clamp(openDropdownScroll - (delta > 0 ? 1 : -1), 0, maxStart);
        rememberDropdownScroll(openDropdownTarget, openDropdownScroll);
        return true;
    }

    public boolean charTyped(char codePoint, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (!isLanguageDropdown() || !Character.isLetterOrDigit(codePoint)) return false;
        openDropdownQuery += Character.toLowerCase(codePoint);
        refreshFilteredDropdown(optionsProvider);
        return true;
    }

    public boolean keyPressed(int keyCode, Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (!isLanguageDropdown()) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !openDropdownQuery.isEmpty()) {
            openDropdownQuery = openDropdownQuery.substring(0, openDropdownQuery.length() - 1);
            refreshFilteredDropdown(optionsProvider);
            return true;
        }
        return false;
    }

    public boolean isOpenDropdownField(EditBox box, EditBox subCategoryBox, EditBox questCategoryBox, EditBox questSubCategoryBox) {
        if (box == null || openDropdownTarget == null) return false;
        return switch (openDropdownTarget) {
            case SUBCATEGORY_PARENT -> box == subCategoryBox;
            case QUEST_CATEGORY -> box == questCategoryBox;
            case QUEST_SUBCATEGORY -> box == questSubCategoryBox;
            case QUEST_DEPENDENCY, QUEST_NAME_LANGUAGE, QUEST_DESCRIPTION_LANGUAGE, PREFERRED_LANGUAGE -> false;
        };
    }

    private void renderMenu(GuiGraphics gg, Font font, int mouseX, int mouseY, int x, int y, int width, int height, int scroll, List<String> options) {
        renderMenu(gg, font, mouseX, mouseY, x, y, width, height, scroll, options, Function.identity());
    }

    private void renderMenu(GuiGraphics gg, Font font, int mouseX, int mouseY, int x, int y,
                            int width, int height, int scroll, List<String> options,
                            Function<String, String> labelProvider) {
        gg.pose().pushPose();
        gg.pose().translate(0.0f, 0.0f, 650.0f);
        gg.fill(x, y, x + width, y + height, ID_SUGGESTION_BG_COLOR);
        gg.fill(x, y, x + width, y + 1, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(x, y + height - 1, x + width, y + height, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(x, y, x + 1, y + height, ID_SUGGESTION_BORDER_COLOR);
        gg.fill(x + width - 1, y, x + width, y + height, ID_SUGGESTION_BORDER_COLOR);
        int visible = visibleRows(options.size());
        int start = Mth.clamp(scroll, 0, Math.max(0, options.size() - visible));
        int end = Math.min(options.size(), start + visible);
        for (int i = start; i < end; i++) {
            int row = i - start;
            int rowY = y + 1 + row * ENTRY_ROW_H;
            boolean hovered = mouseX >= x + 1 && mouseX < x + width - 1
                    && mouseY >= rowY && mouseY < rowY + ENTRY_ROW_H;
            if (hovered) gg.fill(x + 1, rowY, x + width - 1, rowY + ENTRY_ROW_H, 0xFF2D2D2D);
            String label = labelProvider == null ? options.get(i) : labelProvider.apply(options.get(i));
            gg.drawString(font, label, x + 3, rowY + 2, ID_SUGGESTION_TEXT_COLOR, false);
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

    private boolean isLanguageDropdown() {
        return openDropdownTarget == DropdownMenuTarget.QUEST_NAME_LANGUAGE
                || openDropdownTarget == DropdownMenuTarget.QUEST_DESCRIPTION_LANGUAGE
                || openDropdownTarget == DropdownMenuTarget.PREFERRED_LANGUAGE;
    }

    private List<String> filteredDropdownOptions(Function<DropdownMenuTarget, List<String>> optionsProvider) {
        if (openDropdownTarget == null || optionsProvider == null) return List.of();
        List<String> options = optionsProvider.apply(openDropdownTarget);
        if (!isLanguageDropdown() || openDropdownQuery.isBlank()) return options;
        String query = openDropdownQuery.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option != null && option.toLowerCase(Locale.ROOT).contains(query)) filtered.add(option);
        }
        return filtered;
    }

    private void refreshFilteredDropdown(Function<DropdownMenuTarget, List<String>> optionsProvider) {
        List<String> options = filteredDropdownOptions(optionsProvider);
        openDropdownScroll = 0;
        openDropdownH = menuHeight(options.size());
    }

    private static int visibleRows(int size) {
        return Math.min(5, Math.max(0, size));
    }

    private static int menuHeight(int size) {
        return visibleRows(size) * ENTRY_ROW_H + 2;
    }

    private static int rowAt(double mouseY, int menuY) {
        return (int) ((mouseY - menuY - 1) / ENTRY_ROW_H);
    }

    // type selection
    public record TypeSelection(int row, String type) {}
}
