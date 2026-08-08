package net.revilodev.boundless.client.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorEntryKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.MoveDirection;

// shared editor button controls
public final class QuestEditorControls {
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
    private static final ResourceLocation BTN_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");
    private static final ResourceLocation CREATE_BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/button.png");
    private static final int CREATE_BTN_TEX_W = 130;
    private static final int CREATE_BTN_TEX_H = 20;
    private static final ResourceLocation TOGGLE_TEX_OFF =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/x_button.png");
    private static final ResourceLocation TOGGLE_TEX_OFF_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/x_button-hovered.png");
    private static final ResourceLocation TOGGLE_TEX_ON =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/complete_filter.png");
    private static final ResourceLocation LOCK_TEX_ENABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter.png");
    private static final ResourceLocation LOCK_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter_disabled.png");
    private static final ResourceLocation LOCK_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter_hovered.png");
    private static final ResourceLocation BACK_TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/back_tab.png");
    private static final ResourceLocation BACK_TAB_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/back_tab-hovered.png");
    private static final int TOGGLE_SIZE = 20;
    private static final int BACK_TAB_W = 27;
    private static final int BACK_TAB_H = 17;
    private static final int FORMAT_BAR_H = 9;
    private static final ResourceLocation VANILLA_BUTTON_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation VANILLA_BUTTON_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation MOVE_UP_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_up.png");
    private static final ResourceLocation MOVE_DOWN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_down.png");
    private static final ResourceLocation MOVE_UP_HIGHLIGHTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_up_highlighted.png");
    private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/move_down_highlighted.png");
    private static final int ROW_H = 27;
    private static final int ROW_PAD = 1;
    private static final int LIST_CREATE_BUTTON_W = 127;
    private static final int LIST_CREATE_BUTTON_H = 20;
    private static final int MOVE_ICON_SIZE = 14;
    private static final int MOVE_ICON_INSET_RIGHT = 3;
    private static final int MOVE_ICON_TOP_OFFSET = 1;
    private static final int MOVE_ICON_BOTTOM_OFFSET = 13;
    private static final int EDITOR_SUBHEADER_H = 12;
    private static final int EDITOR_SUBHEADER_GAP = 3;
    private static final String ENTRY_NEW = "__new__";

    private QuestEditorControls() {
    }

    // action button
    public static final class ActionButton extends AbstractButton {
        private final Runnable onPress;
        private final List<Component> tooltip;

        public ActionButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            this(x, y, w, h, text, List.of(), onPress);
        }

