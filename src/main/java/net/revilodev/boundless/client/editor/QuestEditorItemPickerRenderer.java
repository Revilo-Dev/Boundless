package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.revilodev.boundless.client.editor.QuestEditorModels.ItemPickerTab;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerMode;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerPageData;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

// item picker rendering
public final class QuestEditorItemPickerRenderer {
    private static final ResourceLocation VANILLA_WIDGETS_TEX =
            ResourceLocation.withDefaultNamespace("textures/gui/widgets.png");
    private static final ResourceLocation TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");
    private static final ResourceLocation BROWSER_GUI_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/browser-gui.png");
    private static final ResourceLocation BROWSER_SLOT_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/slot.png");
    private static final ResourceLocation BROWSER_TAB_INVENTORY_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/inventory.png");
    private static final ResourceLocation BROWSER_TAB_SEARCH_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/search.png");
    private static final ResourceLocation QUEST_TAB_SCROLL_ICON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/scroll-icon.png");
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int TAB_GAP = 3;
    private static final int ITEM_PICKER_W = 172;
    private static final int ITEM_PICKER_H = 112;
    private static final int ITEM_PICKER_Y_OFFSET = -14;
    private static final int ITEM_PICKER_CLOSE_SIZE = 13;
    private static final int ITEM_PICKER_CLOSE_OFFSET = 4;
    private static final int ITEM_PICKER_SEARCH_X = 19;
    private static final int ITEM_PICKER_SEARCH_Y = 94;
    private static final int ITEM_PICKER_SEARCH_W = 146;
    private static final int ITEM_PICKER_GRID_X = 5;
    private static final int ITEM_PICKER_GRID_Y = 19;
    private static final int ITEM_PICKER_CELL = 18;
    private static final int ITEM_PICKER_COLS = 9;
    private static final int ITEM_PICKER_ROWS = 4;
    private static final int ITEM_PICKER_SIDE_TAB_X = -TAB_W + 4;
    private static final int ITEM_PICKER_SIDE_TAB_Y = 17;
    private static final int ITEM_PICKER_SIDE_TAB_ICON_SIZE = 16;
    private static final int ICON_PICKER_SIDE_TAB_Y = 2;
    private static final int ICON_PICKER_TAB_GAP = 0;

    private QuestEditorItemPickerRenderer() {
    }

    // picker x
    public static int pickerX(int screenWidth) {
        return (screenWidth - ITEM_PICKER_W) / 2;
    }

    // picker y
    public static int pickerY(int screenHeight) {
        return Math.max(8, (screenHeight - ITEM_PICKER_H) / 2 + ITEM_PICKER_Y_OFFSET);
    }

    // close x
    public static int closeX(int pickerX) {
        return pickerX + ITEM_PICKER_W - ITEM_PICKER_CLOSE_OFFSET - ITEM_PICKER_CLOSE_SIZE;
    }

    // close y
    public static int closeY(int pickerY) {
        return pickerY + ITEM_PICKER_CLOSE_OFFSET;
    }

    // is point within
    public static boolean isPointWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    // is mouse over side tab
    public static boolean isMouseOverSideTab(double mouseX, double mouseY, int pickerX, int pickerY, ItemPickerTab tab, boolean allowsTags) {
        int tabX = sideTabX(pickerX);
        int tabY = sideTabY(pickerY, tab, allowsTags);
        return mouseX >= tabX && mouseX <= tabX + TAB_W && mouseY >= tabY && mouseY <= tabY + TAB_H;
    }

    public static boolean isMouseOverIconSideTab(double mouseX, double mouseY, int pickerX, int pickerY,
                                                 ItemPickerTab tab) {
        int tabX = sideTabX(pickerX);
        int tabY = iconSideTabY(pickerY, tab);
        return mouseX >= tabX && mouseX <= tabX + TAB_W && mouseY >= tabY && mouseY <= tabY + TAB_H;
    }

