package net.revilodev.boundless.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.CategoryHeaderWidget;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.client.QuestListWidget;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class QuestSettingsScreen extends Screen {

    // shared panel and button textures
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
    private static final ResourceLocation BACK_TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/back_tab.png");
    private static final ResourceLocation BACK_TAB_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/back_tab-hovered.png");
    private static final ResourceLocation HEADER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/3-slice-header.png");
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
    private static final ResourceLocation DEBUG_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/debug.png");
    private static final ResourceLocation UNDEBUG_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/un-debug.png");
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int TAB_GAP = 3;
    private static final int BACK_TAB_W = 27;
    private static final int BACK_TAB_H = 17;
    private static final int SIDE_TAB_X_OFFSET = 4;
    private static final int BACK_TAB_X_OFFSET = SIDE_TAB_X_OFFSET + 6;
    private static final int BACK_TAB_BOTTOM_MARGIN = 6;
    private static final int HEADER_TEX_W = 72;
    private static final int HEADER_TEX_H = 10;
    private static final int HEADER_SLICE = 3;

    private static final String MENU_ID_CONFIG = "01_settings_config";
    private static final String MENU_ID_EDITOR = "02_settings_editor";
    private static final String MENU_ID_ASSETS = "03_settings_assets";
    private static final String MENU_ID_DISCORD = "04_settings_discord";
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

    // settings home page
    private QuestListWidget menuList;
    private CategoryHeaderWidget header;

    // rows on the config page
    private ConfigRow uiPinnedRow;
    private ConfigRow uiHideInventoryRow;
    private ConfigRow uiInventoryButtonPositionRow;
    private ConfigRow uiCenterInventoryWithPanelRow;
    private ConfigRow uiHideHeaderRow;
    private ConfigRow uiFilterDisplayRow;
    private ConfigRow uiDisableCategoriesRow;
    private ConfigRow uiShowIdFieldRow;
    private ConfigRow uiHideQuestWidgetIconsRow;
    private ConfigRow uiQuestTextScaleRow;
    private ConfigRow uiQuestIconScaleRow;
    private ConfigRow uiEnableSearchBoxRow;
    private ConfigRow uiCustomBackgroundsRow;
    private ConfigRow uiStickySubCategoryHeadersRow;
    private ConfigRow uiStickyDescriptionHeadersRow;
    private ConfigRow uiEnableDescriptionColorsRow;
    private ConfigRow uiQuestWidgetTextColorRow;
    private ConfigRow uiDescriptionTextColorRow;
    private EditBox questWidgetTextColorField;
    private EditBox descriptionTextColorField;
    private EditBox configSearchField;
    private String configSearchQuery = "";
    private final List<ColorConfigControl> detailColorControls = new ArrayList<>();
    private ConfigRow uiEnableDescriptionReadMoreRow;
    private ConfigRow uiEnableDescriptionWrappingRow;
    private ConfigRow uiDescriptionAlignmentRow;
    private ConfigRow uiEnableQuestToastsRow;
    private ConfigRow functionalityDisablePinningRow;
    private ConfigRow functionalityAutoClaimRow;
    private ConfigRow functionalityQuestRejectingRow;
    private ConfigRow functionalityQuestScrollsRow;
    private ConfigRow functionalityQuickEditRow;
    private ConfigRow gameplayDisableQuestBookRow;
    private ConfigRow gameplaySpawnWithBookRow;

    private BackButton backButton;
    private HoldResetButton resetConfigButton;
    private DebugButton debugButton;
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
    private int uiShowIdFieldBaseY;
    private int uiHideQuestWidgetIconsBaseY;
    private int uiQuestTextScaleBaseY;
    private int uiQuestIconScaleBaseY;
    private int uiEnableSearchBoxBaseY;
    private int uiEnableDescriptionColorsBaseY;
    private int uiQuestWidgetTextColorBaseY;
    private int uiDescriptionTextColorBaseY;
    private int uiEnableDescriptionReadMoreBaseY;
    private int uiEnableDescriptionWrappingBaseY;
    private int uiDescriptionAlignmentBaseY;
    private int uiEnableQuestToastsBaseY;
    private int functionalityDisablePinningBaseY;
    private int functionalityAutoClaimBaseY;
    private int functionalityQuestRejectingBaseY;
    private int functionalityQuestScrollsBaseY;
    private int functionalityQuickEditBaseY;
    private int gameplayDisableQuestBookBaseY;
    private int gameplaySpawnWithBookBaseY;

    private String pinnedHudPos;
    private boolean hideQuestBookInInventory;
    private String questBookInventoryButtonPosition;
    private boolean centerInventoryWithQuestPanel;
    private boolean hideCategoryHeader;
    private String filterDisplayMode;
    private boolean disableCategories;
    private boolean showIdField;
    private boolean hideQuestWidgetIcons;
    private double questTextScale;
    private double questIconScale;
    private boolean enableQuestSearchBox;
    private boolean customBackgrounds;
    private boolean stickySubCategoryHeaders;
    private boolean stickyDescriptionHeaders;
    private boolean enableDescriptionColors;
    private String questWidgetTextColor;
    private String descriptionTextColor;
    private boolean enableDescriptionReadMore;
    private boolean enableDescriptionTextWrapping;
    private String descriptionTextAlignment;
    private boolean enableQuestToasts;
    private boolean disableQuestPinning;
    private boolean autoClaimQuestRewards;
    private boolean enableQuestRejecting;
    private boolean enableQuestScrolls;
    private boolean quickEdit;
    private boolean disableQuestBook;
    private boolean spawnWithQuestBook;
    private ConfigTab configTab = ConfigTab.ALL;

    public QuestSettingsScreen(Screen parent) {
        super(tr("title"));
        this.parent = parent;
    }

    private boolean canManageServerSettings() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && (mc.hasSingleplayerServer() || mc.player.hasPermissions(2));
    }

    @Override
    protected void init() {
        // Every player can manage client settings; authority-only rows are filtered below.
        var mc = Minecraft.getInstance();
        if (mc.player == null) {
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
        // The first page reuses the normal quest list as a simple menu
        menuList = new QuestListWidget(px + 2, py, pw, ph, this::handleMenuClick);
        menuList.setUseConfigScaling(false);
        menuList.setQuests(buildMenuQuests());
        menuList.setCategory("all");
        menuList.setBypassFilters(true);
        addRenderableWidget(menuList);
    }

    private void initConfigWidgets() {
        // Rows are laid out again when active tab or scroll position changes
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
        uiShowIdFieldBaseY = uiRow1 + rowGap * 7;
        uiHideQuestWidgetIconsBaseY = uiRow1 + rowGap * 8;
        uiQuestTextScaleBaseY = uiRow1 + rowGap * 9;
        uiQuestIconScaleBaseY = uiRow1 + rowGap * 10;
        uiEnableSearchBoxBaseY = uiRow1 + rowGap * 11;
        uiEnableDescriptionColorsBaseY = uiRow1 + rowGap * 12;
        uiQuestWidgetTextColorBaseY = uiRow1 + rowGap * 13;
        uiDescriptionTextColorBaseY = uiRow1 + rowGap * 14;
        uiEnableDescriptionReadMoreBaseY = uiRow1 + rowGap * 15;
        uiEnableDescriptionWrappingBaseY = uiRow1 + rowGap * 16;
        uiDescriptionAlignmentBaseY = uiRow1 + rowGap * 17;
        uiEnableQuestToastsBaseY = uiRow1 + rowGap * 18;
        functionalityHeaderBaseY = uiRow1 + rowGap * 19 + 4;
        functionalityDisablePinningBaseY = functionalityHeaderBaseY + 10;
        functionalityAutoClaimBaseY = functionalityDisablePinningBaseY + rowGap;
        functionalityQuestRejectingBaseY = functionalityAutoClaimBaseY + rowGap;
        functionalityQuestScrollsBaseY = functionalityQuestRejectingBaseY + rowGap;
        functionalityQuickEditBaseY = functionalityQuestScrollsBaseY + rowGap;
        gameplayHeaderBaseY = functionalityQuickEditBaseY + rowGap + 2;
        gameplayDisableQuestBookBaseY = gameplayHeaderBaseY + 10;
        gameplaySpawnWithBookBaseY = gameplayDisableQuestBookBaseY + rowGap;

        uiPinnedRow = new ConfigRow(px, uiPinnedBaseY, pw, trs("row.pinned_gui"),
                trs("row.pinned_gui.subtitle"),
                () -> pinnedHudPos, this::cyclePinnedHudPosition);
        uiHideInventoryRow = new ConfigRow(px, uiHideInventoryBaseY, pw, trs("row.inventory_questbook"),
                trs("row.inventory_questbook.subtitle"),
                () -> trs(hideQuestBookInInventory ? "state.on" : "state.off"),
                () -> {
                    hideQuestBookInInventory = !hideQuestBookInInventory;
                    if (hideQuestBookInInventory) disableQuestBook = false;
                });
        uiInventoryButtonPositionRow = new ConfigRow(px, uiInventoryButtonPositionBaseY, pw, trs("row.book_gui_position"),
                trs("row.book_gui_position.subtitle"),
                this::formatQuestBookInventoryButtonPosition,
                this::cycleQuestBookInventoryButtonPosition);
        uiCenterInventoryWithPanelRow = new ConfigRow(px, uiCenterInventoryWithPanelBaseY, pw, trs("row.centre_quest_ui"),
                trs("row.centre_quest_ui.subtitle"),
                () -> trs(centerInventoryWithQuestPanel ? "state.enabled" : "state.disabled"),
                () -> centerInventoryWithQuestPanel = !centerInventoryWithQuestPanel);
        uiHideHeaderRow = new ConfigRow(px, uiHideHeaderBaseY, pw, trs("row.display_headers"),
                trs("row.display_headers.subtitle"),
                () -> trs(hideCategoryHeader ? "state.disabled" : "state.enabled"),
                () -> hideCategoryHeader = !hideCategoryHeader);
        uiFilterDisplayRow = new ConfigRow(px, uiFilterDisplayBaseY, pw, trs("row.display_filters"),
                trs("row.display_filters.subtitle"),
                this::formatFilterDisplayMode,
                this::cycleFilterDisplayMode);
        uiDisableCategoriesRow = new ConfigRow(px, uiDisableCategoriesBaseY, pw, trs("row.categories"),
                trs("row.categories.subtitle"),
                () -> trs(disableCategories ? "state.disabled" : "state.enabled"),
                () -> disableCategories = !disableCategories);
        uiShowIdFieldRow = new ConfigRow(px, uiShowIdFieldBaseY, pw, trs("row.show_id_field"),
                trs("row.show_id_field.subtitle"),
                () -> trs(showIdField ? "state.on" : "state.off"),
                () -> showIdField = !showIdField);
        uiHideQuestWidgetIconsRow = new ConfigRow(px, uiHideQuestWidgetIconsBaseY, pw, trs("row.disable_widget_icons"),
                trs("row.disable_widget_icons.subtitle"),
                () -> trs(hideQuestWidgetIcons ? "state.on" : "state.off"),
                () -> hideQuestWidgetIcons = !hideQuestWidgetIcons);
        uiQuestTextScaleRow = new ConfigRow(px, uiQuestTextScaleBaseY, pw, trs("row.text_scale"),
                trs("row.text_scale.subtitle"),
                this::formatQuestTextScale,
                this::cycleQuestTextScale);
        uiQuestIconScaleRow = new ConfigRow(px, uiQuestIconScaleBaseY, pw, trs("row.icon_scale"),
                trs("row.icon_scale.subtitle"),
                this::formatQuestIconScale,
                this::cycleQuestIconScale);
        uiEnableSearchBoxRow = new ConfigRow(px, uiEnableSearchBoxBaseY, pw, trs("row.search_widget"),
                trs("row.search_widget.subtitle"),
                () -> trs(enableQuestSearchBox ? "state.on" : "state.off"),
                () -> enableQuestSearchBox = !enableQuestSearchBox);
        uiCustomBackgroundsRow = new ConfigRow(px, uiEnableSearchBoxBaseY, pw, "Custom backgrounds",
                "Render category backgrounds in quest lists and trees.",
                () -> trs(customBackgrounds ? "state.on" : "state.off"),
                () -> customBackgrounds = !customBackgrounds);
        uiStickySubCategoryHeadersRow = new ConfigRow(px, uiEnableSearchBoxBaseY, pw, "Sticky sub-category headers",
                "Keep the active sub-category heading visible while scrolling.",
                () -> trs(stickySubCategoryHeaders ? "state.on" : "state.off"),
                () -> stickySubCategoryHeaders = !stickySubCategoryHeaders);
        uiStickyDescriptionHeadersRow = new ConfigRow(px, uiEnableSearchBoxBaseY, pw, "Sticky description headers",
                "Keep the quest title visible above long descriptions.",
                () -> trs(stickyDescriptionHeaders ? "state.on" : "state.off"),
                () -> stickyDescriptionHeaders = !stickyDescriptionHeaders);
        uiEnableDescriptionColorsRow = new ConfigRow(px, uiEnableDescriptionColorsBaseY, pw, trs("row.text_coloring"),
                trs("row.text_coloring.subtitle"),
                () -> trs(enableDescriptionColors ? "state.on" : "state.off"),
                () -> enableDescriptionColors = !enableDescriptionColors);
        uiQuestWidgetTextColorRow = new ConfigRow(px, uiQuestWidgetTextColorBaseY, pw, trs("row.widget_text_color"),
                trs("row.widget_text_color.subtitle"),
                () -> "", null);
        uiDescriptionTextColorRow = new ConfigRow(px, uiDescriptionTextColorBaseY, pw, trs("row.description_color"),
                trs("row.description_color.subtitle"),
                () -> "", null);
        uiEnableDescriptionReadMoreRow = new ConfigRow(px, uiEnableDescriptionReadMoreBaseY, pw, trs("row.read_more"),
                trs("row.read_more.subtitle"),
                () -> trs(enableDescriptionReadMore ? "state.on" : "state.off"),
                () -> enableDescriptionReadMore = !enableDescriptionReadMore);
        uiEnableDescriptionWrappingRow = new ConfigRow(px, uiEnableDescriptionWrappingBaseY, pw, trs("row.description_wrap"),
                trs("row.description_wrap.subtitle"),
                () -> trs(enableDescriptionTextWrapping ? "state.on" : "state.off"),
                () -> enableDescriptionTextWrapping = !enableDescriptionTextWrapping);
        uiDescriptionAlignmentRow = new ConfigRow(px, uiDescriptionAlignmentBaseY, pw, trs("row.description_align"),
                trs("row.description_align.subtitle"),
                this::formatDescriptionTextAlignment,
                this::cycleDescriptionTextAlignment);
        uiEnableQuestToastsRow = new ConfigRow(px, uiEnableQuestToastsBaseY, pw, trs("row.quest_toasts"),
                trs("row.quest_toasts.subtitle"),
                () -> trs(enableQuestToasts ? "state.on" : "state.off"),
                () -> enableQuestToasts = !enableQuestToasts);

        functionalityDisablePinningRow = new ConfigRow(px, functionalityDisablePinningBaseY, pw, trs("row.quest_pinning"),
                trs("row.quest_pinning.subtitle"),
                () -> trs(disableQuestPinning ? "state.disabled" : "state.enabled"),
                () -> disableQuestPinning = !disableQuestPinning);
        functionalityAutoClaimRow = new ConfigRow(px, functionalityAutoClaimBaseY, pw, trs("row.auto_claim_rewards"),
                trs("row.auto_claim_rewards.subtitle"),
                () -> trs(autoClaimQuestRewards ? "state.on" : "state.off"),
                () -> autoClaimQuestRewards = !autoClaimQuestRewards);
        functionalityQuestRejectingRow = new ConfigRow(px, functionalityQuestRejectingBaseY, pw, "Enable quest rejecting",
                "Allow players to reject and restore optional quests.",
                () -> trs(enableQuestRejecting ? "state.on" : "state.off"),
                () -> enableQuestRejecting = !enableQuestRejecting);
        functionalityQuestScrollsRow = new ConfigRow(px, functionalityQuestScrollsBaseY, pw, trs("row.quest_scrolls"),
                trs("row.quest_scrolls.subtitle"),
                () -> trs(enableQuestScrolls ? "state.on" : "state.off"),
                () -> enableQuestScrolls = !enableQuestScrolls);
        functionalityQuickEditRow = new ConfigRow(px, functionalityQuickEditBaseY, pw, "Quick edit",
                "Show an operator-only edit button on quest pages.",
                () -> trs(quickEdit ? "state.on" : "state.off"),
                () -> quickEdit = !quickEdit);

        gameplayDisableQuestBookRow = new ConfigRow(px, gameplayDisableQuestBookBaseY, pw, trs("row.quest_book"),
                trs("row.quest_book.subtitle"),
                () -> trs(disableQuestBook ? "state.disabled" : "state.enabled"),
                () -> {
                    disableQuestBook = !disableQuestBook;
                    if (disableQuestBook) hideQuestBookInInventory = false;
                });
        gameplaySpawnWithBookRow = new ConfigRow(px, gameplaySpawnWithBookBaseY, pw, trs("row.spawn_with_quest_book"),
                trs("row.spawn_with_quest_book.subtitle"),
                () -> trs(spawnWithQuestBook ? "state.on" : "state.off"),
                () -> spawnWithQuestBook = !spawnWithQuestBook);

        uiDisableCategoriesRow.serverOnly = true;
        functionalityDisablePinningRow.serverOnly = true;
        functionalityAutoClaimRow.serverOnly = true;
        functionalityQuestRejectingRow.serverOnly = true;
        functionalityQuestScrollsRow.serverOnly = true;
        functionalityQuickEditRow.serverOnly = true;
        gameplayDisableQuestBookRow.serverOnly = true;
        gameplaySpawnWithBookRow.serverOnly = true;

        addConfigRow(uiHideQuestWidgetIconsRow, ConfigTab.STYLE);
        addConfigRow(uiQuestTextScaleRow, ConfigTab.STYLE);
        addConfigRow(uiQuestIconScaleRow, ConfigTab.STYLE);
        addConfigRow(uiEnableDescriptionColorsRow, ConfigTab.STYLE);
        addConfigRow(uiQuestWidgetTextColorRow, ConfigTab.STYLE);
        addConfigRow(uiDescriptionTextColorRow, ConfigTab.STYLE);
        initDetailColorControls();
        addConfigRow(uiEnableDescriptionReadMoreRow, ConfigTab.STYLE);
        addConfigRow(uiEnableDescriptionWrappingRow, ConfigTab.STYLE);
        addConfigRow(uiDescriptionAlignmentRow, ConfigTab.STYLE);
        addConfigRow(uiHideHeaderRow, ConfigTab.STYLE);
        addConfigRow(uiCustomBackgroundsRow, ConfigTab.STYLE);

        addConfigRow(uiPinnedRow, ConfigTab.UI);
        addConfigRow(uiHideInventoryRow, ConfigTab.UI);
        addConfigRow(uiInventoryButtonPositionRow, ConfigTab.UI);
        addConfigRow(uiCenterInventoryWithPanelRow, ConfigTab.UI);
        addConfigRow(uiFilterDisplayRow, ConfigTab.UI);
        addConfigRow(uiDisableCategoriesRow, ConfigTab.UI);
        addConfigRow(uiShowIdFieldRow, ConfigTab.UI);
        addConfigRow(uiEnableSearchBoxRow, ConfigTab.UI);
        addConfigRow(uiStickySubCategoryHeadersRow, ConfigTab.STYLE);
        addConfigRow(uiStickyDescriptionHeadersRow, ConfigTab.STYLE);

        addConfigRow(uiEnableQuestToastsRow, ConfigTab.FEATURES);
        addConfigRow(functionalityDisablePinningRow, ConfigTab.FEATURES);
        addConfigRow(functionalityAutoClaimRow, ConfigTab.FEATURES);
        addConfigRow(functionalityQuestRejectingRow, ConfigTab.FEATURES);
        addConfigRow(functionalityQuestScrollsRow, ConfigTab.FEATURES);
        addConfigRow(functionalityQuickEditRow, ConfigTab.FEATURES);
        addConfigRow(gameplayDisableQuestBookRow, ConfigTab.FEATURES);
        addConfigRow(gameplaySpawnWithBookRow, ConfigTab.FEATURES);

        addConfigTabButton(ConfigTab.ALL, CONFIG_TAB_ALL_TEX, trs("tab.all"));
        addConfigTabButton(ConfigTab.STYLE, CONFIG_TAB_STYLE_TEX, trs("tab.style"));
        addConfigTabButton(ConfigTab.UI, CONFIG_TAB_UI_TEX, trs("tab.ui"));
        addConfigTabButton(ConfigTab.FEATURES, CONFIG_TAB_FEATURES_TEX, trs("tab.features"));
        initColorFields();
        initConfigSearch();
        applyConfigScrollLayout();
    }

    private void initConfigSearch() {
        configSearchField = new EditBox(font, px + 2, configViewportTop() + 2, pw - 4, 16,
                Component.literal("Search settings"));
        configSearchField.setHint(Component.literal("Search settings"));
        configSearchField.setMaxLength(64);
        configSearchField.setValue(configSearchQuery);
        configSearchField.setResponder(value -> {
            configSearchQuery = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
            configScrollY = 0f;
            applyConfigScrollLayout();
        });
        addRenderableWidget(configSearchField);
    }

    private void initColorFields() {
        // These sit over their rows so hex values can be typed directly.
        questWidgetTextColorField = new EditBox(font, px + pw - 45, uiQuestWidgetTextColorBaseY + 3, 39, 14, Component.empty());
        descriptionTextColorField = new EditBox(font, px + pw - 45, uiDescriptionTextColorBaseY + 3, 39, 14, Component.empty());
        configureColorField(questWidgetTextColorField, true);
        configureColorField(descriptionTextColorField, false);
        addRenderableWidget(questWidgetTextColorField);
        addRenderableWidget(descriptionTextColorField);
    }

    private void configureColorField(EditBox field, boolean widgetColor) {
        field.setMaxLength(7);
        field.setEditable(true);
        field.setFilter(value -> value.matches("#?[0-9a-fA-F]{0,6}"));
        field.setResponder(value -> {
            if (widgetColor) questWidgetTextColor = value;
            else descriptionTextColor = value;
            updateColorFieldText(field, value, widgetColor ? "FFFFFF" : "CFCFCF");
        });
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
        // Back stays outside the scrolling content; reset scrolls with the rows
        int btnY = py + ph - 20;
        backButton = new BackButton(leftX - TAB_W + BACK_TAB_X_OFFSET, topY + PANEL_H - BACK_TAB_H - BACK_TAB_BOTTOM_MARGIN, this::goBack);
        resetConfigButton = new HoldResetButton(px + 2, btnY, pw - 4, 20, () -> tr(configTab == ConfigTab.ALL ? "reset.all" : "reset.current"), this::resetCurrentConfigTab);
        debugButton = new DebugButton(leftX + PANEL_W - 24, topY + PANEL_H - 36);

        addRenderableWidget(backButton);
        addRenderableWidget(resetConfigButton);
        addRenderableWidget(debugButton);
    }

    private void goBack() {
        if (page == Page.MENU) {
            Minecraft.getInstance().setScreen(parent);
        } else {
            saveConfig(false);
            setPage(Page.MENU);
        }
    }

    private void initHeader() {
        header = new CategoryHeaderWidget(leftX, topY, PANEL_W, () -> trs("header"));
        header.setPanelBounds(leftX, topY, PANEL_W);
        addRenderableWidget(header);
    }

    private void handleMenuClick(QuestData.Quest q) {
        if (q == null) return;
        if (MENU_ID_CONFIG.equals(q.id)) {
            setPage(Page.CONFIG);
        } else if (MENU_ID_EDITOR.equals(q.id) && canManageServerSettings()) {
            Minecraft.getInstance().setScreen(new QuestEditorScreen(this));
        } else if (MENU_ID_ASSETS.equals(q.id) && canManageServerSettings()) {
            Minecraft.getInstance().setScreen(new QuestAssetManagerScreen(this));
        } else if (MENU_ID_DISCORD.equals(q.id)) {
            Util.getPlatform().openUri("https://discord.gg/DARzByw6VW");
        }
    }

    private void setPage(Page next) {
        // Both pages share one widget list, so visibility does the page switching
        page = next;

        boolean menu = page == Page.MENU;
        menuList.visible = menu;
        menuList.active = menu;

        boolean config = page == Page.CONFIG;
        if (questWidgetTextColorField != null) {
            questWidgetTextColorField.visible = false;
            questWidgetTextColorField.active = false;
        }
        if (descriptionTextColorField != null) {
            descriptionTextColorField.visible = false;
            descriptionTextColorField.active = false;
        }
        if (configSearchField != null) {
            configSearchField.visible = false;
            configSearchField.active = false;
        }
        for (ColorConfigControl control : detailColorControls) {
            if (control.field == null) continue;
            control.field.visible = false;
            control.field.active = false;
        }
        for (ConfigRow row : configRows) {
            boolean show = config && (!row.serverOnly || canManageServerSettings());
            row.visible = show;
            row.active = show;
        }
        for (ConfigTabButton button : configTabButtons) {
            button.visible = config;
            button.active = config;
        }
        resetConfigButton.visible = config;
        resetConfigButton.active = config;
        debugButton.visible = menu;
        debugButton.active = menu;

        backButton.visible = true;
        backButton.active = true;
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
        // Reads from saved config before showing editor
        pinnedHudPos = normalizeHudPos(Config.pinnedQuestHudPosition());
        hideQuestBookInInventory = Config.hideQuestBookInInventory();
        questBookInventoryButtonPosition = normalizeQuestBookInventoryButtonPosition(Config.questBookInventoryButtonPosition());
        centerInventoryWithQuestPanel = Config.centerInventoryWithQuestPanel();
        hideCategoryHeader = Config.hideCategoryHeader();
        filterDisplayMode = Config.filterDisplayMode();
        disableCategories = Config.disableCategories();
        showIdField = Config.showIdField();
        hideQuestWidgetIcons = Config.hideQuestWidgetIcons();
        questTextScale = Config.questTextScale();
        questIconScale = Config.questIconScale();
        enableQuestSearchBox = Config.enableQuestSearchBox();
        customBackgrounds = Config.customBackgrounds();
        stickySubCategoryHeaders = Config.stickySubCategoryHeaders();
        stickyDescriptionHeaders = Config.stickyDescriptionHeaders();
        enableDescriptionColors = Config.enableDescriptionColors();
        questWidgetTextColor = String.format(java.util.Locale.ROOT, "%06X", Config.questWidgetTextColor());
        descriptionTextColor = String.format(java.util.Locale.ROOT, "%06X", Config.descriptionTextColor());
        if (questWidgetTextColorField != null) questWidgetTextColorField.setValue(questWidgetTextColor);
        if (descriptionTextColorField != null) descriptionTextColorField.setValue(descriptionTextColor);
        for (ColorConfigControl control : detailColorControls) {
            control.value = normalizeThemeColor(control.config.get(), control.fallback);
            if (control.field != null) control.field.setValue(control.value);
        }
        enableDescriptionReadMore = Config.enableDescriptionReadMore();
        enableDescriptionTextWrapping = Config.enableDescriptionTextWrapping();
        descriptionTextAlignment = normalizeDescriptionTextAlignment(Config.descriptionTextAlignment());
        enableQuestToasts = Config.enableQuestToasts();
        disableQuestPinning = Config.disableQuestPinning();
        autoClaimQuestRewards = Config.autoClaimQuestRewards();
        enableQuestRejecting = Config.enableQuestRejecting();
        enableQuestScrolls = Config.enableQuestScrolls();
        quickEdit = Config.quickEdit();
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
            case "buttons" -> trs("filter.buttons");
            case "hidden" -> trs("filter.hidden");
            default -> trs("filter.tabs");
        };
    }

    private void cycleQuestBookInventoryButtonPosition() {
        questBookInventoryButtonPosition = "above_offhand_slot".equals(questBookInventoryButtonPosition)
                ? "beside_recipe_book"
                : "above_offhand_slot";
    }

    private String formatQuestBookInventoryButtonPosition() {
        return "above_offhand_slot".equals(questBookInventoryButtonPosition)
                ? trs("book_position.above_offhand_slot")
                : trs("book_position.beside_recipe_book");
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

    private void initDetailColorControls() {
        detailColorControls.clear();
        addDetailColorControl("Description title color", "Quest title color in the details panel.", Config.DESCRIPTION_TITLE_COLOR, "FFFFFF");
        addDetailColorControl("Requires text color", "Requires heading color.", Config.REQUIRES_TEXT_COLOR, "FF9F0F");
        addDetailColorControl("Submit text color", "Submit and input heading color.", Config.SUBMIT_TEXT_COLOR, "1D9633");
        addDetailColorControl("Collect text color", "Collect heading color.", Config.COLLECT_TEXT_COLOR, "1D9633");
        addDetailColorControl("Kill text color", "Kill heading color.", Config.KILL_TEXT_COLOR, "FF5555");
        addDetailColorControl("Effect text color", "Effect heading color.", Config.EFFECT_TEXT_COLOR, "55FFFF");
        addDetailColorControl("Achieve text color", "Advancement heading color.", Config.ACHIEVE_TEXT_COLOR, "55FFFF");
        addDetailColorControl("Observe text color", "Observation heading color.", Config.OBSERVE_TEXT_COLOR, "B46CFF");
        addDetailColorControl("Biome text color", "Biome heading color.", Config.BIOME_TEXT_COLOR, "55CC55");
        addDetailColorControl("Dimension text color", "Dimension heading color.", Config.DIMENSION_TEXT_COLOR, "FF5555");
        addDetailColorControl("Reward text color", "Reward heading and entry color.", Config.REWARD_TEXT_COLOR, "A8FFA8");
        addDetailColorControl("Completed text color", "Completed objective progress color.", Config.COMPLETED_TEXT_COLOR, "55FF55");
        addDetailColorControl("Incomplete text color", "Incomplete and invalid objective color.", Config.INCOMPLETE_TEXT_COLOR, "FF5555");
        addDetailColorControl("Unlock text color", "Category unlock message color.", Config.UNLOCK_TEXT_COLOR, "FFD700");
    }

    private void addDetailColorControl(String label, String subtitle,
                                       ModConfigSpec.ConfigValue<String> config, String fallback) {
        ColorConfigControl control = new ColorConfigControl(config, fallback);
        control.row = new ConfigRow(px, uiDescriptionTextColorBaseY, pw, label, subtitle, () -> "", null);
        addConfigRow(control.row, ConfigTab.STYLE);
        control.field = new EditBox(font, px + pw - 45, uiDescriptionTextColorBaseY + 3, 39, 14, Component.empty());
        control.field.setMaxLength(7);
        control.field.setEditable(true);
        control.field.setFilter(value -> value.matches("#?[0-9a-fA-F]{0,6}"));
        control.field.setResponder(value -> {
            control.value = value;
            updateColorFieldText(control.field, value, control.fallback);
        });
        detailColorControls.add(control);
        addRenderableWidget(control.field);
    }

    private void cycleQuestWidgetTextColor() {
        questWidgetTextColor = nextThemeColor(questWidgetTextColor, "FFFFFF");
    }

    private String formatQuestWidgetTextColor() {
        return "#" + normalizeThemeColor(questWidgetTextColor, "FFFFFF");
    }

    private void cycleDescriptionTextColor() {
        descriptionTextColor = nextThemeColor(descriptionTextColor, "CFCFCF");
    }

    private String formatDescriptionTextColor() {
        return "#" + normalizeThemeColor(descriptionTextColor, "CFCFCF");
    }

    private void cycleDescriptionTextAlignment() {
        descriptionTextAlignment = switch (normalizeDescriptionTextAlignment(descriptionTextAlignment)) {
            case "center" -> "right";
            case "right" -> "adjust";
            case "adjust" -> "left";
            default -> "center";
        };
    }

    private String formatDescriptionTextAlignment() {
        return switch (normalizeDescriptionTextAlignment(descriptionTextAlignment)) {
            case "center" -> trs("align.center");
            case "right" -> trs("align.right");
            case "adjust" -> trs("align.adjust");
            default -> trs("align.left");
        };
    }

    private void saveConfig() {
        saveConfig(false);
    }

    private void saveConfig(boolean close) {
        // These two options would otherwise hide every inventory way to open the book.
        if (disableQuestBook && hideQuestBookInInventory) {
            hideQuestBookInInventory = false;
        }

        Config.PINNED_QUEST_HUD_POSITION.set(pinnedHudPos);
        Config.HIDE_QUEST_BOOK_IN_INVENTORY.set(hideQuestBookInInventory);
        Config.QUEST_BOOK_INVENTORY_BUTTON_POSITION.set(questBookInventoryButtonPosition);
        Config.CENTER_INVENTORY_WITH_QUEST_PANEL.set(centerInventoryWithQuestPanel);
        Config.HIDE_CATEGORY_HEADER.set(hideCategoryHeader);
        Config.FILTER_DISPLAY_MODE.set(filterDisplayMode);
        Config.SHOW_ID_FIELD.set(showIdField);
        Config.HIDE_QUEST_WIDGET_ICONS.set(hideQuestWidgetIcons);
        Config.QUEST_TEXT_SCALE.set(Math.max(0.5D, Math.min(1.0D, questTextScale)));
        Config.QUEST_ICON_SCALE.set(Math.max(0.5D, Math.min(1.0D, questIconScale)));
        Config.ENABLE_QUEST_SEARCH_BOX.set(enableQuestSearchBox);
        Config.CUSTOM_BACKGROUNDS.set(customBackgrounds);
        Config.STICKY_SUBCATEGORY_HEADERS.set(stickySubCategoryHeaders);
        Config.STICKY_DESCRIPTION_HEADERS.set(stickyDescriptionHeaders);
        Config.ENABLE_DESCRIPTION_COLORS.set(enableDescriptionColors);
        Config.QUEST_WIDGET_TEXT_COLOR.set(normalizeThemeColor(questWidgetTextColor, "FFFFFF"));
        Config.DESCRIPTION_TEXT_COLOR.set(normalizeThemeColor(descriptionTextColor, "CFCFCF"));
        for (ColorConfigControl control : detailColorControls) {
            control.config.set(normalizeThemeColor(control.value, control.fallback));
        }
        Config.ENABLE_DESCRIPTION_READ_MORE.set(enableDescriptionReadMore);
        Config.ENABLE_DESCRIPTION_TEXT_WRAPPING.set(enableDescriptionTextWrapping);
        Config.DESCRIPTION_TEXT_ALIGNMENT.set(normalizeDescriptionTextAlignment(descriptionTextAlignment));
        Config.ENABLE_QUEST_TOASTS.set(enableQuestToasts);
        if (canManageServerSettings()) {
            Config.DISABLE_CATEGORIES.set(disableCategories);
            Config.DISABLE_QUEST_PINNING.set(disableQuestPinning);
            Config.AUTO_CLAIM_QUEST_REWARDS.set(autoClaimQuestRewards);
            Config.ENABLE_QUEST_REJECTING.set(enableQuestRejecting);
            Config.ENABLE_QUEST_SCROLLS.set(enableQuestScrolls);
            Config.QUICK_EDIT.set(quickEdit);
            Config.DISABLE_QUEST_BOOK.set(disableQuestBook);
            Config.SPAWN_WITH_QUEST_BOOK.set(spawnWithQuestBook);
        }
        Config.SPEC.save();
        if (canManageServerSettings()) sendConfigToServer();
        QuestPanelClient.applyConfigChanges();
        if (close) {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private void sendConfigToServer() {
        // local save updates the client immediately, keeps server in sync
        try {
            PacketDistributor.sendToServer(new BoundlessNetwork.UpdateServerConfig(
                    pinnedHudPos,
                    hideQuestBookInInventory,
                    questBookInventoryButtonPosition,
                    centerInventoryWithQuestPanel,
                    hideCategoryHeader,
                    filterDisplayMode,
                    disableCategories,
                    hideQuestWidgetIcons,
                    questTextScale,
                    questIconScale,
                    enableQuestSearchBox,
                    customBackgrounds,
                    enableDescriptionColors,
                    normalizeThemeColor(questWidgetTextColor, "FFFFFF"),
                    normalizeThemeColor(descriptionTextColor, "CFCFCF"),
                    enableDescriptionReadMore,
                    enableDescriptionTextWrapping,
                    normalizeDescriptionTextAlignment(descriptionTextAlignment),
                    enableQuestToasts,
                    disableQuestPinning,
                    autoClaimQuestRewards,
                    enableQuestRejecting,
                    enableQuestScrolls,
                    quickEdit,
                    disableQuestBook,
                    spawnWithQuestBook
            ));
        } catch (Throwable ignored) {
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
            if (canManageServerSettings()) disableCategories = false;
            showIdField = false;
            enableQuestSearchBox = false;
        }
        if (tab == ConfigTab.ALL || tab == ConfigTab.STYLE) {
            hideQuestWidgetIcons = false;
            questTextScale = 1.0D;
            questIconScale = 1.0D;
            enableDescriptionColors = true;
            questWidgetTextColor = "FFFFFF";
            descriptionTextColor = "CFCFCF";
            for (ColorConfigControl control : detailColorControls) control.value = control.fallback;
            enableDescriptionReadMore = true;
            enableDescriptionTextWrapping = true;
            descriptionTextAlignment = "left";
            hideCategoryHeader = false;
            stickySubCategoryHeaders = true;
            stickyDescriptionHeaders = true;
            customBackgrounds = true;
        }
        if (tab == ConfigTab.ALL || tab == ConfigTab.FEATURES) {
            enableQuestToasts = true;
            if (canManageServerSettings()) {
                disableQuestPinning = false;
                autoClaimQuestRewards = false;
                enableQuestRejecting = true;
                enableQuestScrolls = true;
                quickEdit = false;
                disableQuestBook = false;
                spawnWithQuestBook = false;
            }
        }
    }

    private List<QuestData.Quest> buildMenuQuests() {
        // Menu items use lightweight quest records so they render like the rest of the UI.
        List<QuestData.Quest> out = new ArrayList<>();
        out.add(buildMenuQuest(MENU_ID_CONFIG, trs("menu.config"), MENU_CONFIG_TEX.toString()));
        if (canManageServerSettings()) {
            out.add(buildMenuQuest(MENU_ID_EDITOR, trs("menu.editor"), MENU_EDITOR_TEX.toString()));
            out.add(buildMenuQuest(MENU_ID_ASSETS, trs("menu.assets"), "minecraft:painting"));
        }
        out.add(buildMenuQuest("settings_spacer_1", "", "minecraft:air"));
        out.add(buildMenuQuest("settings_spacer_2", "", "minecraft:air"));
        out.add(buildMenuQuest(MENU_ID_DISCORD, trs("menu.discord"), MENU_DISCORD_TEX.toString()));
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
                new QuestData.Rewards(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "", 0),
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

    private String normalizeDescriptionTextAlignment(String raw) {
        if (raw == null) return "left";
        String lower = raw.trim().toLowerCase();
        return ("left".equals(lower) || "center".equals(lower) || "right".equals(lower) || "adjust".equals(lower))
                ? lower
                : "left";
    }

    private String normalizeThemeColor(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        value = value.toUpperCase(java.util.Locale.ROOT);
        return value.matches("[0-9A-F]{6}") ? value : fallback;
    }

    private void updateColorFieldText(EditBox field, String raw, String fallback) {
        if (field == null) return;
        field.setTextColor(Integer.parseInt(normalizeThemeColor(raw, fallback), 16));
    }

    private String nextThemeColor(String current, String fallback) {
        List<String> palette = List.of("FFFFFF", "CFCFCF", "FFD166", "55CCFF", "A8FFA8", "FFAA55");
        String normalized = normalizeThemeColor(current, fallback);
        int index = palette.indexOf(normalized);
        return palette.get(index < 0 ? 0 : (index + 1) % palette.size());
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
            // Render rows inside the panel only. Tabs and navigation stay above the scissor.
            boolean backVisible = backButton != null && backButton.visible;
            boolean resetVisible = resetConfigButton != null && resetConfigButton.visible;
            boolean debugVisible = debugButton != null && debugButton.visible;
            List<Boolean> tabVisible = new ArrayList<>();
            for (ConfigTabButton button : configTabButtons) {
                tabVisible.add(button.visible);
                button.visible = false;
            }
            if (backButton != null) backButton.visible = false;
            if (resetConfigButton != null) resetConfigButton.visible = false;
            if (debugButton != null) debugButton.visible = false;

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
            if (debugButton != null) debugButton.visible = debugVisible;
            if (backButton != null && backButton.visible) backButton.render(gg, mouseX, mouseY, partialTick);
            if (resetConfigButton != null && resetConfigButton.visible) resetConfigButton.render(gg, mouseX, mouseY, partialTick);
            if (debugButton != null && debugButton.visible) debugButton.render(gg, mouseX, mouseY, partialTick);

            renderConfigScrollbar(gg);
            renderPendingTooltipOnTop(gg);
            return;
        }

        super.render(gg, mouseX, mouseY, partialTick);
        renderVersionFooter(gg);
        renderPendingTooltipOnTop(gg);
    }

    private void renderVersionFooter(GuiGraphics graphics) {
        String label = versionLabel();
        int availableWidth = PANEL_W - 22;
        float scale = Math.min(1f, availableWidth / (float) Math.max(1, font.width(label)));
        int scaledWidth = Math.round(font.width(label) * scale);
        graphics.pose().pushPose();
        graphics.pose().translate(leftX + (PANEL_W - scaledWidth) / 2f,
                topY + PANEL_H - font.lineHeight * scale - 13, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, label, 0, 0, 0xFFAAAAAA, false);
        graphics.pose().popPose();
    }

    private String versionLabel() {
        String modVersion = ModList.get().getModContainerById("boundless")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown");
        String loaderVersion = ModList.get().getModContainerById("neoforge")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown");
        return "Boundless " + modVersion + " | NeoForge " + loaderVersion;
    }

    private void queueTooltip(List<Component> tooltip, int mouseX, int mouseY) {
        // Widgets collect tooltips while rendering; the screen draws one after everything else.
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
        String text = Component.translatable(configTab.title).getString();
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
        // Leave room for the reset button after the last row.
        return Math.max(0, visibleConfigRows().size() * 21 + 27 + (configTab == ConfigTab.ALL ? 20 : 0));
    }

    private int maxConfigScroll() {
        return Math.max(0, configContentHeight() - configViewportHeight());
    }

    private int scrolledY(int baseY) {
        return baseY - Math.round(configScrollY);
    }

    private void applyConfigScrollLayout() {
        // Only rows in the chosen tab join the vertical flow.
        int max = maxConfigScroll();
        if (configScrollY < 0f) configScrollY = 0f;
        if (configScrollY > max) configScrollY = max;
        layoutBackButton();

        int top = configViewportTop();
        int bottom = configViewportBottom();
        int baseY = top + 2 + (configTab == ConfigTab.ALL ? 20 : 0);
        int rowGap = 21;
        List<ConfigRow> visibleRows = visibleConfigRows();
        for (ConfigRow row : configRows) {
            row.visible = false;
            row.active = false;
        }
        for (int i = 0; i < visibleRows.size(); i++) {
            layoutRow(visibleRows.get(i), baseY + i * rowGap, top, bottom);
        }
        layoutColorField(questWidgetTextColorField, uiQuestWidgetTextColorRow, top, bottom);
        layoutColorField(descriptionTextColorField, uiDescriptionTextColorRow, top, bottom);
        for (ColorConfigControl control : detailColorControls) {
            layoutColorField(control.field, control.row, top, bottom);
        }
        if (configSearchField != null) {
            configSearchField.setPosition(px + 2, top + 2);
            configSearchField.setWidth(pw - 4);
            configSearchField.visible = page == Page.CONFIG && configTab == ConfigTab.ALL;
            configSearchField.active = configSearchField.visible;
        }
        if (resetConfigButton != null) {
            resetConfigButton.setX(px + 2);
            resetConfigButton.setWidth(pw - 4);
            resetConfigButton.setY(scrolledY(baseY + visibleRows.size() * rowGap + 2));
            boolean inView = resetConfigButton.getY() + resetConfigButton.getHeight() > top && resetConfigButton.getY() < bottom;
            resetConfigButton.visible = page == Page.CONFIG && inView;
            resetConfigButton.active = page == Page.CONFIG && inView;
        }
        layoutConfigTabButtons();
    }

    private List<ConfigRow> visibleConfigRows() {
        List<ConfigRow> out = new ArrayList<>();
        for (ConfigRow row : configRows) {
            if (row.serverOnly && !canManageServerSettings()) continue;
            if (configTab != ConfigTab.ALL && row.group != configTab) continue;
            if (configTab == ConfigTab.ALL && !configSearchQuery.isBlank() && !row.matches(configSearchQuery)) continue;
            out.add(row);
        }
        return out;
    }

    private void layoutConfigTabButtons() {
        int x = leftX - TAB_W + SIDE_TAB_X_OFFSET;
        int startY = py + 6;
        for (int i = 0; i < configTabButtons.size(); i++) {
            ConfigTabButton button = configTabButtons.get(i);
            button.setX(x);
            button.setY(startY + i * (TAB_H + TAB_GAP));
            button.visible = page == Page.CONFIG;
            button.active = page == Page.CONFIG;
        }
    }

    private void layoutBackButton() {
        if (backButton == null) return;
        backButton.setX(leftX - TAB_W + BACK_TAB_X_OFFSET);
        backButton.setY(topY + PANEL_H - BACK_TAB_H - BACK_TAB_BOTTOM_MARGIN);
    }

    private void layoutRow(ConfigRow row, int baseY, int top, int bottom) {
        if (row == null) return;
        int y = scrolledY(baseY);
        row.setY(y);
        boolean inView = (y + row.getHeight()) > top && y < bottom;
        row.visible = page == Page.CONFIG && inView;
        row.active = row.visible;
    }

    private void layoutColorField(EditBox field, ConfigRow row, int top, int bottom) {
        if (field == null || row == null) return;
        field.setY(row.getY() + 3);
        boolean visible = row.visible && row.getY() + row.getHeight() > top && row.getY() < bottom;
        field.visible = visible;
        field.active = visible;
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
        // Keep wheel input in the panel so background widgets do not consume it.
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
            if (questWidgetTextColorField != null && questWidgetTextColorField.visible
                    && questWidgetTextColorField.isMouseOver(mouseX, mouseY)) {
                questWidgetTextColorField.setFocused(true);
                setFocused(questWidgetTextColorField);
                return true;
            }
            if (descriptionTextColorField != null && descriptionTextColorField.visible
                    && descriptionTextColorField.isMouseOver(mouseX, mouseY)) {
                descriptionTextColorField.setFocused(true);
                setFocused(descriptionTextColorField);
                return true;
            }
            for (ColorConfigControl control : detailColorControls) {
                if (control.field != null && control.field.visible && control.field.isMouseOver(mouseX, mouseY)) {
                    control.field.setFocused(true);
                    setFocused(control.field);
                    return true;
                }
            }
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
        // Centre the thumb under the pointer while dragging.
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
        if (page == Page.CONFIG) saveConfig(false);
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
        ALL("ui.boundless.settings.tab.all"),
        STYLE("ui.boundless.settings.tab.style"),
        UI("ui.boundless.settings.tab.ui"),
        FEATURES("ui.boundless.settings.tab.features");

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
            // Changing tabs always starts at the first row.
            if (configTab == tab) return;
            configTab = tab;
            configScrollY = 0f;
            applyConfigScrollLayout();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean selected = configTab == tab;
            int renderX = getX() + 1 - (selected ? 1 : 0);
            gg.blit(selected ? TAB_SELECTED_TEX : TAB_TEX, renderX, getY(), 0, 0, TAB_W, TAB_H, TAB_W, TAB_H);
            gg.blit(icon, renderX + (TAB_W - 16) / 2, getY() + (TAB_H - 16) / 2, 0, 0, 16, 16, 16, 16);
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
        private boolean serverOnly;

        private boolean matches(String query) {
            if (query == null || query.isBlank()) return true;
            String searchable = (label + " " + subtitle).toLowerCase(java.util.Locale.ROOT);
            return searchable.contains(query);
        }

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
            // Most rows save straight away; text fields save through their responder.
            if (onPress != null) {
                onPress.run();
                saveConfig(false);
                applyConfigScrollLayout();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            if (hovered) RenderSystem.setShaderColor(1.1f, 1.1f, 1.1f, 1.0f);
            gg.blit(ROW_TEX, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            if (hovered) RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            var font = Minecraft.getInstance().font;
            String valueText = value == null ? "" : value.get();
            if (valueText == null) valueText = "";

            int labelX = getX() + 7;
            float valueScale = 0.72f;
            int valueW = (int) (font.width(valueText) * valueScale);
            int valueX = getX() + this.width - valueW - 5;

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
            // Releasing or moving away cancels the hold before a reset can fire.
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
            int textX = getX() + (this.width - textW) / 2 - 5;
            int textY = getY() + (this.height - Math.round(font.lineHeight * scale)) / 2;
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(font, text, (int) (textX * inv), (int) (textY * inv), this.active ? 0xFFFF3030 : 0xFF803030, false);
            gg.pose().popPose();

            if (hovered) {
                gg.renderComponentTooltip(font, List.of(tr("tooltip.hold_reset")), mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class DebugButton extends AbstractButton {
        private DebugButton(int x, int y) {
            super(x, y, 10, 13, Component.empty());
        }

        @Override
        public void onPress() {
            Config.DEV_MODE.set(!Config.devMode());
            Config.SPEC.save();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture = Config.devMode() ? UNDEBUG_TEX : DEBUG_TEX;
            graphics.blit(texture, getX(), getY(), 0, 0, 10, 13, 10, 13);
            if (isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.literal(Config.devMode()
                                ? "Disable debug diagnostics"
                                : "Enable quest, UI, and network diagnostics"), mouseX, mouseY - 12);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    private static final class ColorConfigControl {
        private final ModConfigSpec.ConfigValue<String> config;
        private final String fallback;
        private String value;
        private ConfigRow row;
        private EditBox field;

        private ColorConfigControl(ModConfigSpec.ConfigValue<String> config, String fallback) {
            this.config = config;
            this.fallback = fallback;
            this.value = fallback;
        }
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
            int textX = getX() + (this.width - textW) / 2 - 3;
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
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("ui.boundless.settings." + key, args);
    }

    private static String trs(String key, Object... args) {
        return tr(key, args).getString();
    }
}
