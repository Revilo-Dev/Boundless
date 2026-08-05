package net.revilodev.boundless.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.screen.QuestSettingsScreen;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public final class QuestPanelClient {
    // inventory button and panel textures
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_button_hovered.png");
    private static final ResourceLocation BTN_TEX_TOAST =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_book_toast.png");
    private static final ResourceLocation BTN_TEX_TOAST_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_book_toast_highlighted.png");
    private static final ResourceLocation BTN_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button.png");
    private static final ResourceLocation BTN_SETTINGS_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button_hovered.png");
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int BTN_X_BESIDE_RECIPE = 125;
    private static final int BTN_Y_BESIDE_RECIPE = 61;
    private static final int BTN_X_ABOVE_OFFHAND = 76;
    private static final int BTN_Y_ABOVE_OFFHAND = 43;

    // live quest panel state per inventory screen
    private static final Map<Screen, State> STATES = new WeakHashMap<>();
    private static Field LEFT_FIELD;
    private static boolean lastQuestOpen = false;
    private static String lastSelectedCategory = "all";

    private QuestPanelClient() {
    }

    // attach quest panel widgets to inventory screens
    public static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen inv)) return;
        QuestData.loadClient(false);
        State st = new State(inv);
        st.selectedCategory = lastSelectedCategory;
        STATES.put(s, st);
        ImageButton recipeAtInit = findRecipeButton(inv);
        if (recipeAtInit != null) {
            st.recipeOffsetX = recipeAtInit.getX() - inv.getGuiLeft();
            st.recipeOffsetY = recipeAtInit.getY() - inv.getGuiTop();
            st.hasRecipeOffset = true;
        }

        if (shouldShowInventoryQuestButton()) {
            int btnX = computeQuestButtonX(inv);
            int btnY = computeQuestButtonY(inv);
            QuestToggleButton btn = new QuestToggleButton(btnX, btnY, BTN_TEX, BTN_TEX_HOVER, () -> toggle(st));
            st.btn = btn;
            e.addListener(btn);
        }

        st.bg = new PanelBackground(0, 0, PANEL_W, PANEL_H);
        e.addListener(st.bg);

        st.list = new QuestListWidget(0, 0, 127, PANEL_H - 20, q -> openDetails(st, q));
        st.list.setQuests(QuestData.all());
        st.list.setCategory(st.selectedCategory);
        e.addListener(st.list);

        st.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 127, 16, Component.translatable("ui.boundless.questbook.search_quests"));
        st.searchBox.setHint(Component.translatable("ui.boundless.questbook.search"));
        st.searchBox.setMaxLength(64);
        st.searchBox.setValue(st.searchQuery);
        st.searchBox.setResponder(value -> {
            st.searchQuery = value == null ? "" : value;
            if (st.list != null) st.list.setSearchQuery(st.searchQuery);
        });
        st.list.setSearchQuery(st.searchQuery);

        st.details = new QuestDetailsPanel(0, 0, 127, PANEL_H - 20, () -> closeDetails(st));
        e.addListener(st.details);
        e.addListener(st.details.backButton());
        e.addListener(st.details.completeButton());
        e.addListener(st.details.rejectButton());
        e.addListener(st.details.scrollButton());
        e.addListener(st.searchBox);

        st.tabs = new CategoryTabsWidget(0, 0, 44, PANEL_H + 34, id -> {
            if (Config.disableCategories()) return;
            st.selectedCategory = id;
            lastSelectedCategory = id;
            if (st.list != null) st.list.setCategory(id);
        });
        if (!Config.disableCategories()) {
            st.tabs.setSelected(st.selectedCategory);
            st.tabs.setCategories(QuestData.categoriesOrdered());
            st.selectedCategory = st.tabs.getSelectedId();
            if (st.selectedCategory == null || st.selectedCategory.isBlank()) {
                st.selectedCategory = st.tabs.selectFirstCategory();
            }
            lastSelectedCategory = st.selectedCategory;
            if (st.list != null) st.list.setCategory(st.selectedCategory);
        } else {
            st.selectedCategory = "all";
            lastSelectedCategory = "all";
            if (st.list != null) st.list.setCategory("all");
        }
        e.addListener(st.tabs);

        st.header = new CategoryHeaderWidget(0, 0, PANEL_W, () -> sectionTitle(st));
        e.addListener(st.header);

        int filterX = computePanelX(inv) + 10;
        int filterY = inv.getGuiTop() + PANEL_H + 6;
        st.filter = new QuestFilterBar(filterX, filterY, () -> openSettings(inv));
        e.addListener(st.filter);
        st.settingsButton = new SettingsButton(0, 0, () -> openSettings(inv));
        e.addListener(st.settingsButton);

        reposition(inv, st);

        if (lastQuestOpen && isQuestBookEnabled()) {
            st.open = true;
            if (Config.centerInventoryWithQuestPanel()) {
                st.originalLeft = getLeft(inv);
                setLeft(inv, computeCenteredLeft(inv));
            }
            updateVisibility(st);
        }
    }

    // restore saved panel state when inventory closes
    public static void onScreenClosing(ScreenEvent.Closing e) {
        State st = STATES.remove(e.getScreen());
        if (st == null) return;
        if (st.selectedCategory != null && !st.selectedCategory.isBlank()) {
            lastSelectedCategory = st.selectedCategory;
        }
        if (st.open && st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
    }

    // keep panel layout and button state in sync every frame
    public static void onScreenRenderPre(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (!isQuestBookEnabled() && st.open) {
            st.open = false;
            if (st.originalLeft != null) setLeft(inv, st.originalLeft);
        }
        if (st.btn != null && Minecraft.getInstance().player != null) {
            if (QuestTracker.hasAnyCompleted(Minecraft.getInstance().player)) {
                st.btn.setTextures(BTN_TEX_TOAST, BTN_TEX_TOAST_HOVER);
            } else {
                st.btn.setTextures(BTN_TEX, BTN_TEX_HOVER);
            }
        }
        if (st.open && Config.centerInventoryWithQuestPanel()) {
            setLeft(inv, computeCenteredLeft(inv));
        }
        reposition(inv, st);
        updateVisibility(st);
        handleRecipeButtonRules(inv, st);
    }

    // route wheel input into the open panel
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (!st.open) return;

        int px = computePanelX(inv) + 10;
        int py = inv.getGuiTop() + 10;
        int pw = 127;
        int ph = PANEL_H - 20;
        double mx = e.getMouseX();
        double my = e.getMouseY();
        boolean used = false;
        boolean overSearch = st.searchBox != null && st.searchBox.visible
                && mx >= st.searchBox.getX() && mx <= st.searchBox.getX() + st.searchBox.getWidth()
                && my >= st.searchBox.getY() && my <= st.searchBox.getY() + st.searchBox.getHeight();

        if (st.list != null && st.list.visible) {
            if (!overSearch && mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                double dY = e.getScrollDeltaY();
                used = st.list.mouseScrolled(mx, my, dY) || st.list.mouseScrolled(mx, my, 0.0, dY);
            }
        }
        if (st.details != null && st.details.visible) {
            if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                double dY = e.getScrollDeltaY();
                used = st.details.mouseScrolled(mx, my, dY) || st.details.mouseScrolled(mx, my, 0.0, dY) || used;
            }
        }
        if (used) e.setCanceled(true);
    }

    // close the panel when the recipe book button is pressed
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (e.getButton() != 0) return;
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (!st.open) return;
        ImageButton recipe = findRecipeButton(inv);
        if (recipe == null || !recipe.visible || !recipe.active) return;
        if (!recipe.isMouseOver(e.getMouseX(), e.getMouseY())) return;

        st.open = false;
        lastQuestOpen = false;
        if (st.originalLeft != null) setLeft(inv, st.originalLeft);
        reposition(inv, st);
        updateVisibility(st);
    }

    // refresh live panels after config or quest data changes
    public static void applyConfigChanges() {
        for (State st : STATES.values()) {
            if (st == null) continue;
            if (st.list != null) {
                st.list.setQuests(QuestData.all());
                st.list.setCategory(st.selectedCategory);
                st.list.setSearchQuery(st.searchQuery);
            }
            if (st.tabs != null) {
                if (!Config.disableCategories()) {
                    st.tabs.setSelected(st.selectedCategory);
                    st.tabs.setCategories(QuestData.categoriesOrdered());
                    st.selectedCategory = st.tabs.getSelectedId();
                    if (st.selectedCategory == null || st.selectedCategory.isBlank()) {
                        st.selectedCategory = st.tabs.selectFirstCategory();
                    }
                    lastSelectedCategory = st.selectedCategory;
                    if (st.list != null) st.list.setCategory(st.selectedCategory);
                } else {
                    st.selectedCategory = "all";
                    lastSelectedCategory = "all";
                    if (st.list != null) st.list.setCategory("all");
                }
            }
            if (st.btn != null) {
                boolean show = shouldShowInventoryQuestButton();
                st.btn.visible = show;
                st.btn.active = show;
            }
            reposition(st.inv, st);
            updateVisibility(st);
        }
    }

    // open or close the inventory quest panel
    private static void toggle(State st) {
        if (!isQuestBookEnabled()) return;
        st.open = !st.open;
        lastQuestOpen = st.open;
        if (st.open) {
            if (isRecipePanelOpen(st.inv)) {
                ImageButton recipe = findRecipeButton(st.inv);
                if (recipe != null && recipe.visible && recipe.active) {
                    recipe.onPress();
                }
            }
            if (Config.centerInventoryWithQuestPanel()) {
                if (st.originalLeft == null) st.originalLeft = getLeft(st.inv);
                setLeft(st.inv, computeCenteredLeft(st.inv));
            }
            applySelectedCategory(st);
            st.showingDetails = false;
        } else if (st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
        reposition(st.inv, st);
        updateVisibility(st);
    }

    // center inventory and quest panel as one layout
    private static int computeCenteredLeft(InventoryScreen inv) {
        int screenW = inv.width;
        int invW = inv.getXSize();
        int total = PANEL_W + 2 + invW;
        return (screenW - total) / 2 + PANEL_W + 2;
    }

    // place the quest panel to the left of inventory
    private static int computePanelX(InventoryScreen inv) {
        return inv.getGuiLeft() - PANEL_W - 2;
    }

    // place category tabs beside the quest panel
    private static int computeTabsX(InventoryScreen inv) {
        return computePanelX(inv) - 41;
    }

    // lay out all widgets that live inside the panel
    private static void setPanelChildBounds(InventoryScreen inv, State st) {
        int bgx = computePanelX(inv);
        int bgy = inv.getGuiTop();
        int px = bgx + 10;
        int py = bgy + 10;
        int pw = 127;
        int ph = PANEL_H - 20;

        if (st.bg != null) st.bg.setBounds(bgx, bgy, PANEL_W, PANEL_H);

        if (st.searchBox != null) {
            st.searchBox.setPosition(px, py);
            st.searchBox.setWidth(pw);
            st.searchBox.setHeight(16);
        }

        if (st.list != null) {
            st.list.setBounds(px, py, pw, ph);
            st.list.setTopInset(Config.enableQuestSearchBox() ? 18 : 0);
        }
        if (st.details != null) {
            st.details.setBounds(px, py, pw, ph);

            st.details.backButton().setPosition(px, py + ph - st.details.backButton().getHeight() - 4);
            st.details.completeButton().setPosition(
                    px + (pw - st.details.completeButton().getWidth()) / 2,
                    py + ph - st.details.completeButton().getHeight() - 4
            );
            st.details.rejectButton().setPosition(
                    px + pw - st.details.rejectButton().getWidth() - 2,
                    py + ph - st.details.rejectButton().getHeight() - 4
            );
        }

        if (st.tabs != null) st.tabs.setBounds(computeTabsX(inv), bgy + 4, 44, PANEL_H + 34);
        if (st.header != null) st.header.setPanelBounds(bgx, bgy, PANEL_W);

        if (st.filter != null) {
            int filterX = px;
            int filterY = bgy + PANEL_H - st.filter.getPreferredHeight() + 29;
            st.filter.setBounds(filterX, filterY, st.filter.getPreferredWidth(), st.filter.getPreferredHeight());
        }
    }

    // reposition panel widgets after inventory moves
    private static void reposition(InventoryScreen inv, State st) {
        repositionRecipeButton(inv, st);
        if (st.btn != null) {
            int x = computeQuestButtonX(inv);
            int y = computeQuestButtonY(inv);
            st.btn.setPosition(x, y);
        }
        if (st.settingsButton != null) {
            int bgx = computePanelX(inv);
            int bgy = inv.getGuiTop();
            st.settingsButton.setPosition(bgx - 22, bgy + PANEL_H - st.settingsButton.getHeight());
        }
        setPanelChildBounds(inv, st);
    }

    // keep the inventory quest button visibility in sync
    private static void handleRecipeButtonRules(InventoryScreen inv, State st) {
        if (st.btn != null) {
            boolean show = shouldShowInventoryQuestButton();
            st.btn.visible = show;
            st.btn.active = show;
        }
    }

    // find the recipe book button on the inventory screen
    private static ImageButton findRecipeButton(InventoryScreen inv) {
        for (var child : inv.children()) {
            if (child instanceof ImageButton btn && btn.getWidth() == 20 && btn.getHeight() == 18) {
                return btn;
            }
        }
        return null;
    }

    // keep the recipe book button anchored to the inventory gui
    private static void repositionRecipeButton(InventoryScreen inv, State st) {
        ImageButton recipe = findRecipeButton(inv);
        if (recipe == null) return;
        if (!st.hasRecipeOffset) {
            st.recipeOffsetX = recipe.getX() - inv.getGuiLeft();
            st.recipeOffsetY = recipe.getY() - inv.getGuiTop();
            st.hasRecipeOffset = true;
        }
        recipe.setPosition(inv.getGuiLeft() + st.recipeOffsetX, inv.getGuiTop() + st.recipeOffsetY);
    }

    // detect when the recipe book is already expanded
    private static boolean isRecipePanelOpen(InventoryScreen inv) {
        int centeredLeft = (inv.width - inv.getXSize()) / 2;
        return inv.getGuiLeft() > centeredLeft + 10;
    }

    // read the private inventory left position
    private static Integer getLeft(InventoryScreen inv) {
        try {
            if (LEFT_FIELD == null) LEFT_FIELD = findLeftField(inv.getClass());
            return (Integer) LEFT_FIELD.get(inv);
        } catch (Throwable t) {
            return inv.getGuiLeft();
        }
    }

    // write the private inventory left position
    private static void setLeft(InventoryScreen inv, int v) {
        try {
            if (LEFT_FIELD == null) LEFT_FIELD = findLeftField(inv.getClass());
            LEFT_FIELD.setInt(inv, v);
        } catch (Throwable ignored) {
        }
    }

    // find the left position field across gui superclasses
    private static Field findLeftField(Class<?> c) throws NoSuchFieldException {
        Class<?> cur = c;
        while (cur != null) {
            try {
                Field f = cur.getDeclaredField("leftPos");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException("leftPos");
    }

    // switch from the list view into quest details
    private static void openDetails(State st, QuestData.Quest quest) {
        if (st.details == null) return;
        st.details.setQuest(quest);
        st.showingDetails = true;
        updateVisibility(st);
    }

    // return from quest details to the quest list
    private static void closeDetails(State st) {
        st.showingDetails = false;
        updateVisibility(st);
    }

    // show only the widgets for the current panel mode
    private static void updateVisibility(State st) {
        boolean listVisible = st.open && !st.showingDetails;
        boolean detailsVisible = st.open && st.showingDetails;

        if (st.bg != null) {
            st.bg.visible = st.open;
            st.bg.active = st.open;
        }

        if (st.list != null) {
            st.list.visible = listVisible;
            st.list.active = listVisible;
        }

        if (st.searchBox != null) {
            boolean showSearch = listVisible && Config.enableQuestSearchBox();
            st.searchBox.visible = showSearch;
            st.searchBox.active = showSearch;
        }

        if (st.details != null) {
            st.details.visible = detailsVisible;
            st.details.active = detailsVisible;

            if (st.details.backButton() != null) {
                st.details.backButton().visible = detailsVisible;
                st.details.backButton().active = detailsVisible;
            }

            if (st.details.completeButton() != null) {
                st.details.completeButton().visible = detailsVisible;
                st.details.completeButton().active = detailsVisible;
            }

            if (st.details.rejectButton() != null) {
                st.details.rejectButton().visible = detailsVisible;
                st.details.rejectButton().active = detailsVisible;
            }
            if (st.details.scrollButton() != null) {
                st.details.scrollButton().visible = detailsVisible;
                st.details.scrollButton().active = detailsVisible;
            }
        }

        if (st.tabs != null) {
            boolean showTabs = st.open && !Config.disableCategories();
            st.tabs.visible = showTabs;
            st.tabs.active = showTabs;
        }

        if (st.header != null) {
            boolean showHeader = st.open && !Config.hideCategoryHeader() && !Config.disableCategories();
            st.header.visible = showHeader;
            st.header.active = false;
        }

        if (st.filter != null) {
            boolean showFilters = st.open && !Config.hideFilters();
            st.filter.visible = showFilters;
            st.filter.active = showFilters;
        }
        if (st.settingsButton != null) {
            boolean canAccessSettings = Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.hasPermissions(2);
            boolean showSettings = st.open && !Config.displayFiltersAsTabs() && canAccessSettings;
            st.settingsButton.visible = showSettings;
            st.settingsButton.active = showSettings;
        }
    }

    // block panel access when the quest book is disabled
    private static boolean isQuestBookEnabled() {
        return !Config.disableQuestBook();
    }

    // hide the inventory quest button when config disables it
    private static boolean shouldShowInventoryQuestButton() {
        return isQuestBookEnabled() && !Config.hideQuestBookInInventory();
    }

    // pick the quest button x offset from config
    private static int computeQuestButtonX(InventoryScreen inv) {
        String mode = Config.questBookInventoryButtonPosition();
        int offset = "above_offhand_slot".equals(mode) ? BTN_X_ABOVE_OFFHAND : BTN_X_BESIDE_RECIPE;
        return inv.getGuiLeft() + offset;
    }

    // pick the quest button y offset from config
    private static int computeQuestButtonY(InventoryScreen inv) {
        String mode = Config.questBookInventoryButtonPosition();
        int offset = "above_offhand_slot".equals(mode) ? BTN_Y_ABOVE_OFFHAND : BTN_Y_BESIDE_RECIPE;
        return inv.getGuiTop() + offset;
    }

    // open the settings screen for admins
    private static void openSettings(InventoryScreen inv) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasPermissions(2)) return;
        mc.setScreen(new QuestSettingsScreen(inv));
    }

    // reapply the saved category after reopening the panel
    private static void applySelectedCategory(State st) {
        if (st.tabs == null) return;
        if (Config.disableCategories()) {
            st.selectedCategory = "all";
            lastSelectedCategory = "all";
            if (st.list != null) st.list.setCategory("all");
        } else {
            st.tabs.setSelected(st.selectedCategory);
            st.selectedCategory = st.tabs.getSelectedId();
            if (st.selectedCategory == null || st.selectedCategory.isBlank()) {
                st.selectedCategory = st.tabs.selectFirstCategory();
            }
            lastSelectedCategory = st.selectedCategory;
            if (st.list != null) st.list.setCategory(st.selectedCategory);
        }
    }

    // show the selected category name in the panel header
    private static String sectionTitle(State st) {
        if (st == null) return "";
        return st.tabs == null ? "" : st.tabs.getSelectedName();
    }

    // panel background widget
    private static final class PanelBackground extends AbstractWidget {
        public PanelBackground(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
        }

        // sync the background with the panel bounds
        public void setBounds(int x, int y, int w, int h) {
            this.setX(x);
            this.setY(y);
            this.width = w;
            this.height = h;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.disableBlend();
            gg.blit(PANEL_TEX, getX(), getY(), 0, 0, width, height, width, height);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // per screen quest panel state
    private static final class State {
        final InventoryScreen inv;
        QuestToggleButton btn;
        SettingsButton settingsButton;
        PanelBackground bg;
        QuestListWidget list;
        QuestDetailsPanel details;
        CategoryTabsWidget tabs;
        CategoryHeaderWidget header;
        QuestFilterBar filter;
        EditBox searchBox;
        boolean showingDetails;
        boolean open;
        Integer originalLeft;
        String selectedCategory = "all";
        String searchQuery = "";
        int recipeOffsetX;
        int recipeOffsetY;
        boolean hasRecipeOffset;

        State(InventoryScreen inv) {
            this.inv = inv;
        }
    }

    // settings button beside the quest panel
    private static final class SettingsButton extends net.minecraft.client.gui.components.AbstractButton {
        private final Runnable onPress;

        SettingsButton(int x, int y, Runnable onPress) {
            super(x, y, 20, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = this.isMouseOver(mouseX, mouseY) ? BTN_SETTINGS_HOVER : BTN_SETTINGS;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }
}
