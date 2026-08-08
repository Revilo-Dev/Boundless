package net.revilodev.boundless.client.editor;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.Mode;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

// editor row controls
public final class QuestEditorRowControls {
    private static final ResourceLocation TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int ENTRY_ROW_H = 14;
    private static final int ENTRY_REMOVE_BTN_W = 10;
    private static final int ENTRY_TYPE_BTN_W = 21;
    private static final int ENTRY_ITEM_PICK_BTN_W = 12;
    private static final int ENTRY_COUNT_BTN_W = 12;
    private static final float ENTRY_COUNT_TEXT_SCALE = 0.55f;
    private static final float ENTRY_TYPE_TEXT_SCALE = 0.45f;

    private QuestEditorRowControls() {
    }

    @FunctionalInterface
    public interface RemoveEntryHandler {
        void remove(EntryRowKind kind, EntryRemoveButton button);
    }

    @FunctionalInterface
    public interface OpenTypeMenuHandler {
        void open(EntryRowKind kind, int row, int x, int y, int width);
    }

    @FunctionalInterface
    public interface EntryCountHandler {
        void update(EntryRowKind kind, int row, int count);
    }

    // editor tab button
    public static final class EditorTabButton extends AbstractButton {
        private final String tooltip;
        private final String iconId;
        private final ResourceLocation iconTexture;
        private final Mode targetMode;
        private final BooleanSupplier canSelect;
        private final Supplier<Mode> currentMode;
        private final Consumer<Mode> onSelect;

        public EditorTabButton(String tooltip, String iconId, Mode targetMode, BooleanSupplier canSelect, Supplier<Mode> currentMode, Consumer<Mode> onSelect) {
            this(tooltip, iconId, null, targetMode, canSelect, currentMode, onSelect);
        }

        public EditorTabButton(String tooltip, ResourceLocation iconTexture, Mode targetMode, BooleanSupplier canSelect, Supplier<Mode> currentMode, Consumer<Mode> onSelect) {
            this(tooltip, "", iconTexture, targetMode, canSelect, currentMode, onSelect);
        }

        private EditorTabButton(String tooltip, String iconId, ResourceLocation iconTexture, Mode targetMode, BooleanSupplier canSelect, Supplier<Mode> currentMode, Consumer<Mode> onSelect) {
            super(0, 0, TAB_W, TAB_H, Component.empty());
            this.tooltip = tooltip == null ? "" : tooltip;
            this.iconId = iconId == null ? "" : iconId;
            this.iconTexture = iconTexture;
            this.targetMode = targetMode;
            this.canSelect = canSelect;
            this.currentMode = currentMode;
            this.onSelect = onSelect;
        }

        public String tooltip() {
            return tooltip;
        }

