package net.revilodev.boundless.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.CategoryTabsWidget;
import net.revilodev.boundless.client.CategoryHeaderWidget;
import net.revilodev.boundless.client.QuestDetailsPanel;
import net.revilodev.boundless.client.QuestFilterBar;
import net.revilodev.boundless.client.QuestListWidget;
import net.revilodev.boundless.client.QuestPanelBackgroundRenderer;
import net.revilodev.boundless.client.QuestPanelExpandButton;
import net.revilodev.boundless.client.QuestTreeWidget;
import net.revilodev.boundless.quest.QuestData;

@OnlyIn(Dist.CLIENT)
public final class StandaloneQuestBookScreen extends Screen {

    // book screen textures
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    private static final ResourceLocation BTN_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button.png");
    private static final ResourceLocation BTN_SETTINGS_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button_hovered.png");
    private static String lastSelectedCategory = "all";

    private int panelWidth = 147;
    private int detailsPanelWidth = 147;
    private int panelHeight = 166;

    private int leftX;
    private int rightX;
    private int topY;

    private CategoryTabsWidget tabs;
    private CategoryHeaderWidget header;
    private QuestListWidget list;
    private QuestTreeWidget tree;
    private QuestDetailsPanel details;
    private QuestFilterBar filter;
    private SettingsButton settingsButton;
    private QuestPanelExpandButton expandButton;
    private EditBox searchBox;

    private boolean showingDetails = false;
    private boolean expanded = false;
    private QuestData.Quest selectedQuest;
    private String searchQuery = "";
    private String selectedCategory = "all";

    public StandaloneQuestBookScreen() {
        super(Component.translatable("ui.boundless.questbook.title"));
    }

