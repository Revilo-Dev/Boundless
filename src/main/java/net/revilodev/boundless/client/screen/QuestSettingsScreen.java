package net.revilodev.boundless.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.CategoryHeaderWidget;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.client.QuestListWidget;
import net.revilodev.boundless.quest.QuestData;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class QuestSettingsScreen extends Screen {
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;

    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
    private static final ResourceLocation BTN_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");
    private static final ResourceLocation BTN_BACK_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
    private static final ResourceLocation BTN_BACK_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_highlighted.png");
    private static final ResourceLocation HEADER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/9-slice-header.png");
    private static final ResourceLocation TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");
    private static final ResourceLocation CONFIG_TAB_ALL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/search.png");
    private static final ResourceLocation CONFIG_TAB_STYLE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/style-icon.png");
    private static final ResourceLocation CONFIG_TAB_UI_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/ui-icon.png");
    private static final ResourceLocation CONFIG_TAB_FEATURES_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/features-icon.png");
    private static final ResourceLocation MENU_CONFIG_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/style-icon.png");
    private static final ResourceLocation MENU_EDITOR_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/editor-icon.png");
    private static final ResourceLocation MENU_DISCORD_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/discord-icon.png");
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int TAB_GAP = 3;
    private static final int HEADER_TEX_W = 72;
    private static final int HEADER_TEX_H = 10;
    private static final int HEADER_SLICE = 3;

    private static final String MENU_ID_CONFIG = "01_settings_config";
    private static final String MENU_ID_EDITOR = "02_settings_editor";
    private static final String MENU_ID_DISCORD = "03_settings_discord";
    private static final List<String> HUD_POSITIONS = List.of(
            "bottom_left",
            "bottom_right",
            "top_left",
            "top_right"
    );

    private final Screen parent;
    private Page page = Page.MENU;

    private int leftX;
    private int topY;
    private int px;
    private int py;
    private int pw;
    private int ph;

    private QuestListWidget menuList;
    private CategoryHeaderWidget header;

    private ConfigRow uiPinnedRow;
    private ConfigRow uiHideInventoryRow;
    private ConfigRow uiInventoryButtonPositionRow;
    private ConfigRow uiCenterInventoryWithPanelRow;
    private ConfigRow uiHideHeaderRow;
    private ConfigRow uiFilterDisplayRow;
    private ConfigRow uiDisableCategoriesRow;
    private ConfigRow uiHideQuestWidgetIconsRow;
    private ConfigRow uiQuestTextScaleRow;
    private ConfigRow uiQuestIconScaleRow;
    private ConfigRow uiEnableSearchBoxRow;
    private ConfigRow uiEnableDescriptionColorsRow;
    private ConfigRow uiEnableQuestToastsRow;
    private ConfigRow functionalityDisablePinningRow;
    private ConfigRow functionalityAutoClaimRow;
    private ConfigRow functionalityQuestScrollsRow;
    private ConfigRow gameplayDisableQuestBookRow;
    private ConfigRow gameplaySpawnWithBookRow;

    private BackButton backButton;
    private HoldResetButton resetConfigButton;
    private final List<ConfigTabButton> configTabButtons = new ArrayList<>();
    private final List<ConfigRow> configRows = new ArrayList<>();
    private List<Component> pendingTooltip = List.of();
    private int pendingTooltipX;
    private int pendingTooltipY;

    private float configScrollY = 0f;
    private boolean configScrollbarDragging = false;
    private int uiHeaderBaseY;
    private int functionalityHeaderBaseY;
    private int gameplayHeaderBaseY;
    private int uiPinnedBaseY;
    private int uiHideInventoryBaseY;
    private int uiInventoryButtonPositionBaseY;
    private int uiCenterInventoryWithPanelBaseY;
    private int uiHideHeaderBaseY;
    private int uiFilterDisplayBaseY;
    private int uiDisableCategoriesBaseY;
    private int uiHideQuestWidgetIconsBaseY;
    private int uiQuestTextScaleBaseY;
    private int uiQuestIconScaleBaseY;
    private int uiEnableSearchBoxBaseY;
    private int uiEnableDescriptionColorsBaseY;
    private int uiEnableQuestToastsBaseY;
    private int functionalityDisablePinningBaseY;
    private int functionalityAutoClaimBaseY;
    private int functionalityQuestScrollsBaseY;
    private int gameplayDisableQuestBookBaseY;
    private int gameplaySpawnWithBookBaseY;

    private String pinnedHudPos;
    private boolean hideQuestBookInInventory;
    private String questBookInventoryButtonPosition;
    private boolean centerInventoryWithQuestPanel;
    private boolean hideCategoryHeader;
    private String filterDisplayMode;
    private boolean disableCategories;
    private boolean hideQuestWidgetIcons;
    private double questTextScale;
    private double questIconScale;
    private boolean enableQuestSearchBox;
    private boolean enableDescriptionColors;
    private boolean enableQuestToasts;
    private boolean disableQuestPinning;
    private boolean autoClaimQuestRewards;
    private boolean enableQuestScrolls;
    private boolean disableQuestBook;
    private boolean spawnWithQuestBook;
    private ConfigTab configTab = ConfigTab.ALL;

    public QuestSettingsScreen(Screen parent) {
        super(Component.literal("Quest Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasPermissions(2)) {
            mc.setScreen(parent);
            return;
        }

        leftX = (width - PANEL_W) / 2;
        topY = (height - PANEL_H) / 2;
        px = leftX + 8;
        py = topY + 8;
        pw = 130;
        ph = 149;

        initMenu();
        initConfigWidgets();
        initNavButtons();
        initHeader();

        setPage(Page.MENU);
    }

    private void initMenu() {
        menuList = new QuestListWidget(px, py, pw, ph, this::handleMenuClick);
        menuList.setQuests(buildMenuQuests());
        menuList.setCategory("all");
        menuList.setBypassFilters(true);
        addRenderableWidget(menuList);
    }

    private void initConfigWidgets() {
        configRows.clear();
        configTabButtons.clear();
        int uiHeaderY = py + 2;
        int uiRow1 = uiHeaderY + 10;
        int rowGap = 21;

        uiHeaderBaseY = uiHeaderY;
        uiPinnedBaseY = uiRow1;
        uiHideInventoryBaseY = uiRow1 + rowGap;
        uiInventoryButtonPositionBaseY = uiRow1 + rowGap * 2;
        uiCenterInventoryWithPanelBaseY = uiRow1 + rowGap * 3;
        uiHideHeaderBaseY = uiRow1 + rowGap * 4;
        uiFilterDisplayBaseY = uiRow1 + rowGap * 5;
        uiDisableCategoriesBaseY = uiRow1 + rowGap * 6;
        uiHideQuestWidgetIconsBaseY = uiRow1 + rowGap * 7;
        uiQuestTextScaleBaseY = uiRow1 + rowGap * 8;
        uiQuestIconScaleBaseY = uiRow1 + rowGap * 9;
        uiEnableSearchBoxBaseY = uiRow1 + rowGap * 10;
        uiEnableDescriptionColorsBaseY = uiRow1 + rowGap * 11;
        uiEnableQuestToastsBaseY = uiRow1 + rowGap * 12;
        functionalityHeaderBaseY = uiRow1 + rowGap * 13 + 4;
        functionalityDisablePinningBaseY = functionalityHeaderBaseY + 10;
        functionalityAutoClaimBaseY = functionalityDisablePinningBaseY + rowGap;
        functionalityQuestScrollsBaseY = functionalityAutoClaimBaseY + rowGap;
        gameplayHeaderBaseY = functionalityQuestScrollsBaseY + rowGap + 2;
        gameplayDisableQuestBookBaseY = gameplayHeaderBaseY + 10;
        gameplaySpawnWithBookBaseY = gameplayDisableQuestBookBaseY + rowGap;

        uiPinnedRow = new ConfigRow(px, uiPinnedBaseY, pw, "Pinned GUI",
                "Set where pinned quest toasts appear.",
                () -> pinnedHudPos, this::cyclePinnedHudPosition);
        uiHideInventoryRow = new ConfigRow(px, uiHideInventoryBaseY, pw, "Inventory Questbook",
                "Hide the quest-book button in inventory.",
                () -> hideQuestBookInInventory ? "On" : "Off",
                () -> {
                    hideQuestBookInInventory = !hideQuestBookInInventory;
                    if (hideQuestBookInInventory) disableQuestBook = false;
                });
        uiInventoryButtonPositionRow = new ConfigRow(px, uiInventoryButtonPositionBaseY, pw, "Book GUI position",
                "Choose where the inventory quest-book button appears.",
                this::formatQuestBookInventoryButtonPosition,
                this::cycleQuestBookInventoryButtonPosition);
        uiCenterInventoryWithPanelRow = new ConfigRow(px, uiCenterInventoryWithPanelBaseY, pw, "Centre Quest UI",
                "Center inventory and quest panel together when the panel is open.",
                () -> centerInventoryWithQuestPanel ? "Enabled" : "Disabled",
                () -> centerInventoryWithQuestPanel = !centerInventoryWithQuestPanel);
        uiHideHeaderRow = new ConfigRow(px, uiHideHeaderBaseY, pw, "Display Headers",
                "Hide the category header above the quest list.",
                () -> hideCategoryHeader ? "Disabled" : "Enabled",
                () -> hideCategoryHeader = !hideCategoryHeader);
        uiFilterDisplayRow = new ConfigRow(px, uiFilterDisplayBaseY, pw, "Display Filters",
                "Choose whether filters appear as buttons, tabs, or stay hidden.",
                this::formatFilterDisplayMode,
                this::cycleFilterDisplayMode);
        uiDisableCategoriesRow = new ConfigRow(px, uiDisableCategoriesBaseY, pw, "Categories",
                "Disable category tabs and category-based filtering.",
                () -> disableCategories ? "Disabled" : "Enabled",
                () -> disableCategories = !disableCategories);
        uiHideQuestWidgetIconsRow = new ConfigRow(px, uiHideQuestWidgetIconsBaseY, pw, "Disable Widget Icons",
                "Hide icons in quest list widgets.",
                () -> hideQuestWidgetIcons ? "On" : "Off",
                () -> hideQuestWidgetIcons = !hideQuestWidgetIcons);
        uiQuestTextScaleRow = new ConfigRow(px, uiQuestTextScaleBaseY, pw, "Text Scale",
                "Scale quest widget titles and detail description, task, and reward text.",
                this::formatQuestTextScale,
                this::cycleQuestTextScale);
        uiQuestIconScaleRow = new ConfigRow(px, uiQuestIconScaleBaseY, pw, "Icon Scale",
                "Scale quest widget icons and detail panel icons.",
                this::formatQuestIconScale,
                this::cycleQuestIconScale);
        uiEnableSearchBoxRow = new ConfigRow(px, uiEnableSearchBoxBaseY, pw, "Search Widget",
                "Show a search box above the quest list.",
                () -> enableQuestSearchBox ? "On" : "Off",
                () -> enableQuestSearchBox = !enableQuestSearchBox);
        uiEnableDescriptionColorsRow = new ConfigRow(px, uiEnableDescriptionColorsBaseY, pw, "Text coloring",
                "Allow Boundless color tokens to tint quest descriptions.",
                () -> enableDescriptionColors ? "On" : "Off",
                () -> enableDescriptionColors = !enableDescriptionColors);
        uiEnableQuestToastsRow = new ConfigRow(px, uiEnableQuestToastsBaseY, pw, "Quest Toasts",
                "Show toast popups when quests are unlocked.",
                () -> enableQuestToasts ? "On" : "Off",
                () -> enableQuestToasts = !enableQuestToasts);

        functionalityDisablePinningRow = new ConfigRow(px, functionalityDisablePinningBaseY, pw, "Quest pinning",
                "Disable pin buttons and pinned quest HUD.",
                () -> disableQuestPinning ? "Disabled" : "Enabled",
                () -> disableQuestPinning = !disableQuestPinning);
        functionalityAutoClaimRow = new ConfigRow(px, functionalityAutoClaimBaseY, pw, "Auto Claim Rewards",
                "Automatically claim rewards when a quest becomes complete.",
                () -> autoClaimQuestRewards ? "On" : "Off",
                () -> autoClaimQuestRewards = !autoClaimQuestRewards);
        functionalityQuestScrollsRow = new ConfigRow(px, functionalityQuestScrollsBaseY, pw, "Quest scrolls",
                "Show and allow quest completion scrolls.",
                () -> enableQuestScrolls ? "On" : "Off",
                () -> enableQuestScrolls = !enableQuestScrolls);

        gameplayDisableQuestBookRow = new ConfigRow(px, gameplayDisableQuestBookBaseY, pw, "Quest book",
                "Disable opening the quest book from key/item/network open.",
                () -> disableQuestBook ? "Disabled" : "Enabled",
                () -> {
                    disableQuestBook = !disableQuestBook;
                    if (disableQuestBook) hideQuestBookInInventory = false;
                });
        gameplaySpawnWithBookRow = new ConfigRow(px, gameplaySpawnWithBookBaseY, pw, "Spawn With Quest Book",
                "Give players a quest book on first join.",
                () -> spawnWithQuestBook ? "On" : "Off",
                () -> spawnWithQuestBook = !spawnWithQuestBook);

        addConfigRow(uiHideQuestWidgetIconsRow, ConfigTab.STYLE);
        addConfigRow(uiQuestTextScaleRow, ConfigTab.STYLE);
        addConfigRow(uiQuestIconScaleRow, ConfigTab.STYLE);
        addConfigRow(uiEnableDescriptionColorsRow, ConfigTab.STYLE);
        addConfigRow(uiHideHeaderRow, ConfigTab.STYLE);

        addConfigRow(uiPinnedRow, ConfigTab.UI);
        addConfigRow(uiHideInventoryRow, ConfigTab.UI);
        addConfigRow(uiInventoryButtonPositionRow, ConfigTab.UI);
        addConfigRow(uiCenterInventoryWithPanelRow, ConfigTab.UI);
        addConfigRow(uiFilterDisplayRow, ConfigTab.UI);
        addConfigRow(uiDisableCategoriesRow, ConfigTab.UI);
        addConfigRow(uiEnableSearchBoxRow, ConfigTab.UI);

        addConfigRow(uiEnableQuestToastsRow, ConfigTab.FEATURES);
        addConfigRow(functionalityDisablePinningRow, ConfigTab.FEATURES);
        addConfigRow(functionalityAutoClaimRow, ConfigTab.FEATURES);
        addConfigRow(functionalityQuestScrollsRow, ConfigTab.FEATURES);
        addConfigRow(gameplayDisableQuestBookRow, ConfigTab.FEATURES);
        addConfigRow(gameplaySpawnWithBookRow, ConfigTab.FEATURES);

        addConfigTabButton(ConfigTab.ALL, CONFIG_TAB_ALL_TEX, "All");
        addConfigTabButton(ConfigTab.STYLE, CONFIG_TAB_STYLE_TEX, "Style");
        addConfigTabButton(ConfigTab.UI, CONFIG_TAB_UI_TEX, "UI");
        addConfigTabButton(ConfigTab.FEATURES, CONFIG_TAB_FEATURES_TEX, "Features");
        applyConfigScrollLayout();
    }

    private void addConfigRow(ConfigRow row, ConfigTab group) {
        row.group = group;
        configRows.add(row);
        addRenderableWidget(row);
    }

    private void addConfigTabButton(ConfigTab tab, ResourceLocation icon, String tooltip) {
        ConfigTabButton button = new ConfigTabButton(tab, icon, tooltip);
        configTabButtons.add(button);
        addRenderableWidget(button);
    }

    private void initNavButtons() {
        int btnY = py + ph - 20;
        backButton = new BackButton(px, btnY, () -> setPage(Page.MENU));
        resetConfigButton = new HoldResetButton(px + 31, btnY, 78, 20, () -> Component.literal(configTab == ConfigTab.ALL ? "Reset all configs" : "Reset to default"), this::resetCurrentConfigTab);

        addRenderableWidget(backButton);
        addRenderableWidget(resetConfigButton);
    }

    private void initHeader() {
        header = new CategoryHeaderWidget(leftX, topY, PANEL_W, () -> "Settings");
        header.setPanelBounds(leftX, topY, PANEL_W);
        addRenderableWidget(header);
    }

    private void handleMenuClick(QuestData.Quest q) {
        if (q == null) return;
        if (MENU_ID_CONFIG.equals(q.id)) {
            setPage(Page.CONFIG);
        } else if (MENU_ID_EDITOR.equals(q.id)) {
            Minecraft.getInstance().setScreen(new QuestEditorScreen(this));
        } else if (MENU_ID_DISCORD.equals(q.id)) {
            Util.getPlatform().openUri("https://discord.gg/DARzByw6VW");
        }
    }

    private void setPage(Page next) {
        page = next;

        boolean menu = page == Page.MENU;
        menuList.visible = menu;
        menuList.active = menu;

        boolean config = page == Page.CONFIG;
        uiPinnedRow.visible = config;
        uiPinnedRow.active = config;
        uiHideInventoryRow.visible = config;
        uiHideInventoryRow.active = config;
        uiInventoryButtonPositionRow.visible = config;
        uiInventoryButtonPositionRow.active = config;
        uiCenterInventoryWithPanelRow.visible = config;
        uiCenterInventoryWithPanelRow.active = config;
        uiHideHeaderRow.visible = config;
        uiHideHeaderRow.active = config;
        uiFilterDisplayRow.visible = config;
        uiFilterDisplayRow.active = config;
        uiDisableCategoriesRow.visible = config;
        uiDisableCategoriesRow.active = config;
        uiHideQuestWidgetIconsRow.visible = config;
        uiHideQuestWidgetIconsRow.active = config;
        uiQuestTextScaleRow.visible = config;
        uiQuestTextScaleRow.active = config;
        uiQuestIconScaleRow.visible = config;
        uiQuestIconScaleRow.active = config;
        uiEnableSearchBoxRow.visible = config;
        uiEnableSearchBoxRow.active = config;
        uiEnableDescriptionColorsRow.visible = config;
        uiEnableDescriptionColorsRow.active = config;
        uiEnableQuestToastsRow.visible = config;
        uiEnableQuestToastsRow.active = config;
        functionalityDisablePinningRow.visible = config;
        functionalityDisablePinningRow.active = config;
        functionalityAutoClaimRow.visible = config;
        functionalityAutoClaimRow.active = config;
        functionalityQuestScrollsRow.visible = config;
        functionalityQuestScrollsRow.active = config;
        gameplayDisableQuestBookRow.visible = config;
        gameplayDisableQuestBookRow.active = config;
        gameplaySpawnWithBookRow.visible = config;
        gameplaySpawnWithBookRow.active = config;
        for (ConfigTabButton button : configTabButtons) {
            button.visible = config;
            button.active = config;
        }
        resetConfigButton.visible = config;
        resetConfigButton.active = config;

        boolean showBack = page != Page.MENU;
        backButton.visible = showBack;
        backButton.active = showBack;
        if (header != null) {
            header.visible = true;
            header.active = false;
        }

        if (config) {
            refreshConfigFields();
            applyConfigScrollLayout();
        }
    }

    private void refreshConfigFields() {
        pinnedHudPos = normalizeHudPos(Config.pinnedQuestHudPosition());
        hideQuestBookInInventory = Config.hideQuestBookInInventory();
        questBookInventoryButtonPosition = normalizeQuestBookInventoryButtonPosition(Config.questBookInventoryButtonPosition());
        centerInventoryWithQuestPanel = Config.centerInventoryWithQuestPanel();
        hideCategoryHeader = Config.hideCategoryHeader();
        filterDisplayMode = Config.filterDisplayMode();
        disableCategories = Config.disableCategories();
        hideQuestWidgetIcons = Config.hideQuestWidgetIcons();
        questTextScale = Config.questTextScale();
        questIconScale = Config.questIconScale();
        enableQuestSearchBox = Config.enableQuestSearchBox();
        enableDescriptionColors = Config.enableDescriptionColors();
        enableQuestToasts = Config.enableQuestToasts();
        disableQuestPinning = Config.disableQuestPinning();
        autoClaimQuestRewards = Config.autoClaimQuestRewards();
        enableQuestScrolls = Config.enableQuestScrolls();
        disableQuestBook = Config.disableQuestBook();
        spawnWithQuestBook = Config.spawnWithQuestBook();
    }

    private void cyclePinnedHudPosition() {
        int idx = HUD_POSITIONS.indexOf(pinnedHudPos);
        int next = idx < 0 ? 0 : (idx + 1) % HUD_POSITIONS.size();
        pinnedHudPos = HUD_POSITIONS.get(next);
    }

    private void cycleFilterDisplayMode() {
        filterDisplayMode = switch (filterDisplayMode == null ? "tabs" : filterDisplayMode) {
            case "buttons" -> "tabs";
            case "tabs" -> "hidden";
            default -> "buttons";
        };
    }

    private String formatFilterDisplayMode() {
        return switch (filterDisplayMode == null ? "tabs" : filterDisplayMode) {
            case "buttons" -> "As Buttons";
            case "hidden" -> "Hidden";
            default -> "As Tabs";
        };
    }

    private void cycleQuestBookInventoryButtonPosition() {
        questBookInventoryButtonPosition = "above_offhand_slot".equals(questBookInventoryButtonPosition)
                ? "beside_recipe_book"
                : "above_offhand_slot";
    }

    private String formatQuestBookInventoryButtonPosition() {
        return "above_offhand_slot".equals(questBookInventoryButtonPosition)
                ? "Above Offhand Slot"
                : "Beside Recipe Book";
    }

    private void cycleQuestTextScale() {
        questTextScale = Math.round((questTextScale + 0.1D) * 10.0D) / 10.0D;
        if (questTextScale > 1.0D) questTextScale = 0.5D;
    }

    private String formatQuestTextScale() {
        return String.format(java.util.Locale.ROOT, "%.1fx", Math.max(0.5D, Math.min(1.0D, questTextScale)));
    }

    private void cycleQuestIconScale() {
        questIconScale = Math.round((questIconScale + 0.1D) * 10.0D) / 10.0D;
        if (questIconScale > 1.0D) questIconScale = 0.5D;
    }

    private String formatQuestIconScale() {
        return String.format(java.util.Locale.ROOT, "%.1fx", Math.max(0.5D, Math.min(1.0D, questIconScale)));
    }

    private void saveConfig() {
        saveConfig(false);
    }

    private void saveConfig(boolean close) {
        if (disableQuestBook && hideQuestBookInInventory) {
            hideQuestBookInInventory = false;
        }

        Config.PINNED_QUEST_HUD_POSITION.set(pinnedHudPos);
        Config.HIDE_QUEST_BOOK_IN_INVENTORY.set(hideQuestBookInInventory);
        Config.QUEST_BOOK_INVENTORY_BUTTON_POSITION.set(questBookInventoryButtonPosition);
        Config.CENTER_INVENTORY_WITH_QUEST_PANEL.set(centerInventoryWithQuestPanel);
        Config.HIDE_CATEGORY_HEADER.set(hideCategoryHeader);
        Config.FILTER_DISPLAY_MODE.set(filterDisplayMode);
        Config.DISABLE_CATEGORIES.set(disableCategories);
        Config.HIDE_QUEST_WIDGET_ICONS.set(hideQuestWidgetIcons);
        Config.QUEST_TEXT_SCALE.set(Math.max(0.5D, Math.min(1.0D, questTextScale)));
        Config.QUEST_ICON_SCALE.set(Math.max(0.5D, Math.min(1.0D, questIconScale)));
        Config.ENABLE_QUEST_SEARCH_BOX.set(enableQuestSearchBox);
        Config.ENABLE_DESCRIPTION_COLORS.set(enableDescriptionColors);
        Config.ENABLE_QUEST_TOASTS.set(enableQuestToasts);
        Config.DISABLE_QUEST_PINNING.set(disableQuestPinning);
        Config.AUTO_CLAIM_QUEST_REWARDS.set(autoClaimQuestRewards);
        Config.ENABLE_QUEST_SCROLLS.set(enableQuestScrolls);
        Config.DISABLE_QUEST_BOOK.set(disableQuestBook);
        Config.SPAWN_WITH_QUEST_BOOK.set(spawnWithQuestBook);
        Config.SPEC.save();
        QuestPanelClient.applyConfigChanges();
        if (close) {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private void resetCurrentConfigTab() {
        resetConfigTab(configTab);
        saveConfig(false);
        refreshConfigFields();
        applyConfigScrollLayout();
    }

    private void resetConfigTab(ConfigTab tab) {
        if (tab == ConfigTab.ALL || tab == ConfigTab.UI) {
            pinnedHudPos = "bottom_left";
            hideQuestBookInInventory = false;
            questBookInventoryButtonPosition = "beside_recipe_book";
            centerInventoryWithQuestPanel = true;
            filterDisplayMode = "tabs";
            disableCategories = false;
            enableQuestSearchBox = false;
        }
        if (tab == ConfigTab.ALL || tab == ConfigTab.STYLE) {
            hideQuestWidgetIcons = false;
            questTextScale = 1.0D;
            questIconScale = 1.0D;
            enableDescriptionColors = false;
            hideCategoryHeader = false;
        }
        if (tab == ConfigTab.ALL || tab == ConfigTab.FEATURES) {
            enableQuestToasts = true;
            disableQuestPinning = false;
            autoClaimQuestRewards = false;
            enableQuestScrolls = true;
            disableQuestBook = false;
            spawnWithQuestBook = false;
        }
    }

    private List<QuestData.Quest> buildMenuQuests() {
        List<QuestData.Quest> out = new ArrayList<>();
        out.add(buildMenuQuest(MENU_ID_CONFIG, "Config", MENU_CONFIG_TEX.toString()));
        out.add(buildMenuQuest(MENU_ID_EDITOR, "Quest Editor", MENU_EDITOR_TEX.toString()));
        out.add(buildMenuQuest("settings_spacer_1", "", "minecraft:air"));
        out.add(buildMenuQuest("settings_spacer_2", "", "minecraft:air"));
        out.add(buildMenuQuest(MENU_ID_DISCORD, "Discord", MENU_DISCORD_TEX.toString()));
        return out;
    }

    private QuestData.Quest buildMenuQuest(String id, String name, String icon) {
        return new QuestData.Quest(
                id,
                name,
                icon,
                "",
                List.of(),
                false,
                false,
                false,
                false,
                false,
                new QuestData.Rewards(List.of(), List.of(), List.of(), List.of(), "", 0),
                "all",
                null,
                "",
                "",
                id
        );
    }

    private String normalizeHudPos(String raw) {
        if (raw == null) return HUD_POSITIONS.get(0);
        String lower = raw.trim().toLowerCase();
        return HUD_POSITIONS.contains(lower) ? lower : HUD_POSITIONS.get(0);
    }

    private String normalizeQuestBookInventoryButtonPosition(String raw) {
        if (raw == null) return "beside_recipe_book";
        String lower = raw.trim().toLowerCase();
        return ("beside_recipe_book".equals(lower) || "above_offhand_slot".equals(lower))
                ? lower
                : "beside_recipe_book";
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = List.of();
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        if (page == Page.CONFIG) {
            boolean backVisible = backButton != null && backButton.visible;
            boolean resetVisible = resetConfigButton != null && resetConfigButton.visible;
            List<Boolean> tabVisible = new ArrayList<>();
            for (ConfigTabButton button : configTabButtons) {
                tabVisible.add(button.visible);
                button.visible = false;
            }
            if (backButton != null) backButton.visible = false;
            if (resetConfigButton != null) resetConfigButton.visible = false;

            renderConfigPageHeader(gg);

            int top = configViewportTop();
            int bottom = configViewportBottom();
            gg.enableScissor(px, top, px + pw, bottom);
            super.render(gg, mouseX, mouseY, partialTick);
            gg.disableScissor();

            for (int i = 0; i < configTabButtons.size(); i++) {
                ConfigTabButton button = configTabButtons.get(i);
                button.visible = i < tabVisible.size() ? tabVisible.get(i) : true;
                if (button.visible) button.render(gg, mouseX, mouseY, partialTick);
            }
            if (backButton != null) backButton.visible = backVisible;
            if (resetConfigButton != null) resetConfigButton.visible = resetVisible;
            if (backButton != null && backButton.visible) backButton.render(gg, mouseX, mouseY, partialTick);
            if (resetConfigButton != null && resetConfigButton.visible) resetConfigButton.render(gg, mouseX, mouseY, partialTick);

            renderConfigScrollbar(gg);
            renderPendingTooltipOnTop(gg);
            return;
        }

        super.render(gg, mouseX, mouseY, partialTick);
        renderPendingTooltipOnTop(gg);
    }

    private void queueTooltip(List<Component> tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.isEmpty()) return;
        pendingTooltip = tooltip;
        pendingTooltipX = mouseX;
        pendingTooltipY = mouseY;
    }

    private void renderPendingTooltipOnTop(GuiGraphics gg) {
        if (pendingTooltip == null || pendingTooltip.isEmpty()) return;
        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 500.0F);
        gg.renderComponentTooltip(Minecraft.getInstance().font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        gg.pose().popPose();
        pendingTooltip = List.of();
    }

    private void renderSectionHeader(GuiGraphics gg, String text, int x, int y, int color) {
        if (text == null || text.isBlank()) return;
        int top = configViewportTop();
        int bottom = configViewportBottom();
        if (y < top || y > bottom - 8) return;
        float scale = 0.72f;
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1f);
        float inv = 1f / scale;
        gg.drawString(font, text, (int) (x * inv), (int) (y * inv), color, false);
        gg.pose().popPose();
    }

    private void renderConfigPageHeader(GuiGraphics gg) {
        String text = configTab.title;
        int textW = font.width(text);
        int headerW = Math.max(22, textW + 10);
        int x = leftX + 5;
        int y = topY - 7;
        blitHeader(gg, x, y, headerW);
        gg.drawString(font, text, x + (headerW - textW) / 2, y + 4, 0x404040, false);
    }

    private void blitHeader(GuiGraphics gg, int x, int y, int w) {
        int middleW = Math.max(0, w - HEADER_SLICE * 2);
        gg.blit(HEADER_TEX, x, y, 0, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
        for (int i = 0; i < middleW; i++) {
            gg.blit(HEADER_TEX, x + HEADER_SLICE + i, y, HEADER_SLICE, 0, 1, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
        }
        gg.blit(HEADER_TEX, x + HEADER_SLICE + middleW, y, HEADER_TEX_W - HEADER_SLICE, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
    }

    private int configViewportTop() {
        return topY + 8;
    }

    private int configViewportBottom() {
        return topY + 157;
    }

    private int configViewportHeight() {
        return Math.max(0, configViewportBottom() - configViewportTop());
    }

    private int configContentHeight() {
        return Math.max(0, visibleConfigRows().size() * 21 + 27);
    }

    private int maxConfigScroll() {
        return Math.max(0, configContentHeight() - configViewportHeight());
    }

    private int scrolledY(int baseY) {
        return baseY - Math.round(configScrollY);
    }

    private void applyConfigScrollLayout() {
        int max = maxConfigScroll();
        if (configScrollY < 0f) configScrollY = 0f;
        if (configScrollY > max) configScrollY = max;

        int top = configViewportTop();
        int bottom = configViewportBottom();
        int baseY = top + 2;
        int rowGap = 21;
        List<ConfigRow> visibleRows = visibleConfigRows();
        for (ConfigRow row : configRows) {
            row.visible = false;
            row.active = false;
        }
        for (int i = 0; i < visibleRows.size(); i++) {
            layoutRow(visibleRows.get(i), baseY + i * rowGap, top, bottom);
        }
        if (resetConfigButton != null) {
            resetConfigButton.setX(px + 24);
            resetConfigButton.setY(scrolledY(baseY + visibleRows.size() * rowGap + 2));
            boolean inView = resetConfigButton.getY() + resetConfigButton.getHeight() > top && resetConfigButton.getY() < bottom;
            resetConfigButton.visible = page == Page.CONFIG && inView;
            resetConfigButton.active = page == Page.CONFIG && inView;
        }
        layoutConfigTabButtons();
    }

    private List<ConfigRow> visibleConfigRows() {
        if (configTab == ConfigTab.ALL) return new ArrayList<>(configRows);
        List<ConfigRow> out = new ArrayList<>();
        for (ConfigRow row : configRows) {
            if (row.group == configTab) out.add(row);
        }
        return out;
    }

    private void layoutConfigTabButtons() {
        int x = leftX - TAB_W + 4;
        int startY = py + 6;
        for (int i = 0; i < configTabButtons.size(); i++) {
            ConfigTabButton button = configTabButtons.get(i);
            button.setX(x);
            button.setY(startY + i * (TAB_H + TAB_GAP));
            button.visible = page == Page.CONFIG;
            button.active = page == Page.CONFIG;
        }
    }

    private void layoutRow(ConfigRow row, int baseY, int top, int bottom) {
        if (row == null) return;
        int y = scrolledY(baseY);
        row.setY(y);
        boolean inView = (y + row.getHeight()) > top && y < bottom;
        row.visible = page == Page.CONFIG;
        row.active = page == Page.CONFIG && inView;
    }

    private void renderConfigScrollbar(GuiGraphics gg) {
        int max = maxConfigScroll();
        if (max <= 0) return;
        int trackX1 = px + pw + 4;
        int trackX2 = px + pw + 6;
        int top = configViewportTop();
        int vh = configViewportHeight();
        int content = configContentHeight();
        if (vh <= 0 || content <= 0) return;
        int thumbH = Math.max(12, (int) ((vh * (float) vh) / (float) content));
        float t = max <= 0 ? 0f : configScrollY / (float) max;
        int thumbY = top + (int) ((vh - thumbH) * t);
        gg.fill(trackX1, thumbY, trackX2, thumbY + thumbH, 0xFF909090);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == Page.CONFIG) {
            int top = configViewportTop();
            int bottom = configViewportBottom();
            if (mouseX < px || mouseX > px + pw || mouseY < top || mouseY > bottom) {
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            int max = maxConfigScroll();
            if (max > 0) {
                configScrollY = Math.max(0f, Math.min(max, configScrollY - (float) (scrollY * 12)));
                applyConfigScrollLayout();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.CONFIG && button == 0) {
            if (backButton != null && backButton.visible && backButton.active && backButton.isMouseOver(mouseX, mouseY)) {
                backButton.onPress();
                return true;
            }
            if (resetConfigButton != null && resetConfigButton.visible && resetConfigButton.active && resetConfigButton.isMouseOver(mouseX, mouseY)) {
                return resetConfigButton.mouseClicked(mouseX, mouseY, button);
            }
            if (isOverConfigScrollbar(mouseX, mouseY) && maxConfigScroll() > 0) {
                configScrollbarDragging = true;
                setConfigScrollFromMouse(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && configScrollbarDragging) {
            configScrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (page == Page.CONFIG && button == 0 && configScrollbarDragging) {
            setConfigScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isOverConfigScrollbar(double mouseX, double mouseY) {
        int top = configViewportTop();
        int bottom = configViewportBottom();
        return mouseX >= px + pw + 2 && mouseX <= px + pw + 8 && mouseY >= top && mouseY <= bottom;
    }

    private void setConfigScrollFromMouse(double mouseY) {
        int max = maxConfigScroll();
        int vh = configViewportHeight();
        int content = configContentHeight();
        if (max <= 0 || vh <= 0 || content <= 0) return;
        int thumbH = Math.max(12, (int) ((vh * (float) vh) / (float) content));
        float t = (float) ((mouseY - configViewportTop() - thumbH / 2.0D) / Math.max(1.0D, vh - thumbH));
        configScrollY = Math.max(0f, Math.min(max, t * max));
        applyConfigScrollLayout();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Page {
        MENU,
        CONFIG
    }

    private enum ConfigTab {
        ALL("All"),
        STYLE("Style"),
        UI("UI"),
        FEATURES("Features");

        final String title;

        ConfigTab(String title) {
            this.title = title;
        }
    }

    private final class ConfigTabButton extends AbstractButton {
        private final ConfigTab tab;
        private final ResourceLocation icon;
        private final String tooltip;

        ConfigTabButton(ConfigTab tab, ResourceLocation icon, String tooltip) {
            super(0, 0, TAB_W, TAB_H, Component.empty());
            this.tab = tab;
            this.icon = icon;
            this.tooltip = tooltip == null ? "" : tooltip;
        }

        @Override
        public void onPress() {
            if (configTab == tab) return;
            configTab = tab;
            configScrollY = 0f;
            applyConfigScrollLayout();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean selected = configTab == tab;
            int renderX = getX() + (selected ? -2 : 0);
            gg.blit(selected ? TAB_SELECTED_TEX : TAB_TEX, renderX, getY(), 0, 0, this.width, this.height, TAB_W, TAB_H);
            gg.blit(icon, renderX + (this.width - 16) / 2, getY() + (this.height - 16) / 2, 0, 0, 16, 16, 16, 16);
            if (this.isMouseOver(mouseX, mouseY)) {
                queueTooltip(List.of(Component.literal(tooltip)), mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private final class ConfigRow extends AbstractButton {
        private final String label;
        private final String subtitle;
        private final java.util.function.Supplier<String> value;
        private final Runnable onPress;
        private ConfigTab group = ConfigTab.ALL;

        public ConfigRow(int x, int y, int w, String label, String subtitle,
                         java.util.function.Supplier<String> value,
                         Runnable onPress) {
            super(x, y, w, 20, Component.empty());
            this.label = label == null ? "" : label;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.value = value;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) {
                onPress.run();
                saveConfig(false);
                applyConfigScrollLayout();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            gg.blit(ROW_TEX, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            boolean hovered = this.isMouseOver(mouseX, mouseY);
            if (hovered) {
                gg.fill(getX() + 1, getY() + 1, getX() + this.width - 1, getY() + this.height - 1, 0x20FFFFFF);
            }

            var font = Minecraft.getInstance().font;
            String valueText = value == null ? "" : value.get();
            if (valueText == null) valueText = "";

            int labelX = getX() + 6;
            float valueScale = 0.72f;
            int valueW = (int) (font.width(valueText) * valueScale);
            int valueX = getX() + this.width - valueW - 6;

            String labelText = label;
            int maxLabelW = valueX - labelX + 9;
            if (maxLabelW > 0 && font.width(labelText) > maxLabelW) {
                int ellipsisW = font.width("...");
                int allowed = Math.max(0, maxLabelW - ellipsisW);
                labelText = font.plainSubstrByWidth(labelText, allowed) + "...";
            }

            drawScaledString(gg, labelText, 0.82f, labelX, getY() + 5, 0xFFFFFF);
            drawScaledString(gg, valueText, valueScale, valueX, getY() + 7, 0xA0C8FF);

            if (hovered) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(label));
                if (!subtitle.isBlank()) tooltip.add(Component.literal(subtitle));
                queueTooltip(tooltip, mouseX, mouseY);
            }
        }

        private void drawScaledString(GuiGraphics gg, String text, float scale, int x, int y, int color) {
            if (text == null || text.isEmpty()) return;
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(Minecraft.getInstance().font, text, (int) (x * inv), (int) (y * inv), color, false);
            gg.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class HoldResetButton extends AbstractButton {
        private static final long HOLD_MS = 3000L;

        private final java.util.function.Supplier<Component> label;
        private final Runnable onComplete;
        private boolean holding;
        private long holdStartMs;
        private boolean completed;

        HoldResetButton(int x, int y, int w, int h, java.util.function.Supplier<Component> label, Runnable onComplete) {
            super(x, y, w, h, Component.empty());
            this.label = label;
            this.onComplete = onComplete;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.active || !this.visible || button != 0 || !this.isMouseOver(mouseX, mouseY)) return false;
            holding = true;
            completed = false;
            holdStartMs = Util.getMillis();
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) {
                holding = false;
                completed = false;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void onPress() {
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            if (holding && (!this.isMouseOver(mouseX, mouseY) || !this.active || !this.visible)) {
                holding = false;
                completed = false;
            }
            float progress = 0f;
            if (holding) {
                progress = Math.min(1f, (Util.getMillis() - holdStartMs) / (float) HOLD_MS);
                if (progress >= 1f && !completed) {
                    completed = true;
                    holding = false;
                    if (onComplete != null) onComplete.run();
                }
            }

            if (progress > 0f) {
                gg.fill(getX() + 2, getY() + this.height - 3, getX() + 2 + Math.round((this.width - 4) * progress), getY() + this.height - 1, 0xFFFF3030);
            }

            Component message = label == null ? Component.empty() : label.get();
            String text = message.getString();
            var font = Minecraft.getInstance().font;
            float scale = 0.72f;
            int textW = Math.round(font.width(text) * scale);
            int textX = getX() + (this.width - textW) / 2;
            int textY = getY() + (this.height - Math.round(font.lineHeight * scale)) / 2;
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(font, text, (int) (textX * inv), (int) (textY * inv), this.active ? 0xFFFF3030 : 0xFF803030, false);
            gg.pose().popPose();

            if (hovered) {
                gg.renderComponentTooltip(font, List.of(Component.literal("Hold to delete")), mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class ActionButton extends AbstractButton {
        private final Runnable onPress;

        public ActionButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? BTN_TEX_DISABLED : (hovered ? BTN_TEX_HOVER : BTN_TEX);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            var font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(font, getMessage(), textX, textY, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class BackButton extends AbstractButton {
        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? BTN_BACK_TEX_HOVER : BTN_BACK_TEX;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