    // render
    public static void render(RenderRequest request) {
        if (request == null || request.page == null) return;
        GuiGraphics gg = request.gg;
        Font font = request.font;
        int x = pickerX(request.screenWidth);
        int y = pickerY(request.screenHeight);
        gg.pose().pushPose();
        gg.pose().translate(0.0f, 0.0f, 700.0f);
        gg.blit(BROWSER_GUI_TEX, x, y, 0, 0, ITEM_PICKER_W, ITEM_PICKER_H, ITEM_PICKER_W, ITEM_PICKER_H);
        String title = request.blockTexturePicker ? text("ui.boundless.editor.picker.block_textures") : request.iconPicker ? switch (request.itemPickerTab) {
            case EFFECTS -> text("ui.boundless.editor.picker.icons");
            case CUSTOM -> text("ui.boundless.editor.picker.custom_assets");
            default -> text("ui.boundless.editor.picker.item_icons");
        } : switch (request.pickerMode) {
            case EFFECTS -> text("ui.boundless.editor.picker.effects");
            case MOBS -> text("ui.boundless.editor.picker.mobs");
            default -> request.itemPickerTab == ItemPickerTab.TAGS
                    ? text("ui.boundless.editor.picker.item_groups")
                    : text("ui.boundless.editor.picker.items");
        };
        gg.drawString(font, title, x + (ITEM_PICKER_W - font.width(title)) / 2, y + 4, 0xFF000000, false);
        int closeX = closeX(x);
        int closeY = closeY(y);
        String closeText = "X";
        int closeColor = isPointWithin(request.mouseX, request.mouseY, closeX, closeY,
                ITEM_PICKER_CLOSE_SIZE, ITEM_PICKER_CLOSE_SIZE) ? 0xFFFF7777 : 0xFFFF3030;
        gg.drawString(font, closeText,
                closeX + (ITEM_PICKER_CLOSE_SIZE - font.width(closeText)) / 2,
                closeY + (ITEM_PICKER_CLOSE_SIZE - font.lineHeight) / 2,
                closeColor, false);
        if (request.blockTexturePicker) {
            renderSideTab(gg, x, y, ItemPickerTab.CREATIVE, BROWSER_TAB_SEARCH_TEX, 16, 16, request.itemPickerTab, false);
        } else if (request.backgroundAssetPicker) {
            renderSideTab(gg, x, y, ItemPickerTab.CUSTOM, ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/style-icon.png"), 16, 16, request.itemPickerTab, false);
        } else if (request.iconPicker) {
            renderIconSideTab(gg, x, y, ItemPickerTab.CREATIVE, BROWSER_TAB_SEARCH_TEX, request.itemPickerTab);
            renderIconSideTab(gg, x, y, ItemPickerTab.EFFECTS, QUEST_TAB_SCROLL_ICON_TEX, request.itemPickerTab);
            renderIconSideTab(gg, x, y, ItemPickerTab.CUSTOM,
                    ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/style-icon.png"),
                    request.itemPickerTab);
            renderIconSideTab(gg, x, y, ItemPickerTab.INVENTORY, BROWSER_TAB_INVENTORY_TEX, request.itemPickerTab);
        } else if (request.pickerMode == PickerMode.ITEMS) {
            renderSideTab(gg, x, y, ItemPickerTab.CREATIVE, BROWSER_TAB_SEARCH_TEX, 16, 16, request.itemPickerTab, request.allowsTags);
            if (request.allowsTags) renderSideTab(gg, x, y, ItemPickerTab.TAGS, QUEST_TAB_SCROLL_ICON_TEX, 16, 16, request.itemPickerTab, true);
            renderSideTab(gg, x, y, ItemPickerTab.INVENTORY, BROWSER_TAB_INVENTORY_TEX, 16, 16, request.itemPickerTab, request.allowsTags);
        }
        if (request.searchBox != null && !(request.iconPicker && request.itemPickerTab == ItemPickerTab.CUSTOM)) {
            request.searchBox.setX(x + ITEM_PICKER_SEARCH_X);
            request.searchBox.setY(y + ITEM_PICKER_SEARCH_Y + 1);
            request.searchBox.setWidth(ITEM_PICKER_SEARCH_W);
            request.searchBox.setTextColor(0xFFFFFFFF);
            request.searchBox.visible = true;
        }

        renderGrid(request, x, y);
        if (request.iconPicker && request.itemPickerTab == ItemPickerTab.CUSTOM) {
            renderVanillaFooterButton(gg, font, x + 24, y + ITEM_PICKER_H + 4, ITEM_PICKER_W - 48, 16,
                    text("ui.boundless.editor.picker.open_assets_folder"), true, false);
        }

        if (request.searchBox != null && !(request.iconPicker && request.itemPickerTab == ItemPickerTab.CUSTOM)) {
            request.searchBox.render(gg, request.mouseX, request.mouseY, 0f);
        }
        renderSideTabTooltip(request, x, y);
        if (request.multiToggleAvailable) {
            renderFooterControls(gg, font, x, y, request.multiSelect, request.pendingSelection);
        }
        gg.pose().popPose();
    }

    private static void renderGrid(RenderRequest request, int x, int y) {
        GuiGraphics gg = request.gg;
        Font font = request.font;
        List<ItemStack> items = request.page.items();
        List<String> ids = request.page.ids();
        int gridX = x + ITEM_PICKER_GRID_X;
        int gridY = y + ITEM_PICKER_GRID_Y;
        ItemStack hovered = ItemStack.EMPTY;
        String hoveredId = "";
        for (int i = 0; i < ITEM_PICKER_COLS * ITEM_PICKER_ROWS; i++) {
            int col = i % ITEM_PICKER_COLS;
            int row = i / ITEM_PICKER_COLS;
            int sx = gridX + col * ITEM_PICKER_CELL;
            int sy = gridY + row * ITEM_PICKER_CELL;
            boolean cellHover = request.mouseX >= sx && request.mouseX <= sx + ITEM_PICKER_CELL && request.mouseY >= sy && request.mouseY <= sy + ITEM_PICKER_CELL;
            gg.blit(BROWSER_SLOT_TEX, sx, sy, 0, 0, ITEM_PICKER_CELL, ITEM_PICKER_CELL, ITEM_PICKER_CELL, ITEM_PICKER_CELL);
            if (i < items.size()) {
                ItemStack stack = items.get(i);
                String selectedId = i < ids.size() ? ids.get(i) : request.selectionId.apply(stack);
                if (request.blockTexturePicker && stack.getItem() instanceof BlockItem blockItem) {
                    var sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                            .getParticleIcon(blockItem.getBlock().defaultBlockState());
                    gg.blit(sx + 1, sy + 1, 0, 16, 16, sprite);
                } else if (request.iconPicker && request.itemPickerTab == ItemPickerTab.CUSTOM) {
                    ResourceLocation texture = QuestEditorCustomIcons.texture(selectedId);
                    if (texture != null) gg.blit(texture, sx + 1, sy + 1, 0, 0, 16, 16, 16, 16);
                } else if (request.pickerMode == PickerMode.EFFECTS || (request.iconPicker && request.itemPickerTab == ItemPickerTab.EFFECTS)) {
                    String emoji = QuestEditorEmojis.nameFromIconId(selectedId);
                    if (emoji != null) gg.blit(QuestEditorEmojis.texture(emoji), sx + 1, sy + 1, 0, 0, 16, 16, 16, 16);
                    else QuestEditorItemIcons.renderEffectPickerIcon(gg, stack, sx + 1, sy + 1);
                } else {
                    gg.renderItem(stack, sx + 1, sy + 1);
                }
                if (request.multiSelect && request.pendingSelection.contains(selectedId)) {
                    gg.fill(sx + 1, sy + 1, sx + ITEM_PICKER_CELL - 1, sy + ITEM_PICKER_CELL - 1, 0x6637A24A);
                    gg.fill(sx, sy, sx + ITEM_PICKER_CELL, sy + 1, 0xFF7CFF7C);
                    gg.fill(sx, sy + ITEM_PICKER_CELL - 1, sx + ITEM_PICKER_CELL, sy + ITEM_PICKER_CELL, 0xFF7CFF7C);
                    gg.fill(sx, sy, sx + 1, sy + ITEM_PICKER_CELL, 0xFF7CFF7C);
                    gg.fill(sx + ITEM_PICKER_CELL - 1, sy, sx + ITEM_PICKER_CELL, sy + ITEM_PICKER_CELL, 0xFF7CFF7C);
                }
                if (cellHover) {
                    hovered = stack;
                    hoveredId = selectedId;
                }
            }
        }
        renderHoveredTooltip(request, font, hovered, hoveredId);
    }

    private static void renderHoveredTooltip(RenderRequest request, Font font, ItemStack hovered, String hoveredId) {
        if (hovered.isEmpty()) return;
        if (request.iconPicker && request.itemPickerTab == ItemPickerTab.CUSTOM) {
            QuestEditorTooltips.render(request.gg, font, Component.literal(hoveredId.substring(QuestEditorCustomIcons.PREFIX.length())), request.mouseX, request.mouseY);
        } else if (QuestEditorEmojis.nameFromIconId(hoveredId) != null) {
            return;
        } else if (request.pickerMode == PickerMode.ITEMS && request.itemPickerTab == ItemPickerTab.TAGS && request.allowsTags) {
            QuestEditorTooltips.render(request.gg, font, Component.literal(hoveredId), request.mouseX, request.mouseY);
        } else if (request.pickerMode == PickerMode.MOBS) {
            QuestEditorTooltips.render(request.gg, font, QuestEditorItemIcons.mobDisplayNameForMobId(hoveredId), request.mouseX, request.mouseY);
        } else if (request.pickerMode == PickerMode.EFFECTS) {
            QuestEditorTooltips.render(request.gg, font, QuestEditorItemIcons.effectDisplayNameForPickerItem(hovered, request.effectIds), request.mouseX, request.mouseY);
        } else {
            request.gg.pose().pushPose();
            request.gg.pose().translate(0f, 0f, 2000f);
            request.gg.renderTooltip(font, hovered, request.mouseX, request.mouseY);
            request.gg.pose().popPose();
        }
    }

    private static void renderSideTabTooltip(RenderRequest request, int pickerX, int pickerY) {
        if (request.blockTexturePicker) {
            if (isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.CREATIVE, false)) {
                QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.full_block_textures"), request.mouseX, request.mouseY);
            }
            return;
        }
        if (request.backgroundAssetPicker) {
            if (isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.CUSTOM, false)) {
                QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.custom_background_assets"), request.mouseX, request.mouseY);
            }
            return;
        }
        if (request.iconPicker) {
            if (isMouseOverIconSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.CREATIVE)) QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.items_tab"), request.mouseX, request.mouseY);
            else if (isMouseOverIconSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.EFFECTS)) QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.icons"), request.mouseX, request.mouseY);
            else if (isMouseOverIconSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.CUSTOM)) QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.custom_assets"), request.mouseX, request.mouseY);
            else if (isMouseOverIconSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.INVENTORY)) QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.inventory"), request.mouseX, request.mouseY);
            return;
        }
        if (request.pickerMode != PickerMode.ITEMS) return;
        if (isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.CREATIVE, request.allowsTags)) {
            QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.all_items"), request.mouseX, request.mouseY);
        } else if (request.allowsTags && isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.TAGS, true)) {
            QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.item_tags"), request.mouseX, request.mouseY);
        } else if (isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.INVENTORY, request.allowsTags)) {
            QuestEditorTooltips.render(request.gg, request.font, Component.translatable("ui.boundless.editor.picker.inventory"), request.mouseX, request.mouseY);
        }
    }

    private static void renderFooterControls(GuiGraphics gg, Font font, int pickerX, int pickerY, boolean multiSelect, Set<String> pendingSelection) {
        int toggleX = pickerX + 6;
        int footerY = pickerY + ITEM_PICKER_H + 4;
        int doneX = pickerX + ITEM_PICKER_W - 62;
        renderVanillaFooterButton(gg, font, toggleX, footerY, 86, 16, text("ui.boundless.editor.picker.select_multiple"), true, multiSelect);
        renderVanillaFooterButton(gg, font, doneX, footerY, 56, 16, text("ui.boundless.editor.picker.done"), !pendingSelection.isEmpty(), false);
        if (!pendingSelection.isEmpty()) {
            String countText = Integer.toString(pendingSelection.size());
            gg.drawString(font, countText, doneX + 4, footerY + 4, 0xFFFFFFFF, false);
        }
    }

    private static void renderVanillaFooterButton(GuiGraphics gg, Font font, int x, int y, int width, int height, String text, boolean enabled, boolean selected) {
        gg.blit(VANILLA_WIDGETS_TEX, x, y, 0, enabled ? 66 : 46, width, height, 256, 256);
        if (selected) {
            gg.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x6637A24A);
        }
        int textColor = enabled ? 0xFFFFFF : 0xA0A0A0;
        int textX = x + (width - font.width(text)) / 2;
        int textY = y + (height - 8) / 2;
        gg.drawString(font, text, textX, textY, textColor, false);
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static void renderSideTab(GuiGraphics gg, int pickerX, int pickerY, ItemPickerTab tab,
                                      ResourceLocation icon, int iconTexW, int iconTexH, ItemPickerTab selectedTab, boolean allowsTags) {
        boolean selected = selectedTab == tab;
        int tabX = sideTabX(pickerX);
        int tabY = sideTabY(pickerY, tab, allowsTags);
        int renderX = tabX + 1 - (selected ? 1 : 0);
        gg.blit(selected ? TAB_SELECTED_TEX : TAB_TEX, renderX, tabY, 0, 0, TAB_W, TAB_H, TAB_W, TAB_H);
        int iconSize = Math.min(ITEM_PICKER_SIDE_TAB_ICON_SIZE, Math.min(iconTexW, iconTexH));
        int iconX = renderX + (TAB_W - iconSize) / 2;
        int iconY = tabY + (TAB_H - iconSize) / 2;
        blitScaled(gg, icon, iconX, iconY, iconSize, iconSize, iconTexW, iconTexH);
    }

    private static void renderIconSideTab(GuiGraphics gg, int pickerX, int pickerY, ItemPickerTab tab,
                                          ResourceLocation icon, ItemPickerTab selectedTab) {
        boolean selected = selectedTab == tab;
        int tabX = sideTabX(pickerX);
        int tabY = iconSideTabY(pickerY, tab);
        int renderX = tabX + 1 - (selected ? 1 : 0);
        gg.blit(selected ? TAB_SELECTED_TEX : TAB_TEX, renderX, tabY, 0, 0, TAB_W, TAB_H, TAB_W, TAB_H);
        int iconX = renderX + (TAB_W - ITEM_PICKER_SIDE_TAB_ICON_SIZE) / 2;
        int iconY = tabY + (TAB_H - ITEM_PICKER_SIDE_TAB_ICON_SIZE) / 2;
        blitScaled(gg, icon, iconX, iconY, ITEM_PICKER_SIDE_TAB_ICON_SIZE,
                ITEM_PICKER_SIDE_TAB_ICON_SIZE, 16, 16);
    }

    private static int iconTabIndex(ItemPickerTab tab) {
        return switch (tab) {
            case CREATIVE -> 0;
            case EFFECTS -> 1;
            case CUSTOM -> 2;
            case INVENTORY -> 3;
            case TAGS -> 1;
        };
    }

    private static int iconSideTabY(int pickerY, ItemPickerTab tab) {
        return pickerY + ICON_PICKER_SIDE_TAB_Y + iconTabIndex(tab) * (TAB_H + ICON_PICKER_TAB_GAP);
    }

    private static int sideTabX(int pickerX) {
        return pickerX + ITEM_PICKER_SIDE_TAB_X;
    }

    private static int sideTabY(int pickerY, ItemPickerTab tab, boolean allowsTags) {
        int index = switch (tab) {
            case CREATIVE -> 0;
            case EFFECTS, TAGS -> 1;
            case CUSTOM -> 2;
            case INVENTORY -> allowsTags ? 2 : 1;
        };
        return pickerY + ITEM_PICKER_SIDE_TAB_Y + index * (TAB_H + TAB_GAP);
    }

    private static void blitScaled(GuiGraphics gg, ResourceLocation texture, int x, int y, int width, int height, int texW, int texH) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0);
        gg.pose().scale(width / (float) texW, height / (float) texH, 1f);
        gg.blit(texture, 0, 0, 0, 0, texW, texH, texW, texH);
        gg.pose().popPose();
    }

    // captures one immutable picker frame so layout and hover checks use the same state
    public static final class RenderRequest {
        public final GuiGraphics gg;
        public final Font font;
        public final EditBox searchBox;
        public final int screenWidth;
        public final int screenHeight;
        public final int mouseX;
        public final int mouseY;
        public final PickerMode pickerMode;
        public final boolean iconPicker;
        public final ItemPickerTab itemPickerTab;
        public final boolean allowsTags;
        public final boolean multiToggleAvailable;
        public final boolean multiSelect;
        public final Set<String> pendingSelection;
        public final PickerPageData page;
        public final List<String> effectIds;
        public final Function<ItemStack, String> selectionId;
        public final boolean blockTexturePicker;
        public final boolean backgroundAssetPicker;

        public RenderRequest(GuiGraphics gg, Font font, EditBox searchBox, int screenWidth, int screenHeight, int mouseX, int mouseY,
                             PickerMode pickerMode, ItemPickerTab itemPickerTab, boolean iconPicker, boolean allowsTags, boolean multiToggleAvailable,
                             boolean multiSelect, Set<String> pendingSelection, PickerPageData page, List<String> effectIds,
                             Function<ItemStack, String> selectionId, boolean blockTexturePicker, boolean backgroundAssetPicker) {
            this.gg = gg;
            this.font = font;
            this.searchBox = searchBox;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.pickerMode = pickerMode;
            this.itemPickerTab = itemPickerTab;
            this.iconPicker = iconPicker;
            this.allowsTags = allowsTags;
            this.multiToggleAvailable = multiToggleAvailable;
            this.multiSelect = multiSelect;
            this.pendingSelection = pendingSelection == null ? Set.of() : pendingSelection;
            this.page = page;
            this.effectIds = effectIds == null ? List.of() : effectIds;
            this.selectionId = selectionId == null ? stack -> "" : selectionId;
            this.blockTexturePicker = blockTexturePicker;
            this.backgroundAssetPicker = backgroundAssetPicker;
        }
    }
}
