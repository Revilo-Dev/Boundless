package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.client.editor.QuestEditorModels.ItemPickerTab;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerMode;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerPageData;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

// item picker rendering
public final class QuestEditorItemPickerRenderer {
    private static final ResourceLocation VANILLA_BUTTON_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation VANILLA_BUTTON_DISABLED_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button_disabled");
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
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/search.png");
    private static final ResourceLocation X_BUTTON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_reject.png");
    private static final ResourceLocation QUEST_TAB_SCROLL_ICON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/scroll-icon.png");
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
        String title = switch (request.pickerMode) {
            case EFFECTS -> "Effect Browser";
            case MOBS -> "Mob Browser";
            default -> request.itemPickerTab == ItemPickerTab.TAGS ? "Item groups" : "Item Browser";
        };
        gg.drawString(font, title, x + (ITEM_PICKER_W - font.width(title)) / 2, y + 4, 0xFF000000, false);
        gg.blit(X_BUTTON_TEX, closeX(x), closeY(y), 0, 0, ITEM_PICKER_CLOSE_SIZE, ITEM_PICKER_CLOSE_SIZE, ITEM_PICKER_CLOSE_SIZE, ITEM_PICKER_CLOSE_SIZE);
        if (request.pickerMode == PickerMode.ITEMS) {
            renderSideTab(gg, x, y, ItemPickerTab.CREATIVE, BROWSER_TAB_SEARCH_TEX, 16, 16, request.itemPickerTab, request.allowsTags);
            if (request.allowsTags) renderSideTab(gg, x, y, ItemPickerTab.TAGS, QUEST_TAB_SCROLL_ICON_TEX, 16, 16, request.itemPickerTab, true);
            renderSideTab(gg, x, y, ItemPickerTab.INVENTORY, BROWSER_TAB_INVENTORY_TEX, 16, 16, request.itemPickerTab, request.allowsTags);
        }
        if (request.searchBox != null) {
            request.searchBox.setX(x + ITEM_PICKER_SEARCH_X);
            request.searchBox.setY(y + ITEM_PICKER_SEARCH_Y + 1);
            request.searchBox.setWidth(ITEM_PICKER_SEARCH_W);
            request.searchBox.setTextColor(0xFFFFFFFF);
            request.searchBox.visible = true;
        }

        renderGrid(request, x, y);

        if (request.searchBox != null) {
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
                if (request.pickerMode == PickerMode.EFFECTS) {
                    QuestEditorItemIcons.renderEffectPickerIcon(gg, stack, sx + 1, sy + 1);
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
        if (request.pickerMode == PickerMode.ITEMS && request.itemPickerTab == ItemPickerTab.TAGS && request.allowsTags) {
            request.gg.renderTooltip(font, Component.literal(hoveredId), request.mouseX, request.mouseY);
        } else if (request.pickerMode == PickerMode.MOBS) {
            request.gg.renderTooltip(font, QuestEditorItemIcons.mobDisplayNameForMobId(hoveredId), request.mouseX, request.mouseY);
        } else if (request.pickerMode == PickerMode.EFFECTS) {
            request.gg.renderTooltip(font, QuestEditorItemIcons.effectDisplayNameForPickerItem(hovered, request.effectIds), request.mouseX, request.mouseY);
        } else {
            request.gg.renderTooltip(font, hovered, request.mouseX, request.mouseY);
        }
    }

    private static void renderSideTabTooltip(RenderRequest request, int pickerX, int pickerY) {
        if (request.pickerMode != PickerMode.ITEMS) return;
        if (isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.CREATIVE, request.allowsTags)) {
            request.gg.renderTooltip(request.font, Component.literal("All Items"), request.mouseX, request.mouseY);
        } else if (request.allowsTags && isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.TAGS, true)) {
            request.gg.renderTooltip(request.font, Component.literal("Item Tags"), request.mouseX, request.mouseY);
        } else if (isMouseOverSideTab(request.mouseX, request.mouseY, pickerX, pickerY, ItemPickerTab.INVENTORY, request.allowsTags)) {
            request.gg.renderTooltip(request.font, Component.literal("Inventory"), request.mouseX, request.mouseY);
        }
    }

    private static void renderFooterControls(GuiGraphics gg, Font font, int pickerX, int pickerY, boolean multiSelect, Set<String> pendingSelection) {
        int toggleX = pickerX + 6;
        int footerY = pickerY + ITEM_PICKER_H + 4;
        int doneX = pickerX + ITEM_PICKER_W - 62;
        renderVanillaFooterButton(gg, font, toggleX, footerY, 86, 16, "Select Multiple", true, multiSelect);
        renderVanillaFooterButton(gg, font, doneX, footerY, 56, 16, "Done", !pendingSelection.isEmpty(), false);
        if (!pendingSelection.isEmpty()) {
            String countText = Integer.toString(pendingSelection.size());
            gg.drawString(font, countText, doneX + 4, footerY + 4, 0xFFFFFFFF, false);
        }
    }

    private static void renderVanillaFooterButton(GuiGraphics gg, Font font, int x, int y, int width, int height, String text, boolean enabled, boolean selected) {
        ResourceLocation sprite = enabled ? VANILLA_BUTTON_SPRITE : VANILLA_BUTTON_DISABLED_SPRITE;
        gg.blitSprite(sprite, x, y, width, height);
        if (selected) {
            gg.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x6637A24A);
        }
        int textColor = enabled ? 0xFFFFFF : 0xA0A0A0;
        int textX = x + (width - font.width(text)) / 2;
        int textY = y + (height - 8) / 2;
        gg.drawString(font, text, textX, textY, textColor, false);
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

    private static int sideTabX(int pickerX) {
        return pickerX + ITEM_PICKER_SIDE_TAB_X;
    }

    private static int sideTabY(int pickerY, ItemPickerTab tab, boolean allowsTags) {
        int index = switch (tab) {
            case CREATIVE -> 0;
            case TAGS -> 1;
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

    // render request
    public static final class RenderRequest {
        public final GuiGraphics gg;
        public final Font font;
        public final EditBox searchBox;
        public final int screenWidth;
        public final int screenHeight;
        public final int mouseX;
        public final int mouseY;
        public final PickerMode pickerMode;
        public final ItemPickerTab itemPickerTab;
        public final boolean allowsTags;
        public final boolean multiToggleAvailable;
        public final boolean multiSelect;
        public final Set<String> pendingSelection;
        public final PickerPageData page;
        public final List<String> effectIds;
        public final Function<ItemStack, String> selectionId;

        public RenderRequest(GuiGraphics gg, Font font, EditBox searchBox, int screenWidth, int screenHeight, int mouseX, int mouseY,
                             PickerMode pickerMode, ItemPickerTab itemPickerTab, boolean allowsTags, boolean multiToggleAvailable,
                             boolean multiSelect, Set<String> pendingSelection, PickerPageData page, List<String> effectIds,
                             Function<ItemStack, String> selectionId) {
            this.gg = gg;
            this.font = font;
            this.searchBox = searchBox;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.pickerMode = pickerMode;
            this.itemPickerTab = itemPickerTab;
            this.allowsTags = allowsTags;
            this.multiToggleAvailable = multiToggleAvailable;
            this.multiSelect = multiSelect;
            this.pendingSelection = pendingSelection == null ? Set.of() : pendingSelection;
            this.page = page;
            this.effectIds = effectIds == null ? List.of() : effectIds;
            this.selectionId = selectionId == null ? stack -> "" : selectionId;
        }
    }
}