        public ActionButton(int x, int y, int w, int h, Component text, List<Component> tooltip, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
            this.tooltip = tooltip == null ? List.of() : List.copyOf(tooltip);
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? BTN_TEX_DISABLED : (hovered ? BTN_TEX_HOVER : BTN_TEX);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            var font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(font, getMessage(), textX, textY, color, false);
            if (hovered && !tooltip.isEmpty()) {
                gg.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // create button
    public static final class CreateButton extends AbstractButton {
        private final Runnable onPress;

        public CreateButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            gg.blit(CREATE_BTN_TEX, getX(), getY(), 0, 0, this.width, this.height, CREATE_BTN_TEX_W, CREATE_BTN_TEX_H);
            Font font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            gg.drawString(font, getMessage(), textX, textY, 0xFFFFFF, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // compact action button
    public static final class CompactActionButton extends AbstractButton {
        private final Runnable onPress;

        public CompactActionButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int bg = !this.active ? 0xFF3A3A3A : (this.isMouseOver(mouseX, mouseY) ? 0xFF6A6A6A : 0xFF555555);
            int border = this.active ? 0xFF9A9A9A : 0xFF666666;
            gg.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);
            gg.fill(getX(), getY(), getX() + this.width, getY() + 1, border);
            gg.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, border);
            gg.fill(getX(), getY(), getX() + 1, getY() + this.height, border);
            gg.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, border);

            var font = Minecraft.getInstance().font;
            int color = this.active ? 0xFFFFFFFF : 0xFF8A8A8A;
            int textX = getX() + (this.width - font.width(getMessage())) / 2;
            int textY = getY() + (this.height - font.lineHeight) / 2;
            gg.drawString(font, getMessage(), textX, textY, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // text insert button
    public static final class TextInsertButton extends AbstractButton {
        private final String insertText;
        private final int fillColor;
        private final float textScale;
        private final Consumer<String> onPress;

        public TextInsertButton(int x, int y, int width, String label, String insertText, int fillColor, float textScale, Consumer<String> onPress) {
            super(x, y, width, FORMAT_BAR_H, Component.literal(label));
            this.insertText = insertText == null ? "" : insertText;
            this.fillColor = fillColor;
            this.textScale = textScale <= 0f ? 1f : textScale;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.accept(insertText);
        }

        public String insertText() {
            return insertText;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int bg = this.fillColor;
            if (!this.active) {
                bg = 0xFF4A4A4A;
            } else if (this.isMouseOver(mouseX, mouseY)) {
                bg = brighten(this.fillColor, 24);
            }
            int border = this.active ? 0xFFBEBEBE : 0xFF6E6E6E;
            gg.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);
            gg.fill(getX(), getY(), getX() + this.width, getY() + 1, border);
            gg.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, border);
            gg.fill(getX(), getY(), getX() + 1, getY() + this.height, border);
            gg.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, border);

            if (!getMessage().getString().isBlank()) {
                var font = Minecraft.getInstance().font;
                int textColor = this.active ? 0xFFFFFFFF : 0xFF8A8A8A;
                float scale = this.textScale;
                int textWidth = Math.round(font.width(getMessage()) * scale);
                int textHeight = Math.round(font.lineHeight * scale);
                int textX = getX() + (this.width - textWidth) / 2;
                int textY = getY() + (this.height - textHeight) / 2;
                gg.pose().pushPose();
                gg.pose().translate(textX, textY, 0.0f);
                gg.pose().scale(scale, scale, 1.0f);
                gg.drawString(font, getMessage(), 0, 0, textColor, false);
                gg.pose().popPose();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }

        private int brighten(int color, int amount) {
            int a = (color >>> 24) & 0xFF;
            int r = Math.min(255, ((color >>> 16) & 0xFF) + amount);
            int g = Math.min(255, ((color >>> 8) & 0xFF) + amount);
            int b = Math.min(255, (color & 0xFF) + amount);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    // icon button
    public static final class IconButton extends AbstractButton {
        private ResourceLocation texture;
        private ResourceLocation hoverTexture;
        private final Runnable onPress;

        public IconButton(int x, int y, int size, ResourceLocation texture, ResourceLocation hoverTexture, Runnable onPress) {
            super(x, y, size, size, Component.empty());
            this.texture = texture;
            this.hoverTexture = hoverTexture == null ? texture : hoverTexture;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        public void setTexture(ResourceLocation texture) {
            if (texture != null) {
                this.texture = texture;
            }
        }

        public void setTextures(ResourceLocation texture, ResourceLocation hoverTexture) {
            if (texture != null) this.texture = texture;
            this.hoverTexture = hoverTexture == null ? this.texture : hoverTexture;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            float alpha = this.active ? 1.0f : 0.5f;
            ResourceLocation tex = this.isMouseOver(mouseX, mouseY) ? (hoverTexture == null ? texture : hoverTexture) : texture;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // toggle button
    public static final class ToggleButton extends AbstractButton {
        private boolean on;

        public ToggleButton(int x, int y, int w, int h, boolean initial) {
            super(x, y, w, h, Component.empty());
            this.on = initial;
        }

        @Override
        public void onPress() {
            on = !on;
        }

        public boolean isOn() {
            return on;
        }

        public void setState(boolean next) {
            on = next;
        }

        public void setSize(int w, int h) {
            this.width = w;
            this.height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = on ? TOGGLE_TEX_ON : (this.isMouseOver(mouseX, mouseY) ? TOGGLE_TEX_OFF_HOVER : TOGGLE_TEX_OFF);
            gg.blit(tex, getX(), getY(), 0, 0, TOGGLE_SIZE, TOGGLE_SIZE, TOGGLE_SIZE, TOGGLE_SIZE);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // lock toggle button
    public static final class LockToggleButton extends AbstractButton {
        private boolean on;

        public LockToggleButton(int x, int y, int w, int h, boolean initial) {
            super(x, y, w, h, Component.empty());
            this.on = initial;
        }

        @Override
        public void onPress() {
            on = !on;
        }

        public boolean isOn() {
            return on;
        }

        public void setState(boolean next) {
            on = next;
        }

        public void setSize(int w, int h) {
            this.width = w;
            this.height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex;
            if (this.isMouseOver(mouseX, mouseY)) {
                tex = LOCK_TEX_HOVER;
            } else {
                tex = on ? LOCK_TEX_ENABLED : LOCK_TEX_DISABLED;
            }
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // back button
    public static final class BackButton extends AbstractButton {
        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, BACK_TAB_W, BACK_TAB_H, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? BACK_TAB_HOVER_TEX : BACK_TAB_TEX;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // editor list widget
    public static final class EditorListWidget extends AbstractButton {
        private final List<EditorEntry> entries = new ArrayList<>();
        private final Consumer<EditorEntry> onClick;
        private final Consumer<EditorEntry> onActionClick;
        private final Consumer<EditorEntry> onSecondaryClick;
        private final BiConsumer<EditorEntry, MoveDirection> onMoveClick;
            private final Function<String, ItemStack> iconResolver;
        private float scrollY = 0f;
        private String selectedId = "";
        private List<Component> pendingTooltip = List.of();
        private int pendingTooltipX;
        private int pendingTooltipY;

        public EditorListWidget(int x, int y, int w, int h, Consumer<EditorEntry> onClick, Consumer<EditorEntry> onActionClick, Consumer<EditorEntry> onSecondaryClick, BiConsumer<EditorEntry, MoveDirection> onMoveClick, Function<String, ItemStack> iconResolver) {
            super(x, y, w, h, Component.empty());
            this.onClick = onClick;
            this.onActionClick = onActionClick;
            this.onSecondaryClick = onSecondaryClick;
            this.onMoveClick = onMoveClick;
                this.iconResolver = iconResolver;
        }

        public void setBounds(int x, int y, int w, int h) {
            setX(x);
            setY(y);
            this.width = w;
            this.height = h;
        }

        public void setEntries(List<EditorEntry> list) {
            entries.clear();
            entries.addAll(list);
            scrollY = 0f;
        }

        public void setSelectedId(String id) {
            selectedId = id == null ? "" : id;
        }

        public EditorEntry entryById(String id) {
            String target = id == null ? "" : id;
            for (EditorEntry entry : entries) {
                if (Objects.equals(entry.id, target)) return entry;
            }
            return null;
        }

        public float getScrollY() {
            return scrollY;
        }

        public void setScrollY(float value) {
            int contentHeight = entries.size() * (ROW_H + ROW_PAD);
            float max = Math.max(0f, contentHeight - height);
            scrollY = Math.max(0f, Math.min(value, max));
        }

        @Override
        public void onPress() {
        }

        public void renderHoverTooltipOnTop(GuiGraphics gg) {
            if (pendingTooltip == null || pendingTooltip.isEmpty()) return;
            gg.pose().pushPose();
            gg.pose().translate(0.0F, 0.0F, 500.0F);
            gg.renderComponentTooltip(Minecraft.getInstance().font, pendingTooltip, pendingTooltipX, pendingTooltipY);
            gg.pose().popPose();
            pendingTooltip = List.of();
        }

        @Override
        public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            pendingTooltip = List.of();
            gg.enableScissor(getX(), getY(), getX() + width, getY() + height);

            int yCursor = getY() - (int) scrollY;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                int top = yCursor;
                int rowH = headerEntry ? EDITOR_SUBHEADER_H : ROW_H;
                int rowGap = headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD;

                if (top > getY() + height) break;
                if (top + rowH < getY()) {
                    yCursor += rowH + rowGap;
                    continue;
                }
                if (headerEntry && (top < getY() || top + rowH > getY() + height)) {
                    yCursor += rowH + rowGap;
                    continue;
                }

                if (ENTRY_NEW.equals(entry.id)) {
                    int buttonWidth = LIST_CREATE_BUTTON_W;
                    int buttonHeight = LIST_CREATE_BUTTON_H;
                    int buttonX = getX() + (width - buttonWidth) / 2;
                    int buttonY = top + (rowH - buttonHeight) / 2;
                    boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                            && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
                    gg.blitSprite(hovered ? VANILLA_BUTTON_HIGHLIGHTED_SPRITE : VANILLA_BUTTON_SPRITE, buttonX, buttonY, buttonWidth, buttonHeight);
                    Font font = Minecraft.getInstance().font;
                    gg.drawCenteredString(font, entry.label, buttonX + buttonWidth / 2, buttonY + (buttonHeight - font.lineHeight) / 2 + 1, 0xFFFFFF);
                } else {
                    int textX = getX() + 6 + entry.indent;
                    ItemStack iconStack = iconResolver == null ? ItemStack.EMPTY : iconResolver.apply(entry.icon);
                    boolean hoverRow = mouseX >= getX() && mouseX <= getX() + width
                            && mouseY >= top && mouseY <= top + rowH;

                    if (!headerEntry) {
                        blitQuestWidget(gg, entry.rowTexture, getX(), top, width, ROW_H, hoverRow);
                        if (!selectedId.isBlank() && selectedId.equals(entry.id)) {
                            gg.fill(getX() + 1, top + 1, getX() + width - 1, top + ROW_H - 1, 0x20FFFFFF);
                        }
                    } else {
                        float iconScale = 0.45f;
                        int iconSize = (int) (16 * iconScale);
                        int iconX = getX() + 2;
                        int iconY = top + (rowH - iconSize) / 2;
                        int textIconSize = iconStack.isEmpty() ? 0 : iconSize;

                        if (!iconStack.isEmpty()) {
                            gg.pose().pushPose();
                            gg.pose().translate(iconX, iconY, 0);
                            gg.pose().scale(iconScale, iconScale, 1f);
                            gg.renderItem(iconStack, 0, 0);
                            gg.pose().popPose();
                        }

                        float textScale = 0.66f;
                        String name = entry.label == null ? "" : entry.label.substring(Math.min(2, entry.label.length()));
                        textX = iconX + textIconSize + 2;
                        int textH = (int) (Minecraft.getInstance().font.lineHeight * textScale);
                        int textY = top + (rowH - textH) / 2 + 1;
                        int maxW = (getX() + width - 2) - textX - 2;
                        int maxWUnscaled = maxW > 0 ? (int) (maxW / textScale) : 0;
                        if (Minecraft.getInstance().font.width(name) > maxWUnscaled) {
                            name = Minecraft.getInstance().font.plainSubstrByWidth(name, Math.max(0, maxWUnscaled - Minecraft.getInstance().font.width("..."))) + "...";
                        }
                        if (entry.kind == EditorEntryKind.CATEGORY_HEADER) {
                            drawScaledComponent(gg, Component.literal(name).withStyle(style -> style.withBold(true)), textScale, textX, textY, 0xFFFFFF);
                        } else {
                            drawScaledString(gg, name, textScale, textX, textY, 0xD0D0D0);
                        }
                        yCursor += rowH + rowGap;
                        continue;
                    }

                    if (!iconStack.isEmpty()) {
                        gg.renderItem(iconStack, getX() + 6 + entry.indent, top + 5);
                        textX = getX() + 25 + entry.indent;
                    }
                    int labelColor = 0xFFFFFF;
                    gg.drawString(Minecraft.getInstance().font, entry.label, textX, top + 7, labelColor, false);

                    if (entry.kind == EditorEntryKind.NORMAL && entry.subtitle != null && !entry.subtitle.isBlank()) {
                        drawScaledString(gg, entry.subtitle, 0.65f, textX, top + 17, 0xB0B0B0);
                    }

                    if (entry.showMoveArrows) {
                        int moveX = getX() + width - MOVE_ICON_SIZE - MOVE_ICON_INSET_RIGHT;
                        int upY = top + MOVE_ICON_TOP_OFFSET;
                        int downY = top + MOVE_ICON_BOTTOM_OFFSET;
                        boolean hoverUp = mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                                && mouseY >= upY && mouseY <= upY + MOVE_ICON_SIZE;
                        boolean hoverDown = mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                                && mouseY >= downY && mouseY <= downY + MOVE_ICON_SIZE;
                        gg.blit(hoverUp ? MOVE_UP_HIGHLIGHTED_TEX : MOVE_UP_TEX, moveX, upY, 0, 0, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE);
                        gg.blit(hoverDown ? MOVE_DOWN_HIGHLIGHTED_TEX : MOVE_DOWN_TEX, moveX, downY, 0, 0, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE, MOVE_ICON_SIZE);
                    }

                    if (hoverRow && entry.rowTooltip != null && !entry.rowTooltip.isBlank()) {
                        pendingTooltip = List.of(Component.literal(entry.rowTooltip));
                        pendingTooltipX = mouseX;
                        pendingTooltipY = mouseY;
                    }

                    if (entry.actionIcon != null) {
                        int iconX = getX() + width - 17;
                        int iconY = top + 7;
                        gg.blit(entry.actionIcon, iconX, iconY, 0, 0, 13, 13, 13, 13);
                        boolean hoverAction = mouseX >= iconX && mouseX <= iconX + 13
                                && mouseY >= iconY && mouseY <= iconY + 13;
                        if (hoverAction && entry.actionTooltip != null && !entry.actionTooltip.isBlank()) {
                            pendingTooltip = List.of(Component.literal(entry.actionTooltip));
                            pendingTooltipX = mouseX;
                            pendingTooltipY = mouseY;
                        }
                    }
                }

                yCursor += rowH + rowGap;
            }

            gg.disableScissor();

            int contentHeight = 0;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                contentHeight += (headerEntry ? EDITOR_SUBHEADER_H : ROW_H) + (headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD);
            }
            if (contentHeight > height) {
                float ratio = (float) height / (float) contentHeight;
                int barH = Math.max(12, (int) (height * ratio));
                float scrollRatio = scrollY / (contentHeight - height);
                int barY = getY() + (int) ((height - barH) * scrollRatio);
                gg.fill(getX() + width + 4, barY, getX() + width + 6, barY + barH, 0xFF808080);
            }
        }

        private void blitQuestWidget(GuiGraphics gg, ResourceLocation texture, int x, int y, int w, int h, boolean hovered) {
            if (hovered) RenderSystem.setShaderColor(1.1f, 1.1f, 1.1f, 1.0f);
            gg.blit(texture, x, y, 0, 0, w, h, w, h);
            if (hovered) RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible || !active || (button != 0 && button != 1)) return false;
            if (mouseX < getX() || mouseX > getX() + width || mouseY < getY() || mouseY > getY() + height)
                return false;

            int localY = (int) (mouseY - getY() + scrollY);
            int yCursor = 0;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                int rowH = headerEntry ? EDITOR_SUBHEADER_H : ROW_H;
                int rowGap = headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD;
                if (localY < yCursor || localY >= yCursor + rowH + rowGap) {
                    yCursor += rowH + rowGap;
                    continue;
                }
                int top = getY() - (int) scrollY + yCursor;
                if (entry.showMoveArrows && button == 0) {
                    int moveX = getX() + width - MOVE_ICON_SIZE - MOVE_ICON_INSET_RIGHT;
                    int upY = top + MOVE_ICON_TOP_OFFSET;
                    int downY = top + MOVE_ICON_BOTTOM_OFFSET;
                    if (mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                            && mouseY >= upY && mouseY <= upY + MOVE_ICON_SIZE) {
                        if (onMoveClick != null) onMoveClick.accept(entry, MoveDirection.UP);
                        return true;
                    }
                    if (mouseX >= moveX && mouseX <= moveX + MOVE_ICON_SIZE
                            && mouseY >= downY && mouseY <= downY + MOVE_ICON_SIZE) {
                        if (onMoveClick != null) onMoveClick.accept(entry, MoveDirection.DOWN);
                        return true;
                    }
                }
                if (entry.actionIcon != null) {
                    int iconX = getX() + width - 17;
                    int iconY = top + 7;
                    if (button == 0 && mouseX >= iconX && mouseX <= iconX + 13 && mouseY >= iconY && mouseY <= iconY + 13) {
                        if (onActionClick != null) onActionClick.accept(entry);
                        return true;
                    }
                }
                if (button == 1) {
                    if (onSecondaryClick != null) onSecondaryClick.accept(entry);
                    return true;
                }
                if (onClick != null) onClick.accept(entry);
                return true;
            }
            return false;
        }

        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!visible || !active) return false;
            int contentHeight = 0;
            for (EditorEntry entry : entries) {
                boolean headerEntry = entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER;
                contentHeight += (headerEntry ? EDITOR_SUBHEADER_H : ROW_H) + (headerEntry ? EDITOR_SUBHEADER_GAP : ROW_PAD);
            }
            if (contentHeight <= height) return false;
            scrollY = Math.max(0f, Math.min(scrollY - (float) (delta * 12), contentHeight - height));
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
            return mouseScrolled(mouseX, mouseY, deltaY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }

        private void drawScaledString(GuiGraphics gg, String text, float scale, int x, int y, int color) {
            if (text == null || text.isEmpty()) return;
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(Minecraft.getInstance().font, text, (int) (x * inv), (int) (y * inv), color, false);
            gg.pose().popPose();
        }

        private void drawScaledComponent(GuiGraphics gg, Component text, float scale, int x, int y, int color) {
            if (text == null) return;
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(Minecraft.getInstance().font, text, (int) (x * inv), (int) (y * inv), color, false);
            gg.pose().popPose();
        }
    }
}