        @Override
        public void onPress() {
            if ((canSelect != null && !canSelect.getAsBoolean()) || currentMode != null && currentMode.get() == targetMode) return;
            if (onSelect != null) onSelect.accept(targetMode);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean selected = currentMode != null && currentMode.get() == targetMode;
            int renderX = getX() + 1 - (selected ? 1 : 0);
            gg.blit(selected ? TAB_SELECTED_TEX : TAB_TEX, renderX, getY(), 0, 0, TAB_W, TAB_H, TAB_W, TAB_H);
            int iconX = renderX + (TAB_W - 16) / 2;
            int iconY = getY() + 5;
            if (iconTexture != null) {
                gg.blit(iconTexture, iconX, iconY, 0, 0, 16, 16, 16, 16);
                return;
            }
            ItemStack icon = QuestEditorItemIcons.iconStackFromId(iconId);
            if (!icon.isEmpty()) {
                gg.renderItem(icon, iconX, iconY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // entry remove button
    public static final class EntryRemoveButton extends AbstractButton {
        private final EntryRowKind kind;
        private final RemoveEntryHandler onRemove;

        public EntryRemoveButton(EntryRowKind kind, RemoveEntryHandler onRemove) {
            super(0, 0, ENTRY_REMOVE_BTN_W, ENTRY_ROW_H, Component.literal("X"));
            this.kind = kind;
            this.onRemove = onRemove;
        }

        @Override
        public void onPress() {
            if (onRemove != null) onRemove.remove(kind, this);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int bg = this.active ? (this.isMouseOver(mouseX, mouseY) ? 0xAA5A1A1A : 0xAA3A1212) : 0x553A1212;
            int fg = this.active ? 0xFFFF5555 : 0xFF804040;
            gg.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);
            gg.fill(getX(), getY(), getX() + this.width, getY() + 1, 0xFFC0C0C0);
            gg.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, 0xFFC0C0C0);
            gg.fill(getX(), getY(), getX() + 1, getY() + this.height, 0xFFC0C0C0);
            gg.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, 0xFFC0C0C0);
            gg.drawString(Minecraft.getInstance().font, "X", getX() + 2, getY() + 2, fg, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // entry type button
    public static final class EntryTypeButton extends AbstractButton {
        private final EntryRowKind kind;
        private final OpenTypeMenuHandler openTypeMenu;
        private final BiFunction<EntryRowKind, Integer, String> labelProvider;
        private int row;

        public EntryTypeButton(EntryRowKind kind, int row, OpenTypeMenuHandler openTypeMenu, BiFunction<EntryRowKind, Integer, String> labelProvider) {
            super(0, 0, ENTRY_TYPE_BTN_W, ENTRY_ROW_H, Component.empty());
            this.kind = kind;
            this.row = row;
            this.openTypeMenu = openTypeMenu;
            this.labelProvider = labelProvider;
        }

        public void setRow(int row) {
            this.row = row;
        }

        @Override
        public void onPress() {
            if (openTypeMenu != null) openTypeMenu.open(kind, row, getX(), getY(), getWidth());
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            String text = labelProvider == null ? "" : labelProvider.apply(kind, row);
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            boolean focused = this.isFocused();
            int bg = hovered || focused ? 0xFF101010 : 0xFF000000;
            int border = hovered || focused ? 0xFFFFFFFF : 0xFF8A8A8A;
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
            gg.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
            gg.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
            gg.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
            Font font = Minecraft.getInstance().font;
            int textW = Math.round(font.width(text) * ENTRY_TYPE_TEXT_SCALE);
            int tx = getX() + (getWidth() - textW) / 2;
            gg.pose().pushPose();
            gg.pose().translate(tx, getY() + 3, 0f);
            gg.pose().scale(ENTRY_TYPE_TEXT_SCALE, ENTRY_TYPE_TEXT_SCALE, 1f);
            gg.drawString(font, text, 0, 0, 0xFFFFFF00, false);
            gg.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // entry item picker button
    public static final class EntryItemPickerButton extends AbstractButton {
        private final EntryRowKind kind;
        private final BiFunction<EntryRowKind, Integer, ItemStack> selectedStackProvider;
        private final BiFunction<EntryRowKind, Integer, String> typeProvider;
        private final BiFunction<EntryRowKind, Integer, Boolean> openPicker;
        private int row;

        public EntryItemPickerButton(EntryRowKind kind, int row, BiFunction<EntryRowKind, Integer, Boolean> openPicker, BiFunction<EntryRowKind, Integer, ItemStack> selectedStackProvider, BiFunction<EntryRowKind, Integer, String> typeProvider) {
            super(0, 0, ENTRY_ITEM_PICK_BTN_W, ENTRY_ROW_H, Component.empty());
            this.kind = kind;
            this.row = row;
            this.openPicker = openPicker;
            this.selectedStackProvider = selectedStackProvider;
            this.typeProvider = typeProvider;
        }

        public void setRow(int row) {
            this.row = row;
        }

        @Override
        public void onPress() {
            if (openPicker != null) openPicker.apply(kind, row);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            boolean focused = this.isFocused();
            int bg = hovered || focused ? 0xFF404040 : 0xFF3A3A3A;
            int border = hovered || focused ? 0xFFFFFFFF : 0xFF8A8A8A;
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
            gg.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
            gg.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
            gg.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
            ItemStack stack = selectedStackProvider == null ? ItemStack.EMPTY : selectedStackProvider.apply(kind, row);
            String type = typeProvider == null ? "" : typeProvider.apply(kind, row);
            if (!stack.isEmpty()) {
                gg.pose().pushPose();
                gg.pose().translate(getX() + 2.0f, getY() + 2.0f, 0f);
                gg.pose().scale(0.5f, 0.5f, 1f);
                if ("effect".equals(type)) {
                    QuestEditorItemIcons.renderEffectPickerIcon(gg, stack, 0, 0);
                } else {
                    gg.renderItem(stack, 0, 0);
                }
                gg.pose().popPose();
                return;
            }
            gg.drawString(Minecraft.getInstance().font, "?", getX() + 3, getY() + 2, 0xFFFFFF00, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // entry count box
    public static final class EntryCountBox extends EditBox {
        private final EntryRowKind kind;
        private final EntryCountHandler countHandler;
        private int row;
        private boolean settingValue;

        public EntryCountBox(Font font, EntryRowKind kind, int row, EntryCountHandler countHandler) {
            super(font, 0, 0, ENTRY_COUNT_BTN_W, ENTRY_ROW_H, Component.literal("count"));
            this.kind = kind;
            this.row = row;
            this.countHandler = countHandler;
            setMaxLength(3);
            setBordered(true);
            setTooltip(Tooltip.create(Component.literal("count")));
            setValue("1");
            setResponder(this::handleValueChanged);
        }

        public void setRow(int row) {
            this.row = row;
        }

        public void setValueSilently(String value) {
            settingValue = true;
            try {
                setValue(value);
            } finally {
                settingValue = false;
            }
        }

        private void handleValueChanged(String value) {
            if (settingValue) return;
            String digits = safe(value).replaceAll("[^0-9]", "");
            if (!digits.equals(value)) {
                int cursor = Math.min(digits.length(), getCursorPosition());
                setValueSilently(digits);
                setCursorPosition(cursor);
            }
            if (digits.isBlank()) {
                updateRowCount(1);
                return;
            }
            int count;
            try {
                count = Math.max(1, Integer.parseInt(digits));
            } catch (NumberFormatException ignored) {
                count = 1;
            }
            String normalized = Integer.toString(count);
            if (!normalized.equals(value)) {
                int cursor = Math.min(normalized.length(), getCursorPosition());
                setValueSilently(normalized);
                setCursorPosition(cursor);
            }
            updateRowCount(count);
        }

        private void updateRowCount(int count) {
            if (countHandler != null) countHandler.update(kind, row, count);
        }

        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = isFocused();
            super.setFocused(focused);
            if (wasFocused && !focused && safe(getValue()).trim().isBlank()) {
                setValueSilently("1");
                updateRowCount(1);
            }
        }

        @Override
        public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            boolean focused = this.isFocused();
            int bg = 0xFF000000;
            int border = focused ? 0xFFFFFFFF : (hovered ? 0xFFE0E0E0 : 0xFF8A8A8A);
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            gg.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
            gg.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
            gg.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
            gg.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
            if (focused) {
                gg.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, 0xFF101010);
            }

            String text = getValue();
            Font font = Minecraft.getInstance().font;
            int textW = Math.round(font.width(text) * ENTRY_COUNT_TEXT_SCALE);
            int textH = Math.round(font.lineHeight * ENTRY_COUNT_TEXT_SCALE);
            int textX = getX() + (getWidth() - textW) / 2;
            int textY = getY() + (getHeight() - textH) / 2;
            gg.pose().pushPose();
            gg.pose().translate(textX, textY, 0f);
            gg.pose().scale(ENTRY_COUNT_TEXT_SCALE, ENTRY_COUNT_TEXT_SCALE, 1f);
            gg.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
            gg.pose().popPose();
            if (focused && (Util.getMillis() / 300L) % 2L == 0L) {
                int cursorPos = Math.max(0, Math.min(getCursorPosition(), text.length()));
                int cursorOffset = Math.round(font.width(text.substring(0, cursorPos)) * ENTRY_COUNT_TEXT_SCALE);
                int cursorX = textX + cursorOffset;
                gg.fill(cursorX, getY() + 2, cursorX + 1, getY() + getHeight() - 2, 0xFFFFFFFF);
            }
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