    // build the standalone quest book
    @Override
    protected void init() {
        if (Config.disableQuestBook()) {
            if (minecraft != null) minecraft.setScreen(null);
            return;
        }

        selectedCategory = lastSelectedCategory;
        int maxPanelWidth = Math.max(96, (this.width - 49) / 2);
        boolean expandTree = expanded && usesTreeView();
        panelWidth = Math.min(expandTree ? 220 : 147, maxPanelWidth);
        detailsPanelWidth = Math.min(147, maxPanelWidth);
        panelHeight = Math.min(166, Math.max(120, this.height - 16));

        int cx = this.width / 2 + 20;
        int cy = this.height / 2;

        leftX = cx - panelWidth - 2;
        rightX = cx + 2;
        topY = cy - panelHeight / 2;

        int pxLeft = leftX + 10;
        int pxRight = rightX + 10;
        int py = topY + 10;
        int leftWidth = panelWidth - 20;
        int detailsWidth = detailsPanelWidth - 20;
        int ph = panelHeight - 20;

        tabs = new CategoryTabsWidget(leftX - 41, topY + 4, 44, panelHeight + 34, id -> {
            if (Config.disableCategories()) return;
            selectedCategory = id;
            lastSelectedCategory = id;
            list.setCategory(id);
            refreshTree();
            showingDetails = false;
            selectedQuest = null;
            updateVisibility();
        });
        if (!Config.disableCategories()) {
            tabs.setSelected(selectedCategory);
            tabs.setCategories(QuestData.categoriesOrdered());
            selectedCategory = tabs.getSelectedId();
            if (selectedCategory == null || selectedCategory.isBlank()) {
                selectedCategory = tabs.selectFirstCategory();
            }
            lastSelectedCategory = selectedCategory;
        } else {
            selectedCategory = "all";
            lastSelectedCategory = "all";
        }

        searchBox = new EditBox(font, pxLeft, py, leftWidth, 16, Component.translatable("ui.boundless.questbook.search_quests"));
        searchBox.setHint(Component.translatable("ui.boundless.questbook.search"));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            searchQuery = value == null ? "" : value;
            if (list != null) list.setSearchQuery(searchQuery);
        });

        list = new QuestListWidget(pxLeft, py, leftWidth, ph, q -> {
            details.setQuest(q);
            selectedQuest = q;
            showingDetails = true;
            updateVisibility();
        });
        list.setTopInset(Config.enableQuestSearchBox() ? 18 : 0);
        list.setQuests(QuestData.all());
        list.setCategory(selectedCategory);
        list.setSearchQuery(searchQuery);

        tree = new QuestTreeWidget(leftX + 8, topY + 8, panelWidth - 17, panelHeight - 17, q -> {
            details.setQuest(q);
            selectedQuest = q;
            showingDetails = true;
            updateVisibility();
        });
        refreshTree();

        details = new QuestDetailsPanel(pxRight, py, detailsWidth, ph, () -> {
            showingDetails = false;
            selectedQuest = null;
            updateVisibility();
        });
        details.setBounds(pxRight, py, detailsWidth, ph);
        if (showingDetails && selectedQuest != null) details.setQuest(selectedQuest);

        int filterX = pxLeft;
        filter = new QuestFilterBar(filterX, 0, () -> {
            if (!canAccessSettings()) return;
            minecraft.setScreen(new QuestSettingsScreen(this));
        });
        int filterY = topY + panelHeight - filter.getPreferredHeight() + 29;
        filter.setBounds(filterX, filterY, filter.getPreferredWidth(), filter.getPreferredHeight());

        header = new CategoryHeaderWidget(leftX, topY, panelWidth, this::sectionTitle);
        header.setPanelBounds(leftX, topY, panelWidth);
        settingsButton = new SettingsButton(leftX - 22, topY + panelHeight - 20, () -> {
            if (!canAccessSettings()) return;
            minecraft.setScreen(new QuestSettingsScreen(this));
        });
        expandButton = new QuestPanelExpandButton(leftX + panelWidth - 50, topY + 11, () -> expanded, () -> {
            expanded = !expanded;
            resize(minecraft, width, height);
        });

        addRenderableWidget(tabs);
        addRenderableWidget(header);
        addRenderableWidget(list);
        addRenderableWidget(tree);
        addRenderableWidget(details);
        addRenderableWidget(details.backButton());
        addRenderableWidget(details.completeButton());
        addRenderableWidget(details.rejectButton());
        addRenderableWidget(details.scrollButton());
        addRenderableWidget(searchBox);
        addRenderableWidget(filter);
        addRenderableWidget(settingsButton);
        addRenderableWidget(expandButton);

        updateVisibility();
    }

    // swap between list and details
    private void updateVisibility() {
        boolean treeView = usesTreeView();
        list.visible = !treeView;
        list.active = !treeView;
        tree.visible = treeView;
        tree.active = treeView;

        if (searchBox != null) {
            boolean showSearch = !treeView && Config.enableQuestSearchBox();
            searchBox.visible = showSearch;
            searchBox.active = showSearch;
        }

        details.visible = showingDetails;
        details.active = showingDetails;

        details.backButton().visible = showingDetails;
        details.backButton().active = showingDetails;

        details.completeButton().visible = showingDetails;
        details.completeButton().active = showingDetails;

        details.rejectButton().visible = showingDetails;
        details.rejectButton().active = showingDetails;

        details.scrollButton().visible = showingDetails;
        details.scrollButton().active = showingDetails;

        boolean showTabs = !Config.disableCategories();
        tabs.visible = showTabs;
        tabs.active = showTabs;

        if (header != null) {
            boolean showHeader = !Config.hideCategoryHeader() && !Config.disableCategories();
            header.visible = showHeader;
            header.active = false;
        }

        if (filter != null) {
            boolean showFilters = !Config.hideFilters();
            filter.visible = showFilters;
            filter.active = showFilters;
        }
        if (settingsButton != null) {
            boolean showSettings = !Config.displayFiltersAsTabs() && canAccessSettings();
            settingsButton.visible = showSettings;
            settingsButton.active = showSettings;
        }
        if (expandButton != null) {
            boolean showExpand = treeView && !showingDetails;
            expandButton.visible = showExpand;
            expandButton.active = showExpand;
        }
    }

    public void refreshSyncedData() {
        if (Config.disableQuestBook()) {
            if (minecraft != null) minecraft.setScreen(null);
            return;
        }

        if (list != null) {
            list.setTopInset(Config.enableQuestSearchBox() ? 18 : 0);
            list.setQuests(QuestData.all());
            list.setCategory(selectedCategory);
            list.setSearchQuery(searchQuery);
        }
        refreshTree();

        if (tabs != null) {
            if (!Config.disableCategories()) {
                tabs.setSelected(selectedCategory);
                tabs.setCategories(QuestData.categoriesOrdered());
                selectedCategory = tabs.getSelectedId();
                if (selectedCategory == null || selectedCategory.isBlank()) {
                    selectedCategory = tabs.selectFirstCategory();
                }
                lastSelectedCategory = selectedCategory;
                if (list != null) list.setCategory(selectedCategory);
                refreshTree();
            } else {
                selectedCategory = "all";
                lastSelectedCategory = "all";
                if (list != null) list.setCategory("all");
                refreshTree();
            }
        }

        showingDetails = false;
        updateVisibility();
    }

    private String sectionTitle() {
        return tabs == null ? "" : tabs.getSelectedName();
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (Config.disableQuestBook()) {
            if (minecraft != null) minecraft.setScreen(null);
            return;
        }
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        QuestPanelBackgroundRenderer.render(gg, PANEL_TEX, leftX, topY, panelWidth, panelHeight);
        QuestPanelBackgroundRenderer.render(gg, PANEL_TEX, rightX, topY, detailsPanelWidth, panelHeight);
        if (tree != null) tree.setTooltipSuppressed(expandButton != null && expandButton.visible && expandButton.isMouseOver(mouseX, mouseY));
        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean overSearch = searchBox != null && searchBox.visible
                && mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth()
                && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight();
        if (list.visible && list.active) {
            if (!overSearch && mouseX >= list.getX() && mouseX <= list.getX() + list.getWidth()
                    && mouseY >= list.getY() && mouseY <= list.getY() + list.getHeight()) {
                if (list.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
        }
        if (tree.visible && tree.active
                && mouseX >= tree.getX() && mouseX <= tree.getX() + tree.getWidth()
                && mouseY >= tree.getY() && mouseY <= tree.getY() + tree.getHeight()) {
            if (tree.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        if (details.visible && details.active) {
            if (mouseX >= details.getX() && mouseX <= details.getX() + details.getWidth()
                    && mouseY >= details.getY() && mouseY <= details.getY() + details.getHeight()) {
                if (details.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshTree() {
        if (tree == null) return;
        java.util.List<QuestData.Quest> quests = new java.util.ArrayList<>();
        for (QuestData.Quest quest : QuestData.all()) {
            if ("all".equalsIgnoreCase(selectedCategory) || selectedCategory.equalsIgnoreCase(quest.category)) {
                quests.add(quest);
            }
        }
        tree.setQuests(quests);
        tree.setBackground(QuestData.categoryById(selectedCategory)
                .map(category -> category.background).orElse(""));
    }

    private boolean usesTreeView() {
        if ("all".equalsIgnoreCase(selectedCategory)) {
            for (QuestData.Category category : QuestData.categoriesOrdered()) {
                if (category.treeLayout) return true;
            }
        } else {
            QuestData.Category category = QuestData.categoryById(selectedCategory).orElse(null);
            if (category != null && category.treeLayout) return true;
        }
        for (QuestData.Quest quest : QuestData.all()) {
            if (("all".equalsIgnoreCase(selectedCategory) || selectedCategory.equalsIgnoreCase(quest.category))
                    && (quest.treeX != -1 || quest.treeY != -1)) return true;
        }
        return false;
    }

    private boolean canAccessSettings() {
        return minecraft != null && minecraft.player != null
                && (minecraft.hasSingleplayerServer() || minecraft.player.hasPermissions(2));
    }

    private static final class SettingsButton extends AbstractButton {
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
            ResourceLocation tex = isMouseOver(mouseX, mouseY) ? BTN_SETTINGS_HOVER : BTN_SETTINGS;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }
}
