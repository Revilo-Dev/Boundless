package net.revilodev.boundless.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.EmptyBlockGetter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.BoundlessMod;
import net.revilodev.boundless.client.editor.FtbQuestImporter;
import net.revilodev.boundless.client.editor.QuestEditorControls.ActionButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.BackButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.EditorListWidget;
import net.revilodev.boundless.client.editor.QuestEditorControls.IconButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.LockToggleButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.TextInsertButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.ToggleButton;
import net.revilodev.boundless.client.editor.QuestEditorCustomIcons;
import net.revilodev.boundless.client.editor.QuestEditorEmojis;
import net.revilodev.boundless.client.editor.QuestEditorEntryCodec;
import net.revilodev.boundless.client.editor.QuestEditorEntryRows.RowMaps;
import net.revilodev.boundless.client.editor.QuestEditorEntryRows;
import net.revilodev.boundless.client.editor.QuestEditorEntryTypes;
import net.revilodev.boundless.client.editor.QuestEditorItemIcons;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerData.PickerPageRequest;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerData.PickerPageResult;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerData;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerRenderer.RenderRequest;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerRenderer;
import net.revilodev.boundless.client.editor.QuestEditorJsonBuilder.QuestBuildFields;
import net.revilodev.boundless.client.editor.QuestEditorJsonBuilder;
import net.revilodev.boundless.client.editor.QuestEditorJsonFiles;
import net.revilodev.boundless.client.editor.QuestEditorModels.CategoryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.CommandReward;
import net.revilodev.boundless.client.editor.QuestEditorModels.DropdownMenuTarget;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorEntryKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorType;
import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.FormField;
import net.revilodev.boundless.client.editor.QuestEditorModels.ItemPickerTab;
import net.revilodev.boundless.client.editor.QuestEditorModels.Mode;
import net.revilodev.boundless.client.editor.QuestEditorModels.MoveDirection;
import net.revilodev.boundless.client.editor.QuestEditorModels.MultiLineEntryContext;
import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.PackMeta;
import net.revilodev.boundless.client.editor.QuestEditorModels.ParsedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerMode;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerPageData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListIndex;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListItem;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;
import net.revilodev.boundless.client.editor.QuestEditorModels.ScreenState;
import net.revilodev.boundless.client.editor.QuestEditorModels.SubCategoryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.SuggestionBounds;
import net.revilodev.boundless.client.editor.QuestEditorModels.ValidationSnapshot;
import net.revilodev.boundless.client.editor.QuestEditorNaming;
import net.revilodev.boundless.client.editor.QuestEditorPackFiles;
import net.revilodev.boundless.client.editor.QuestEditorPopupMenus;
import net.revilodev.boundless.client.editor.QuestEditorQuestOrdering.QuestOrderResult;
import net.revilodev.boundless.client.editor.QuestEditorQuestOrdering;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EditorTabButton;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryCountBox;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryItemPickerButton;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryRemoveButton;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryTypeButton;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.IconPickerBox;
import net.revilodev.boundless.client.editor.QuestEditorStateSignatures.SignatureFields;
import net.revilodev.boundless.client.editor.QuestEditorStateSignatures;
import net.revilodev.boundless.client.editor.QuestEditorSuggestionCaches;
import net.revilodev.boundless.client.editor.QuestEditorSuggestionRenderer;
import net.revilodev.boundless.client.editor.QuestEditorTooltips;
import net.revilodev.boundless.client.editor.QuestEditorTreeWidget.Tool;
import net.revilodev.boundless.client.editor.QuestEditorTreeWidget;
import net.revilodev.boundless.client.editor.QuestEditorValidation.ValidationRequest;
import net.revilodev.boundless.client.QuestTextFormatting;
import net.revilodev.boundless.client.editor.QuestEditorValidation;
import net.revilodev.boundless.client.editor.ScaledMultiLineEditBox;
import net.revilodev.boundless.client.QuestCategoryBackgroundRenderer;
import net.revilodev.boundless.client.QuestPanelBackgroundRenderer;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.client.QuestPanelExpandButton;
import net.revilodev.boundless.compat.LevelUpCompat;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestItemSpec;
import net.revilodev.boundless.quest.QuestPackStorage;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

// public client screen entry point for the quest editor
public final class QuestEditorScreen extends QuestEditor {
    private static final ResourceLocation LANGUAGE_ICON = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/sprites/icons/language.png");
    public QuestEditorScreen(Screen parent) {
        super(parent);
    }

    public QuestEditorScreen(Screen parent, QuestData.Quest quest) {
        super(parent, quest);
    }

    // create state-aware widgets inside the concrete screen
    @Override
    protected CategoryWorkspaceTab createCategoryWorkspaceTab(NamedEntry category, int y) {
        return new CategoryWorkspaceTab(category, y);
    }

    @Override
    protected LanguageToggleButton createLanguageToggleButton(boolean description) {
        return new LanguageToggleButton(description);
    }

    @Override
    protected TranslationRow createTranslationRow(boolean description, String locale) {
        return new TranslationRow(description, locale);
    }

    // media selector keeps browsing and clearing behind one compact control
    static final class MediaSelector extends AbstractWidget {
        private final Font font;
        private final Runnable onBrowse;
        private final Runnable onClear;
        private String value = "";

        MediaSelector(Font font, Runnable onBrowse, Runnable onClear) {
            super(0, 0, 1, 20, Component.empty());
            this.font = font;
            this.onBrowse = onBrowse;
            this.onClear = onClear;
        }

        String value() {
            return value;
        }

        void setValue(String value) {
            this.value = value == null ? "" : value;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible || !active || !isMouseOver(mouseX, mouseY)) return false;
            if (button == 1) {
                setValue("");
                onClear.run();
                return true;
            }
            if (button != 0) return false;
            onBrowse.run();
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation sprite = isMouseOver(mouseX, mouseY)
                    ? ResourceLocation.withDefaultNamespace("widget/button_highlighted")
                    : ResourceLocation.withDefaultNamespace("widget/button");
            graphics.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
            String label = Component.translatable("ui.boundless.editor.browse").getString();
            String clipped = font.plainSubstrByWidth(label, Math.max(1, getWidth() - 8));
            graphics.drawString(font, clipped, getX() + 4, getY() + 6, 0xFFFFFFFF, false);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // import panel keeps the standard button above its short explanation
    static final class FtbImportSection extends AbstractWidget {
        private static final int BUTTON_HEIGHT = 20;
        private final Font font;
        private final Runnable onImport;
        private final BooleanSupplier running;

        FtbImportSection(Font font, Runnable onImport, BooleanSupplier running) {
            super(0, 0, 1, 138, Component.empty());
            this.font = font;
            this.onImport = onImport;
            this.running = running;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isButtonHovered(mouseX, mouseY)) return false;
            onImport.run();
            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return isButtonHovered(mouseX, mouseY);
        }

