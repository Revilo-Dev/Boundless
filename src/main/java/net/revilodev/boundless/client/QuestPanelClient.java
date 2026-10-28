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
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.BoundlessDebug;
import net.revilodev.boundless.compat.AuraCompat;
import net.revilodev.boundless.compat.JeiCompat;
import net.revilodev.boundless.client.screen.QuestSettingsScreen;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.lang.reflect.Field;
import java.util.List;
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
    private static Screen pendingQuickEditSource;
    private static String pendingQuickEditQuestId = "";

    private QuestPanelClient() {
    }

    public static List<Rect2i> getJeiExtraAreas(InventoryScreen inv) {
        State st = STATES.get(inv);
        if (st == null || !st.open) return List.of();
        int panelX = computePanelX(inv, st);
        int panelY = computePanelY(inv, st);
        int left = Config.disableCategories() ? panelX : computeTabsX(inv, st);
        if (st.settingsButton != null && st.settingsButton.visible) left = Math.min(left, st.settingsButton.getX());
        int right = panelX + st.panelWidth;
        int bottom = panelY + st.panelHeight;
        if (st.filter != null && st.filter.visible) bottom = Math.max(bottom, st.filter.getY() + st.filter.getHeight());
        return List.of(new Rect2i(left, panelY, right - left, bottom - panelY));
    }

    // attach quest panel widgets to inventory screens
    public static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen inv)) return;
        QuestData.loadClient(false);
        State st = new State(inv);
        updatePanelSize(inv, st);
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

        st.bg = new PanelBackground(0, 0, st.panelWidth, st.panelHeight);
        e.addListener(st.bg);
        st.detailsBg = new PanelBackground(0, 0, st.detailsPanelWidth, st.panelHeight);
        e.addListener(st.detailsBg);

        st.list = new QuestListWidget(0, 0, st.panelWidth - 20, st.panelHeight - 20, q -> openDetails(st, q));
        st.list.setQuests(QuestData.all());
        st.list.setCategory(st.selectedCategory);
        e.addListener(st.list);

        st.tree = new QuestTreeWidget(0, 0, st.panelWidth - 17, st.panelHeight - 17, q -> openDetails(st, q));
        st.tree.setQuests(new java.util.ArrayList<>(QuestData.all()));
        e.addListener(st.tree);

        st.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 127, 16, Component.translatable("ui.boundless.questbook.search_quests"));
        st.searchBox.setHint(Component.translatable("ui.boundless.questbook.search"));
        st.searchBox.setMaxLength(64);
        st.searchBox.setValue(st.searchQuery);
        st.searchBox.setResponder(value -> {
            st.searchQuery = value == null ? "" : value;
            if (st.list != null) st.list.setSearchQuery(st.searchQuery);
        });
        st.list.setSearchQuery(st.searchQuery);

        st.details = new QuestDetailsPanel(0, 0, st.panelWidth - 20, st.panelHeight - 20, () -> closeDetails(st));
        e.addListener(st.details);
        e.addListener(st.details.backButton());
        e.addListener(st.details.completeButton());
        e.addListener(st.details.rejectButton());
        e.addListener(st.details.scrollButton());
        e.addListener(st.searchBox);

        st.tabs = new CategoryTabsWidget(0, 0, 44, st.panelHeight + 34, id -> {
            if (Config.disableCategories()) return;
            st.selectedCategory = id;
            lastSelectedCategory = id;
            if (st.list != null) st.list.setCategory(id);
            refreshTree(st);
            st.showingDetails = false;
            if (!usesTreeView(st)) st.expanded = false;
            relayoutPanel(inv, st);
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

        st.header = new CategoryHeaderWidget(0, 0, st.panelWidth, () -> sectionTitle(st));
        e.addListener(st.header);

        int filterX = computePanelX(inv, st) + 10;
        int filterY = computePanelY(inv, st) + st.panelHeight + 6;
        st.filter = new QuestFilterBar(filterX, filterY, () -> openSettings(inv));
        e.addListener(st.filter);
        st.settingsButton = new SettingsButton(0, 0, () -> openSettings(inv));
        e.addListener(st.settingsButton);
        st.expandButton = new QuestPanelExpandButton(0, 0, () -> st.expanded, () -> {
            st.expanded = !st.expanded;
            relayoutPanel(inv, st);
        });
        e.addListener(st.expandButton);

        reposition(inv, st);

        if (lastQuestOpen && isQuestBookEnabled()) {
            st.open = true;
            if (requiresResponsiveCenter(inv, st)) {
                st.originalLeft = getLeft(inv);
                setLeft(inv, computeCenteredLeft(inv, st));
            }
            updateVisibility(st);
        }
        if (pendingQuickEditSource == s && !pendingQuickEditQuestId.isBlank()) {
            String questId = pendingQuickEditQuestId;
            pendingQuickEditSource = null;
            pendingQuickEditQuestId = "";
            QuestData.byId(questId).ifPresent(quest -> {
                st.open = true;
                lastQuestOpen = true;
                openDetails(st, quest);
            });
        }
    }

    public static void rememberQuickEditReturn(Screen source, String questId) {
        if (source == null || questId == null || questId.isBlank() || !STATES.containsKey(source)) return;
        pendingQuickEditSource = source;
        pendingQuickEditQuestId = questId;
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
        long startedAt = BoundlessDebug.enabled() ? System.nanoTime() : 0L;
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
        if (st.open && requiresResponsiveCenter(inv, st)) {
            if (st.originalLeft == null) st.originalLeft = getLeft(inv);
            setLeft(inv, computeCenteredLeft(inv, st));
        } else if (st.open && st.originalLeft != null && !Config.centerInventoryWithQuestPanel()) {
            setLeft(inv, st.originalLeft);
            st.originalLeft = null;
        }
        refreshQuestBookData(st);
        reposition(inv, st);
        updateVisibility(st);
        if (st.tree != null) {
            st.tree.setTooltipSuppressed(st.expandButton != null && st.expandButton.visible
                    && st.expandButton.isMouseOver(e.getMouseX(), e.getMouseY()));
        }
        handleRecipeButtonRules(inv, st);
        if (startedAt != 0L) {
            long elapsedMicros = (System.nanoTime() - startedAt) / 1_000L;
            if (elapsedMicros >= 4_000L) {
                BoundlessDebug.rateLimited("quest-panel-render", 2_000L,
                        "elapsed={}ms, panelOpen={}, questCount={}", elapsedMicros / 1_000D, st.open, QuestData.all().size());
            }
        }
    }

    // route wheel input into the open panel
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (!st.open) return;

        int px = computePanelX(inv, st) + 10;
        int py = computePanelY(inv, st) + 10;
        int pw = st.panelWidth - 20;
        int ph = st.panelHeight - 20;
        double mx = e.getMouseX();
        double my = e.getMouseY();
        boolean used = false;
        boolean overSearch = st.searchBox != null && st.searchBox.visible
                && mx >= st.searchBox.getX() && mx <= st.searchBox.getX() + st.searchBox.getWidth()
                && my >= st.searchBox.getY() && my <= st.searchBox.getY() + st.searchBox.getHeight();

        if (st.tabs != null && st.tabs.visible && st.tabs.isMouseOver(mx, my)) {
            used = st.tabs.mouseScrolled(mx, my, e.getScrollDeltaX(), e.getScrollDeltaY());
        }
        if (st.list != null && st.list.visible) {
            if (!used && !overSearch && mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                double dY = e.getScrollDeltaY();
                used = st.list.mouseScrolled(mx, my, dY) || st.list.mouseScrolled(mx, my, 0.0, dY);
            }
        }
        if (!used && st.tree != null && st.tree.visible
                && mx >= st.tree.getX() && mx <= st.tree.getX() + st.tree.getWidth()
                && my >= st.tree.getY() && my <= st.tree.getY() + st.tree.getHeight()) {
            used = st.tree.mouseScrolled(mx, my, 0.0, e.getScrollDeltaY());
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
        relayoutPanel(inv, st);
    }

    // close every open quest panel for another inventory panel
    public static void closeAllOpenPanels() {
        for (State st : STATES.values()) {
            if (st == null || !st.open) continue;
            st.open = false;
            relayoutPanel(st.inv, st);
        }
        lastQuestOpen = false;
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
            AuraCompat.closeAuraInventoryPanels();
            if (isRecipePanelOpen(st.inv)) {
                ImageButton recipe = findRecipeButton(st.inv);
                if (recipe != null && recipe.visible && recipe.active) {
                    recipe.onPress();
                }
            }
            if (requiresResponsiveCenter(st.inv, st)) {
                if (st.originalLeft == null) st.originalLeft = getLeft(st.inv);
                setLeft(st.inv, computeCenteredLeft(st.inv, st));
            }
            applySelectedCategory(st);
            st.showingDetails = false;
        } else if (st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
        relayoutPanel(st.inv, st);
    }

    // center inventory and quest panel as one layout
    private static int computeCenteredLeft(InventoryScreen inv, State st) {
        int screenW = inv.width;
        int invW = inv.getXSize();
        int tabsWidth = Config.disableCategories() ? 0 : 41;
        int panelSpan = st.panelWidth + (detailsDocked(st) ? st.detailsPanelWidth + 2 : 0);
        int total = tabsWidth + panelSpan + 2 + invW;
        int centered = (screenW - total) / 2 + tabsWidth + panelSpan + 2;
        return Math.min(screenW - invW, Math.max(panelSpan + tabsWidth + 2, centered));
    }

    // place the quest panel to the left of inventory
    private static int computePanelX(InventoryScreen inv, State st) {
        int dockedDetailsWidth = detailsDocked(st) ? st.detailsPanelWidth + 2 : 0;
        return inv.getGuiLeft() - st.panelWidth - dockedDetailsWidth - 2;
    }

    private static int computeDetailsX(InventoryScreen inv, State st) {
        return detailsDocked(st) ? computePanelX(inv, st) + st.panelWidth + 2 : computePanelX(inv, st);
    }

    // place category tabs beside the quest panel
    private static int computeTabsX(InventoryScreen inv, State st) {
        return computePanelX(inv, st) - 41;
    }

    private static int computePanelY(InventoryScreen inv, State st) {
        return (inv.height - st.panelHeight) / 2;
    }

    private static void updatePanelSize(InventoryScreen inv, State st) {
        float scale = Config.questBookScale();
        st.detailsPanelWidth = Math.round(PANEL_W * scale);
        int reservedDetails = detailsDocked(st) ? st.detailsPanelWidth + 2 : 0;
        int maxWidth = Math.max(96, inv.width - inv.getXSize() - 49 - reservedDetails);
        int desiredWidth = Math.round((st.expanded ? 220 : PANEL_W) * scale);
        int desiredHeight = Math.round(PANEL_H * scale);
        st.panelWidth = Math.min(desiredWidth, maxWidth);
        st.panelHeight = Math.min(desiredHeight, Math.max(120, inv.height - 16));
    }

    private static void relayoutPanel(InventoryScreen inv, State st) {
        updatePanelSize(inv, st);
        if (st.open && requiresResponsiveCenter(inv, st)) {
            if (st.originalLeft == null) st.originalLeft = getLeft(inv);
            setLeft(inv, computeCenteredLeft(inv, st));
        } else if (st.originalLeft != null) {
            setLeft(inv, st.originalLeft);
            st.originalLeft = null;
        }
        reposition(inv, st);
        updateVisibility(st);
        JeiCompat.refreshScreenLayout(inv);
    }

    private static boolean requiresResponsiveCenter(InventoryScreen inv, State st) {
        int naturalLeft = st.originalLeft == null ? getLeft(inv) : st.originalLeft;
        return Config.centerInventoryWithQuestPanel()
                || naturalLeft - st.panelWidth - 2 < (Config.disableCategories() ? 2 : 41);
    }

    // lay out all widgets that live inside the panel
    private static void setPanelChildBounds(InventoryScreen inv, State st) {
        updatePanelSize(inv, st);
        int bgx = computePanelX(inv, st);
        int bgy = computePanelY(inv, st);
        int px = bgx + 10;
        int py = bgy + 10;
        int pw = st.panelWidth - 20;
        int ph = st.panelHeight - 20;

        if (st.bg != null) st.bg.setBounds(bgx, bgy, st.panelWidth, st.panelHeight);
        if (st.detailsBg != null) {
            st.detailsBg.setBounds(computeDetailsX(inv, st), bgy, st.detailsPanelWidth, st.panelHeight);
        }

        if (st.searchBox != null) {
            st.searchBox.setPosition(px, py);
            st.searchBox.setWidth(pw);
            st.searchBox.setHeight(16);
        }

        if (st.list != null) {
            st.list.setBounds(px, py, pw, ph);
            st.list.setTopInset(Config.enableQuestSearchBox() ? 18 : 0);
        }
        if (st.tree != null) {
            st.tree.setX(bgx + 8);
            st.tree.setY(bgy + 8);
            st.tree.setWidth(st.panelWidth - 17);
            st.tree.setHeight(st.panelHeight - 17);
            refreshTree(st);
        }
        if (st.details != null) {
            if (detailsDocked(st)) {
                st.details.setBounds(computeDetailsX(inv, st) + 10, py, st.detailsPanelWidth - 20, ph);
            } else {
                st.details.setBounds(px, py, pw, ph);
            }
        }

        if (st.tabs != null) st.tabs.setBounds(computeTabsX(inv, st), bgy + 4, 44, st.panelHeight + 34);
        if (st.header != null) st.header.setPanelBounds(bgx, bgy, st.panelWidth);

        if (st.filter != null) {
            int filterX = px;
            int filterY = bgy + st.panelHeight - st.filter.getPreferredHeight() + 29;
            st.filter.setBounds(filterX, filterY, st.filter.getPreferredWidth(), st.filter.getPreferredHeight());
        }
    }

    // reposition panel widgets after inventory moves
    private static void reposition(InventoryScreen inv, State st) {
        updatePanelSize(inv, st);
        repositionRecipeButton(inv, st);
        if (st.btn != null) {
            int x = computeQuestButtonX(inv);
            int y = computeQuestButtonY(inv);
            st.btn.setPosition(x, y);
        }
        setPanelChildBounds(inv, st);
        if (st.settingsButton != null) {
            int bgx = computePanelX(inv, st);
            int bgy = computePanelY(inv, st);
            st.settingsButton.setPosition(bgx - 22, bgy + st.panelHeight - st.settingsButton.getHeight());
        }
        if (st.expandButton != null && st.tree != null) {
            st.expandButton.setPosition(st.tree.getX() + st.tree.getWidth() - 40, st.tree.getY() + 3);
        }
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
        RecipeBookComponent recipeBook = inv.getRecipeBookComponent();
        return recipeBook != null && recipeBook.isVisible();
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
        if (!usesTreeView(st)) st.expanded = false;
        relayoutPanel(st.inv, st);
    }

    private static void refreshQuestBookData(State st) {
        int signature = 31 * QuestData.all().hashCode() + QuestData.categoriesOrdered().hashCode();
        signature = 31 * signature + Config.disabledCategories().hashCode();
        if (signature == st.questBookDataSignature) return;
        st.questBookDataSignature = signature;
        if (st.list != null) {
            st.list.setQuests(QuestData.all());
            st.list.setCategory(st.selectedCategory);
        }
        if (st.tabs != null && !Config.disableCategories()) {
            st.tabs.setSelected(st.selectedCategory);
            st.tabs.setCategories(QuestData.categoriesOrdered());
            st.selectedCategory = st.tabs.getSelectedId();
            if (st.selectedCategory == null || st.selectedCategory.isBlank()) st.selectedCategory = st.tabs.selectFirstCategory();
            lastSelectedCategory = st.selectedCategory;
            if (st.list != null) st.list.setCategory(st.selectedCategory);
        }
        refreshTree(st);
    }

    private static void refreshTree(State st) {
        if (st == null || st.tree == null) return;
        java.util.List<QuestData.Quest> quests = new java.util.ArrayList<>();
        for (QuestData.Quest quest : QuestData.all()) {
            if ("all".equalsIgnoreCase(st.selectedCategory) || st.selectedCategory.equalsIgnoreCase(quest.category)) quests.add(quest);
        }
        st.tree.setQuests(quests);
        QuestData.Category category = QuestData.categoryById(st.selectedCategory).orElse(null);
        st.tree.setBackground(category == null ? "" : category.background,
                category != null && category.backgroundDarkening,
                category == null ? 0.45f : category.backgroundDarkeningOpacity);
        st.tree.setStickers(category == null ? java.util.List.of() : category.stickers);
    }

    private static boolean usesTreeView(State st) {
        if (st == null) return false;
        if ("all".equalsIgnoreCase(st.selectedCategory)) {
            for (QuestData.Category category : QuestData.categoriesOrdered()) {
                if (category.treeLayout) return true;
            }
        } else {
            QuestData.Category category = QuestData.categoryById(st.selectedCategory).orElse(null);
            if (category != null && category.treeLayout) return true;
        }
        for (QuestData.Quest quest : QuestData.all()) {
            if (("all".equalsIgnoreCase(st.selectedCategory) || st.selectedCategory.equalsIgnoreCase(quest.category))
                    && (quest.treeX != -1 || quest.treeY != -1)) return true;
        }
        return false;
    }

    private static boolean detailsDocked(State st) {
        return st != null && st.expanded && st.showingDetails && usesTreeView(st);
    }

    // return from quest details to the quest list
    private static void closeDetails(State st) {
        st.showingDetails = false;
        relayoutPanel(st.inv, st);
    }

    // show only the widgets for the current panel mode
    private static void updateVisibility(State st) {
        boolean treeView = usesTreeView(st);
        boolean listVisible = st.open && !st.showingDetails && !treeView;
        boolean treeVisible = st.open && treeView && (!st.showingDetails || st.expanded);
        boolean detailsVisible = st.open && st.showingDetails;

        if (st.bg != null) {
            st.bg.visible = st.open;
            st.bg.active = st.open;
        }

        if (st.list != null) {
            st.list.visible = listVisible;
            st.list.active = listVisible;
        }
        if (st.tree != null) {
            st.tree.visible = treeVisible;
            st.tree.active = treeVisible;
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
        if (st.detailsBg != null) {
            boolean showDetailsBackground = detailsVisible && detailsDocked(st);
            st.detailsBg.visible = showDetailsBackground;
            st.detailsBg.active = false;
        }
        if (st.settingsButton != null) {
            boolean canAccessSettings = canAccessSettings();
            boolean showSettings = st.open && !Config.displayFiltersAsTabs() && canAccessSettings;
            st.settingsButton.visible = showSettings;
            st.settingsButton.active = showSettings;
        }
        if (st.expandButton != null) {
            st.expandButton.visible = treeVisible;
            st.expandButton.active = treeVisible;
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

    // open settings for every player; the screen filters authority-only controls
    private static void openSettings(InventoryScreen inv) {
        var mc = Minecraft.getInstance();
        if (!canAccessSettings()) return;
        mc.setScreen(new QuestSettingsScreen(inv));
    }

    private static boolean canAccessSettings() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null;
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
            QuestPanelBackgroundRenderer.render(gg, PANEL_TEX, getX(), getY(), width, height);
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
        QuestPanelExpandButton expandButton;
        PanelBackground bg;
        PanelBackground detailsBg;
        QuestListWidget list;
        QuestTreeWidget tree;
        QuestDetailsPanel details;
        CategoryTabsWidget tabs;
        CategoryHeaderWidget header;
        QuestFilterBar filter;
        EditBox searchBox;
        boolean showingDetails;
        boolean expanded;
        int panelWidth = PANEL_W;
        int detailsPanelWidth = PANEL_W;
        int panelHeight = PANEL_H;
        boolean open;
        Integer originalLeft;
        String selectedCategory = "all";
        String searchQuery = "";
        int recipeOffsetX;
        int recipeOffsetY;
        boolean hasRecipeOffset;
        int questBookDataSignature = Integer.MIN_VALUE;

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