        private boolean isButtonHovered(double mouseX, double mouseY) {
            return mouseX >= getX() && mouseX <= getX() + getWidth()
                    && mouseY >= getY() && mouseY <= getY() + BUTTON_HEIGHT;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation sprite = !active
                    ? ResourceLocation.withDefaultNamespace("widget/button_disabled")
                    : isButtonHovered(mouseX, mouseY)
                    ? ResourceLocation.withDefaultNamespace("widget/button_highlighted")
                    : ResourceLocation.withDefaultNamespace("widget/button");
            graphics.blitSprite(sprite, getX(), getY(), getWidth(), BUTTON_HEIGHT);
            Component label = Component.translatable(running.getAsBoolean()
                    ? "ui.boundless.editor.import.running" : "ui.boundless.editor.import.action");
            graphics.drawString(font, label, getX() + (getWidth() - font.width(label)) / 2,
                    getY() + 6, active ? 0xFFFFFFFF : 0xFF808080, false);
            int lineY = getY() + BUTTON_HEIGHT + 6;
            Component description = Component.translatable("ui.boundless.editor.import.description");
            for (var line : font.split(description, Math.max(20, getWidth()))) {
                if (lineY + font.lineHeight > getY() + getHeight()) break;
                graphics.drawString(font, line, getX(), lineY, 0xFFA0A0A0, false);
                lineY += font.lineHeight;
            }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // layout selector shares the standard two-choice editor control
    static final class CategoryLayoutSelector extends AbstractWidget {
        private final Font font;
        private final Runnable onChange;
        private boolean treeLayout;

        CategoryLayoutSelector(Font font, Runnable onChange) {
            super(0, 0, 1, 22, Component.empty());
            this.font = font;
            this.onChange = onChange;
        }

        boolean isTreeLayout() {
            return treeLayout;
        }

        void setTreeLayout(boolean treeLayout) {
            this.treeLayout = treeLayout;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            int buttonY = getY() + 1;
            int halfWidth = Math.max(1, (getWidth() - 3) / 2);
            if (mouseY < buttonY || mouseY > buttonY + 20) return false;
            boolean nextTree = mouseX > getX() + halfWidth + 3;
            if (treeLayout != nextTree) {
                treeLayout = nextTree;
                onChange.run();
            }
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int buttonY = getY() + 1;
            int halfWidth = Math.max(1, (getWidth() - 3) / 2);
            renderChoice(graphics, getX(), buttonY, halfWidth,
                    Component.translatable("ui.boundless.editor.layout.list").getString(), !treeLayout);
            renderChoice(graphics, getX() + halfWidth + 3, buttonY, halfWidth,
                    Component.translatable("ui.boundless.editor.layout.tree").getString(), treeLayout);
        }

        private void renderChoice(GuiGraphics graphics, int x, int y, int width, String label, boolean selected) {
            ResourceLocation sprite = ResourceLocation.withDefaultNamespace(
                    selected ? "widget/button" : "widget/button_disabled");
            graphics.blitSprite(sprite, x, y, width, 20);
            graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 6,
                    selected ? 0xFFFFFFFF : 0xFF808080, false);
        }

        Component hoveredTooltip(double mouseX, double mouseY) {
            int halfWidth = Math.max(1, (getWidth() - 3) / 2);
            int buttonY = getY() + 1;
            if (mouseY < buttonY || mouseY > buttonY + 20) return null;
            return Component.translatable(mouseX <= getX() + halfWidth
                    ? "ui.boundless.editor.tooltip.layout_list" : "ui.boundless.editor.tooltip.layout_tree");
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // category settings reuse one compact sub-category list
    static final class CategorySubCategorySection extends AbstractWidget {
        private static final int ROW_HEIGHT = 20;
        private static final int ROW_GAP = 2;
        private static final int COLLAPSED_ROWS = 3;
        private final Font font;
        private final Consumer<SubCategoryData> onSelect;
        private final Runnable onCreate;
        private List<SubCategoryData> entries = List.of();
        private boolean expanded;

        CategorySubCategorySection(Font font, Consumer<SubCategoryData> onSelect, Runnable onCreate) {
            super(0, 0, 1, ROW_HEIGHT, Component.empty());
            this.font = font;
            this.onSelect = onSelect;
            this.onCreate = onCreate;
        }

        void setEntries(List<SubCategoryData> entries) {
            this.entries = entries == null ? List.of() : List.copyOf(entries);
            if (this.entries.size() <= COLLAPSED_ROWS) expanded = false;
            updateHeight();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            int row = (int) ((mouseY - getY()) / (ROW_HEIGHT + ROW_GAP));
            int visibleEntries = visibleEntryCount();
            if (row < visibleEntries) {
                onSelect.accept(entries.get(row));
                return true;
            }
            if (entries.size() > COLLAPSED_ROWS && row == visibleEntries) {
                expanded = !expanded;
                updateHeight();
                return true;
            }
            onCreate.run();
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int y = getY();
            int visibleEntries = visibleEntryCount();
            for (int index = 0; index < visibleEntries; index++) {
                SubCategoryData entry = entries.get(index);
                renderButton(graphics, mouseX, mouseY, y, safeLabel(entry.name, entry.id));
                y += ROW_HEIGHT + ROW_GAP;
            }
            if (entries.size() > COLLAPSED_ROWS) {
                renderButton(graphics, mouseX, mouseY, y, Component.translatable(expanded
                        ? "ui.boundless.editor.show_less" : "ui.boundless.editor.show_more").getString());
                y += ROW_HEIGHT + ROW_GAP;
            }
            renderButton(graphics, mouseX, mouseY, y,
                    Component.translatable("ui.boundless.editor.create_new_subcategory").getString());
        }

        private void renderButton(GuiGraphics graphics, int mouseX, int mouseY, int y, String label) {
            boolean hovered = mouseX >= getX() && mouseX <= getX() + getWidth()
                    && mouseY >= y && mouseY <= y + ROW_HEIGHT;
            graphics.blitSprite(ResourceLocation.withDefaultNamespace(hovered
                    ? "widget/button_highlighted" : "widget/button"), getX(), y, getWidth(), ROW_HEIGHT);
            Component clipped = QuestTextFormatting.componentByWidth(label, 0xFFFFFF, font, Math.max(1, getWidth() - 8));
            graphics.drawString(font, clipped, getX() + 4, y + 6, 0xFFFFFFFF, false);
        }

        private int visibleEntryCount() {
            return expanded ? entries.size() : Math.min(COLLAPSED_ROWS, entries.size());
        }

        private void updateHeight() {
            int rows = visibleEntryCount() + 1 + (entries.size() > COLLAPSED_ROWS ? 1 : 0);
            setHeight(rows * ROW_HEIGHT + Math.max(0, rows - 1) * ROW_GAP);
        }

        private static String safeLabel(String name, String id) {
            return name == null || name.isBlank() ? (id == null ? "" : id) : name;
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // node background picker autosaves only after a choice change
    static final class TreeBackgroundSelector extends AbstractWidget {
        private static final String[] VALUES = {"circle", "diamond", "rounded_square", "square"};
        private final BooleanSupplier hasUnsavedChanges;
        private final BiConsumer<String, Boolean> onChange;
        private String background = "square";

        TreeBackgroundSelector(BooleanSupplier hasUnsavedChanges, BiConsumer<String, Boolean> onChange) {
            super(0, 0, 1, 34, Component.empty());
            this.hasUnsavedChanges = hasUnsavedChanges;
            this.onChange = onChange;
        }

        void setBackground(String background) {
            this.background = normalize(background);
        }

        String background() {
            return background;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            int buttonWidth = Math.max(1, (getWidth() - 6) / 4);
            int index = (int) ((mouseX - getX()) / (buttonWidth + 2));
            int buttonX = getX() + index * (buttonWidth + 2);
            if (index < 0 || index >= VALUES.length || mouseX > buttonX + buttonWidth
                    || mouseY < getY() + 1 || mouseY > getY() + 33) return false;
            if (background.equals(VALUES[index])) return true;
            boolean alreadyUnsaved = hasUnsavedChanges.getAsBoolean();
            background = VALUES[index];
            onChange.accept(background, alreadyUnsaved);
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int buttonWidth = Math.max(1, (getWidth() - 6) / 4);
            for (int index = 0; index < VALUES.length; index++) {
                int x = getX() + index * (buttonWidth + 2);
                int y = getY() + 1;
                boolean selected = background.equals(VALUES[index]);
                boolean hovered = mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + 32;
                ResourceLocation buttonTexture = ResourceLocation.withDefaultNamespace(
                        selected || hovered ? "widget/button_highlighted" : "widget/button");
                graphics.blitSprite(buttonTexture, x, y, buttonWidth, 32);
                graphics.pose().pushPose();
                graphics.pose().translate(x + (buttonWidth - 20) / 2f, y + 6, 0);
                graphics.pose().scale(0.625f, 0.625f, 1f);
                graphics.blit(ResourceLocation.fromNamespaceAndPath("boundless",
                                "textures/gui/sprites/editor/tree/" + VALUES[index].replace('_', '-') + "-incomplete.png"),
                        0, 0, 0, 0, 32, 32, 32, 32);
                graphics.pose().popPose();
            }
        }

        private static String normalize(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "circle", "diamond", "rounded_square", "square" -> normalized;
                default -> "square";
            };
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // compact icon button used by editor header actions
    static final class ScaledIconButton extends net.minecraft.client.gui.components.AbstractButton {
        private ResourceLocation texture;
        private ResourceLocation hoverTexture;
        private final Runnable onPress;

        ScaledIconButton(int x, int y, int size, ResourceLocation texture, ResourceLocation hoverTexture, Runnable onPress) {
            super(x, y, size, size, Component.empty());
            this.texture = texture;
            this.hoverTexture = hoverTexture;
            this.onPress = onPress;
        }

        void setTextures(ResourceLocation texture, ResourceLocation hoverTexture) {
            this.texture = texture;
            this.hoverTexture = hoverTexture;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation selectedTexture = isMouseOver(mouseX, mouseY) ? hoverTexture : texture;
            graphics.pose().pushPose();
            graphics.pose().translate(getX(), getY(), 0);
            graphics.pose().scale(0.5f, 0.5f, 1.0f);
            graphics.blit(selectedTexture, 0, 0, 0, 0, 20, 20, 20, 20);
            graphics.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // tree action button keeps its render and click bounds together
    static final class TreeToolButton extends AbstractWidget {
        private final Tool tool;
        private final Runnable onPress;
        private final ResourceLocation deleteTexture;
        private final ResourceLocation deleteSelectedTexture;
        private boolean selected;

        TreeToolButton(Tool tool, Runnable onPress, int size, ResourceLocation deleteTexture,
                       ResourceLocation deleteSelectedTexture) {
            super(0, 0, size, size, Component.empty());
            this.tool = tool;
            this.onPress = onPress;
            this.deleteTexture = deleteTexture;
            this.deleteSelectedTexture = deleteSelectedTexture;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            onPress();
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation background = selected || isMouseOver(mouseX, mouseY)
                    ? ResourceLocation.withDefaultNamespace("widget/button_highlighted")
                    : ResourceLocation.withDefaultNamespace("widget/button");
            graphics.blitSprite(background, getX(), getY(), getWidth(), getHeight());
            if (tool == Tool.DELETE) {
                ResourceLocation texture = selected || isMouseOver(mouseX, mouseY) ? deleteSelectedTexture : deleteTexture;
                graphics.blit(texture, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
                return;
            }
            int iconSize = 9;
            int iconX = getX() + (getWidth() - iconSize) / 2;
            int iconY = getY() + (getHeight() - iconSize) / 2;
            graphics.blit(deleteTexture, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // category paging uses one control for both arrow directions
    static final class CategoryScrollButton extends AbstractWidget {
        private final boolean up;
        private final IntConsumer onScroll;
        private final ResourceLocation upTexture;
        private final ResourceLocation upHoverTexture;
        private final ResourceLocation downTexture;
        private final ResourceLocation downHoverTexture;

        CategoryScrollButton(int x, int y, boolean up, int size, IntConsumer onScroll,
                             ResourceLocation upTexture, ResourceLocation upHoverTexture,
                             ResourceLocation downTexture, ResourceLocation downHoverTexture) {
            super(x, y, size, size, Component.empty());
            this.up = up;
            this.onScroll = onScroll;
            this.upTexture = upTexture;
            this.upHoverTexture = upHoverTexture;
            this.downTexture = downTexture;
            this.downHoverTexture = downHoverTexture;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            onScroll.accept(up ? -1 : 1);
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture = up
                    ? (isMouseOver(mouseX, mouseY) ? upHoverTexture : upTexture)
                    : (isMouseOver(mouseX, mouseY) ? downHoverTexture : downTexture);
            graphics.blit(texture, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // background scale writes through the editor debounce callback
    static final class BackgroundScaleSlider extends AbstractSliderButton {
        private static final float MIN_SCALE = 0.1f;
        private static final float MAX_SCALE = 2f;
        private final Runnable onChange;

        BackgroundScaleSlider(int x, int y, int width, int height, float scale, Runnable onChange) {
            super(x, y, width, height, Component.empty(), normalizedValue(scale));
            this.onChange = onChange;
            updateMessage();
        }

        float getScale() {
            return MIN_SCALE + (float) value * (MAX_SCALE - MIN_SCALE);
        }

        void setScale(float scale) {
            value = normalizedValue(scale);
            updateMessage();
        }

        private static double normalizedValue(float scale) {
            float clamped = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
            return (clamped - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Math.round(getScale() * 100f) + "%"));
        }

        @Override
        protected void applyValue() {
            onChange.run();
        }
    }

    // background opacity writes through the same debounce path
    static final class BackgroundOpacitySlider extends AbstractSliderButton {
        private final Runnable onChange;

        BackgroundOpacitySlider(int x, int y, int width, int height, float opacity, Runnable onChange) {
            super(x, y, width, height, Component.empty(), Math.max(0d, Math.min(1d, opacity)));
            this.onChange = onChange;
            updateMessage();
        }

        float getOpacity() {
            return (float) value;
        }

        void setOpacity(float opacity) {
            value = Math.max(0d, Math.min(1d, opacity));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Math.round(value * 100d) + "%"));
        }

        @Override
        protected void applyValue() {
            onChange.run();
        }
    }

    enum CategoryBackgroundMode {
        TILEABLE("background.tile_block"),
        CUSTOM_TILEABLE("background.tile_asset"),
        FULL_IMAGE("background.full_image");

        final String translationKey;

        CategoryBackgroundMode(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // the editor draws its own scaled panel background
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // scale input and rendering through the same editor coordinate space
        mouseX = Math.round(mouseX / uiScale);
        mouseY = Math.round(mouseY / uiScale);
        gg.pose().pushPose();
        gg.pose().scale(uiScale, uiScale, 1.0f);
        pendingEditorTooltip = List.of();
        updateLeftPaneLayout();
        if (editorType == EditorType.PACK_CREATE || editorType == EditorType.PACK_OPTIONS) {
        }
        updateDeleteHold(mouseX, mouseY);
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        QuestPanelBackgroundRenderer.render(gg, PANEL_TEX, leftX, topY, panelWidth, panelHeight);
        QuestPanelBackgroundRenderer.render(gg, PANEL_TEX, rightX, topY, editorPanelWidth, panelHeight);
        renderListPackBackground(gg);

        gg.enableScissor(pxRight, py, pxRight + pw, py + ph);
        renderEditorFields(gg, mouseX, mouseY);
        gg.disableScissor();
        renderPanelHeader(gg, leftX, panelHeaderTitle());
        renderSideTabs(gg, mouseX, mouseY);

        boolean leftVisible = leftList != null && leftList.visible;
        boolean treeVisible = questTree != null && questTree.visible;
        boolean backVisible = backButton != null && backButton.visible;
        boolean saveVisible = saveButton != null && saveButton.visible;
        boolean exportVisible = exportPackButton != null && exportPackButton.visible;
        boolean duplicateVisible = duplicateButton != null && duplicateButton.visible;
        boolean deleteQuestVisible = deleteQuestButton != null && deleteQuestButton.visible;
        boolean questSearchVisible = questSearchBox != null && questSearchBox.visible;
        boolean createEntryVisible = createEntryButton != null && createEntryButton.visible;
        boolean treeMoveVisible = treeMoveButton != null && treeMoveButton.visible;
        boolean treeDeleteVisible = treeDeleteButton != null && treeDeleteButton.visible;
        boolean treeConnectorVisible = treeConnectorButton != null && treeConnectorButton.visible;
        boolean treeExpandVisible = treeExpandButton != null && treeExpandButton.visible;
        if (leftList != null) leftList.visible = false;
        if (questTree != null) questTree.visible = false;
        if (backButton != null) backButton.visible = false;
        if (saveButton != null) saveButton.visible = false;
        if (duplicateButton != null) duplicateButton.visible = false;
        if (deleteQuestButton != null) deleteQuestButton.visible = false;
        if (questSearchBox != null) questSearchBox.visible = false;
        if (createEntryButton != null) createEntryButton.visible = false;
        if (treeMoveButton != null) treeMoveButton.visible = false;
        if (treeDeleteButton != null) treeDeleteButton.visible = false;
        if (treeConnectorButton != null) treeConnectorButton.visible = false;
        if (treeExpandButton != null) treeExpandButton.visible = false;

        gg.enableScissor(pxRight, py, pxRight + pw, py + ph);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.disableScissor();

        if (leftList != null) leftList.visible = leftVisible;
        if (questTree != null) questTree.visible = treeVisible;
        if (backButton != null) backButton.visible = backVisible;
        if (saveButton != null) saveButton.visible = saveVisible;
        if (duplicateButton != null) duplicateButton.visible = duplicateVisible;
        if (deleteQuestButton != null) deleteQuestButton.visible = deleteQuestVisible;
        if (questSearchBox != null) questSearchBox.visible = questSearchVisible;
        if (createEntryButton != null) createEntryButton.visible = createEntryVisible;
        if (treeMoveButton != null) treeMoveButton.visible = treeMoveVisible;
        if (treeDeleteButton != null) treeDeleteButton.visible = treeDeleteVisible;
        if (treeConnectorButton != null) treeConnectorButton.visible = treeConnectorVisible;
        if (treeExpandButton != null) treeExpandButton.visible = treeExpandVisible;

        if (leftList != null && leftList.visible) leftList.render(gg, mouseX, mouseY, partialTick);
        if (questTree != null && questTree.visible) {
            questTree.setTooltipSuppressed(isTreeTooltipCovered(mouseX, mouseY) || hasForegroundOverlay());
            questTree.render(gg, mouseX, mouseY, partialTick);
        }
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 500);
        for (CategoryWorkspaceTab tab : categoryWorkspaceTabs) {
            if (tab.visible) tab.render(gg, mouseX, mouseY, partialTick);
        }
        if (categoryWorkspaceUpButton != null && categoryWorkspaceUpButton.visible) {
            categoryWorkspaceUpButton.render(gg, mouseX, mouseY, partialTick);
        }
        if (categoryWorkspaceDownButton != null && categoryWorkspaceDownButton.visible) {
            categoryWorkspaceDownButton.render(gg, mouseX, mouseY, partialTick);
        }
        if (backButton != null && backButton.visible) backButton.render(gg, mouseX, mouseY, partialTick);
        if (saveButton != null && saveButton.visible) saveButton.render(gg, mouseX, mouseY, partialTick);
        if (duplicateButton != null && duplicateButton.visible) duplicateButton.render(gg, mouseX, mouseY, partialTick);
        if (deleteQuestButton != null && deleteQuestButton.visible) deleteQuestButton.render(gg, mouseX, mouseY, partialTick);
        renderDeleteHoldProgress(gg);
        if (questSearchBox != null && questSearchBox.visible) questSearchBox.render(gg, mouseX, mouseY, partialTick);
        if (createEntryButton != null && createEntryButton.visible) createEntryButton.render(gg, mouseX, mouseY, partialTick);
        if (treeMoveButton != null && treeMoveButton.visible) treeMoveButton.render(gg, mouseX, mouseY, partialTick);
        if (treeDeleteButton != null && treeDeleteButton.visible) treeDeleteButton.render(gg, mouseX, mouseY, partialTick);
        if (treeConnectorButton != null && treeConnectorButton.visible) treeConnectorButton.render(gg, mouseX, mouseY, partialTick);
        if (treeExpandButton != null && treeExpandButton.visible) treeExpandButton.render(gg, mouseX, mouseY, partialTick);
        if (createPackButton != null && createPackButton.visible) createPackButton.render(gg, mouseX, mouseY, partialTick);
        if (importQuestPackButton != null && importQuestPackButton.visible) importQuestPackButton.render(gg, mouseX, mouseY, partialTick);
        gg.pose().popPose();
        renderIconOverlays(gg);
        renderTypeMenu(gg, mouseX, mouseY);
        renderDropdownMenu(gg, mouseX, mouseY);
        renderItemPicker(gg, mouseX, mouseY);
        renderEmojiButtonIcon(gg);
        renderEmojiPicker(gg, mouseX, mouseY);

        if (!statusMessage.isBlank()) {
            int msgW = font.width(statusMessage);
            int msgX = (this.width - msgW) / 2;
            gg.drawString(font, statusMessage, msgX, topY + panelHeight + 8, statusColor, false);
        }
        renderSavedState(gg);
        renderIdSuggestions(gg, mouseX, mouseY);
        renderUnsavedChangesPopup(gg, mouseX, mouseY);
        if (!hasForegroundOverlay()) {
            if (leftList != null) leftList.renderHoverTooltipOnTop(gg);
            renderTabTooltip(gg, mouseX, mouseY);
            renderPendingEditorTooltip(gg);
        } else {
            pendingEditorTooltip = List.of();
        }
        gg.pose().popPose();
    }

    protected void queueEditorTooltip(Component tooltip, int mouseX, int mouseY) {
        // keep only the latest tooltip requested by the topmost control
        if (tooltip == null || tooltip.getString().isBlank()) return;
        queueEditorTooltip(List.of(tooltip), mouseX, mouseY);
    }

    protected void queueEditorTooltip(List<Component> tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.isEmpty()) return;
        List<Component> wrapped = new ArrayList<>();
        for (Component line : tooltip) {
            String remaining = line.getString();
            while (!remaining.isBlank()) {
                String part = font.plainSubstrByWidth(remaining, 160);
                int split = part.length() < remaining.length() ? part.lastIndexOf(' ') : -1;
                if (split > 0) part = part.substring(0, split);
                if (part.isBlank()) part = remaining.substring(0, Math.min(1, remaining.length()));
                wrapped.add(Component.literal(part));
                remaining = remaining.substring(part.length()).trim();
            }
        }
        pendingEditorTooltip = wrapped;
        pendingEditorTooltipX = mouseX;
        pendingEditorTooltipY = mouseY;
    }

    protected void renderPendingEditorTooltip(GuiGraphics gg) {
        // tooltips render last so popups and controls never cover them
        if (pendingEditorTooltip == null || pendingEditorTooltip.isEmpty()) return;
        gg.pose().pushPose();
        gg.pose().translate(0f, 0f, 2000f);
        QuestEditorTooltips.renderComponents(gg, font, pendingEditorTooltip, pendingEditorTooltipX, pendingEditorTooltipY);
        gg.pose().popPose();
        pendingEditorTooltip = List.of();
    }

    protected void renderListPackBackground(GuiGraphics graphics) {
        // draw the active category background behind list content
        if (currentPack == null || isTreePack() || !isListSearchMode() || leftList == null || !leftList.visible) return;
        String categoryId = listBackgroundCategoryId();
        CategoryData category = categoryId.isBlank() ? null : categoryData(categoryId);
        if (category == null) {
            List<NamedEntry> categories = categoryEntries();
            if (!categories.isEmpty()) category = categoryData(categories.getFirst().id);
        }
        String background = category == null ? "" : category.background;
        float tileScale = category == null ? 0.5f : parseFloat(category.backgroundTileScale, 0.5f);
        boolean darkening = category != null && parseBool(category.backgroundDarkening, false);
        float opacity = category == null ? 0f : parseFloat(category.backgroundDarkeningOpacity, 0.45f);
        if (editorType == EditorType.CATEGORY && catIdBox != null && categoryId.equals(safe(catIdBox.getValue()).trim())) {
            background = encodedCategoryBackground();
            tileScale = catBackgroundScaleSlider == null ? 0.5f : catBackgroundScaleSlider.getScale();
            opacity = catBackgroundOpacitySlider == null ? 0f : catBackgroundOpacitySlider.getOpacity();
            darkening = opacity > 0f;
        }
        QuestCategoryBackgroundRenderer.render(graphics, background, darkening, opacity, tileScale,
                leftX + 8, topY + 8, panelWidth - 17, panelHeight - 17);
    }

    protected String listBackgroundCategoryId() {
        if (editorType == EditorType.CATEGORY && catIdBox != null) return safe(catIdBox.getValue()).trim();
        if (mode == Mode.QUEST_LIST && !treeCategoryId.isBlank()) return treeCategoryId;
        if (editorType == EditorType.SUBCATEGORY && subCategoryBox != null) return dropdownBoxValue(subCategoryBox);
        if (editorType == EditorType.QUEST && questCategoryBox != null) return dropdownBoxValue(questCategoryBox);
        if (currentPack == null || selectedEntryId.isBlank() || ENTRY_NEW.equals(selectedEntryId)) return "";
        if (mode == Mode.CATEGORY_LIST) return selectedEntryId;
        if (mode == Mode.SUBCATEGORY_LIST) {
            SubCategoryData subCategory = QuestEditorJsonFiles.loadSubCategory(currentPack, selectedEntryId);
            return subCategory == null ? "" : safe(subCategory.category).trim();
        }
        if (mode == Mode.QUEST_LIST) {
            QuestEntryData quest = questListIndex().questDataByEntryId.get(selectedEntryId);
            return quest == null ? "" : safe(quest.category).trim();
        }
        return "";
    }

    protected void updateLeftPaneLayout() {
        // keep list, tree, tabs, and hitboxes on one shared layout pass
        if (leftList != null) {
            leftList.setBounds(pxLeft, py, leftPw, listH);
        }
        boolean showTree = mode == Mode.QUEST_LIST && isTreePack();
        if (questTree != null) {
            questTree.setX(leftX + 8);
            questTree.setY(topY + 8);
            questTree.setWidth(panelWidth - 17);
            questTree.setHeight(panelHeight - 17);
            questTree.visible = showTree;
            questTree.active = showTree;
            if (questTree.tool() != treeTool) questTree.setTool(treeTool);
        }
        if (leftList != null) {
            leftList.visible = !showTree;
            leftList.active = !showTree;
        }
        if (backButton != null) {
            backButton.setX(leftX - TAB_W + BACK_TAB_X_OFFSET);
            backButton.setY(topY + panelHeight - BACK_TAB_H - BACK_TAB_BOTTOM_MARGIN);
        }
        int barY = py + ph + (BOTTOM_BAR_H - 20) / 2;
        if (saveButton != null) {
            saveButton.setX(editorType == EditorType.PACK_CREATE ? pxRight + (pw - saveButton.getWidth()) / 2 : pxRight + pw - 68);
            saveButton.setY(barY);
        }
        if (createPackButton != null) {
            createPackButton.setX(pxLeft + 2);
            createPackButton.setY(barY);
            createPackButton.setWidth(leftPw - SMALL_BTN_SIZE - SMALL_BTN_GAP - 4);
            createPackButton.visible = mode == Mode.PACK_LIST;
            createPackButton.active = mode == Mode.PACK_LIST;
        }
        if (importQuestPackButton != null && createPackButton != null) {
            importQuestPackButton.setX(createPackButton.getX() + createPackButton.getWidth() + 2);
            importQuestPackButton.setY(createPackButton.getY());
            importQuestPackButton.visible = mode == Mode.PACK_LIST;
            importQuestPackButton.active = mode == Mode.PACK_LIST;
        }
        boolean createEntryVisible = canCreateEntryInCurrentMode();
        if (createEntryButton != null) {
            createEntryButton.setX(isTreePack()
                    ? pxLeft + 2 + SMALL_BTN_SIZE + SMALL_BTN_GAP
                    : pxLeft + 2);
            createEntryButton.setY(barY);
            createEntryButton.visible = createEntryVisible;
            createEntryButton.active = createEntryVisible;
        }
        if (treeMoveButton != null) {
            treeMoveButton.setX(pxLeft + 2);
            treeMoveButton.setY(barY);
            treeMoveButton.visible = showTree;
            treeMoveButton.active = showTree;
        }
        if (treeDeleteButton != null) {
            treeDeleteButton.setX(pxLeft + 2 + (SMALL_BTN_SIZE + SMALL_BTN_GAP) * 2);
            treeDeleteButton.setY(barY);
            treeDeleteButton.visible = showTree;
            treeDeleteButton.active = showTree;
        }
        if (treeConnectorButton != null) {
            treeConnectorButton.setX(pxLeft + 2 + (SMALL_BTN_SIZE + SMALL_BTN_GAP) * 3);
            treeConnectorButton.setY(barY);
            treeConnectorButton.visible = showTree;
            treeConnectorButton.active = showTree;
        }
        if (treeExpandButton != null) {
            treeExpandButton.setX(questTree.getX() + questTree.getWidth() - 40);
            treeExpandButton.setY(questTree.getY() + 3);
            boolean showExpand = showTree && Config.expandTreeToggle();
            treeExpandButton.visible = showExpand;
            treeExpandButton.active = showExpand;
        }
        if (originButton != null) {
            originButton.setX(pxRight + pw - HEADER_ICON_SIZE - 2);
            originButton.setTextures(questTreeOrigin ? ORIGIN_HOVER_TEX : ORIGIN_TEX, ORIGIN_HOVER_TEX);
        }
        if (questSearchBox != null) {
            boolean showQuestSearch = isListSearchMode() && currentPack != null && !isTreePack();
            int searchX = pxLeft + 2 + SMALL_BTN_SIZE + SMALL_BTN_GAP;
            questSearchBox.setX(searchX);
            questSearchBox.setY(barY);
            questSearchBox.setWidth(leftPw - 4 - SMALL_BTN_SIZE - SMALL_BTN_GAP);
            questSearchBox.visible = showQuestSearch;
            questSearchBox.active = showQuestSearch;
            questSearchBox.setHint(tr("search"));
        }
        layoutTabButtons();
    }

    protected boolean canCreateEntryInCurrentMode() {
        return mode == Mode.CATEGORY_LIST || mode == Mode.SUBCATEGORY_LIST || mode == Mode.QUEST_LIST;
    }

    protected void layoutTabButtons() {
        layoutTabButton(categoriesTabButton, 0, 0);
        layoutTabButton(subCategoriesTabButton, 0, 0);
        layoutTabButton(questsTabButton, 0, 0);
    }

    protected boolean isTreeTooltipCovered(int mouseX, int mouseY) {
        // suppress tree tooltips under panels menus and picker overlays
        if (isHovered(treeMoveButton, mouseX, mouseY)
                || isHovered(createEntryButton, mouseX, mouseY)
                || isHovered(treeDeleteButton, mouseX, mouseY)
                || isHovered(treeConnectorButton, mouseX, mouseY)
                || isHovered(treeExpandButton, mouseX, mouseY)) return true;
        for (CategoryWorkspaceTab tab : categoryWorkspaceTabs) {
            if (isHovered(tab, mouseX, mouseY)) return true;
        }
        if (isHovered(categoryWorkspaceUpButton, mouseX, mouseY)
                || isHovered(categoryWorkspaceDownButton, mouseX, mouseY)) return true;
        return false;
    }

    protected boolean isHovered(AbstractWidget widget, double mouseX, double mouseY) {
        return widget != null && widget.visible && widget.isMouseOver(mouseX, mouseY);
    }

    protected void layoutTabButton(EditorTabButton button, int x, int y) {
        if (button == null) return;
        button.setX(x);
        button.setY(y);
        button.visible = false;
        button.active = false;
    }

    protected void renderSideTabs(GuiGraphics gg, int mouseX, int mouseY) {
        // side tabs render outside the clipped panel content
        if (categoriesTabButton != null && categoriesTabButton.visible) categoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible) subCategoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (questsTabButton != null && questsTabButton.visible) questsTabButton.render(gg, mouseX, mouseY, 0f);
    }

    protected void renderTabTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        // prefer the active topmost tab when hitboxes touch
        boolean directTooltipHovered = isHovered(categoriesTabButton, mouseX, mouseY)
                || isHovered(subCategoriesTabButton, mouseX, mouseY)
                || isHovered(questsTabButton, mouseX, mouseY)
                || isHovered(importQuestPackButton, mouseX, mouseY)
                || isHovered(createEntryButton, mouseX, mouseY)
                || isHovered(treeMoveButton, mouseX, mouseY)
                || isHovered(treeDeleteButton, mouseX, mouseY)
                || isHovered(treeConnectorButton, mouseX, mouseY)
                || isHovered(originButton, mouseX, mouseY)
                || isHovered(catLayoutSelector, mouseX, mouseY)
                || isHovered(duplicateButton, mouseX, mouseY)
                || isHovered(deleteQuestButton, mouseX, mouseY)
                || isHovered(backButton, mouseX, mouseY);
        if (directTooltipHovered) pendingEditorTooltip = List.of();
        if (categoriesTabButton != null && categoriesTabButton.visible && categoriesTabButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(Component.literal(categoriesTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible && subCategoriesTabButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(Component.literal(subCategoriesTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (questsTabButton != null && questsTabButton.visible && questsTabButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(Component.literal(questsTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (importQuestPackButton != null && importQuestPackButton.visible && importQuestPackButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(tr("tooltip.import_questpacks"), mouseX, mouseY);
            return;
        }
        if (createEntryButton != null && createEntryButton.visible && createEntryButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(createEntryTooltip(), mouseX, mouseY);
            return;
        }
        if (treeMoveButton != null && treeMoveButton.visible && treeMoveButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(tr("tooltip.tree_move"), mouseX, mouseY);
            return;
        }
        if (treeDeleteButton != null && treeDeleteButton.visible && treeDeleteButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(tr("tooltip.tree_delete"), mouseX, mouseY);
            return;
        }
        if (treeConnectorButton != null && treeConnectorButton.visible && treeConnectorButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(tr("tooltip.tree_connect"), mouseX, mouseY);
            return;
        }
        if (originButton != null && originButton.visible && originButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(tr(questTreeOrigin ? "tooltip.tree_origin_selected" : "tooltip.tree_origin"), mouseX, mouseY);
            return;
        }
        if (catLayoutSelector != null && catLayoutSelector.visible && catLayoutSelector.isMouseOver(mouseX, mouseY)) {
            Component tooltip = catLayoutSelector.hoveredTooltip(mouseX, mouseY);
            if (tooltip != null) queueEditorTooltip(tooltip, mouseX, mouseY);
            return;
        }
        if (duplicateButton != null && duplicateButton.visible && duplicateButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(tr("tooltip.duplicate"), mouseX, mouseY);
            return;
        }
        if (deleteQuestButton != null && deleteQuestButton.visible && deleteQuestButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(deleteConfirmArmed ? tr("tooltip.hold_delete") : tr("tooltip.delete"), mouseX, mouseY);
            return;
        }
        if (backButton != null && backButton.visible && backButton.isMouseOver(mouseX, mouseY)) {
            queueEditorTooltip(tr("tooltip.back"), mouseX, mouseY);
        }
    }

    protected boolean hasForegroundOverlay() {
        // foreground menus block controls and tooltips underneath
        return isUnsavedChangesPopupOpen() || isItemPickerOpen() || popupMenus.hasOpenMenu()
                || emojiPickerOpen || suggestionBounds() != null;
    }

    protected boolean isUnsavedChangesPopupOpen() {
        return pendingTreeDeleteAction != null
                || pendingEditorExit
                || pendingBackNavigation
                || (pendingDiscardMode != null && !pendingDiscardEntryId.isBlank());
    }

    protected void renderUnsavedChangesPopup(GuiGraphics gg, int mouseX, int mouseY) {
        // the modal backdrop renders before its panel and actions
        if (!isUnsavedChangesPopupOpen()) return;
        boolean confirmingTreeDelete = pendingTreeDeleteAction != null;
        String message = confirmingTreeDelete ? pendingTreeDeleteMessage : trs("popup.unsaved_changes");
        String save = confirmingTreeDelete ? "Delete" : trs("popup.save");
        String discard = confirmingTreeDelete ? "Cancel" : trs("popup.discard");
        int popupW = Math.max(116, font.width(message) + 22);
        int popupH = 48;
        int x = (this.width - popupW) / 2;
        int y = (this.height - popupH) / 2;
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 700);
        gg.fill(0, 0, this.width, this.height, 0x70000000);
        renderNineSlice(gg, UNSAVED_POPUP_BG_TEX, x, y, popupW, popupH, UNSAVED_POPUP_TEX_SIZE, UNSAVED_POPUP_TEX_SIZE, UNSAVED_POPUP_SLICE);
        int messageW = font.width(message);
        int messageGroupW = UNSAVED_EXCLAMATION_W + UNSAVED_EXCLAMATION_GAP + messageW;
        int messageX = x + (popupW - messageGroupW) / 2 + UNSAVED_EXCLAMATION_W + UNSAVED_EXCLAMATION_GAP;
        int iconX = messageX - UNSAVED_EXCLAMATION_GAP - UNSAVED_EXCLAMATION_W;
        int messageY = y + 10;
        int iconY = messageY + (font.lineHeight - UNSAVED_EXCLAMATION_H) / 2;
        gg.blit(UNSAVED_EXCLAMATION_TEX, iconX, iconY, 0, 0, UNSAVED_EXCLAMATION_W, UNSAVED_EXCLAMATION_H, UNSAVED_EXCLAMATION_W, UNSAVED_EXCLAMATION_H);
        gg.drawString(font, message, messageX, messageY, 0x000000, false);

        int gap = 18;
        int saveX = x + popupW / 2 - font.width(save) - gap / 2;
        int discardX = x + popupW / 2 + gap / 2;
        int actionY = y + 30;
        int saveColor = isInsideText(mouseX, mouseY, saveX, actionY, save) ? 0x208020 : 0x000000;
        int discardColor = isInsideText(mouseX, mouseY, discardX, actionY, discard) ? 0xA02020 : 0x000000;
        gg.drawString(font, save, saveX, actionY, saveColor, false);
        gg.drawString(font, discard, discardX, actionY, discardColor, false);
        gg.pose().popPose();
    }

    protected void renderNineSlice(GuiGraphics gg, ResourceLocation texture, int x, int y, int width, int height, int texW, int texH, int slice) {
        // keep all four corners fixed and stretch the middle slices
        int centerTexW = texW - slice * 2;
        int centerTexH = texH - slice * 2;
        int centerW = Math.max(0, width - slice * 2);
        int centerH = Math.max(0, height - slice * 2);

        gg.blit(texture, x, y, 0, 0, slice, slice, texW, texH);
        gg.blit(texture, x + slice + centerW, y, texW - slice, 0, slice, slice, texW, texH);
        gg.blit(texture, x, y + slice + centerH, 0, texH - slice, slice, slice, texW, texH);
        gg.blit(texture, x + slice + centerW, y + slice + centerH, texW - slice, texH - slice, slice, slice, texW, texH);

        if (centerW > 0) {
            gg.blit(texture, x + slice, y, centerW, slice, slice, 0, centerTexW, slice, texW, texH);
            gg.blit(texture, x + slice, y + slice + centerH, centerW, slice, slice, texH - slice, centerTexW, slice, texW, texH);
        }
        if (centerH > 0) {
            gg.blit(texture, x, y + slice, slice, centerH, 0, slice, slice, centerTexH, texW, texH);
            gg.blit(texture, x + slice + centerW, y + slice, slice, centerH, texW - slice, slice, slice, centerTexH, texW, texH);
        }
        if (centerW > 0 && centerH > 0) {
            gg.blit(texture, x + slice, y + slice, centerW, centerH, slice, slice, centerTexW, centerTexH, texW, texH);
        }
    }

    protected boolean isInsideText(double mouseX, double mouseY, int x, int y, String text) {
        return mouseX >= x && mouseX <= x + font.width(text) && mouseY >= y && mouseY <= y + font.lineHeight;
    }

    protected void renderPanelHeader(GuiGraphics gg, int panelX, String title) {
        // centre the title over the repeatable three slice header
        String rawText = safe(title);
        if (rawText.isBlank()) return;
        Component text = QuestTextFormatting.component(rawText, 0x404040);
        int textW = font.width(text);
        int headerW = Math.max(22, textW + 10);
        int x = panelX + 5;
        int y = topY - 7;
        renderThreeSliceHeader(gg, x, y, headerW);
        gg.drawString(font, text, x + (headerW - textW) / 2, y + 4, 0x404040, false);
    }

    protected void renderThreeSliceHeader(GuiGraphics gg, int x, int y, int width) {
        // keep both fixed ends and repeat the centre section
        int middleW = Math.max(0, width - HEADER_SLICE * 2);
        gg.blit(HEADER_TEX, x, y, 0, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
        if (middleW > 0) {
            for (int i = 0; i < middleW; i++) {
                gg.blit(HEADER_TEX, x + HEADER_SLICE + i, y, HEADER_SLICE, 0, 1, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
            }
        }
        gg.blit(HEADER_TEX, x + HEADER_SLICE + middleW, y, HEADER_TEX_W - HEADER_SLICE, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
    }

    protected String panelHeaderTitle() {
        return switch (mode) {
            case PACK_LIST -> trs("header.quest_packs");
            case PACK_CREATE -> trs("header.create_pack");
            case CATEGORY_LIST -> trs("header.categories");
            case SUBCATEGORY_LIST -> trs("header.subcategories");
            case QUEST_LIST -> {
                CategoryData category = categoryData(treeCategoryId);
                yield category == null || safe(category.name).isBlank() ? trs("header.categories") : category.name;
            }
            case PACK_MENU -> trs("header.quest_packs");
        };
    }

    protected void renderSavedState(GuiGraphics gg) {
        // show save state beside the editor header without shifting fields
        if (editorType == EditorType.NONE || saveButton == null || !saveButton.visible) return;
        String text = hasUnsavedEditorChanges() ? trs("state.unsaved") : trs("state.saved");
        int color = hasUnsavedEditorChanges() ? 0x7A5A20 : 0x4F7A4F;
        int textW = font.width(text);
        int headerW = Math.max(22, textW + 10);
        int x = pxRight + pw - headerW - 2;
        int y = py - font.lineHeight - 8;
        renderThreeSliceHeader(gg, x, y, headerW);
        gg.drawString(font, text, x + (headerW - textW) / 2, y + 4, color, false);
    }

    protected void renderIconOverlays(GuiGraphics gg) {
        // draw item icons after their text fields so previews stay visible
        int clipLeft = pxRight + 2;
        int clipRight = pxRight + pw - 2;
        int clipTop = py;
        int clipBottom = py + ph;

        gg.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        renderIconOverlay(gg, packIconPathBox);
        renderIconOverlay(gg, questIconBox);
        renderIconOverlay(gg, catIconBox);
        renderIconOverlay(gg, catBackgroundBox);
        renderIconOverlay(gg, subIconBox);
        // no extra reward fields
        gg.disableScissor();
    }

    protected void renderIconOverlay(GuiGraphics gg, EditBox box) {
        if (box == null || !box.visible) return;

        if (box.getWidth() <= 20) {
            gg.fill(box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(), 0xFF808080);
            gg.fill(box.getX() + 1, box.getY() + 1,
                    box.getX() + box.getWidth() - 1, box.getY() + box.getHeight() - 1, 0xFF000000);
        }

        if (box == catBackgroundBox) {
            renderCategoryBackgroundPreview(gg, box);
            return;
        }

        if (QuestEditorItemIcons.hasRenderableIcon(box.getValue())) {
            int iconX = box.getWidth() <= 20 ? box.getX() + 2 : box.getX() + box.getWidth() - 18;
            int iconY = box.getY() + (box.getHeight() - 16) / 2;
            QuestEditorItemIcons.renderIcon(gg, box.getValue(), iconX, iconY);
        }

        if (!box.isFocused()) return;
        String value = box.getValue();
        if (value == null || value.isBlank()) return;

        String suggestion = suggestionCaches.computeIconSuggestion(value);
        if (suggestion == null || suggestion.isBlank()) return;
        String valueLower = value.toLowerCase(Locale.ROOT);
        String suggestionLower = suggestion.toLowerCase(Locale.ROOT);
        if (!suggestionLower.startsWith(valueLower)) return;
        if (suggestion.length() <= value.length()) return;

        String remainder = suggestion.substring(value.length());
        int textX = box.getX() + 4 + font.width(value);
        int textY = box.getY() + (box.getHeight() - font.lineHeight) / 2 + 1;
        gg.drawString(font, remainder, textX, textY, 0x808080, false);
    }

    protected void renderIdSuggestions(GuiGraphics gg, int mouseX, int mouseY) {
        // render suggestions above fields but below modal overlays
        updateIdSuggestions();
        if (activeIdSuggestions.isEmpty()) return;
        QuestEditorSuggestionRenderer.render(new QuestEditorSuggestionRenderer.RenderRequest(
                gg,
                font,
                activeIdSuggestions,
                idSuggestionScroll,
                suggestionBounds(),
                pxRight + 1,
                py + 1,
                pxRight + pw - 1,
                py + ph - 1
        ));
    }

    protected void updateIdSuggestions() {
        // suggestions rebuild only when focus or field text changes
        EditBox focused = focusedIdSuggestionField();
        ScaledMultiLineEditBox focusedMulti = focusedMultiIdSuggestionField();
        if (suppressIdSuggestions || (focused == null && focusedMulti == null)) {
            clearIdSuggestions();
            return;
        }
        Object source = focused != null ? focused : focusedMulti;
        String prefix = focused != null
                ? idSuggestionPrefix(focused).toLowerCase(Locale.ROOT)
                : multiLineSuggestionPrefix(focusedMulti).toLowerCase(Locale.ROOT);
        if (!idSuggestionsDirty
                && Objects.equals(lastIdSuggestionSource, source)
                && Objects.equals(lastIdSuggestionPrefix, prefix)) {
            return;
        }

        idSuggestionField = focused;
        idSuggestionMultiLineField = focusedMulti;
        lastIdSuggestionSource = source;
        lastIdSuggestionPrefix = prefix;
        idSuggestionsDirty = false;
        activeIdSuggestions.clear();

        List<String> all = focused != null ? idSuggestionValuesForField(focused) : idSuggestionValuesForMultiField(focusedMulti);
        if (all.isEmpty()) return;

        for (String id : all) {
            if (id == null || id.isBlank()) continue;
            if (!prefix.isBlank() && !matchesIdPrefix(id, prefix)) continue;
            activeIdSuggestions.add(id);
            if (activeIdSuggestions.size() >= ID_SUGGESTION_MAX) break;
        }
        idSuggestionScroll = Mth.clamp(idSuggestionScroll, 0, Math.max(0, activeIdSuggestions.size() - ID_SUGGESTION_VISIBLE_ROWS));
    }

    protected void clearIdSuggestions() {
        idSuggestionField = null;
        idSuggestionMultiLineField = null;
        lastIdSuggestionSource = null;
        lastIdSuggestionPrefix = "";
        idSuggestionsDirty = true;
        activeIdSuggestions.clear();
    }

    protected void invalidateIdSuggestions() {
        idSuggestionsDirty = true;
    }

    protected EditBox focusedIdSuggestionField() {
        if (packIconPathBox != null && packIconPathBox.visible && packIconPathBox.isFocused()) return packIconPathBox;
        if (questIconBox != null && questIconBox.visible && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.visible && catIconBox.isFocused()) return catIconBox;
        if (catBackgroundBox != null && catBackgroundBox.visible && catBackgroundBox.isFocused()) return catBackgroundBox;
        if (subIconBox != null && subIconBox.visible && subIconBox.isFocused()) return subIconBox;
        if (catDependencyBox != null && catDependencyBox.visible && catDependencyBox.isFocused()) return catDependencyBox;
        return null;
    }

    protected ScaledMultiLineEditBox focusedMultiIdSuggestionField() {
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box.visible && box.isFocused()) return box;
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box.visible && box.isFocused()) return box;
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box.visible && box.isFocused()) return box;
        }
        if (questCompletionBox != null && questCompletionBox.visible && questCompletionBox.isFocused()) return questCompletionBox;
        if (questRewardBox != null && questRewardBox.visible && questRewardBox.isFocused()) return questRewardBox;
        return null;
    }

    protected String idSuggestionPrefix(EditBox field) {
        if (field == null) return "";
        return safe(field.getValue()).trim();
    }

    protected List<String> idSuggestionValuesForField(EditBox field) {
        // choose the smallest cache that matches the focused field
        if (field == null) return List.of();
        if (isIconBox(field)) {
            return suggestionCaches.itemSuggestions();
        } else if (field == catDependencyBox) {
            return questSuggestionCache;
        } else if (field == subCategoryBox || field == questCategoryBox) {
            return categorySuggestionCache;
        } else if (field == questSubCategoryBox) {
            String cat = dropdownBoxValue(questCategoryBox);
            if (!cat.isBlank()) {
                List<String> scoped = subCategoryByCategorySuggestion.get(cat.toLowerCase(Locale.ROOT));
                if (scoped != null && !scoped.isEmpty()) return scoped;
            }
            return subCategorySuggestionCache;
        }
        return List.of();
    }

    protected List<String> idSuggestionValuesForMultiField(ScaledMultiLineEditBox field) {
        // choose suggestions from the active repeatable row type
        if (field == null) return List.of();
        if (isDependencyEntryField(field)) {
            return questPackDependencySuggestionCache;
        }
        EntryRowKind rowKind = rowKindForField(field);
        if ((rowKind == EntryRowKind.COMPLETION || rowKind == EntryRowKind.REWARD)
                && QuestEditorEntryTypes.hasRowBrowser(rowKind, effectiveRowType(rowKind, field))) {
            return List.of();
        }
        MultiLineEntryContext ctx = parseMultiLineEntryContext(field);
        if (ctx == null) return List.of();
        if (!ctx.hasTypeSeparator) {
            return isCompletionEntryField(field)
                ? (LevelUpCompat.isAvailable()
                        ? List.of("collect", "submit", "kill", "achieve", "effect", "observe", "check", "biome", "dimension", "xp", "levelup", "field")
                        : List.of("collect", "submit", "kill", "achieve", "effect", "observe", "check", "biome", "dimension", "xp", "field"))
                    : (LevelUpCompat.isAvailable()
                        ? List.of("item", "xp", "levelup", "command", "loot", "advancement", "toast")
                        : List.of("item", "xp", "command", "loot", "advancement", "toast"));
        }
        return switch (ctx.type) {
            case "collect", "submit", "item" -> suggestionCaches.itemSuggestions();
            case "kill", "entity" -> suggestionCaches.entitySuggestions();
            case "effect" -> suggestionCaches.effectSuggestions();
            case "achieve", "advancement" -> suggestionCaches.advancementSuggestions();
            case "observe" -> suggestionCaches.observeSuggestions();
            case "check" -> List.of("understand");
            case "biome" -> suggestionCaches.biomeSuggestions();
            case "dimension" -> suggestionCaches.dimensionSuggestions();
            case "loot", "loottable" -> suggestionCaches.lootTableSuggestions(currentPack);
            case "xp", "exp" -> List.of("points", "levels");
            case "levelup" -> LevelUpCompat.isAvailable() ? List.of("xp", "levels") : List.of();
            case "field", "input" -> List.of("\"expected text\" \"input hint text\"");
            case "toast" -> List.of("\"Toast title\" \"Toast description\" minecraft:paper");
            case "icon" -> suggestionCaches.itemSuggestions();
            default -> List.of();
        };
    }

    protected boolean isCompletionEntryField(ScaledMultiLineEditBox field) {
        return field == questCompletionBox || completionEntryBoxes.contains(field);
    }

    protected boolean isRewardEntryField(ScaledMultiLineEditBox field) {
        return field == questRewardBox || rewardEntryBoxes.contains(field);
    }

    protected boolean isDependencyEntryField(ScaledMultiLineEditBox field) {
        return field == questDependenciesBox || dependencyEntryBoxes.contains(field);
    }

    protected EntryRowKind rowKindForField(ScaledMultiLineEditBox field) {
        if (field == null) return null;
        if (completionEntryBoxes.contains(field)) return EntryRowKind.COMPLETION;
        if (rewardEntryBoxes.contains(field)) return EntryRowKind.REWARD;
        if (dependencyEntryBoxes.contains(field)) return EntryRowKind.DEPENDENCY;
        return null;
    }

    protected String multiLineSuggestionPrefix(ScaledMultiLineEditBox field) {
        if (field == null) return "";
        if (isDependencyEntryField(field)) {
            String value = safe(field.getValue());
            if (value.isEmpty()) return "";
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), value.length()));
            int lineStart = cursor <= 0 ? 0 : value.lastIndexOf('\n', Math.max(0, cursor - 1));
            lineStart = lineStart < 0 ? 0 : lineStart + 1;
            int lineEnd = value.indexOf('\n', cursor);
            if (lineEnd < 0) lineEnd = value.length();
            lineStart = Math.max(0, Math.min(lineStart, value.length()));
            lineEnd = Math.max(lineStart, Math.min(lineEnd, value.length()));
            if (lineStart >= value.length()) return "";
            return value.substring(lineStart, lineEnd).trim().toLowerCase(Locale.ROOT);
        }
        MultiLineEntryContext ctx = parseMultiLineEntryContext(field);
        if (ctx == null) return "";
        return ctx.hasTypeSeparator ? ctx.idPrefix : ctx.typePrefix;
    }

    protected boolean matchesIdPrefix(String suggestion, String prefix) {
        if (suggestion == null || prefix == null) return false;
        String lowSuggestion = suggestion.toLowerCase(Locale.ROOT);
        String lowPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowSuggestion.startsWith(lowPrefix)) return true;
        int colon = lowSuggestion.indexOf(':');
        return colon >= 0 && colon + 1 < lowSuggestion.length()
                && lowSuggestion.substring(colon + 1).startsWith(lowPrefix);
    }

    protected boolean clickIdSuggestion(double mouseX, double mouseY) {
        // apply the clicked suggestion to its original focused field
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null || activeIdSuggestions.isEmpty()) return false;
        if (mouseX < bounds.x || mouseX > bounds.x + bounds.w || mouseY < bounds.y || mouseY > bounds.y + bounds.h) return false;
        int idx = (int) ((mouseY - bounds.y) / ID_SUGGESTION_ROW_H) + idSuggestionScroll;
        if (idx < 0 || idx >= activeIdSuggestions.size()) return false;
        if (idSuggestionField != null) {
            applyIdSuggestion(idSuggestionField, activeIdSuggestions.get(idx));
        } else if (idSuggestionMultiLineField != null) {
            applyMultiLineIdSuggestion(idSuggestionMultiLineField, activeIdSuggestions.get(idx));
        }
        return true;
    }

    protected boolean scrollIdSuggestions(double mouseX, double mouseY, double scrollY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null || activeIdSuggestions.isEmpty()) return false;
        if (mouseX < bounds.x || mouseX > bounds.x + bounds.w || mouseY < bounds.y || mouseY > bounds.y + bounds.h) return false;
        int max = Math.max(0, activeIdSuggestions.size() - ID_SUGGESTION_VISIBLE_ROWS);
        if (max <= 0) return true;
        int next = idSuggestionScroll - (int) Math.signum(scrollY);
        idSuggestionScroll = Mth.clamp(next, 0, max);
        return true;
    }

    protected SuggestionBounds suggestionBounds() {
        if ((idSuggestionField == null && idSuggestionMultiLineField == null) || activeIdSuggestions.isEmpty()) return null;
        int x = idSuggestionField != null ? idSuggestionField.getX() : idSuggestionMultiLineField.getX();
        int yBase = idSuggestionField != null ? idSuggestionField.getY() + idSuggestionField.getHeight() + 1
                : idSuggestionMultiLineField.getY() + idSuggestionMultiLineField.getHeight() + 1;
        int w = idSuggestionField != null ? idSuggestionField.getWidth() : idSuggestionMultiLineField.getWidth();
        int rows = Math.min(ID_SUGGESTION_VISIBLE_ROWS, activeIdSuggestions.size());
        int h = rows * ID_SUGGESTION_ROW_H;
        int panelTop = py + 1;
        int panelBottom = py + ph - 1;
        int y = yBase;
        if (y + h > panelBottom) {
            int above = (idSuggestionField != null ? idSuggestionField.getY() : idSuggestionMultiLineField.getY()) - h - 1;
            y = Math.max(panelTop, above);
        }
        return new SuggestionBounds(x, y, w, Math.min(h, Math.max(0, panelBottom - y)));
    }

    protected void applyIdSuggestion(EditBox field, String suggestion) {
        if (field == null || suggestion == null) return;
        field.setValue(suggestion);
        field.setCursorPosition(field.getValue().length());
        field.setHighlightPos(field.getCursorPosition());
        field.setFocused(true);
        updateIdSuggestions();
    }

    protected void applyMultiLineIdSuggestion(ScaledMultiLineEditBox field, String suggestion) {
        // replace only the id portion of the active compact row
        if (isDependencyEntryField(field)) {
            if (field == null || suggestion == null || suggestion.isBlank()) return;
            String value = safe(field.getValue());
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), value.length()));
            int lineStart = cursor <= 0 ? 0 : value.lastIndexOf('\n', Math.max(0, cursor - 1));
            lineStart = lineStart < 0 ? 0 : lineStart + 1;
            int lineEnd = value.indexOf('\n', cursor);
            if (lineEnd < 0) lineEnd = value.length();
            lineStart = Math.max(0, Math.min(lineStart, value.length()));
            lineEnd = Math.max(lineStart, Math.min(lineEnd, value.length()));
            String next = value.substring(0, lineStart) + suggestion + value.substring(lineEnd);
            field.setValue(next);
            field.setCursorPosition(lineStart + suggestion.length());
            field.setFocused(true);
            entryRowsDirty = true;
            syncEntryBackingValues();
            updateIdSuggestions();
            return;
        }
        MultiLineEntryContext ctx = parseMultiLineEntryContext(field);
        if (field == null || ctx == null || suggestion == null || suggestion.isBlank()) return;
        String value = safe(field.getValue());
        String replacement = suggestion;
        if (ctx.hasTypeSeparator && field != null) {
            if (isCompletionEntryField(field) || isRewardEntryField(field)) {
                if (ctx.type.equals("collect") || ctx.type.equals("submit") || ctx.type.equals("item")
                        || ctx.type.equals("kill") || ctx.type.equals("entity") || ctx.type.equals("effect")
                        || ctx.type.equals("achieve") || ctx.type.equals("advancement")
                        || ctx.type.equals("icon")) {
                    replacement = QuestEditorEntryCodec.normalizeNamespacedId(suggestion, false);
                }
            }
        }
        String next;
        int nextCursor;
        if (!ctx.hasTypeSeparator) {
            next = value.substring(0, ctx.typeStart) + replacement + ": " + value.substring(ctx.typeEnd);
            nextCursor = ctx.typeStart + replacement.length() + 2;
        } else {
            next = value.substring(0, ctx.idStart) + replacement + value.substring(ctx.idEnd);
            nextCursor = ctx.idStart + replacement.length();
        }
        field.setValue(next);
        field.setCursorPosition(nextCursor);
        field.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
        updateIdSuggestions();
    }

    protected MultiLineEntryContext parseMultiLineEntryContext(ScaledMultiLineEditBox field) {
        // locate the active value without disturbing row metadata
        if (field == null) return null;
        try {
            String value = safe(field.getValue());
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), value.length()));
            int lineStart = cursor <= 0 ? 0 : value.lastIndexOf('\n', Math.max(0, cursor - 1));
            lineStart = lineStart < 0 ? 0 : lineStart + 1;
            int lineEnd = value.indexOf('\n', cursor);
            if (lineEnd < 0) lineEnd = value.length();
            lineStart = Math.max(0, Math.min(lineStart, value.length()));
            lineEnd = Math.max(lineStart, Math.min(lineEnd, value.length()));
            String line = value.substring(lineStart, lineEnd);
            int localCursor = Math.max(0, Math.min(cursor - lineStart, line.length()));

            int firstNonWs = 0;
            while (firstNonWs < line.length() && Character.isWhitespace(line.charAt(firstNonWs))) firstNonWs++;
            firstNonWs = Math.max(0, Math.min(firstNonWs, line.length()));
            int colon = line.indexOf(':');
            if (colon < 0 || localCursor <= colon) {
                EntryRowKind rowKind = rowKindForField(field);
                if (rowKind != null) {
                    String fixedType = effectiveRowType(rowKind, field);
                    int idStart = lineStart + firstNonWs;
                    int idEnd = lineStart + line.length();
                    int prefixEnd = Math.max(firstNonWs, Math.min(localCursor, line.length()));
                    String idPrefix = line.substring(firstNonWs, prefixEnd).trim().toLowerCase(Locale.ROOT);
                    return new MultiLineEntryContext(true, fixedType, "", -1, -1, idStart, idEnd, idPrefix);
                }
                int prefixEnd = Math.max(firstNonWs, Math.min(localCursor, line.length()));
                String typePrefix = line.substring(firstNonWs, prefixEnd).trim().toLowerCase(Locale.ROOT);
                return new MultiLineEntryContext(false, "", typePrefix, lineStart + firstNonWs, lineStart + prefixEnd, -1, -1, "");
            }

            int safeColon = Math.max(0, Math.min(colon, line.length()));
            int typeStart = Math.min(firstNonWs, safeColon);
            int typeEnd = Math.max(typeStart, safeColon);
            String type = line.substring(typeStart, typeEnd).trim().toLowerCase(Locale.ROOT);
            if (isRewardEntryField(field) && "command".equals(type)) {
                String lineLower = line.toLowerCase(Locale.ROOT);
                int iconKey = lineLower.indexOf("icon:");
                if (iconKey >= 0) {
                    int iconStart = iconKey + "icon:".length();
                    while (iconStart < line.length() && Character.isWhitespace(line.charAt(iconStart))) iconStart++;
                    int iconEnd = iconStart;
                    while (iconEnd < line.length()) {
                        char ch = line.charAt(iconEnd);
                        if (ch == '|' || Character.isWhitespace(ch)) break;
                        iconEnd++;
                    }
                    if (localCursor >= iconStart && localCursor <= iconEnd) {
                        int prefixEnd = Math.max(iconStart, Math.min(localCursor, iconEnd));
                        String idPrefix = line.substring(iconStart, prefixEnd).trim().toLowerCase(Locale.ROOT);
                        return new MultiLineEntryContext(true, "icon", "", -1, -1,
                                lineStart + iconStart, lineStart + iconEnd, idPrefix);
                    }
                }
            }
            int idStartLocal = Math.min(line.length(), safeColon + 1);
            while (idStartLocal < line.length() && Character.isWhitespace(line.charAt(idStartLocal))) idStartLocal++;
            int idEndLocal = idStartLocal;
            while (idEndLocal < line.length() && !Character.isWhitespace(line.charAt(idEndLocal))) idEndLocal++;
            int prefixEnd = Math.max(idStartLocal, Math.min(localCursor, idEndLocal));
            String idPrefix = line.substring(idStartLocal, prefixEnd).trim().toLowerCase(Locale.ROOT);
            return new MultiLineEntryContext(true, type, "", -1, -1, lineStart + idStartLocal, lineStart + idEndLocal, idPrefix);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    protected EditBox embeddedIconBoxForDisplayField(AbstractWidget widget) {
        if (widget == packNameBox) return packIconPathBox;
        if (widget == catNameBox) return catIconBox;
        if (widget == subNameBox) return subIconBox;
        if (widget == questNameBox) return questIconBox;
        return null;
    }

    protected void renderEditorFields(GuiGraphics gg, int mouseX, int mouseY) {
        // field layout and clipping share this single ordered pass
        backupRowHitboxes.clear();
        if (catBackgroundModeButton != null) {
            catBackgroundModeButton.visible = false;
            catBackgroundModeButton.active = false;
        }
        if (catBackgroundAssetButton != null) {
            catBackgroundAssetButton.visible = false;
            catBackgroundAssetButton.active = false;
        }
        if (entryRowsDirty) {
            normalizeEntryRows(EntryRowKind.DEPENDENCY);
            normalizeEntryRows(EntryRowKind.COMPLETION);
            normalizeEntryRows(EntryRowKind.REWARD);
            syncEntryBackingValues();
            entryRowsDirty = false;
        }
        updateEntryRowFocusDisplays(EntryRowKind.COMPLETION);
        updateEntryRowFocusDisplays(EntryRowKind.REWARD);
        updateDynamicFieldSizes();
        updateInvalidFieldStyles();
        int contentHeight = contentHeight();
        int maxScroll = Math.max(0, contentHeight - ph);
        editorScroll = Math.max(0f, Math.min(editorScroll, maxScroll));

        int clipTop = py;
        int clipBottom = py + ph - 2;

        int yCursor = py - (int) editorScroll;
        if (originButton != null) {
            int originY = yCursor + 1;
            originButton.setX(pxRight + pw - HEADER_ICON_SIZE - 2);
            originButton.setY(originY);
            boolean showOrigin = editorType == EditorType.QUEST && isTreePack()
                    && originY + originButton.getHeight() > clipTop && originY < clipBottom;
            originButton.visible = showOrigin;
            originButton.active = showOrigin;
        }
        for (FormField field : activeFields) {
            int labelY = yCursor;
            int boxY = labelY + font.lineHeight + FIELD_LABEL_GAP;
            int boxX = pxRight + 2;
            int widgetWidth = pw - 4;
            boolean dependencyEntriesField = field.widget == questDependenciesBox;
            boolean completionEntriesField = field.widget == questCompletionBox;
            boolean rewardEntriesField = field.widget == questRewardBox;
            int widgetHeight;

            if (!dependencyEntriesField && !completionEntriesField && !rewardEntriesField) {
                field.widget.setX(boxX);
                field.widget.setY(boxY);
                EditBox embeddedIcon = embeddedIconBoxForDisplayField(field.widget);
                if (field.widget instanceof EditBox eb) {
                    if (embeddedIcon != null) {
                        eb.setX(boxX + 22);
                        eb.setWidth(Math.max(10, widgetWidth - 22));
                    } else if (eb instanceof IconPickerBox) {
                        eb.setWidth(20);
                    } else {
                        eb.setWidth(widgetWidth);
                    }
                } else if (field.widget instanceof ScaledMultiLineEditBox mb) {
                    mb.setWidth(widgetWidth);
                } else if (field.widget instanceof ToggleButton tb) {
                    tb.setSize(TOGGLE_SIZE, TOGGLE_SIZE);
                } else if (field.widget instanceof QuestEditorScreen.CategoryLayoutSelector selector) {
                    selector.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestEditorScreen.CategorySubCategorySection section) {
                    section.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestEditorScreen.TreeBackgroundSelector selector) {
                    selector.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestEditorScreen.BackgroundScaleSlider slider) {
                    slider.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestEditorScreen.BackgroundOpacitySlider slider) {
                    slider.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestEditorScreen.FtbImportSection section) {
                    section.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestEditorScreen.MediaSelector selector) {
                    selector.setWidth(widgetWidth);
                }
            }

            if (dependencyEntriesField || completionEntriesField || rewardEntriesField) {
                field.widget.visible = false;
                field.widget.active = false;
                EntryRowKind rowKind = dependencyEntriesField
                        ? EntryRowKind.DEPENDENCY
                        : (rewardEntriesField ? EntryRowKind.REWARD : EntryRowKind.COMPLETION);
                widgetHeight = layoutEntryRows(rowKind, boxX, boxY, widgetWidth, clipTop, clipBottom);
            } else if (field.widget == questOptionalToggle) {
                widgetHeight = renderInlineQuestFlags(gg, mouseX, mouseY, boxX, boxY, widgetWidth, clipTop, clipBottom);
            } else {
                widgetHeight = field.widget.getHeight();
                boolean inside = boxY + widgetHeight > clipTop && boxY < clipBottom;
                field.widget.visible = inside;
                field.widget.active = inside;
                if (field.widget == ftbImportSection && ftbImportRunning) field.widget.active = false;
                if (field.widget == questNameBox || field.widget == catNameBox || field.widget == subNameBox) {
                    LanguageToggleButton languageButton = questNameLanguageButton;
                    if (languageButton != null) {
                        languageButton.setPosition(boxX + widgetWidth - 14, boxY);
                        languageButton.setWidth(14);
                        languageButton.setHeight(BOX_H);
                        languageButton.visible = Config.multiLanguageQuests() && inside;
                        languageButton.active = Config.multiLanguageQuests() && inside;
                    }
                }
                EditBox embeddedIcon = embeddedIconBoxForDisplayField(field.widget);
                if (embeddedIcon != null) {
                    embeddedIcon.setX(boxX);
                    embeddedIcon.setY(boxY);
                    embeddedIcon.setWidth(20);
                    embeddedIcon.visible = inside;
                    embeddedIcon.active = inside;
                }
                if (field.widget == catBackgroundBox && catBackgroundModeButton != null && catBackgroundAssetButton != null) {
                    int gap = 2;
                    int assetWidth = 38;
                    int modeX = boxX + 20 + gap;
                    int modeWidth = Math.max(28, widgetWidth - 20 - assetWidth - gap * 2);
                    catBackgroundModeButton.setPosition(modeX, boxY);
                    catBackgroundModeButton.setWidth(modeWidth);
                    catBackgroundModeButton.setHeight(BOX_H);
                    catBackgroundModeButton.setMessage(tr(catBackgroundMode.translationKey));
                    catBackgroundModeButton.visible = inside;
                    catBackgroundModeButton.active = inside;
                    catBackgroundAssetButton.setPosition(modeX + modeWidth + gap, boxY);
                    catBackgroundAssetButton.setWidth(assetWidth);
                    catBackgroundAssetButton.setHeight(BOX_H);
                    catBackgroundAssetButton.setMessage(tr(catBackgroundMode == CategoryBackgroundMode.TILEABLE
                            ? "background.blocks" : "background.asset"));
                    catBackgroundAssetButton.visible = inside;
                    catBackgroundAssetButton.active = inside;
                }
                if (inside && field.widget instanceof EditBox eb && isDropdownField(eb)) {
                    boolean hovered = mouseX >= eb.getX() && mouseX <= eb.getX() + eb.getWidth()
                            && mouseY >= eb.getY() && mouseY <= eb.getY() + eb.getHeight();
                    boolean selected = isOpenDropdownField(eb);
                    if (hovered || selected || eb.isFocused()) {
                        int x0 = eb.getX();
                        int y0 = eb.getY();
                        int x1 = x0 + eb.getWidth();
                        int y1 = y0 + eb.getHeight();
                        gg.fill(x0, y0, x1, y0 + 1, 0xFFFFFFFF);
                        gg.fill(x0, y1 - 1, x1, y1, 0xFFFFFFFF);
                        gg.fill(x0, y0, x0 + 1, y1, 0xFFFFFFFF);
                        gg.fill(x1 - 1, y0, x1, y1, 0xFFFFFFFF);
                    }
                }
            }
            if (field.widget == questDescriptionBox) {
                layoutDescriptionFormatterButtons(boxX, labelY, boxY + widgetHeight + FORMAT_BAR_GAP, widgetWidth, clipTop, clipBottom);
            } else if (field.widget == catNameBox || field.widget == subNameBox) {
                layoutTitleFormatterButtons(boxX, boxY + widgetHeight + FORMAT_BAR_GAP, widgetWidth, clipTop, clipBottom);
            }
            int translationHeight = 0;
            if (field.widget == questNameBox || field.widget == catNameBox || field.widget == subNameBox) {
                int formatOffset = isFocusedTitleField(field.widget) ? FORMAT_BAR_GAP + FORMAT_BAR_H : 0;
                translationHeight = layoutTranslationRows(false, boxX, boxY + widgetHeight + formatOffset,
                        widgetWidth, clipTop, clipBottom);
            } else if (field.widget == questDescriptionBox) {
                translationHeight = layoutTranslationRows(true, boxX,
                        boxY + widgetHeight + FORMAT_BAR_GAP + FORMAT_BAR_H, widgetWidth, clipTop, clipBottom);
            }

            if (labelY + font.lineHeight > clipTop && labelY < clipBottom) {
                gg.drawString(font, field.displayLabel, pxRight + 2, labelY, 0xFFFFFF, false);
                String tooltip = tooltipForField(field);
                if (!tooltip.isBlank()) {
                    int labelW = font.width(field.displayLabel);
                    boolean hoverLabel = mouseX >= pxRight + 2 && mouseX <= pxRight + 2 + labelW
                            && mouseY >= labelY && mouseY <= labelY + font.lineHeight;
                    boolean hoverWidget = mouseX >= boxX && mouseX <= boxX + (pw - 4)
                            && mouseY >= boxY && mouseY <= boxY + widgetHeight;
                    if (!isItemPickerOpen() && (hoverLabel || hoverWidget)) {
                        queueEditorTooltip(Component.literal(tooltip), mouseX, mouseY);
                    }
                }
            }
            if (dependencyEntriesField) {
                for (LockToggleButton lockButton : dependencyEntryLockButtons) {
                    if (lockButton == null || !lockButton.visible) continue;
                    if (!lockButton.isMouseOver(mouseX, mouseY)) continue;
                    if (!isItemPickerOpen()) queueEditorTooltip(tr(dependencyLockState()
                            ? "tooltip.dependency_lock_after" : "tooltip.dependency_lock_until"), mouseX, mouseY);
                    break;
                }
            }
            if (!isItemPickerOpen() && catBackgroundModeButton != null && catBackgroundModeButton.visible
                    && catBackgroundModeButton.isMouseOver(mouseX, mouseY)) {
                queueEditorTooltip(tr(switch (catBackgroundMode) {
                    case TILEABLE -> "tooltip.background_tile_block";
                    case CUSTOM_TILEABLE -> "tooltip.background_tile_asset";
                    case FULL_IMAGE -> "tooltip.background_full_image";
                }), mouseX, mouseY);
            } else if (!isItemPickerOpen() && catBackgroundAssetButton != null && catBackgroundAssetButton.visible
                    && catBackgroundAssetButton.isMouseOver(mouseX, mouseY)) {
                queueEditorTooltip(tr(catBackgroundMode == CategoryBackgroundMode.TILEABLE
                        ? "tooltip.background_choose_block" : "tooltip.background_choose_asset"), mouseX, mouseY);
            }
            yCursor += font.lineHeight + FIELD_LABEL_GAP + widgetHeight + FIELD_ROW_GAP;
            if (field.widget == questDescriptionBox) {
                yCursor += FORMAT_BAR_GAP + FORMAT_BAR_H;
            } else if (isFocusedTitleField(field.widget)) {
                yCursor += FORMAT_BAR_GAP + FORMAT_BAR_H;
            }
            yCursor += translationHeight;
            if (editorType == EditorType.PACK_OPTIONS && field.widget == packNameBox && exportPackButton != null) {
                int exportX = boxX;
                int exportY = yCursor;
                exportPackButton.setX(exportX);
                exportPackButton.setY(exportY);
                exportPackButton.setWidth(pw - 4);
                exportPackButton.setHeight(20);
                exportPackButton.visible = true;
                exportPackButton.active = true;
                yCursor += exportPackButton.getHeight() + FIELD_ROW_GAP;
                renderPackBackups(gg, mouseX, mouseY, yCursor, clipTop, clipBottom);
            }
        }
        renderDependencyRowIcons(gg);

        if (contentHeight > ph) {
            float ratio = (float) ph / (float) contentHeight;
            int barH = Math.max(12, (int) (ph * ratio));
            float scrollRatio = maxScroll <= 0 ? 0f : editorScroll / maxScroll;
            int barY = py + (int) ((ph - barH) * scrollRatio);
            gg.fill(pxRight + pw + 4, barY, pxRight + pw + 6, barY + barH, 0xFF808080);
        }
    }

    protected void renderPackBackups(GuiGraphics gg, int mouseX, int mouseY, int yCursor, int clipTop, int clipBottom) {
        // build backup hitboxes during the same pass that renders their rows
        int x = pxRight + 2;
        int width = pw - 4;
        gg.enableScissor(x, clipTop, x + width, clipBottom);
        if (yCursor + font.lineHeight > clipTop && yCursor < clipBottom) {
            gg.drawString(font, Component.literal(trs("backup.heading")), x, yCursor, 0xFFFFFF, false);
        }
        int rowY = yCursor + font.lineHeight + FIELD_LABEL_GAP;
        if (packBackups.isEmpty()) {
            if (rowY + font.lineHeight > clipTop && rowY < clipBottom) {
                drawScaledText(gg, trs("backup.none"), x + 2, rowY + 1, 0xFF909090, 0.7f);
            }
            gg.disableScissor();
            return;
        }

        for (QuestPackStorage.Snapshot snapshot : packBackups) {
            boolean visible = rowY + BACKUP_ROW_H > clipTop && rowY < clipBottom;
            if (visible) {
                boolean hovered = mouseX >= x && mouseX <= x + width
                        && mouseY >= Math.max(rowY, clipTop)
                        && mouseY <= Math.min(rowY + BACKUP_ROW_H, clipBottom);
                int background = hovered ? 0xFF555555 : 0xFF353535;
                int border = hovered ? 0xFFFFFFFF : 0xFF858585;
                gg.fill(x, rowY, x + width, rowY + BACKUP_ROW_H, background);
                gg.fill(x, rowY, x + width, rowY + 1, border);
                gg.fill(x, rowY + BACKUP_ROW_H - 1, x + width, rowY + BACKUP_ROW_H, border);
                gg.fill(x, rowY, x + 1, rowY + BACKUP_ROW_H, border);
                gg.fill(x + width - 1, rowY, x + width, rowY + BACKUP_ROW_H, border);

                String timestamp = BACKUP_DISPLAY_TIME.format(snapshot.createdAt());
                drawScaledText(gg, timestamp, x + 4, rowY + 3, 0xFFFFFFFF, 0.65f);
                String reason = safe(snapshot.reason()).isBlank() ? "backup" : snapshot.reason();
                drawScaledText(gg, reason, x + 4, rowY + 11, 0xFFAAAAAA, 0.5f);
                int hitY = Math.max(rowY, clipTop);
                int hitBottom = Math.min(rowY + BACKUP_ROW_H, clipBottom);
                backupRowHitboxes.add(new BackupRowHitbox(snapshot, x, hitY, width, hitBottom - hitY));
                if (hovered) {
                    queueEditorTooltip(Component.literal(trs("backup.restore_tooltip")), mouseX, mouseY);
                }
            }
            rowY += BACKUP_ROW_H + BACKUP_ROW_GAP;
        }
        gg.disableScissor();
    }

    protected void drawScaledText(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0f);
        gg.pose().scale(scale, scale, 1f);
        gg.drawString(font, text, 0, 0, color, false);
        gg.pose().popPose();
    }

    protected void renderEntryComponentOverlays(GuiGraphics gg) {
        // draw selected item components above their compact row text
        int clipLeft = pxRight + 2;
        int clipRight = pxRight + pw - 2;
        int clipTop = py;
        int clipBottom = py + ph;
        gg.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        renderEntryComponentOverlays(gg, completionEntryBoxes);
        renderEntryComponentOverlays(gg, rewardEntryBoxes);
        gg.disableScissor();
    }

    protected void renderEntryComponentOverlays(GuiGraphics gg, List<ScaledMultiLineEditBox> boxes) {
        for (ScaledMultiLineEditBox box : boxes) {
            if (box == null || !box.visible || !box.isFocused()) continue;
            String components = safe(selectedItemComponentsByBox.get(box)).trim();
            if (components.isBlank()) continue;
            String value = safe(box.getValue());
            int componentX = box.getX() + 3 + Math.round(font.width(value) * ENTRY_INPUT_TEXT_SCALE) + 3;
            int componentY = box.getY() + 3;
            gg.pose().pushPose();
            gg.pose().translate(componentX, componentY, 0f);
            gg.pose().scale(ENTRY_INPUT_TEXT_SCALE, ENTRY_INPUT_TEXT_SCALE, 1f);
            gg.drawString(font, components, 0, 0, 0xA0A0A0, false);
            gg.pose().popPose();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // foreground menus and pickers receive scroll before the editor body
        if (!ensureEditorAccess()) return true;
        mouseX /= uiScale;
        mouseY /= uiScale;
        int categoryStripX = leftX - TAB_W + SIDE_TAB_X_OFFSET;
        int categoryStripBottom = py + CATEGORY_TAB_PAGE_SIZE * (TAB_H + TAB_GAP) + TAB_H;
        if (mode == Mode.QUEST_LIST && isTreePack()
                && mouseX >= categoryStripX && mouseX < categoryStripX + TAB_W
                && mouseY >= py - CATEGORY_SCROLL_SIZE - TAB_GAP && mouseY < categoryStripBottom) {
            int direction = scrollY > 0 ? -1 : scrollY < 0 ? 1 : 0;
            if (direction != 0) {
                scrollCategoryWorkspace(direction);
                return true;
            }
        }
        if (scrollTypeMenu(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollDropdownMenu(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollItemPicker(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollIdSuggestions(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollEntryFields(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (mouseX >= pxRight && mouseX <= pxRight + pw && mouseY >= py && mouseY <= py + ph) {
            int contentHeight = contentHeight();
            if (contentHeight > ph) {
                editorScroll = Math.max(0f, Math.min(editorScroll - (float) scrollY * 12f, contentHeight - ph));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    protected boolean scrollEntryFields(double mouseX, double mouseY, double scrollY) {
        // let focused multiline fields scroll before moving the full form
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (TranslationRow row : questNameTranslationRows) {
            if (row.box.visible && row.box.isMouseOver(mouseX, mouseY) && row.box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (TranslationRow row : questDescriptionTranslationRows) {
            if (row.box.visible && row.box.isMouseOver(mouseX, mouseY) && row.box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        return false;
    }

    protected boolean isInsideSuggestionBox(double mouseX, double mouseY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null) return false;
        return mouseX >= bounds.x && mouseX <= bounds.x + bounds.w
                && mouseY >= bounds.y && mouseY <= bounds.y + bounds.h;
    }

    protected boolean isInsideSuggestionTargetField(double mouseX, double mouseY) {
        return isInsideBox(packIconPathBox, mouseX, mouseY)
                || isInsideBox(catIconBox, mouseX, mouseY)
                || isInsideBox(subIconBox, mouseX, mouseY)
                || isInsideBox(questIconBox, mouseX, mouseY)
                || isInsideBox(catBackgroundBox, mouseX, mouseY)
                || isInsideBox(catDependencyBox, mouseX, mouseY)
                || isInsideBox(subCategoryBox, mouseX, mouseY)
                || isInsideBox(questCategoryBox, mouseX, mouseY)
                || isInsideBox(questSubCategoryBox, mouseX, mouseY)
                || isInsideMultiBox(questCompletionBox, mouseX, mouseY)
                || isInsideMultiBox(questRewardBox, mouseX, mouseY)
                || isInsideMultiBox(questDependenciesBox, mouseX, mouseY)
                || anyVisibleMultiBoxContains(dependencyEntryBoxes, mouseX, mouseY)
                || anyVisibleMultiBoxContains(completionEntryBoxes, mouseX, mouseY)
                || anyVisibleMultiBoxContains(rewardEntryBoxes, mouseX, mouseY)
                || translationRowContains(questNameTranslationRows, mouseX, mouseY)
                || translationRowContains(questDescriptionTranslationRows, mouseX, mouseY);
    }

    protected boolean translationRowContains(List<TranslationRow> rows, double mouseX, double mouseY) {
        for (TranslationRow row : rows) if (isInsideMultiBox(row.box, mouseX, mouseY)) return true;
        return false;
    }

    protected boolean isInsideBox(EditBox box, double mouseX, double mouseY) {
        return box != null && box.visible
                && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
    }

    protected EditBox clickedVisibleIconBox(double mouseX, double mouseY) {
        EditBox[] boxes = {packIconPathBox, catIconBox, catBackgroundBox, subIconBox, questIconBox};
        for (EditBox box : boxes) {
            if (isInsideBox(box, mouseX, mouseY)) return box;
        }
        return null;
    }

    protected boolean isInsideMultiBox(ScaledMultiLineEditBox box, double mouseX, double mouseY) {
        return box != null && box.visible
                && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
    }

    protected boolean anyVisibleMultiBoxContains(List<ScaledMultiLineEditBox> boxes, double mouseX, double mouseY) {
        if (boxes == null || boxes.isEmpty()) return false;
        for (ScaledMultiLineEditBox box : boxes) {
            if (isInsideMultiBox(box, mouseX, mouseY)) return true;
        }
        return false;
    }

    protected int contentHeight() {
        // calculate scroll range from visible fields and expanded row groups
        int total = 0;
        for (FormField field : activeFields) {
            total += font.lineHeight + FIELD_LABEL_GAP + fieldHeight(field) + FIELD_ROW_GAP;
            if (editorType == EditorType.PACK_OPTIONS && field.widget == packNameBox) {
                total += 20 + FIELD_ROW_GAP;
                total += backupSectionHeight();
            }
        }
        return total;
    }

    protected int backupSectionHeight() {
        int rowsHeight = packBackups.isEmpty()
                ? font.lineHeight
                : packBackups.size() * (BACKUP_ROW_H + BACKUP_ROW_GAP) - BACKUP_ROW_GAP;
        return font.lineHeight + FIELD_LABEL_GAP + rowsHeight + FIELD_ROW_GAP;
    }

    protected int fieldHeight(FormField field) {
        if (field == null || field.widget == null) return 0;
        if (field.widget == questDependenciesBox) return entryRowsHeight(EntryRowKind.DEPENDENCY);
        if (field.widget == questCompletionBox) return entryRowsHeight(EntryRowKind.COMPLETION);
        if (field.widget == questRewardBox) return entryRowsHeight(EntryRowKind.REWARD);
        if (field.widget == questNameBox || field.widget == catNameBox || field.widget == subNameBox) {
            int formatHeight = isFocusedTitleField(field.widget) ? FORMAT_BAR_GAP + FORMAT_BAR_H : 0;
            return field.widget.getHeight() + formatHeight + translationRowsHeight(false);
        }
        if (field.widget == questOptionalToggle) return TOGGLE_SIZE + INLINE_FLAG_LABEL_H + 3;
        if (field.widget instanceof ScaledMultiLineEditBox mb) {
            int baseHeight = computeMultilineHeight(mb, BOX_H_TALL);
            if (field.widget == questDescriptionBox) {
                return baseHeight + FORMAT_BAR_GAP + FORMAT_BAR_H + translationRowsHeight(true);
            }
            return baseHeight;
        }
        return field.widget.getHeight();
    }

    protected void updateDynamicFieldSizes() {
        // grow multiline fields from their content before calculating scroll
        for (FormField field : activeFields) {
            if (field.widget == questDependenciesBox || field.widget == questCompletionBox || field.widget == questRewardBox) continue;
            if (field.widget instanceof ScaledMultiLineEditBox mb) {
                mb.setWidth(pw - 4);
                mb.setHeight(computeMultilineHeight(mb, BOX_H_TALL));
            }
        }
    }

    protected void updateInvalidFieldStyles() {
        // update validation colours once after field values have settled
        ValidationSnapshot snapshot = currentValidationSnapshot();
        setIdFieldColor(questIdBox, snapshot.invalidQuestId);
        setIdFieldColor(questNameBox, snapshot.invalidQuestName);
        setIdFieldColor(catDependencyBox, snapshot.invalidCategoryDependency);
        setIdFieldColor(subCategoryBox, snapshot.invalidParentCategory);
        setIdFieldColor(questCategoryBox, snapshot.invalidQuestCategory);
        setIdFieldColor(questSubCategoryBox, snapshot.invalidQuestSubCategory);
        setEntryRowsColor(EntryRowKind.DEPENDENCY, snapshot.invalidQuestDependencies);
        setEntryRowsColor(EntryRowKind.COMPLETION, snapshot.invalidCompletionEntries);
        setEntryRowsColor(EntryRowKind.REWARD, snapshot.invalidRewardEntries);
    }

    protected ValidationSnapshot currentValidationSnapshot() {
        // collect related validation results in one reusable snapshot
        ValidationRequest request = new ValidationRequest();
        request.previousSnapshot = validationSnapshot;
        request.editorType = editorType;
        request.hasCurrentPack = currentPack != null;
        request.packName = currentPack == null ? "" : currentPack.name;
        request.editingPathString = safe(editingPath == null ? "" : editingPath.toString());
        request.editingPath = editingPath;
        request.questId = safe(questIdBox == null ? "" : questIdBox.getValue()).trim();
        request.questName = safe(questNameBox == null ? "" : questNameBox.getValue()).trim();
        request.categoryDependency = safe(catDependencyBox == null ? "" : catDependencyBox.getValue()).trim();
        request.parentCategory = dropdownBoxValue(subCategoryBox);
        request.questCategory = dropdownBoxValue(questCategoryBox);
        request.questSubCategory = dropdownBoxValue(questSubCategoryBox);
        request.parentQuestCategory = dropdownBoxValue(questCategoryBox);
        request.dependencyRaw = entryRowsToRaw(EntryRowKind.DEPENDENCY);
        request.completionRaw = entryRowsToRaw(EntryRowKind.COMPLETION).trim();
        request.rewardRaw = entryRowsToRaw(EntryRowKind.REWARD).trim();
        request.categoryIdCache = categoryIdCache;
        request.subCategoryIdCache = subCategoryIdCache;
        request.questIdCache = questIdCache;
        request.questPackDependencySuggestionCache = questPackDependencySuggestionCache;
        request.invalidQuestIdOverride = editorType == EditorType.QUEST
                && (request.questId.isBlank() || isDuplicateQuestId(request.questId));
        request.questEntries = List.of();
        request.errorHandler = this::setError;
        validationSnapshot = QuestEditorValidation.currentSnapshot(request);
        return validationSnapshot;
    }

    protected void setIdFieldColor(EditBox box, boolean invalid) {
        if (box == null) return;
        if (isDropdownField(box)) {
            applyDropdownTextColor(box, invalid);
            return;
        }
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    protected void applyDropdownTextColor(EditBox box, boolean invalid) {
        if (box == null) return;
        int color = invalid ? INVALID_INPUT_TEXT_COLOR : DROPDOWN_INPUT_TEXT_COLOR;
        box.setTextColor(color);
        box.setTextColorUneditable(color);
    }

    protected void setJsonFieldColor(ScaledMultiLineEditBox box, boolean invalid) {
        if (box == null) return;
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    protected String tooltipForField(FormField field) {
        // return the most useful validation reason for the hovered field
        if (field == null || field.widget == null) return "";
        if (field.widget == questIdBox && isInvalidQuestIdField()) {
            return trs(isDuplicateQuestId(safe(questIdBox.getValue()).trim())
                    ? "error.quest_id_exists" : "error.quest_id_required");
        }
        if (field.widget == questNameBox && isInvalidQuestNameField()) return trs("error.quest_name_required");
        if (field.widget == catDependencyBox && isInvalidCategoryDependency()) return trs("error.invalid_quest_id");
        if (field.widget == subCategoryBox && isInvalidParentCategory()) return trs("error.invalid_id");
        if (field.widget == questCategoryBox && isInvalidQuestCategory()) {
            return dropdownBoxValue(questCategoryBox).isBlank() ? trs("error.quest_category_required") : trs("error.invalid_id");
        }
        if (field.widget == questSubCategoryBox && isInvalidQuestSubCategory()) return trs("error.invalid_id");
        if (field.widget == questDependenciesBox && isInvalidQuestDependencies()) return trs("error.invalid_id");
        return field.tooltip == null ? "" : field.tooltip;
    }

    protected boolean isInvalidQuestIdField() {
        if (editorType != EditorType.QUEST) return false;
        String questId = safe(questIdBox == null ? "" : questIdBox.getValue()).trim();
        return questId.isBlank() || isDuplicateQuestId(questId);
    }

    protected boolean isInvalidQuestNameField() {
        if (editorType != EditorType.QUEST) return false;
        return safe(questNameBox == null ? "" : questNameBox.getValue()).trim().isBlank();
    }

    protected int renderInlineQuestFlags(GuiGraphics gg, int mouseX, int mouseY, int x, int y, int width, int clipTop, int clipBottom) {
        // render import warnings beside the quest fields they describe
        ToggleButton[] toggles = new ToggleButton[] { questOptionalToggle, questRepeatableToggle, questAutoCompleteToggle, questHiddenUnderDependencyToggle };
        String[] titles = new String[] {
                trs("flag.optional"), trs("flag.repeatable"), trs("flag.redeem"), trs("flag.hidden")
        };
        String[][] tooltips = new String[][] {
                { trs("flag.optional"), trs("tooltip.flag_optional") },
                { trs("flag.repeatable"), trs("tooltip.flag_repeatable") },
                { trs("flag.redeem"), trs("tooltip.flag_redeem") },
                { trs("flag.hidden_dependency"), trs("tooltip.flag_hidden") }
        };
        int segmentGap = 3;
        int segmentWidth = Math.max(20, (width - segmentGap * 3) / 4);
        int totalHeight = TOGGLE_SIZE + INLINE_FLAG_LABEL_H + 3;
        boolean inside = y + totalHeight > clipTop && y < clipBottom;
        for (int i = 0; i < toggles.length; i++) {
            ToggleButton toggle = toggles[i];
            if (toggle == null) continue;
            int segmentX = x + i * (segmentWidth + segmentGap);
            int toggleX = segmentX + (segmentWidth - TOGGLE_SIZE) / 2;
            int toggleY = y + INLINE_FLAG_LABEL_H + 3;
            toggle.setX(toggleX);
            toggle.setY(toggleY);
            toggle.setSize(TOGGLE_SIZE, TOGGLE_SIZE);
            toggle.visible = inside;
            toggle.active = inside;

            if (inside && y + INLINE_FLAG_LABEL_H > clipTop && y < clipBottom) {
                float scale = 0.55f;
                int maxUnscaledWidth = Math.max(1, (int) Math.floor(segmentWidth / scale));
                String clippedTitle = font.plainSubstrByWidth(titles[i], maxUnscaledWidth);
                int labelW = (int) Math.ceil(font.width(clippedTitle) * scale);
                gg.enableScissor(pxRight + 2, clipTop, pxRight + pw - 2, clipBottom);
                gg.pose().pushPose();
                gg.pose().scale(scale, scale, 1f);
                float inv = 1f / scale;
                gg.drawString(font, clippedTitle, (int) ((segmentX + (segmentWidth - labelW) / 2) * inv), (int) (y * inv), 0xFFFFFF, false);
                gg.pose().popPose();
                gg.disableScissor();
            }

            boolean hovered = mouseX >= segmentX && mouseX <= segmentX + segmentWidth
                    && mouseY >= y && mouseY <= y + totalHeight;
            if (!isItemPickerOpen() && hovered) {
                queueEditorTooltip(List.of(Component.literal(tooltips[i][0]), Component.literal(tooltips[i][1])), mouseX, mouseY);
            }
        }
        return totalHeight;
    }

    protected boolean isInvalidCategoryDependency() {
        return isMissingQuestId(safe(catDependencyBox == null ? "" : catDependencyBox.getValue()));
    }

    protected boolean isInvalidParentCategory() {
        return isMissingCategoryId(dropdownBoxValue(subCategoryBox));
    }

    protected boolean isInvalidQuestCategory() {
        String value = dropdownBoxValue(questCategoryBox);
        return value.isBlank() || isMissingCategoryId(value);
    }

    protected boolean isInvalidQuestSubCategory() {
        return isMissingSubCategoryId(dropdownBoxValue(questSubCategoryBox));
    }

    protected boolean isInvalidQuestDependencies() {
        return QuestEditorValidation.computeInvalidQuestDependencies(
                String.join("\n", collectDependencyEntries()),
                currentPack != null,
                questPackDependencySuggestionCache
        );
    }

    protected boolean isInvalidCompletionEntries() {
        String raw = safe(questCompletionBox == null ? "" : questCompletionBox.getValue()).trim();
        if (raw.isBlank()) return false;
        return QuestEditorEntryCodec.parseCompletionEntries(raw, false, this::setError) == null;
    }

    protected boolean isInvalidRewardEntries() {
        String raw = safe(questRewardBox == null ? "" : questRewardBox.getValue()).trim();
        if (raw.isBlank()) return false;
        return QuestEditorEntryCodec.parseRewardEntries(raw, false, this::setError) == null;
    }

    protected boolean isMissingCategoryId(String raw) {
        return QuestEditorValidation.isMissingCategoryId(raw, currentPack != null, categoryIdCache);
    }

    protected boolean isMissingQuestId(String raw) {
        return QuestEditorValidation.isMissingQuestId(raw, currentPack != null, questIdCache);
    }

    protected void setDropdownBoxValue(EditBox box, String value) {
        if (box == null) return;
        String normalized = normalizeDropdownValue(value);
        box.setValue(normalized.isBlank() ? trs("none") : normalized);
    }

    protected String dropdownBoxValue(EditBox box) {
        return normalizeDropdownValue(safe(box == null ? "" : box.getValue()));
    }

    protected String normalizeDropdownValue(String raw) {
        String value = safe(raw).trim();
        return value.equalsIgnoreCase("none") || value.equalsIgnoreCase(trs("none")) ? "" : value;
    }

    protected boolean isDropdownField(EditBox box) {
        return box == subCategoryBox || box == questCategoryBox || box == questSubCategoryBox;
    }

    protected boolean isOpenDropdownField(EditBox box) {
        return popupMenus.isOpenDropdownField(box, subCategoryBox, questCategoryBox, questSubCategoryBox);
    }

    protected boolean isMissingSubCategoryId(String raw) {
        return QuestEditorValidation.isMissingSubCategoryId(raw, dropdownBoxValue(questCategoryBox), currentPack != null, subCategoryIdCache);
    }

    protected boolean isDuplicateQuestId(String questIdRaw) {
        if (currentPack == null) return false;
        String questId = safe(questIdRaw).trim();
        if (questId.isBlank()) return false;
        if (!localQuestIdCacheReady) {
            return QuestEditorValidation.isDuplicateQuestId(questId, true, cachedQuestEntriesOrLoad(), editingPath);
        }
        int count = questIdCountCache.getOrDefault(questId, 0);
        if (editingPath != null && questPathsByIdCache.getOrDefault(questId, Set.of()).contains(editingPath)) {
            return count > 1;
        }
        return count > 0;
    }

    protected boolean isDuplicateQuestIdAcrossPack(String questIdRaw) {
        // use the pack id cache instead of scanning quest files again
        return QuestEditorValidation.isDuplicateQuestIdAcrossPack(questIdRaw, currentPack != null, cachedQuestEntriesOrLoad());
    }

    protected Iterable<NamedEntry> cachedQuestEntriesOrLoad() {
        if (currentPack != null && questListIndexCache != null && Objects.equals(questListIndexPackRoot, currentPack.root)) {
            List<NamedEntry> entries = new ArrayList<>(questListIndexCache.items.size());
            for (QuestListItem item : questListIndexCache.items) {
                if (item != null && item.entry != null) entries.add(item.entry);
            }
            return entries;
        }
        return currentPack == null ? List.of() : QuestEditorJsonFiles.listQuestEntries(currentPack);
    }

    protected void refreshPackIdCaches() {
        refreshPackIdCaches(null);
    }

    protected void refreshPackIdCaches(QuestListIndex questIndex) {
        // reuse the quest index instead of reopening every quest file
        // rebuild editor suggestion caches from the current pack
        validationSnapshot = null;
        invalidateIdSuggestions();
        categoryIdCache.clear();
        subCategoryIdCache.clear();
        questIdCache.clear();
        questIdCountCache.clear();
        questPathsByIdCache.clear();
        localQuestIdCacheReady = false;
        questIconByIdCache.clear();
        categorySuggestionCache.clear();
        subCategorySuggestionCache.clear();
        questSuggestionCache.clear();
        questPackDependencySuggestionCache.clear();
        subCategoryByCategorySuggestion.clear();
        suggestionCaches.clearPackScopedCaches();

        Set<String> questPackDependencySuggestions = new LinkedHashSet<>();
        if (questIndex != null) {
            categoryIdCache.addAll(questIndex.categoryIds);
            subCategoryIdCache.addAll(questIndex.subCategoryIds);
            questIdCache.addAll(questIndex.questIds);
            for (QuestListItem item : questIndex.items) {
                if (item == null || item.entry == null || item.entry.id == null || item.entry.id.isBlank()) continue;
                cacheLocalQuestEntry(item.entry);
                if (item.entry.icon != null && !item.entry.icon.isBlank()) {
                    questIconByIdCache.put(item.entry.id, item.entry.icon);
                }
            }
            for (String key : questIndex.subCategoryNames.keySet()) {
                int split = key.indexOf("::");
                String category = split >= 0 ? key.substring(0, split) : "";
                String id = split >= 0 ? key.substring(split + 2) : key;
                if (id.isBlank()) continue;
                addSubCategorySuggestion(id, category);
            }
            for (String id : questIndex.questIds) {
                if (id != null && !id.isBlank()) questPackDependencySuggestions.add(id);
            }
        } else if (currentPack != null) {
            List<NamedEntry> localCategoryEntries = QuestEditorJsonFiles.listCategoryEntries(currentPack);
            List<NamedEntry> localSubCategoryEntries = QuestEditorJsonFiles.listSubCategoryEntries(currentPack);
            List<NamedEntry> localQuestEntries = QuestEditorJsonFiles.listQuestEntries(currentPack);
            for (NamedEntry entry : localCategoryEntries) {
                if (entry == null || entry.id == null) continue;
                if ("all".equalsIgnoreCase(entry.id.trim())) continue;
                if (entry != null && entry.id != null && !entry.id.isBlank()) categoryIdCache.add(entry.id);
            }
            for (NamedEntry entry : localSubCategoryEntries) {
                if (entry == null || entry.id == null || entry.id.isBlank()) continue;
                subCategoryIdCache.add(entry.id);
                SubCategoryData data = QuestEditorJsonFiles.loadSubCategory(entry.path, entry.id);
                String category = data == null ? "" : safe(data.category).trim();
                if (!category.isBlank()) {
                    subCategoryIdCache.add(category + "::" + entry.id);
                }
                addSubCategorySuggestion(entry.id, category);
            }
            for (NamedEntry entry : localQuestEntries) {
                if (entry != null && entry.id != null && !entry.id.isBlank()) {
                    cacheLocalQuestEntry(entry);
                    questIdCache.add(entry.id);
                    questPackDependencySuggestions.add(entry.id);
                    if (entry.icon != null && !entry.icon.isBlank()) {
                        questIconByIdCache.put(entry.id, entry.icon);
                    }
                }
            }
        }

        List<String> localSubCategories = sortedIds(subCategoryIdCache);
        List<String> localQuests = sortedIds(questPackDependencySuggestions);
        questPackDependencySuggestionCache.addAll(localQuests);

        for (String localSub : localSubCategories) {
            if (!subCategorySuggestionCache.contains(localSub)) subCategorySuggestionCache.add(localSub);
        }

        for (QuestData.Category c : QuestData.categoriesOrdered()) {
            if (c == null || c.id == null || c.id.isBlank()) continue;
            if ("all".equalsIgnoreCase(c.id.trim())) continue;
            categoryIdCache.add(c.id);
        }
        for (QuestData.SubCategory sc : QuestData.subCategoriesAllOrdered()) {
            if (sc == null || sc.id == null || sc.id.isBlank()) continue;
            subCategoryIdCache.add(sc.id);
            addSubCategorySuggestion(sc.id, sc.category);
        }
        for (QuestData.Quest q : QuestData.all()) {
            if (q == null || q.id == null || q.id.isBlank()) continue;
            questIdCache.add(q.id);
            if (q.icon != null && !q.icon.isBlank()) questIconByIdCache.putIfAbsent(q.id, q.icon);
        }
        categorySuggestionCache.addAll(sortedIds(categoryIdCache));
        questSuggestionCache.addAll(sortedIds(questIdCache));
        localQuestIdCacheReady = true;
    }

    protected void cacheLocalQuestEntry(NamedEntry entry) {
        // update suggestions immediately after a local save
        if (entry == null || entry.id == null || entry.id.isBlank()) return;
        questIdCountCache.merge(entry.id, 1, Integer::sum);
        if (entry.path != null) {
            questPathsByIdCache.computeIfAbsent(entry.id, ignored -> new HashSet<>()).add(entry.path);
        }
    }

    protected void layoutDescriptionFormatterButtons(int startX, int undoY, int formatY, int width, int clipTop, int clipBottom) {
        // keep undo redo formatting language and emoji controls together
        boolean visible = editorType == EditorType.QUEST
                && questDescriptionBox != null
                && questDescriptionBox.visible;
        layoutDescriptionButtonGroup(startX, undoY, width, clipTop, clipBottom, visible, true);
        layoutDescriptionButtonGroup(startX, formatY, width, clipTop, clipBottom, visible, false);
        if (questDescriptionLanguageButton != null) {
            TextInsertButton emojiButton = descriptionFormatButtons.stream()
                    .filter(button -> "__emoji__".equals(button.insertText())).findFirst().orElse(null);
            boolean languageVisible = visible && Config.multiLanguageQuests() && emojiButton != null;
            if (languageVisible) {
                questDescriptionLanguageButton.setPosition(emojiButton.getX() - 16, emojiButton.getY());
                questDescriptionLanguageButton.setWidth(14);
                questDescriptionLanguageButton.setHeight(14);
            }
            questDescriptionLanguageButton.visible = languageVisible;
            questDescriptionLanguageButton.active = languageVisible;
        }
    }

    protected void layoutTitleFormatterButtons(int startX, int y, int width, int clipTop, int clipBottom) {
        boolean visible = focusedTitleFormatBox() != null
                && y + FORMAT_BAR_H > clipTop
                && y < clipBottom;
        int totalWidth = 0;
        int count = 0;
        for (TextInsertButton button : descriptionFormatButtons) {
            if (isDescriptionUndoRedoButton(button)) continue;
            totalWidth += button.getWidth();
            count++;
        }
        totalWidth += Math.max(0, count - 1) * FORMAT_BTN_GAP;
        int x = startX + Math.max(0, width - totalWidth);
        for (TextInsertButton button : descriptionFormatButtons) {
            if (isDescriptionUndoRedoButton(button)) continue;
            button.setX(x);
            button.setY(y);
            button.visible = visible;
            button.active = visible;
            x += button.getWidth() + FORMAT_BTN_GAP;
        }
    }

    protected boolean isFocusedTitleField(AbstractWidget widget) {
        return (widget == catNameBox || widget == subNameBox)
                && widget != null
                && widget.isFocused();
    }

    protected void layoutDescriptionButtonGroup(int startX, int y, int width, int clipTop, int clipBottom, boolean baseVisible, boolean undoRedo) {
        boolean visible = baseVisible
                && y + FORMAT_BAR_H > clipTop
                && y < clipBottom;
        int totalWidth = 0;
        int count = 0;
        for (TextInsertButton button : descriptionFormatButtons) {
            if (isDescriptionUndoRedoButton(button) != undoRedo) continue;
            totalWidth += button.getWidth();
            count++;
        }
        totalWidth += Math.max(0, count - 1) * FORMAT_BTN_GAP;
        if (!undoRedo && Config.multiLanguageQuests()) totalWidth += 16;
        int x = startX + Math.max(0, width - totalWidth);
        for (TextInsertButton button : descriptionFormatButtons) {
            if (isDescriptionUndoRedoButton(button) != undoRedo) continue;
            if (!undoRedo && Config.multiLanguageQuests() && "__emoji__".equals(button.insertText())) x += 16;
            button.setX(x);
            button.setY("__emoji__".equals(button.insertText()) ? y - 2 : y);
            button.visible = visible;
            button.active = visible && descriptionFormatButtonActive(button);
            x += button.getWidth() + FORMAT_BTN_GAP;
        }
    }

    protected boolean isDescriptionUndoRedoButton(TextInsertButton button) {
        if (button == null) return false;
        String action = button.insertText();
        return "__emoji__".equals(action) || "__undo__".equals(action) || "__redo__".equals(action);
    }

    protected boolean descriptionFormatButtonActive(TextInsertButton button) {
        if (button == null) return false;
        String action = button.insertText();
        if ("__emoji__".equals(action)) return true;
        if ("__undo__".equals(action)) return questDescriptionBox != null && questDescriptionBox.canUndo();
        if ("__redo__".equals(action)) return questDescriptionBox != null && questDescriptionBox.canRedo();
        return true;
    }

    protected List<String> sortedIds(Set<String> ids) {
        List<String> out = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return out;
        out.addAll(ids);
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    protected void addSubCategorySuggestion(String subId, String categoryId) {
        if (subId == null || subId.isBlank()) return;
        if (!subCategorySuggestionCache.contains(subId)) subCategorySuggestionCache.add(subId);
        String key = safe(categoryId).trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) return;
        List<String> scoped = subCategoryByCategorySuggestion.computeIfAbsent(key, k -> new ArrayList<>());
        if (!scoped.contains(subId)) scoped.add(subId);
    }

    protected int computeMultilineHeight(ScaledMultiLineEditBox box, int minHeight) {
        int lines = Math.max(1, box.getLineCount());
        double lineHeight = box.getLineHeight();
        int content = (int) Math.ceil(lines * lineHeight) + 8;
        int maxHeight = Math.max(minHeight, ph - (font.lineHeight + FIELD_LABEL_GAP + FIELD_ROW_GAP + 6));
        return Math.max(minHeight, Math.min(maxHeight, content));
    }

    protected void updateBackButtonVisibility() {
        // keep the back tab below the fourth visible category tab
        if (backButton == null) return;
        backButton.visible = true;
        backButton.active = true;

        boolean showQuestActions = editorType == EditorType.QUEST
                || editorType == EditorType.CATEGORY
                || editorType == EditorType.SUBCATEGORY
                || editorType == EditorType.PACK_OPTIONS
                || (mode == Mode.PACK_MENU && currentPack != null);
        if (duplicateButton != null) {
            duplicateButton.visible = showQuestActions;
            duplicateButton.active = showQuestActions;
        }
        if (deleteQuestButton != null) {
            deleteQuestButton.visible = showQuestActions;
            deleteQuestButton.active = showQuestActions;
        }
        if (exportPackButton != null) {
            boolean showExport = editorType == EditorType.PACK_OPTIONS && currentPack != null;
            exportPackButton.visible = showExport;
            exportPackButton.active = showExport;
        }

        if (questSearchBox != null) {
            boolean visible = isListSearchMode() && currentPack != null && !isTreePack();
            questSearchBox.visible = visible;
            questSearchBox.active = visible;
            questSearchBox.setHint(tr("search"));
        }
    }

    protected void goBack() {
        // confirm unsaved changes before leaving the active editor form
        flushCategoryBackgroundAutosave();
        if (hasUnsavedEditorChanges()) {
            clearPendingDiscardState();
            pendingBackNavigation = true;
            statusMessage = "";
            statusColor = 0xFFD080;
            return;
        }
        performBackNavigation();
    }

    protected void performBackNavigation() {
        // return to the nearest parent list or pack workspace
        pendingBackNavigation = false;
        if (returnToParentOnBack) {
            exitEditor();
            return;
        }
        switch (mode) {
            case PACK_LIST -> {
                if (editorType == EditorType.PACK_OPTIONS) {
                    selectedEntryId = "";
                    clearEditor();
                    refreshLeftList();
                } else {
                    exitEditor();
                }
            }
            case PACK_CREATE, PACK_MENU -> setMode(Mode.PACK_LIST);
            case CATEGORY_LIST, SUBCATEGORY_LIST, QUEST_LIST -> setMode(Mode.PACK_LIST);
        }
    }

    protected void exitEditor() {
        // apply staged changes only when the editor fully closes
        if (closingEditor) return;
        if (!ensureEditorAccess()) return;
        flushCategoryBackgroundAutosave();
        closingEditor = true;
        clearPendingInitState();
        applyStagedChangesOnClose();
        Minecraft.getInstance().setScreen(parent);
    }

    protected void requestExitEditor() {
        if (!hasUnsavedEditorChanges()) {
            exitEditor();
            return;
        }
        clearPendingDiscardState();
        pendingEditorExit = true;
        statusMessage = "";
        statusColor = 0xFFD080;
    }

    @Override
    public void onClose() {
        exitEditor();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // capture form state before minecraft rebuilds screen widgets
        closeTransientMenus();
        pendingState = captureState();
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // run delayed saves and widget cursors on the client tick
        if (!ensureEditorAccess()) return;
        super.tick();
        tickCategoryBackgroundAutosave();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // route clicks from topmost modal layers down to normal controls
        if (!ensureEditorAccess()) return true;
        mouseX /= uiScale;
        mouseY /= uiScale;
        if (isUnsavedChangesPopupOpen()) {
            return handleUnsavedChangesPopupClick(mouseX, mouseY, button);
        }
        if (button == 0 && clickPackBackup(mouseX, mouseY)) return true;
        if (button == 0 && clickTitleFormatterButton(mouseX, mouseY)) return true;
        if (button == 0 && clickItemPicker(mouseX, mouseY)) return true;
        if (button == 0) {
            EditBox clickedIconBox = clickedVisibleIconBox(mouseX, mouseY);
            if (clickedIconBox != null) {
                openIconItemPicker(clickedIconBox);
                return true;
            }
        }
        if (button == 0 && clickTypeMenu(mouseX, mouseY)) return true;
        if (button == 0 && clickDropdownMenu(mouseX, mouseY)) return true;
        if (button == 0) {
            if (!isInsideSuggestionBox(mouseX, mouseY) && !isInsideSuggestionTargetField(mouseX, mouseY)) {
                suppressIdSuggestions = true;
                clearIdSuggestions();
            } else {
                suppressIdSuggestions = false;
                invalidateIdSuggestions();
            }
        }
        if (button == 0 && deleteConfirmArmed
                && (deleteQuestButton == null || !deleteQuestButton.visible || !deleteQuestButton.isMouseOver(mouseX, mouseY))) {
            disarmDeleteConfirm();
        }
        boolean clickedToolbar = button == 0 && clickToolbarButtons(mouseX, mouseY);
        boolean clickedLeftList = button == 0 && leftList != null && leftList.visible && leftList.isMouseOver(mouseX, mouseY);
        if (button == 0 && pendingDiscardMode != null && !clickedToolbar && !clickedLeftList) {
            clearPendingDiscardState();
        }
        if (clickedToolbar) {
            return true;
        }
        if (button == 0 && clickIdSuggestion(mouseX, mouseY)) {
            return true;
        }
        if (clickDependencyRows(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && clickDropdownFields(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && !isInsideSuggestionTargetField(mouseX, mouseY)) {
            clearEntryTextFocus();
        }
        if (button == 0 && questTree != null && questTree.visible && questTree.isMouseOver(mouseX, mouseY)
                && questTree.tool() == Tool.NONE && !questTree.hasNodeAt(mouseX, mouseY) && !treeCategoryId.isBlank() && currentPack != null) {
            openCurrentCategorySettings();
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        return handled;
    }

    protected boolean clickTitleFormatterButton(double mouseX, double mouseY) {
        EditBox target = focusedTitleFormatBox();
        if (target == null) return false;
        for (TextInsertButton formatButton : descriptionFormatButtons) {
            if (!formatButton.visible || !formatButton.active || !formatButton.isMouseOver(mouseX, mouseY)) continue;
            formatButton.onPress();
            setFocused(target);
            return true;
        }
        return false;
    }

    protected boolean clickPackBackup(double mouseX, double mouseY) {
        if (editorType != EditorType.PACK_OPTIONS) return false;
        for (BackupRowHitbox hitbox : backupRowHitboxes) {
            if (!hitbox.contains(mouseX, mouseY)) continue;
            confirmRestoreBackup(hitbox.snapshot());
            return true;
        }
        return false;
    }

    protected boolean handleUnsavedChangesPopupClick(double mouseX, double mouseY, int button) {
        // modal clicks must not leak into the editor underneath
        if (button != 0) return true;
        boolean confirmingTreeDelete = pendingTreeDeleteAction != null;
        boolean exitingEditor = pendingEditorExit;
        boolean navigatingBack = pendingBackNavigation;
        String save = confirmingTreeDelete ? "Delete" : trs("popup.save");
        String discard = confirmingTreeDelete ? "Cancel" : trs("popup.discard");
        String message = confirmingTreeDelete ? pendingTreeDeleteMessage : trs("popup.unsaved_changes");
        int popupW = Math.max(116, font.width(message) + 22);
        int popupH = 48;
        int x = (this.width - popupW) / 2;
        int y = (this.height - popupH) / 2;
        int gap = 18;
        int actionY = y + 30;
        int saveX = x + popupW / 2 - font.width(save) - gap / 2;
        int discardX = x + popupW / 2 + gap / 2;
        if (isInsideText(mouseX, mouseY, saveX, actionY, save)) {
            if (confirmingTreeDelete) {
                Runnable action = pendingTreeDeleteAction;
                pendingTreeDeleteAction = null;
                pendingTreeDeleteMessage = "";
                action.run();
                return true;
            }
            saveCurrent();
            if (!hasUnsavedEditorChanges()) {
                if (exitingEditor) {
                    pendingEditorExit = false;
                    exitEditor();
                } else if (navigatingBack) {
                    performBackNavigation();
                } else {
                    continuePendingDiscardNavigation();
                }
            }
            return true;
        }
        if (isInsideText(mouseX, mouseY, discardX, actionY, discard)) {
            if (confirmingTreeDelete) {
                pendingTreeDeleteAction = null;
                pendingTreeDeleteMessage = "";
                return true;
            }
            if (exitingEditor) {
                pendingEditorExit = false;
                exitEditor();
            } else if (navigatingBack) {
                performBackNavigation();
            } else {
                continuePendingDiscardNavigation();
            }
            return true;
        }
        return true;
    }

    protected void continuePendingDiscardNavigation() {
        // resume the action that was paused by the unsaved changes popup
        EditorEntry entry = pendingDiscardEntry;
        if (entry == null && leftList != null) {
            entry = leftList.entryById(pendingDiscardEntryId);
        }
        if (entry == null) {
            clearPendingDiscardState();
            return;
        }
        handleLeftClick(entry);
    }

    protected boolean clickEmojiPicker(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int cell = 18;
        int pickerW = 81;
        int pickerH = 112;
        int x = pxRight + pw + 11;
        int y = py;
        if (mouseX < x || mouseX >= x + pickerW || mouseY < y || mouseY >= y + pickerH) {
            emojiPickerOpen = false;
            return true;
        }
        List<String> emojis = QuestEditorEmojis.names();
        int maxPage = Math.max(0, (emojis.size() - 1) / (EMOJI_PICKER_COLS * EMOJI_PICKER_ROWS));
        if (mouseY >= y + 99) {
            if (mouseX < x + 24) emojiPickerPage = Math.max(0, emojiPickerPage - 1);
            else if (mouseX > x + pickerW - 24) emojiPickerPage = Math.min(maxPage, emojiPickerPage + 1);
            return true;
        }
        if (mouseY < y + 10) return true;
        int col = (int) ((mouseX - (x + 4)) / cell);
        int row = (int) ((mouseY - (y + 10)) / cell);
        if (col < 0 || col >= EMOJI_PICKER_COLS || row < 0 || row >= EMOJI_PICKER_ROWS) return true;
        int index = emojiPickerPage * EMOJI_PICKER_COLS * EMOJI_PICKER_ROWS + row * EMOJI_PICKER_COLS + col;
        if (index >= 0 && index < emojis.size() && questDescriptionBox != null) {
            questDescriptionBox.insertText(QuestEditorEmojis.text(emojis.get(index)));
            questDescriptionBox.setFocused(true);
            markCurrentEditorUnsaved();
        }
        return true;
    }

    protected void renderEmojiButtonIcon(GuiGraphics gg) {
        for (TextInsertButton button : descriptionFormatButtons) {
            if ("__emoji__".equals(button.insertText()) && button.visible) {
                gg.blit(EMOJI_BUTTON_TEX, button.getX() + 1, button.getY() + 1, 0, 0, 10, 10, 10, 10);
                return;
            }
        }
    }

    protected void renderEmojiPicker(GuiGraphics gg, int mouseX, int mouseY) {
        // render emoji choices above the description editor and tooltips
        if (!emojiPickerOpen) return;
        int cell = 18;
        int width = 81;
        int height = 112;
        int x = pxRight + pw + 11;
        int y = py;
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 750);
        gg.blit(EMOJI_GUI_TEX, x, y, 0, 0, width, height, width, height);
        gg.drawCenteredString(font, tr("emoji_picker"), x + width / 2, y + 1, 0xFFFFFFFF);
        List<String> emojis = QuestEditorEmojis.names();
        int start = emojiPickerPage * EMOJI_PICKER_COLS * EMOJI_PICKER_ROWS;
        for (int i = 0; i < EMOJI_PICKER_COLS * EMOJI_PICKER_ROWS && start + i < emojis.size(); i++) {
            int px = x + 4 + (i % EMOJI_PICKER_COLS) * cell;
            int py = y + 10 + (i / EMOJI_PICKER_COLS) * cell;
            gg.blit(QuestEditorEmojis.texture(emojis.get(start + i)), px + 1, py + 1, 0, 0, 16, 16, 16, 16);
        }
        int maxPage = Math.max(0, (emojis.size() - 1) / (EMOJI_PICKER_COLS * EMOJI_PICKER_ROWS));
        gg.drawString(font, "<", x + 8, y + 101, emojiPickerPage > 0 ? 0xFF000000 : 0xFF444444, false);
        gg.drawString(font, ">", x + width - 12, y + 101, emojiPickerPage < maxPage ? 0xFF000000 : 0xFF444444, false);
        gg.pose().popPose();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!ensureEditorAccess()) return true;
        mouseX /= uiScale;
        mouseY /= uiScale;
        if (button == 0 && deleteHoldActive) {
            deleteHoldActive = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!ensureEditorAccess()) return true;
        return super.mouseDragged(mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    protected void clearEntryTextFocus() {
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) box.setFocused(false);
        for (ScaledMultiLineEditBox box : completionEntryBoxes) box.setFocused(false);
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) box.setFocused(false);
        for (EntryCountBox box : completionEntryCountBoxes) box.setFocused(false);
        for (EntryCountBox box : rewardEntryCountBoxes) box.setFocused(false);
    }

    protected EntryCountBox focusedEntryCountBox() {
        for (EntryCountBox box : completionEntryCountBoxes) {
            if (box != null && box.visible && box.active && box.isFocused()) return box;
        }
        for (EntryCountBox box : rewardEntryCountBoxes) {
            if (box != null && box.visible && box.active && box.isFocused()) return box;
        }
        return null;
    }

    protected boolean clickDependencyRows(double mouseX, double mouseY, int button) {
        // dependency locks receive clicks before their text rows
        if (button != 0) return false;
        if (editorType != EditorType.QUEST) return false;
        if (clickRowControls(EntryRowKind.COMPLETION, mouseX, mouseY, button)) return true;
        if (clickRowControls(EntryRowKind.REWARD, mouseX, mouseY, button)) return true;
        if (dependencyEntryBoxes.isEmpty()) return false;

        for (EntryRemoveButton removeButton : dependencyEntryRemoveButtons) {
            if (removeButton == null || !removeButton.visible || !removeButton.active) continue;
            if (!removeButton.isMouseOver(mouseX, mouseY)) continue;
            return removeButton.mouseClicked(mouseX, mouseY, button);
        }
        for (LockToggleButton lockButton : dependencyEntryLockButtons) {
            if (lockButton == null || !lockButton.visible || !lockButton.active) continue;
            if (!lockButton.isMouseOver(mouseX, mouseY)) continue;
            if (!lockButton.mouseClicked(mouseX, mouseY, button)) return false;
            setDependencyLockState(lockButton.isOn());
            return true;
        }
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box == null || !box.visible || !box.active) continue;
            if (!box.isMouseOver(mouseX, mouseY)) continue;
            if (box.mouseClicked(mouseX, mouseY, button)) return true;
            box.setFocused(true);
            return true;
        }
        return false;
    }

    protected boolean clickRowControls(EntryRowKind kind, double mouseX, double mouseY, int button) {
        // route remove type picker and count controls for one row group
        for (EntryCountBox countBox : entryCountBoxes(kind)) {
            if (countBox != null && countBox.visible && countBox.active && countBox.isMouseOver(mouseX, mouseY)) {
                clearEntryTextFocus();
                countBox.mouseClicked(mouseX, mouseY, button);
                countBox.setFocused(true);
                countBox.selectAllText();
                return true;
            }
        }
        for (EntryItemPickerButton pickerButton : entryItemPickerButtons(kind)) {
            if (pickerButton != null && pickerButton.visible && pickerButton.active && pickerButton.isMouseOver(mouseX, mouseY)) {
                clearEntryTextFocus();
                pickerButton.onPress();
                return true;
            }
        }
        for (EntryTypeButton typeButton : entryTypeButtons(kind)) {
            if (typeButton != null && typeButton.visible && typeButton.active && typeButton.isMouseOver(mouseX, mouseY)) {
                clearEntryTextFocus();
                typeButton.onPress();
                return true;
            }
        }
        for (EntryRemoveButton removeButton : entryRemoveButtons(kind)) {
            if (removeButton != null && removeButton.visible && removeButton.active && removeButton.isMouseOver(mouseX, mouseY)) {
                clearEntryTextFocus();
                return removeButton.mouseClicked(mouseX, mouseY, button);
            }
        }
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        for (int i = 0; i < rows.size(); i++) {
            ScaledMultiLineEditBox box = rows.get(i);
            if (box == null || !box.visible || !box.active || !box.isMouseOver(mouseX, mouseY)) continue;
            clearEntryTextFocus();
            if (box.mouseClicked(mouseX, mouseY, button)) return true;
            box.setFocused(true);
            return true;
        }
        return false;
    }

    protected boolean clickDropdownFields(double mouseX, double mouseY) {
        // open the shared dropdown for the clicked selector field
        if (editorType == EditorType.SUBCATEGORY && isInsideBox(subCategoryBox, mouseX, mouseY)) {
            openDropdownMenu(DropdownMenuTarget.SUBCATEGORY_PARENT, subCategoryBox);
            return true;
        }
        if (editorType == EditorType.QUEST && isInsideBox(questCategoryBox, mouseX, mouseY)) {
            openDropdownMenu(DropdownMenuTarget.QUEST_CATEGORY, questCategoryBox);
            return true;
        }
        if (editorType == EditorType.QUEST
                && !dropdownBoxValue(questCategoryBox).isBlank()
                && isInsideBox(questSubCategoryBox, mouseX, mouseY)) {
            openDropdownMenu(DropdownMenuTarget.QUEST_SUBCATEGORY, questSubCategoryBox);
            return true;
        }
        return false;
    }

    protected boolean clickToolbarButtons(double mouseX, double mouseY) {
        // toolbar hitboxes follow the same order they render in
        if (treeExpandButton != null && treeExpandButton.visible && treeExpandButton.active && treeExpandButton.isMouseOver(mouseX, mouseY)) {
            treeExpandButton.onPress();
            return true;
        }
        if (createPackButton != null && createPackButton.visible && createPackButton.active && createPackButton.isMouseOver(mouseX, mouseY)) {
            createPackButton.onPress();
            return true;
        }
        if (createEntryButton != null && createEntryButton.visible && createEntryButton.active && createEntryButton.isMouseOver(mouseX, mouseY)) {
            createEntryButton.onPress();
            return true;
        }
        if (treeMoveButton != null && treeMoveButton.visible && treeMoveButton.active && treeMoveButton.isMouseOver(mouseX, mouseY)) {
            treeMoveButton.onPress();
            return true;
        }
        if (treeDeleteButton != null && treeDeleteButton.visible && treeDeleteButton.active && treeDeleteButton.isMouseOver(mouseX, mouseY)) {
            treeDeleteButton.onPress();
            return true;
        }
        if (treeConnectorButton != null && treeConnectorButton.visible && treeConnectorButton.active && treeConnectorButton.isMouseOver(mouseX, mouseY)) {
            treeConnectorButton.onPress();
            return true;
        }
        if (importQuestPackButton != null && importQuestPackButton.visible && importQuestPackButton.active && importQuestPackButton.isMouseOver(mouseX, mouseY)) {
            importQuestPackButton.onPress();
            return true;
        }
        if (categoriesTabButton != null && categoriesTabButton.visible && categoriesTabButton.active && categoriesTabButton.isMouseOver(mouseX, mouseY)) {
            categoriesTabButton.onPress();
            return true;
        }
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible && subCategoriesTabButton.active && subCategoriesTabButton.isMouseOver(mouseX, mouseY)) {
            subCategoriesTabButton.onPress();
            return true;
        }
        if (questsTabButton != null && questsTabButton.visible && questsTabButton.active && questsTabButton.isMouseOver(mouseX, mouseY)) {
            questsTabButton.onPress();
            return true;
        }
        if (saveButton != null && saveButton.visible && saveButton.active && saveButton.isMouseOver(mouseX, mouseY)) {
            saveButton.onPress();
            return true;
        }
        if (exportPackButton != null && exportPackButton.visible && exportPackButton.active && exportPackButton.isMouseOver(mouseX, mouseY)) {
            exportPackButton.onPress();
            return true;
        }
        if (backButton != null && backButton.visible && backButton.active && backButton.isMouseOver(mouseX, mouseY)) {
            backButton.onPress();
            return true;
        }
        if (duplicateButton != null && duplicateButton.visible && duplicateButton.active
                && duplicateButton.isMouseOver(mouseX, mouseY)) {
            duplicateButton.onPress();
            return true;
        }
        if (deleteQuestButton != null && deleteQuestButton.visible && deleteQuestButton.active
                && deleteQuestButton.isMouseOver(mouseX, mouseY)) {
            handleDeleteButtonPress();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // foreground menus handle keys before focused editor fields
        if (!ensureEditorAccess()) return true;
        if (popupMenus.keyPressed(keyCode, this::dropdownOptions)) return true;
        if (questTree != null && questTree.visible && questTree.keyPressed(keyCode, scanCode, modifiers)) {
            if (treeTool != questTree.tool()) setTreeTool(questTree.tool());
            return true;
        }
        if (itemPickerSearchBox != null && itemPickerSearchBox.active && itemPickerSearchBox.isFocused()
                && itemPickerSearchBox.keyPressed(keyCode, scanCode, modifiers)) {
            itemPickerPage = 0;
            return true;
        }
        EntryCountBox focusedCountBox = focusedEntryCountBox();
        if (focusedCountBox != null && focusedCountBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && isItemPickerOpen()) {
            closeItemPicker();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (pendingTreeDeleteAction != null) {
                pendingTreeDeleteAction = null;
                pendingTreeDeleteMessage = "";
                return true;
            }
            if (isUnsavedChangesPopupOpen()) return true;
            requestExitEditor();
            return true;
        }
        suppressIdSuggestions = false;
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (!activeIdSuggestions.isEmpty() && (idSuggestionField != null || idSuggestionMultiLineField != null)) {
                String suggestion = activeIdSuggestions.get(0);
                if (idSuggestionField != null) {
                    applyIdSuggestion(idSuggestionField, suggestion);
                } else if (idSuggestionMultiLineField != null) {
                    applyMultiLineIdSuggestion(idSuggestionMultiLineField, suggestion);
                }
                return true;
            }
            EditBox focused = focusedEditBox();
            if (focused != null) {
                String suggestion = suggestionCaches.computeIconSuggestion(focused.getValue());
                if (suggestion != null && !suggestion.isBlank()) {
                    focused.setValue(suggestion);
                    focused.setCursorPosition(suggestion.length());
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // send text only to the active field or picker search
        if (!ensureEditorAccess()) return true;
        if (popupMenus.charTyped(codePoint, this::dropdownOptions)) return true;
        if (itemPickerSearchBox != null && itemPickerSearchBox.active && itemPickerSearchBox.isFocused()
                && itemPickerSearchBox.charTyped(codePoint, modifiers)) {
            itemPickerPage = 0;
            return true;
        }
        EntryCountBox focusedCountBox = focusedEntryCountBox();
        if (focusedCountBox != null && focusedCountBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        suppressIdSuggestions = false;
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box != null && box.visible && box.active && box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    protected void openTypeMenu(EntryRowKind kind, int row, int x, int y, int w) {
        // reuse the shared row type menu at the clicked control
        popupMenus.openTypeMenu(kind, row, x, y, w);
    }

    protected boolean clickTypeMenu(double mouseX, double mouseY) {
        return popupMenus.clickTypeMenu(mouseX, mouseY, (kind, selection) -> setRowType(kind, selection.row(), selection.type()));
    }

    protected void renderTypeMenu(GuiGraphics gg, int mouseX, int mouseY) {
        popupMenus.renderTypeMenu(gg, font, mouseX, mouseY);
    }

    protected boolean scrollTypeMenu(double mouseX, double mouseY, double delta) {
        return popupMenus.scrollTypeMenu(mouseX, mouseY, delta);
    }

    protected void openDropdownMenu(DropdownMenuTarget target, AbstractWidget box) {
        // reuse the shared dropdown and anchor it to the source widget
        popupMenus.openDropdownMenu(target, box, py, ph, this::dropdownOptions);
    }

    protected boolean clickDropdownMenu(double mouseX, double mouseY) {
        return popupMenus.clickDropdownMenu(mouseX, mouseY, this::dropdownOptions, this::applyDropdownSelection);
    }

    protected void renderDropdownMenu(GuiGraphics gg, int mouseX, int mouseY) {
        popupMenus.renderDropdownMenu(gg, font, mouseX, mouseY, this::dropdownOptions);
    }

    protected boolean scrollDropdownMenu(double mouseX, double mouseY, double delta) {
        return popupMenus.scrollDropdownMenu(mouseX, mouseY, delta, this::dropdownOptions);
    }

    protected List<String> dropdownOptions(DropdownMenuTarget target) {
        // build options from cached ids for the requested selector
        if (target == DropdownMenuTarget.QUEST_NAME_LANGUAGE
                || target == DropdownMenuTarget.QUEST_DESCRIPTION_LANGUAGE) {
            return QUEST_LANGUAGE_CODES;
        }
        List<String> out = new ArrayList<>();
        out.add(trs("none"));
        if (target == DropdownMenuTarget.SUBCATEGORY_PARENT || target == DropdownMenuTarget.QUEST_CATEGORY) {
            for (String id : categorySuggestionCache) {
                if (id == null || id.isBlank()) continue;
                if ("all".equalsIgnoreCase(id.trim())) continue;
                if (!out.contains(id)) out.add(id);
            }
            return out;
        }
        String categoryId = dropdownBoxValue(questCategoryBox);
        if (categoryId.isBlank()) return out;
        String key = categoryId.toLowerCase(Locale.ROOT);
        List<String> scoped = subCategoryByCategorySuggestion.getOrDefault(key, List.of());
        for (String id : scoped) {
            if (id == null || id.isBlank()) continue;
            if (!out.contains(id)) out.add(id);
        }
        return out;
    }

    protected void applyDropdownSelection(DropdownMenuTarget target, String value) {
        // write the selected value back to its owning field or row
        String normalized = normalizeDropdownValue(value);
        switch (target) {
            case SUBCATEGORY_PARENT -> setDropdownBoxValue(subCategoryBox, normalized);
            case QUEST_CATEGORY -> {
                String previous = dropdownBoxValue(questCategoryBox);
                setDropdownBoxValue(questCategoryBox, normalized);
                if (!Objects.equals(previous, normalized)) {
                    setDropdownBoxValue(questSubCategoryBox, "");
                }
                if (editorType == EditorType.QUEST) {
                    applyQuestEditorFields();
                }
            }
            case QUEST_SUBCATEGORY -> setDropdownBoxValue(questSubCategoryBox, normalized);
            case QUEST_NAME_LANGUAGE, QUEST_DESCRIPTION_LANGUAGE -> {
                if (openTranslationLanguageRow != null) {
                    openTranslationLanguageRow.locale = normalizeLanguageCode(value);
                    openTranslationLanguageRow.language.setTooltip(Tooltip.create(
                            Component.literal(openTranslationLanguageRow.locale)));
                }
                openTranslationLanguageRow = null;
            }
        }
    }

    protected void setRowType(EntryRowKind kind, int row, String type) {
        // change row type while preserving compatible body data
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row < 0 || row >= rows.size()) return;
        ScaledMultiLineEditBox box = rows.get(row);
        String normalized = QuestEditorEntryTypes.normalize(kind, type);
        String current = safe(box.getValue());
        ParsedEntry parsed = rowParsedBody(kind, box, current);
        entryTypeByBox.put(box, normalized);
        if (QuestEditorEntryTypes.pickerModeForType(kind, normalized) == PickerMode.NONE) {
            selectedItemIdByBox.remove(box);
            selectedItemIdsByBox.remove(box);
            selectedItemComponentsByBox.remove(box);
        }
        entryCountByBox.putIfAbsent(box, 1);
        if (usesCompactEntryBody(kind, normalized)) {
            String display = entryRowDisplayValue(kind, normalized, parsed, current);
            if (!current.equals(display)) {
                box.setValue(display);
                box.setCursorPosition(Math.min(box.getCursorPosition(), display.length()));
            }
        }
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    protected boolean isItemPickerOpen() {
        return itemPickerIconTarget != null || (itemPickerKind != null && itemPickerRow >= 0);
    }

    protected boolean itemPickerAllowsTags() {
        if (pickerMode != PickerMode.ITEMS || itemPickerIconTarget != null || itemPickerKind != EntryRowKind.COMPLETION) return false;
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size()) return false;
        String type = effectiveRowType(itemPickerKind, rows.get(itemPickerRow));
        return "collect".equals(type) || "submit".equals(type) || "item".equals(type);
    }

    protected void openIconItemPicker(EditBox target) {
        // open the picker in single selection icon mode
        if (target == null) return;
        itemPickerIconTarget = target;
        itemPickerKind = null;
        itemPickerRow = -1;
        pickerMode = PickerMode.ITEMS;
        itemPickerTab = target == catBackgroundBox && catBackgroundMode != CategoryBackgroundMode.TILEABLE
                ? ItemPickerTab.CUSTOM : ItemPickerTab.CREATIVE;
        itemPickerPage = 0;
        if (itemPickerSearchBox == null) {
            itemPickerSearchBox = new EditBox(font, 0, 0, ITEM_PICKER_SEARCH_W, ITEM_PICKER_SEARCH_H, tr("search_picker"));
            itemPickerSearchBox.setMaxLength(128);
            itemPickerSearchBox.setBordered(false);
            itemPickerSearchBox.setResponder(value -> {
                itemPickerSearchQuery = safe(value);
                itemPickerPage = 0;
                invalidateTagPageCache();
            });
            addRenderableWidget(itemPickerSearchBox);
        }
        itemPickerSearchBox.setValue("");
        itemPickerSearchBox.setFocused(true);
        itemPickerSearchBox.visible = true;
        itemPickerSearchBox.active = true;
    }

    protected void openItemPicker(EntryRowKind kind, int row) {
        // configure the shared picker from the selected row type
        if (row < 0 || row >= entryRows(kind).size()) return;
        // reuse one picker across item and registry sources
        String type = effectiveRowType(kind, entryRows(kind).get(row));
        PickerMode mode = QuestEditorEntryTypes.pickerModeForType(kind, type);
        if (mode == PickerMode.NONE) return;
        itemPickerIconTarget = null;
        itemPickerKind = kind;
        itemPickerRow = row;
        pickerMode = mode;
        if (!itemPickerAllowsTags() && itemPickerTab == ItemPickerTab.TAGS) {
            itemPickerTab = ItemPickerTab.CREATIVE;
        }
        itemPickerPage = 0;
        if (itemPickerSearchBox == null) {
            itemPickerSearchBox = new EditBox(font, 0, 0, ITEM_PICKER_SEARCH_W, ITEM_PICKER_SEARCH_H, tr("search_picker"));
            itemPickerSearchBox.setMaxLength(128);
            itemPickerSearchBox.setBordered(false);
            itemPickerSearchBox.setResponder(value -> {
                itemPickerSearchQuery = safe(value);
                itemPickerPage = 0;
                invalidateTagPageCache();
            });
            addRenderableWidget(itemPickerSearchBox);
        }
        itemPickerPendingSelection.clear();
        itemPickerOriginalSelection.clear();
        List<String> existing = selectedIdsForRow(entryRows(kind).get(row));
        itemPickerOriginalSelection.addAll(existing);
        itemPickerPendingSelection.addAll(existing);
        itemPickerMultiSelect = (mode == PickerMode.ITEMS || mode == PickerMode.MOBS) && existing.size() > 1;
        itemPickerSearchBox.setValue("");
        itemPickerSearchBox.setFocused(true);
        itemPickerSearchBox.visible = true;
        itemPickerSearchBox.active = true;
        itemPickerSearchQuery = "";
        invalidateTagPageCache();
    }

    protected void closeItemPicker() {
        // clear picker focus and pending multi selection state
        itemPickerKind = null;
        itemPickerRow = -1;
        itemPickerIconTarget = null;
        itemPickerMultiSelect = false;
        itemPickerPendingSelection.clear();
        itemPickerOriginalSelection.clear();
        if (itemPickerSearchBox != null) {
            itemPickerSearchBox.visible = false;
            itemPickerSearchBox.active = false;
            itemPickerSearchBox.setFocused(false);
        }
    }

    protected boolean clickItemPicker(double mouseX, double mouseY) {
        // handle picker tabs search results and footer actions in order
        if (!isItemPickerOpen()) return false;
        int x = itemPickerX();
        int y = itemPickerY();
        int closeX = pickerCloseX(x);
        int closeY = pickerCloseY(y);
        if (mouseX >= closeX && mouseX <= closeX + ITEM_PICKER_CLOSE_SIZE && mouseY >= closeY && mouseY <= closeY + ITEM_PICKER_CLOSE_SIZE) {
            closeItemPicker();
            return true;
        }
        if (isItemPickerMultiToggleAvailable()) {
            if (QuestEditorItemPickerRenderer.isPointWithin(mouseX, mouseY, x + 6, y + ITEM_PICKER_H + 4, 86, 16)) {
                if (itemPickerMultiSelect && itemPickerPendingSelection.size() > 1) {
                    itemPickerMultiSelect = true;
                    statusMessage = trs("status.reduce_selection_before_disable");
                    statusColor = 0xFF8080;
                } else {
                    itemPickerMultiSelect = !itemPickerMultiSelect;
                    if (itemPickerMultiSelect && itemPickerPendingSelection.isEmpty()) {
                        itemPickerPendingSelection.addAll(itemPickerOriginalSelection);
                    }
                }
                return true;
            }
            if (QuestEditorItemPickerRenderer.isPointWithin(mouseX, mouseY, x + ITEM_PICKER_W - 62, y + ITEM_PICKER_H + 4, 56, 16)) {
                if (!itemPickerPendingSelection.isEmpty()) {
                    applyItemPickerMultiSelection();
                    closeItemPicker();
                }
                return true;
            }
        }
        if (itemPickerSearchBox != null && itemPickerSearchBox.isMouseOver(mouseX, mouseY)) {
            return itemPickerSearchBox.mouseClicked(mouseX, mouseY, 0);
        }
        if ((pickerMode == PickerMode.ITEMS || itemPickerIconTarget != null) && clickPickerSideTab(mouseX, mouseY, x, y)) {
            return true;
        }
        if (itemPickerIconTarget != null && itemPickerTab == ItemPickerTab.CUSTOM
                && QuestEditorItemPickerRenderer.isPointWithin(mouseX, mouseY, x + 24, y + ITEM_PICKER_H + 4, ITEM_PICKER_W - 48, 16)) {
            openCustomAssetsDirectory();
            return true;
        }
        if (mouseX < x || mouseX > x + ITEM_PICKER_W || mouseY < y || mouseY > y + ITEM_PICKER_H) {
            closeItemPicker();
            return true;
        }
        int gridX = x + ITEM_PICKER_GRID_X;
        int gridY = y + ITEM_PICKER_GRID_Y;
        if (mouseX >= gridX && mouseX < x + ITEM_PICKER_GRID_RIGHT
                && mouseY >= gridY && mouseY < y + ITEM_PICKER_GRID_BOTTOM) {
            int col = (int) ((mouseX - gridX) / ITEM_PICKER_CELL);
            int row = (int) ((mouseY - gridY) / ITEM_PICKER_CELL);
            int idx = row * ITEM_PICKER_COLS + col;
            PickerPageData page = currentPickerPageData();
            List<ItemStack> items = page.items();
            List<String> ids = page.ids();
            if (idx >= 0 && idx < items.size()) {
                if (itemPickerMultiSelect && isItemPickerMultiToggleAvailable()) {
                    String selectedId = idx < ids.size() ? ids.get(idx) : pickerSelectionId(items.get(idx));
                    togglePendingItemPickerSelection(selectedId);
                    return true;
                }
                if (pickerMode == PickerMode.ITEMS && itemPickerTab == ItemPickerTab.TAGS && itemPickerAllowsTags()) {
                    if (idx < ids.size()) applyPickedItemTag(ids.get(idx));
                } else if (pickerMode == PickerMode.MOBS) {
                    if (idx < ids.size()) applyPickedMob(ids.get(idx));
                } else if (itemPickerIconTarget != null && itemPickerTab == ItemPickerTab.EFFECTS) {
                    if (idx < ids.size()) {
                        String id = ids.get(idx);
                        applyPickedIcon(id.startsWith(QuestEditorEmojis.ICON_PREFIX) ? id : "boundless:effect/" + id.replace(':', '/'));
                    }
                } else if (itemPickerIconTarget != null && itemPickerTab == ItemPickerTab.CUSTOM) {
                    if (idx < ids.size()) applyPickedIcon(ids.get(idx));
                } else {
                    applyPickedItem(items.get(idx));
                }
                closeItemPicker();
            }
            return true;
        }
        return true;
    }

    protected boolean scrollItemPicker(double mouseX, double mouseY, double delta) {
        if (!isItemPickerOpen()) return false;
        int x = itemPickerX();
        int y = itemPickerY();
        if (mouseX < x || mouseX > x + ITEM_PICKER_W || mouseY < y || mouseY > y + ITEM_PICKER_H) return false;
        int pageSize = ITEM_PICKER_COLS * ITEM_PICKER_ROWS;
        int resultCount = currentPickerPageData().totalCount();
        int maxPage = Math.max(0, (resultCount - 1) / pageSize);
        itemPickerPage = Mth.clamp(itemPickerPage - (delta > 0 ? 1 : -1), 0, maxPage);
        return true;
    }

    protected void renderItemPicker(GuiGraphics gg, int mouseX, int mouseY) {
        // draw the picker above the editor and below final tooltips
        if (!isItemPickerOpen()) return;
        QuestEditorItemPickerRenderer.render(new RenderRequest(
                gg,
                font,
                itemPickerSearchBox,
                width,
                height,
                mouseX,
                mouseY,
                pickerMode,
                itemPickerTab,
                itemPickerIconTarget != null,
                itemPickerAllowsTags(),
                isItemPickerMultiToggleAvailable(),
                itemPickerMultiSelect,
                itemPickerPendingSelection,
                currentPickerPageData(),
                suggestionCaches.effectSuggestions(),
                this::pickerSelectionId,
                itemPickerIconTarget == catBackgroundBox && catBackgroundMode == CategoryBackgroundMode.TILEABLE,
                itemPickerIconTarget == catBackgroundBox && catBackgroundMode != CategoryBackgroundMode.TILEABLE
        ));
    }

    protected boolean isItemPickerMultiToggleAvailable() {
        return (pickerMode == PickerMode.ITEMS || pickerMode == PickerMode.MOBS)
                && (itemPickerKind == EntryRowKind.COMPLETION
                || (itemPickerKind == EntryRowKind.REWARD && pickerMode == PickerMode.ITEMS));
    }

    protected void togglePendingItemPickerSelection(String id) {
        String normalized = safe(id).trim();
        if (normalized.isBlank()) return;
        if (itemPickerPendingSelection.contains(normalized)) itemPickerPendingSelection.remove(normalized);
        else itemPickerPendingSelection.add(normalized);
    }

    protected void applyItemPickerMultiSelection() {
        // commit pending selections to the active repeatable row
        if (!isItemPickerOpen() || itemPickerKind == null) return;
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size()) return;
        ScaledMultiLineEditBox box = rows.get(itemPickerRow);
        List<String> acceptedIds = new ArrayList<>(itemPickerPendingSelection);
        if (acceptedIds.isEmpty()) return;
        selectedItemIdByBox.put(box, QuestItemSpec.stripComponents(acceptedIds.get(0)));
        selectedItemIdsByBox.put(box, List.copyOf(acceptedIds));
        selectedItemComponentsByBox.remove(box);
        box.setValue(displayNameForEntrySelection(itemPickerKind, box, acceptedIds));
        box.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    protected void invalidateTagPageCache() {
        itemPickerData.invalidate();
    }

    protected int itemPickerX() {
        return QuestEditorItemPickerRenderer.pickerX(width);
    }

    protected int itemPickerY() {
        return QuestEditorItemPickerRenderer.pickerY(height);
    }

    protected boolean clickPickerSideTab(double mouseX, double mouseY, int pickerX, int pickerY) {
        // switch picker sources without closing the current search
        if (itemPickerIconTarget != null) {
            if (itemPickerIconTarget == catBackgroundBox) {
                ItemPickerTab allowedTab = catBackgroundMode == CategoryBackgroundMode.TILEABLE
                        ? ItemPickerTab.CREATIVE : ItemPickerTab.CUSTOM;
                boolean overTab = QuestEditorItemPickerRenderer.isMouseOverSideTab(
                        mouseX, mouseY, pickerX, pickerY, allowedTab, false);
                if (overTab) itemPickerTab = allowedTab;
                return overTab;
            }
            if (QuestEditorItemPickerRenderer.isMouseOverSideTab(mouseX, mouseY, pickerX, pickerY, ItemPickerTab.CREATIVE, false)) {
                itemPickerTab = ItemPickerTab.CREATIVE;
            } else if (QuestEditorItemPickerRenderer.isMouseOverSideTab(mouseX, mouseY, pickerX, pickerY, ItemPickerTab.EFFECTS, false)) {
                itemPickerTab = ItemPickerTab.EFFECTS;
            } else if (QuestEditorItemPickerRenderer.isMouseOverSideTab(mouseX, mouseY, pickerX, pickerY, ItemPickerTab.CUSTOM, false)) {
                itemPickerTab = ItemPickerTab.CUSTOM;
            } else return false;
            itemPickerPage = 0;
            itemPickerSearchQuery = "";
            if (itemPickerSearchBox != null) itemPickerSearchBox.setValue("");
            if (itemPickerTab == ItemPickerTab.CUSTOM) {
                statusMessage = trs("status.custom_assets_help");
                statusColor = 0xFFFFFFFF;
            } else if (statusColor == 0xFFFFFFFF) {
                statusMessage = "";
            }
            return true;
        }
        if (QuestEditorItemPickerRenderer.isMouseOverSideTab(mouseX, mouseY, pickerX, pickerY, ItemPickerTab.CREATIVE, itemPickerAllowsTags())) {
            itemPickerTab = ItemPickerTab.CREATIVE;
            itemPickerPage = 0;
            return true;
        }
        if (itemPickerAllowsTags() && QuestEditorItemPickerRenderer.isMouseOverSideTab(mouseX, mouseY, pickerX, pickerY, ItemPickerTab.TAGS, true)) {
            itemPickerTab = ItemPickerTab.TAGS;
            itemPickerPage = 0;
            return true;
        }
        if (QuestEditorItemPickerRenderer.isMouseOverSideTab(mouseX, mouseY, pickerX, pickerY, ItemPickerTab.INVENTORY, itemPickerAllowsTags())) {
            itemPickerTab = ItemPickerTab.INVENTORY;
            itemPickerPage = 0;
            return true;
        }
        return false;
    }

    protected int pickerCloseX(int pickerX) {
        return QuestEditorItemPickerRenderer.closeX(pickerX);
    }

    protected int pickerCloseY(int pickerY) {
        return QuestEditorItemPickerRenderer.closeY(pickerY);
    }

    protected String pickerSelectionId(ItemStack stack) {
        return itemPickerData.pickerSelectionId(stack, pickerMode, itemPickerTab, minecraft);
    }

    protected PickerPageData currentPickerPageData() {
        // cache filtered picker pages while the search text is unchanged
        if (itemPickerIconTarget == catBackgroundBox && catBackgroundMode == CategoryBackgroundMode.TILEABLE) {
            String query = safe(itemPickerSearchQuery).trim().toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            Set<String> seenTextures = new HashSet<>();
            for (String id : suggestionCaches.itemSuggestions()) {
                ResourceLocation resource = ResourceLocation.tryParse(id);
                if (resource == null || !query.isBlank() && !id.contains(query)) continue;
                if (!(BuiltInRegistries.ITEM.get(resource) instanceof BlockItem blockItem)) continue;
                Block block = blockItem.getBlock();
                if (!isFullBackgroundBlock(block)) continue;
                TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModelShaper()
                        .getParticleIcon(block.defaultBlockState());
                if (!seenTextures.add(sprite.contents().name().toString())) continue;
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                if (blockId != null) ids.add(blockId.toString());
            }
            int pageSize = ITEM_PICKER_COLS * ITEM_PICKER_ROWS;
            int maxPage = Math.max(0, (ids.size() - 1) / pageSize);
            itemPickerPage = Mth.clamp(itemPickerPage, 0, maxPage);
            int start = Math.min(ids.size(), itemPickerPage * pageSize);
            int end = Math.min(ids.size(), start + pageSize);
            List<ItemStack> items = new ArrayList<>();
            for (int index = start; index < end; index++) {
                items.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(ids.get(index)))));
            }
            return new PickerPageData(ids.size(), items, ids.subList(start, end));
        }
        if (itemPickerIconTarget != null && itemPickerTab == ItemPickerTab.CUSTOM) {
            List<String> ids = QuestEditorCustomIcons.ids();
            String query = safe(itemPickerSearchQuery).toLowerCase(Locale.ROOT);
            ids = ids.stream().filter(id -> query.isBlank() || id.contains(query)).toList();
            int start = Math.min(ids.size(), itemPickerPage * ITEM_PICKER_COLS * ITEM_PICKER_ROWS);
            int end = Math.min(ids.size(), start + ITEM_PICKER_COLS * ITEM_PICKER_ROWS);
            List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
            for (int i = start; i < end; i++) items.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER));
            return new PickerPageData(ids.size(), items, ids.subList(start, end));
        }
        if (itemPickerIconTarget != null && itemPickerTab == ItemPickerTab.EFFECTS) {
            String query = safe(itemPickerSearchQuery).trim().toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            for (String id : suggestionCaches.effectSuggestions()) if (query.isBlank() || id.contains(query)) ids.add(id);
            for (String name : QuestEditorEmojis.names()) if (query.isBlank() || name.contains(query)) ids.add(QuestEditorEmojis.iconId(name));
            int start = Math.min(ids.size(), itemPickerPage * ITEM_PICKER_COLS * ITEM_PICKER_ROWS);
            int end = Math.min(ids.size(), start + ITEM_PICKER_COLS * ITEM_PICKER_ROWS);
            List<ItemStack> items = new ArrayList<>();
            for (int i = start; i < end; i++) {
                String id = ids.get(i);
                items.add(id.startsWith(QuestEditorEmojis.ICON_PREFIX) ? new ItemStack(net.minecraft.world.item.Items.PAPER) : QuestEditorItemIcons.effectIconStack(id));
            }
            return new PickerPageData(ids.size(), items, ids.subList(start, end));
        }
        PickerMode activeMode = itemPickerIconTarget != null && itemPickerTab == ItemPickerTab.EFFECTS ? PickerMode.EFFECTS : pickerMode;
        PickerPageResult result = itemPickerData.currentPickerPageData(new PickerPageRequest(
                activeMode,
                itemPickerTab,
                itemPickerPage,
                itemPickerSearchQuery,
                ITEM_PICKER_COLS * ITEM_PICKER_ROWS,
                suggestionCaches,
                minecraft
        ));
        itemPickerPage = result.page();
        return result.data();
    }

    protected boolean isFullBackgroundBlock(Block block) {
        if (block == null) return false;
        var state = block.defaultBlockState();
        return !state.isAir()
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                && state.getRenderShape() != net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    protected void applyPickedItem(ItemStack picked) {
        // convert the selected stack into the active row or icon value
        if (!isItemPickerOpen()) return;
        if (itemPickerIconTarget != null) {
            if (picked == null || picked.isEmpty()) return;
            ResourceLocation id;
            if (itemPickerIconTarget == catBackgroundBox) {
                if (catBackgroundMode != CategoryBackgroundMode.TILEABLE) return;
                if (!(picked.getItem() instanceof BlockItem blockItem)) {
                    setError(trs("error.background_must_be_block"));
                    return;
                }
                id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            } else {
                id = BuiltInRegistries.ITEM.getKey(picked.getItem());
            }
            if (id == null) return;
            if (itemPickerIconTarget == stickerIconPickerTarget) {
                if (questTree != null) questTree.addIconSticker(pendingStickerIconX, pendingStickerIconY, id.toString());
                return;
            }
            itemPickerIconTarget.setValue(id.toString());
            itemPickerIconTarget.setCursorPosition(id.toString().length());
            if (itemPickerIconTarget == catBackgroundBox) {
                applyCategoryEditorFields();
                queueCategoryBackgroundAutosave(true);
            }
            else markCurrentEditorUnsaved();
            return;
        }
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size() || picked == null || picked.isEmpty()) return;
        ScaledMultiLineEditBox box = rows.get(itemPickerRow);
        String id;
        if (pickerMode == PickerMode.EFFECTS) {
            id = QuestEditorItemIcons.effectIdFromStack(picked, suggestionCaches.effectSuggestions());
        } else if (pickerMode == PickerMode.MOBS) {
            id = QuestEditorItemIcons.mobIdFromStack(picked, suggestionCaches.entitySuggestions());
        } else {
            id = BuiltInRegistries.ITEM.getKey(picked.getItem()).toString();
        }
        if (id == null || id.isBlank()) return;
        selectedItemIdByBox.put(box, id);
        if (pickerMode == PickerMode.ITEMS && itemPickerTab == ItemPickerTab.INVENTORY) {
            String components = safe(QuestItemSpec.describeStackComponents(
                    picked,
                    minecraft == null || minecraft.level == null ? null : minecraft.level.registryAccess()
            )).trim();
            if (components.isBlank() || "{}".equals(components)) selectedItemComponentsByBox.remove(box);
            else selectedItemComponentsByBox.put(box, components);
            selectedItemIdsByBox.put(box, List.of(pickerSelectionId(picked)));
        } else {
            selectedItemComponentsByBox.remove(box);
            selectedItemIdsByBox.put(box, List.of(id));
        }
        String itemName = picked.getHoverName().getString();
        String value = switch (pickerMode) {
            case MOBS -> itemName + " 1";
            case EFFECTS -> itemName;
            default -> itemName;
        };
        if (QuestEditorEntryTypes.rowHasCount(itemPickerKind, effectiveRowType(itemPickerKind, box))) {
            entryCountByBox.put(box, Math.max(1, picked.getCount()));
        }
        box.setValue(value);
        box.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    protected void applyPickedIcon(String id) {
        // write an icon id to the field that opened the picker
        if (itemPickerIconTarget == null || id == null || id.isBlank()) return;
        if (itemPickerIconTarget == stickerIconPickerTarget) {
            if (questTree != null) questTree.addIconSticker(pendingStickerIconX, pendingStickerIconY, id);
            return;
        }
        if (itemPickerIconTarget == catBackgroundBox) {
            if (catBackgroundMode == CategoryBackgroundMode.TILEABLE) {
                setError(trs("error.background_must_be_full_block"));
                return;
            }
            itemPickerIconTarget.setValue(id);
            itemPickerIconTarget.setCursorPosition(id.length());
            applyCategoryEditorFields();
            queueCategoryBackgroundAutosave(true);
            return;
        }
        itemPickerIconTarget.setValue(id);
        itemPickerIconTarget.setCursorPosition(id.length());
        markCurrentEditorUnsaved();
    }

    protected void openCustomAssetsDirectory() {
        try {
            java.nio.file.Files.createDirectories(QuestEditorCustomIcons.directory());
            Util.getPlatform().openUri(QuestEditorCustomIcons.directory().toUri());
        } catch (Exception e) {
            setError(trs("error.open_assets_folder_failed"));
        }
    }

    protected void applyPickedItemTag(String tagId) {
        // write the selected item tag using the row compact format
        if (!isItemPickerOpen()) return;
        String normalized = QuestEditorEntryCodec.normalizeNamespacedId(tagId, true);
        if (normalized.isBlank() || !normalized.startsWith("#")) return;
        if (itemPickerIconTarget != null) {
            itemPickerIconTarget.setValue(normalized);
            itemPickerIconTarget.setCursorPosition(normalized.length());
            markCurrentEditorUnsaved();
            return;
        }
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size()) return;
        ScaledMultiLineEditBox box = rows.get(itemPickerRow);
        selectedItemIdByBox.put(box, normalized);
        selectedItemIdsByBox.put(box, List.of(normalized));
        selectedItemComponentsByBox.remove(box);
        if (QuestEditorEntryTypes.rowHasCount(itemPickerKind, effectiveRowType(itemPickerKind, box))) {
            entryCountByBox.put(box, 1);
        }
        box.setValue(normalized);
        box.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    protected void applyPickedMob(String mobId) {
        // write the selected mob id to the active completion row
        if (!isItemPickerOpen()) return;
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size()) return;
        if (mobId == null || mobId.isBlank()) return;
        ScaledMultiLineEditBox box = rows.get(itemPickerRow);
        selectedItemIdByBox.put(box, mobId);
        selectedItemIdsByBox.put(box, List.of(mobId));
        selectedItemComponentsByBox.remove(box);
        entryCountByBox.put(box, 1);
        box.setValue(QuestEditorItemIcons.mobDisplayNameForMobId(mobId).getString());
        box.setFocused(true);
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    protected ItemStack selectedItemForRow(EntryRowKind kind, int row) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row < 0 || row >= rows.size()) return ItemStack.EMPTY;
        ScaledMultiLineEditBox box = rows.get(row);
        String type = effectiveRowType(kind, box);
        String idPart = safe(selectedItemIdByBox.get(box)).trim();
        if (idPart.isBlank()) {
            ParsedEntry parsed = QuestEditorEntryCodec.parseEntry(composeEntryRowLine(kind, box));
            if (parsed == null || parsed.id.isBlank()) return ItemStack.EMPTY;
            idPart = QuestItemSpec.stripComponents(parsed.id.trim().split("\s+")[0]);
        }
        return QuestEditorItemIcons.selectedItemStack(idPart, type, itemPickerData.itemTagIconItemCache());
    }

    protected boolean isIconBox(EditBox box) {
        return box == packIconPathBox || box == questIconBox || box == catIconBox
                || box == catBackgroundBox || box == subIconBox;
    }

    protected EditBox focusedEditBox() {
        if (packIconPathBox != null && packIconPathBox.isFocused()) return packIconPathBox;
        if (questIconBox != null && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.isFocused()) return catIconBox;
        if (catBackgroundBox != null && catBackgroundBox.isFocused()) return catBackgroundBox;
        if (subIconBox != null && subIconBox.isFocused()) return subIconBox;
        return null;
    }

    protected static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt((raw == null ? "" : raw).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    protected void selectQuestMediaFile() {
        // copy selected media into the configured quest media root
        try {
            Files.createDirectories(Config.questMediaRoot());
            String selected;
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                org.lwjgl.PointerBuffer filters = stack.mallocPointer(4);
                filters.put(stack.UTF8("*.png"));
                filters.put(stack.UTF8("*.jpg"));
                filters.put(stack.UTF8("*.jpeg"));
                filters.put(stack.UTF8("*.gif"));
                filters.flip();
                selected = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select quest image or looping GIF",
                        Config.questMediaRoot().toAbsolutePath().toString(),
                        filters,
                        "Images and looping GIFs",
                        false);
            }
            if (selected == null || selected.isBlank()) return;
            Path source = Path.of(selected).toAbsolutePath().normalize();
            String fileName = source.getFileName() == null ? "quest-media.png" : source.getFileName().toString();
            Path target = Config.questMediaRoot().resolve(fileName).normalize();
            if (!target.startsWith(Config.questMediaRoot().normalize())) throw new IOException("Invalid media path");
            if (!source.equals(target.toAbsolutePath().normalize())) {
                Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            questMediaSelector.setValue(fileName);
            net.revilodev.boundless.client.QuestMediaRenderer.clear();
            markCurrentEditorUnsaved();
            statusMessage = trs("status.media_copied");
            statusColor = 0xA0FFA0;
        } catch (Exception exception) {
            setError(trs("error.import_media_failed"));
        }
    }

    protected void renderCategoryBackgroundPreview(GuiGraphics gg, EditBox box) {
        // preview tile blocks or custom assets inside the background field
        int iconX = box.getX() + 2;
        int iconY = box.getY() + (box.getHeight() - 16) / 2;
        if (catBackgroundMode == CategoryBackgroundMode.TILEABLE) {
            ResourceLocation id = ResourceLocation.tryParse(safe(box.getValue()).trim());
            if (id == null) return;
            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block == null) return;
            TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModelShaper()
                    .getParticleIcon(block.defaultBlockState());
            gg.blit(iconX, iconY, 0, 16, 16, sprite);
            return;
        }
        ResourceLocation texture = QuestEditorCustomIcons.texture(safe(box.getValue()).trim());
        if (texture != null) gg.blit(texture, iconX, iconY, 0, 0, 16, 16, 16, 16);
    }

    // editor widgets that need direct access to the active screen state
    protected final class CategoryWorkspaceTab extends AbstractWidget {
        protected final NamedEntry category;

        CategoryWorkspaceTab(NamedEntry category, int y) {
            super(leftX - TAB_W + SIDE_TAB_X_OFFSET, y, TAB_W, TAB_H, Component.empty());
            this.category = category;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            if (category == null) {
                if (hasUnsavedEditorChanges()) {
                    setError(trs("error.save_before_navigation"));
                    return true;
                }
                flushCategoryBackgroundAutosave();
                showCategoryEditor(new CategoryData(), null);
                return true;
            }
            flushCategoryBackgroundAutosave();
            selectTreeCategory(category);
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean selected = category != null && category.id.equals(treeCategoryId);
            int x = getX() + (selected ? 0 : 1);
            gg.blit(selected ? ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png")
                            : ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png"),
                    x, getY(), 0, 0, TAB_W, TAB_H, TAB_W, TAB_H);
            if (category == null) {
                ResourceLocation plus = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/tree/button-zoom-in.png");
                gg.pose().pushPose();
                gg.pose().translate(x + 12, getY() + 8, 0);
                gg.pose().scale(1.5f, 1.5f, 1f);
                gg.blit(plus, 0, 0, 0, 0, 7, 7, 7, 7);
                gg.pose().popPose();
                if (isMouseOver(mouseX, mouseY)) queueEditorTooltip(tr("tooltip.create_category_tree"), mouseX, mouseY);
                return;
            }
            ItemStack icon = QuestEditorItemIcons.iconStackFromId(category.icon);
            if (!icon.isEmpty()) {
                gg.renderItem(icon, x + (TAB_W - 16) / 2, getY() + 5);
            }
            if (isMouseOver(mouseX, mouseY)) queueEditorTooltip(Component.literal(category.name), mouseX, mouseY);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // translation row keeps its locale field and actions together
    protected final class TranslationRow {
        protected final boolean description;
        protected String locale;
        protected final ScaledMultiLineEditBox box;
        protected final TranslationLanguageButton language;
        protected final TranslationRemoveButton remove;

        protected TranslationRow(boolean description, String locale) {
            this.description = description;
            this.locale = locale;
            this.box = new ScaledMultiLineEditBox(font, 0, 0, 80, ENTRY_ROW_H,
                    tr(description ? "translation.description" : "translation.title"),
                    Component.empty(), ENTRY_INPUT_TEXT_SCALE, false);
            this.box.setCharacterLimit(description ? 32768 : 512);
            this.box.setValueListener(value -> ensureTrailingTranslationRow(description));
            this.language = new TranslationLanguageButton(this);
            this.remove = new TranslationRemoveButton(this);
            addRenderableWidget(box);
            addRenderableWidget(language);
            addRenderableWidget(remove);
            setVisible(false);
        }

        protected int height() {
            if (!description) return ENTRY_ROW_H;
            int content = (int) Math.ceil(Math.max(1, box.getLineCount()) * Math.max(1.0, box.getLineHeight())) + 4;
            return Math.max(ENTRY_ROW_H, Math.min(64, content));
        }

        protected void setVisible(boolean visible) {
            box.visible = visible;
            box.active = visible;
            language.visible = visible;
            language.active = visible;
            remove.visible = visible;
            remove.active = visible;
        }

        protected void removeWidgets() {
            removeWidget(box);
            removeWidget(language);
            removeWidget(remove);
        }
    }

    // globe button opens and closes alternate language rows
    protected final class LanguageToggleButton extends AbstractButton {
        protected final boolean description;

        protected LanguageToggleButton(boolean description) {
            super(0, 0, 14, BOX_H, Component.empty());
            this.description = description;
            visible = false;
            active = false;
            setTooltip(Tooltip.create(tr("tooltip.edit_translations")));
        }

        protected void setVisible(boolean visible) {
            this.visible = visible;
            this.active = visible;
        }

        @Override
        public void onPress() {
            if (description) questDescriptionTranslationsOpen = !questDescriptionTranslationsOpen;
            else questNameTranslationsOpen = !questNameTranslationsOpen;
            ensureTrailingTranslationRow(description);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = description ? questDescriptionTranslationsOpen : questNameTranslationsOpen;
            int color = isMouseOver(mouseX, mouseY) || selected ? 0xFFFFFFFF : 0xFFA0A0A0;
            RenderSystem.setShaderColor((color >> 16 & 0xFF) / 255.0f,
                    (color >> 8 & 0xFF) / 255.0f, (color & 0xFF) / 255.0f, 1.0f);
            graphics.blit(LANGUAGE_ICON, getX() + (width - 9) / 2, getY() + (height - 9) / 2,
                    0, 0, 9, 9, 9, 9);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // compact language button opens the shared locale dropdown
    protected final class TranslationLanguageButton extends AbstractButton {
        protected final TranslationRow row;

        protected TranslationLanguageButton(TranslationRow row) {
            super(0, 0, 12, 12, Component.empty());
            this.row = row;
            setTooltip(Tooltip.create(Component.literal(row.locale)));
        }

        @Override
        public void onPress() {
            openTranslationLanguageRow = row;
            openDropdownMenu(row.description
                    ? DropdownMenuTarget.QUEST_DESCRIPTION_LANGUAGE
                    : DropdownMenuTarget.QUEST_NAME_LANGUAGE, row.box);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            String text = row.locale.length() <= 2 ? row.locale : row.locale.substring(0, 2);
            boolean hovered = isMouseOver(mouseX, mouseY);
            int border = hovered || isFocused() ? 0xFFFFFFFF : 0xFF8A8A8A;
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                    hovered || isFocused() ? 0xFF101010 : 0xFF000000);
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
            float scale = 0.5f;
            int textWidth = Math.round(font.width(text) * scale);
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + (width - textWidth) / 2.0f, getY() + 3, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, text, 0, 0, hovered ? 0xFFFFFFFF : 0xFFFFFF00, false);
            graphics.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    // remove button deletes one alternate language row
    protected final class TranslationRemoveButton extends AbstractButton {
        protected final TranslationRow row;

        protected TranslationRemoveButton(TranslationRow row) {
            super(0, 0, 10, ENTRY_ROW_H, Component.literal("X"));
            this.row = row;
        }

        @Override
        public void onPress() {
            removeTranslationRow(row);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                    isMouseOver(mouseX, mouseY) ? 0xAA5A1A1A : 0xAA3A1212);
            graphics.drawString(font, "X", getX() + 2, getY() + 2, 0xFFFF5555, false);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }
}
