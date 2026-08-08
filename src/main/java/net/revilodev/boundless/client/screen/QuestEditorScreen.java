
package net.revilodev.boundless.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.client.editor.ScaledMultiLineEditBox;
import net.revilodev.boundless.client.editor.QuestEditorControls.ActionButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.BackButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.IconButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.EditorListWidget;
import net.revilodev.boundless.client.editor.QuestEditorControls.LockToggleButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.TextInsertButton;
import net.revilodev.boundless.client.editor.QuestEditorControls.ToggleButton;
import net.revilodev.boundless.client.editor.QuestEditorEntryCodec;
import net.revilodev.boundless.client.editor.QuestEditorEntryRows;
import net.revilodev.boundless.client.editor.QuestEditorEntryRows.RowMaps;
import net.revilodev.boundless.client.editor.QuestEditorEntryTypes;
import net.revilodev.boundless.client.editor.QuestEditorJsonFiles;
import net.revilodev.boundless.client.editor.QuestEditorItemIcons;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerData;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerData.PickerPageRequest;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerData.PickerPageResult;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerRenderer;
import net.revilodev.boundless.client.editor.QuestEditorItemPickerRenderer.RenderRequest;
import net.revilodev.boundless.client.editor.QuestEditorJsonBuilder;
import net.revilodev.boundless.client.editor.QuestEditorJsonBuilder.QuestBuildFields;
import net.revilodev.boundless.client.editor.QuestEditorPackFiles;
import net.revilodev.boundless.client.editor.QuestEditorNaming;
import net.revilodev.boundless.client.editor.QuestEditorPopupMenus;
import net.revilodev.boundless.client.editor.QuestEditorQuestOrdering;
import net.revilodev.boundless.client.editor.QuestEditorQuestOrdering.QuestOrderResult;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EditorTabButton;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryCountBox;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryItemPickerButton;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryRemoveButton;
import net.revilodev.boundless.client.editor.QuestEditorRowControls.EntryTypeButton;
import net.revilodev.boundless.client.editor.QuestEditorSuggestionCaches;
import net.revilodev.boundless.client.editor.QuestEditorStateSignatures;
import net.revilodev.boundless.client.editor.QuestEditorStateSignatures.SignatureFields;
import net.revilodev.boundless.client.editor.QuestEditorSuggestionRenderer;
import net.revilodev.boundless.client.editor.QuestEditorValidation;
import net.revilodev.boundless.client.editor.QuestEditorValidation.ValidationRequest;
import net.revilodev.boundless.client.editor.QuestEditorModels.CategoryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.CommandReward;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorEntryKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.FormField;
import net.revilodev.boundless.client.editor.QuestEditorModels.MultiLineEntryContext;
import net.revilodev.boundless.client.editor.QuestEditorModels.Mode;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorType;
import net.revilodev.boundless.client.editor.QuestEditorModels.ScreenState;
import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;
import net.revilodev.boundless.client.editor.QuestEditorModels.DropdownMenuTarget;
import net.revilodev.boundless.client.editor.QuestEditorModels.ItemPickerTab;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerMode;
import net.revilodev.boundless.client.editor.QuestEditorModels.ValidationSnapshot;
import net.revilodev.boundless.client.editor.QuestEditorModels.MoveDirection;
import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.PackMeta;
import net.revilodev.boundless.client.editor.QuestEditorModels.ParsedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerPageData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListIndex;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListItem;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;
import net.revilodev.boundless.client.editor.QuestEditorModels.SubCategoryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.SuggestionBounds;
import net.revilodev.boundless.compat.LevelUpCompat;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestItemSpec;
import net.revilodev.boundless.quest.QuestPackStorage;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class QuestEditorScreen extends Screen {
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");
    private static final ResourceLocation DUPLICATE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/duplicate_button.png");
    private static final ResourceLocation DUPLICATE_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/duplicate_button-hovered.png");
    private static final ResourceLocation DELETE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter.png");
    private static final ResourceLocation DELETE_CONFIRM_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button.png");
    private static final ResourceLocation DELETE_CONFIRM_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button-hovered.png");
    private static final ResourceLocation HEADER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/3-slice-header.png");
    private static final ResourceLocation UNSAVED_POPUP_BG_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/20x20-9-slice.png");
    private static final ResourceLocation UNSAVED_EXCLAMATION_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/icon/exclamation.png");
    private static final ResourceLocation QUEST_TAB_SCROLL_ICON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/scroll-icon.png");
    private static final ResourceLocation BUILTIN_PACK_ENABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_confirmation.png");
    private static final ResourceLocation BUILTIN_PACK_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_reject.png");

    // layout and spacing
    private static final int TOGGLE_SIZE = 20;
    private static final int SMALL_BTN_SIZE = 20;
    private static final int SMALL_BTN_GAP = 4;
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int TAB_GAP = 3;
    private static final int BACK_TAB_H = 17;
    private static final int SIDE_TAB_X_OFFSET = 4;
    private static final int BACK_TAB_X_OFFSET = SIDE_TAB_X_OFFSET + 6;
    private static final int BACK_TAB_BOTTOM_MARGIN = 6;
    private static final int HEADER_TEX_W = 72;
    private static final int HEADER_TEX_H = 10;
    private static final int HEADER_SLICE = 3;
    private static final int UNSAVED_POPUP_TEX_SIZE = 20;
    private static final int UNSAVED_POPUP_SLICE = 5;
    private static final int UNSAVED_EXCLAMATION_W = 4;
    private static final int UNSAVED_EXCLAMATION_H = 11;
    private static final int UNSAVED_EXCLAMATION_GAP = 4;
    private static final int INLINE_FLAG_LABEL_H = 8;

    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int BOTTOM_BAR_H = 24;
    private static final int FIELD_LABEL_GAP = 2;
    private static final int FIELD_ROW_GAP = 6;
    private static final int BOX_H = 20;
    private static final int BOX_H_TALL = 30;
    private static final float INPUT_TEXT_SCALE = 0.5f;
    private static final int FORMAT_BAR_GAP = 3;
    private static final int FORMAT_BAR_H = 9;
    private static final int FORMAT_BTN_GAP = 1;
    private static final int DEP_LOCK_SIZE = 13;
    private static final int DEP_LOCK_GAP = 2;
    private static final int DEP_ENTRY_ICON_SPACE = 12;
    private static final float DEP_ENTRY_ICON_SCALE = 0.625f;

    private static final ResourceLocation IMPORT_PACK_BUTTON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/import-button.png");
    private static final ResourceLocation IMPORT_PACK_BUTTON_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/import-button-hovered.png");
    private static final ResourceLocation PLUS_BUTTON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/plus_button.png");
    private static final ResourceLocation PLUS_BUTTON_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/plus_button-hovered.png");
    private static final ResourceLocation PACK_ACTION_NEEDED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/quest_widget-action-needed.png");
    private static final ResourceLocation PACK_CHANGED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_completed.png");
    private static final int DEFAULT_INPUT_TEXT_COLOR = 0xE0E0E0;
    private static final int DROPDOWN_INPUT_TEXT_COLOR = 0xFFFFFF;
    private static final int INVALID_INPUT_TEXT_COLOR = 0xFF4040;
    private static final String INVALID_ID_TOOLTIP = "Invalid ID";

    // id suggestion popup layout
    private static final int ID_SUGGESTION_MAX = 5000;
    private static final int ID_SUGGESTION_VISIBLE_ROWS = 6;
    private static final int ID_SUGGESTION_ROW_H = 8;
    private static final long DELETE_HOLD_MS = 2000L;
    private static final int ENTRY_ROW_H = 14;
    private static final int ENTRY_ROW_GAP = 2;
    private static final float ENTRY_INPUT_TEXT_SCALE = 0.5f;
    private static final int ENTRY_REMOVE_BTN_W = 10;
    private static final int ENTRY_TYPE_BTN_W = 21;
    private static final int ENTRY_ITEM_PICK_BTN_W = 12;
    private static final int ENTRY_COUNT_BTN_W = 12;
    private static final float ENTRY_COUNT_TEXT_SCALE = 0.55f;

    // shared picker layout
    private static final int ITEM_PICKER_COLS = 9;
    private static final int ITEM_PICKER_ROWS = 4;
    private static final int ITEM_PICKER_CELL = 18;
    private static final int ITEM_PICKER_W = 172;
    private static final int ITEM_PICKER_H = 112;
    private static final int ITEM_PICKER_GRID_X = 5;
    private static final int ITEM_PICKER_GRID_Y = 19;
    private static final int ITEM_PICKER_GRID_RIGHT = 163;
    private static final int ITEM_PICKER_GRID_BOTTOM = 95;
    private static final int ITEM_PICKER_SEARCH_W = 146;
    private static final int ITEM_PICKER_SEARCH_H = 12;
    private static final int ITEM_PICKER_CLOSE_SIZE = 13;
    private static final float ENTRY_TYPE_TEXT_SCALE = 0.45f;

    private static final String ENTRY_CREATE_PACK = "__create_pack__";
    private static final String ENTRY_BUILTIN_PACK = "__builtin_pack__";
    private static final String ENTRY_NEW = "__new__";
    private static final String ENTRY_CATEGORIES = "__categories__";
    private static final String ENTRY_SUBCATEGORIES = "__subcategories__";
    private static final String ENTRY_QUESTS = "__quests__";
    private static final long PENDING_INIT_TTL_MS = 15000L;
    private static ScreenState pendingInitState;
    private static long pendingInitUntil = 0L;

    private final Screen parent;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final QuestEditorSuggestionCaches suggestionCaches = new QuestEditorSuggestionCaches();

    private int leftX;
    private int rightX;
    private int topY;
    private int pxLeft;
    private int pxRight;
    private int py;
    private int pw;
    private int ph;
    private int listH;
    private BackButton backButton;
    private EditBox questSearchBox;
    private Button createPackButton;
    private IconButton createEntryButton;
    private IconButton importQuestPackButton;
    private EditorTabButton categoriesTabButton;
    private EditorTabButton subCategoriesTabButton;
    private EditorTabButton questsTabButton;

    // active editor mode
    private Mode mode = Mode.PACK_LIST;
    private EditorType editorType = EditorType.NONE;
    private QuestPack currentPack;
    private String selectedEntryId = "";

    // main editor buttons
    private EditorListWidget leftList;
    private ActionButton saveButton;
    private Button exportPackButton;
    private IconButton duplicateButton;
    private IconButton deleteQuestButton;

    // editor runtime state
    private float editorScroll = 0f;
    private String statusMessage = "";
    private int statusColor = 0xA0A0A0;
    private boolean deleteConfirmArmed = false;
    private boolean deleteHoldActive = false;
    private long deleteHoldStartMs = 0L;
    private ScreenState pendingState;
    private final List<FormField> activeFields = new ArrayList<>();
    private final List<FormField> allFields = new ArrayList<>();

    // pack fields
    private EditBox packNameBox;
    private EditBox packNamespaceBox;
    private EditBox packIconPathBox;
    private ScaledMultiLineEditBox packDescriptionBox;

    // category fields
    private EditBox catIdBox;
    private EditBox catNameBox;
    private EditBox catIconBox;
    private EditBox catDependencyBox;
    private ToggleButton catAutoCompleteToggle;

    // sub category fields
    private EditBox subIdBox;
    private EditBox subCategoryBox;
    private EditBox subNameBox;
    private EditBox subIconBox;
    private ToggleButton subDefaultOpenToggle;

    // quest fields
    private EditBox questIdBox;
    private EditBox questNameBox;
    private EditBox questIconBox;
    private ScaledMultiLineEditBox questDescriptionBox;
    private EditBox questCategoryBox;
    private EditBox questSubCategoryBox;
    private ScaledMultiLineEditBox questDependenciesBox;
    private LockToggleButton questDependencyLockToggle;
    private ToggleButton questOptionalToggle;
    private ToggleButton questRepeatableToggle;
    private ToggleButton questAutoCompleteToggle;
    private ToggleButton questHiddenUnderDependencyToggle;
    private ScaledMultiLineEditBox questCompletionBox;
    private ScaledMultiLineEditBox questRewardBox;
    // entry row widgets
    private final List<ScaledMultiLineEditBox> dependencyEntryBoxes = new ArrayList<>();
    private final List<ScaledMultiLineEditBox> completionEntryBoxes = new ArrayList<>();
    private final List<ScaledMultiLineEditBox> rewardEntryBoxes = new ArrayList<>();
    private final List<EntryRemoveButton> dependencyEntryRemoveButtons = new ArrayList<>();
    private final List<LockToggleButton> dependencyEntryLockButtons = new ArrayList<>();
    private final List<EntryRemoveButton> completionEntryRemoveButtons = new ArrayList<>();
    private final List<EntryRemoveButton> rewardEntryRemoveButtons = new ArrayList<>();
    private final List<EntryTypeButton> completionEntryTypeButtons = new ArrayList<>();
    private final List<EntryTypeButton> rewardEntryTypeButtons = new ArrayList<>();
    private final List<EntryItemPickerButton> completionEntryItemPickerButtons = new ArrayList<>();
    private final List<EntryItemPickerButton> rewardEntryItemPickerButtons = new ArrayList<>();
    private final List<EntryCountBox> completionEntryCountBoxes = new ArrayList<>();
    private final List<EntryCountBox> rewardEntryCountBoxes = new ArrayList<>();
    // entry row state
    private final Map<ScaledMultiLineEditBox, String> entryTypeByBox = new HashMap<>();
    private final Map<ScaledMultiLineEditBox, String> selectedItemIdByBox = new HashMap<>();
    private final Map<ScaledMultiLineEditBox, List<String>> selectedItemIdsByBox = new HashMap<>();
    private final Map<ScaledMultiLineEditBox, String> selectedItemComponentsByBox = new HashMap<>();
    private final Map<ScaledMultiLineEditBox, Integer> entryCountByBox = new HashMap<>();
    private static final String LEGACY_PACK_TOOLTIP = "Incompatible pack";
    private boolean syncingEntryRows = false;
    private boolean entryRowsDirty = false;
    private final List<TextInsertButton> descriptionFormatButtons = new ArrayList<>();
    private String questOrderToken = "";

    // loaded file state
    private Path editingPath;
    private String loadedQuestType = "";
    // editor id caches
    private final Set<String> categoryIdCache = new HashSet<>();
    private final Set<String> subCategoryIdCache = new HashSet<>();
    private final Set<String> questIdCache = new HashSet<>();
    private final Map<String, String> questIconByIdCache = new HashMap<>();
    private final List<String> categorySuggestionCache = new ArrayList<>();
    private final List<String> subCategorySuggestionCache = new ArrayList<>();
    private final List<String> questSuggestionCache = new ArrayList<>();
    private final List<String> questPackDependencySuggestionCache = new ArrayList<>();
    private final Map<String, List<String>> subCategoryByCategorySuggestion = new HashMap<>();
    // staged pack changes
    private final Map<String, QuestPack> stagedPacks = new LinkedHashMap<>();
    private final Set<String> stagedDeletedPackNames = new LinkedHashSet<>();
    private final Set<String> collapsedQuestCategories = new HashSet<>();
    private final Set<String> collapsedQuestSubCategories = new HashSet<>();
    // quest list cache
    private QuestListIndex questListIndexCache;
    private Path questListIndexPackRoot;
    // id suggestion state
    private final List<String> activeIdSuggestions = new ArrayList<>();
    private EditBox idSuggestionField;
    private ScaledMultiLineEditBox idSuggestionMultiLineField;
    private int idSuggestionScroll = 0;
    private Object lastIdSuggestionSource;
    private String lastIdSuggestionPrefix = "";
    private boolean idSuggestionsDirty = true;
    private boolean suppressIdSuggestions = false;
    private boolean suppressIdSanitizer = false;
    private boolean closingEditor = false;
    // unsaved change state
    private String savedEditorState;
    private String pendingDiscardEntryId = "";
    private Mode pendingDiscardMode;
    private EditorEntry pendingDiscardEntry;
    private String questSearchQuery = "";
    // popup menu state
    private final QuestEditorPopupMenus popupMenus = new QuestEditorPopupMenus();
    // item picker state
    private EntryRowKind itemPickerKind;
    private int itemPickerRow = -1;
    private EditBox itemPickerIconTarget;
    private ItemPickerTab itemPickerTab = ItemPickerTab.CREATIVE;
    private ValidationSnapshot validationSnapshot;
    private final QuestEditorItemPickerData itemPickerData = new QuestEditorItemPickerData();
    private PickerMode pickerMode = PickerMode.ITEMS;
    private int itemPickerPage = 0;
    private EditBox itemPickerSearchBox;
    private boolean itemPickerMultiSelect = false;
    private final LinkedHashSet<String> itemPickerPendingSelection = new LinkedHashSet<>();
    private final LinkedHashSet<String> itemPickerOriginalSelection = new LinkedHashSet<>();
    private String itemPickerSearchQuery = "";

    // pending tooltip state
    private List<Component> pendingEditorTooltip = List.of();
    private int pendingEditorTooltipX;
    private int pendingEditorTooltipY;

    // create the editor screen
    public QuestEditorScreen(Screen parent) {
        super(tr("title"));
        this.parent = parent;
    }

    @Override
    // build the editor layout
    protected void init() {
        closeTransientMenus();
        leftX = (width / 2) - PANEL_W - 2;
        rightX = (width / 2) + 2;
        topY = (height / 2) - PANEL_H / 2;
        pxLeft = leftX + 10;
        pxRight = rightX + 10;
        py = topY + 10;
        pw = 127;
        int interiorH = PANEL_H - 20;
        listH = interiorH - BOTTOM_BAR_H;
        ph = listH;

        leftList = new EditorListWidget(pxLeft, py, pw, listH, this::handleLeftClick, this::handleLeftAction, this::handleLeftSecondaryClick, this::handleEntryMoveAction, QuestEditorItemIcons::iconStackFromId);
        addRenderableWidget(leftList);

        initFormFields();

        int barY = py + ph + (BOTTOM_BAR_H - 20) / 2;
        int saveX = pxRight + pw - 68;
        saveButton = new ActionButton(saveX, barY, 68, 20,
                tr("save"), this::saveCurrent);
        addRenderableWidget(saveButton);
        exportPackButton = Button.builder(tr("export"), button -> exportCurrentPack())
                .bounds(saveX - 70, barY, 68, 20)
                .build();
        exportPackButton.visible = false;
        exportPackButton.active = false;
        addRenderableWidget(exportPackButton);

        int deleteQuestX = saveX - SMALL_BTN_GAP - SMALL_BTN_SIZE;
        int duplicateX = deleteQuestX - SMALL_BTN_GAP - SMALL_BTN_SIZE;
        duplicateButton = new IconButton(duplicateX, barY, SMALL_BTN_SIZE, DUPLICATE_TEX, DUPLICATE_TEX_HOVER, this::duplicateCurrent);
        deleteQuestButton = new IconButton(deleteQuestX, barY, SMALL_BTN_SIZE, DELETE_TEX, DELETE_CONFIRM_TEX_HOVER, this::handleDeleteButtonPress);
        addRenderableWidget(duplicateButton);
        addRenderableWidget(deleteQuestButton);

        backButton = new BackButton(leftX - TAB_W + BACK_TAB_X_OFFSET, topY + PANEL_H - BACK_TAB_H - BACK_TAB_BOTTOM_MARGIN, this::goBack);
        addRenderableWidget(backButton);
        createPackButton = Button.builder(tr("create_new"), button -> openPackCreate())
                .bounds(pxLeft + 2, barY, pw - SMALL_BTN_SIZE - SMALL_BTN_GAP - 4, 20)
                .build();
        addRenderableWidget(createPackButton);
        createEntryButton = new IconButton(pxLeft + 2, barY, SMALL_BTN_SIZE, PLUS_BUTTON_TEX, PLUS_BUTTON_HOVER_TEX, this::requestCreateEntryForCurrentMode);
        createEntryButton.visible = false;
        createEntryButton.active = false;
        addRenderableWidget(createEntryButton);
        importQuestPackButton = new IconButton(createPackButton.getX() + createPackButton.getWidth() + 2, barY, SMALL_BTN_SIZE, IMPORT_PACK_BUTTON_TEX, IMPORT_PACK_BUTTON_TEX_HOVER, this::openImportQuestPackDirectory);
        addRenderableWidget(importQuestPackButton);
        categoriesTabButton = new EditorTabButton(trs("categories"), "boundless:quest_book", Mode.CATEGORY_LIST, () -> currentPack != null, () -> mode, this::setMode);
        subCategoriesTabButton = new EditorTabButton(trs("subcategories"), "minecraft:book", Mode.SUBCATEGORY_LIST, () -> currentPack != null, () -> mode, this::setMode);
        questsTabButton = new EditorTabButton(trs("quests"), QUEST_TAB_SCROLL_ICON_TEX, Mode.QUEST_LIST, () -> currentPack != null, () -> mode, this::setMode);
        questSearchBox = new EditBox(font, pxLeft + 2, barY, pw - 4, 20, tr("search_quests"));
        questSearchBox.setMaxLength(128);
        questSearchBox.setHint(tr("search_quests"));
        questSearchBox.setValue(questSearchQuery);
        questSearchBox.setResponder(value -> {
            questSearchQuery = safe(value);
            if (mode == Mode.QUEST_LIST) refreshLeftList();
        });
        questSearchBox.visible = false;
        questSearchBox.active = false;
        addRenderableWidget(questSearchBox);

        if (pendingState != null) {
            ScreenState state = pendingState;
            pendingState = null;
            restoreState(state);
            return;
        }
        ScreenState initState = takePendingInitState();
        if (initState != null) {
            restoreState(initState);
            return;
        }
        setMode(Mode.PACK_LIST);
    }

    // create all reusable form widgets
    private void initFormFields() {
        packNameBox = createBox("Pack name", BOX_H);
        packNamespaceBox = createBox("Namespace", BOX_H);
        packIconPathBox = createBox("Pack icon path", BOX_H);
        packIconPathBox.setTextColor(0x00000000);
        packIconPathBox.setTextColorUneditable(0x00000000);
        packDescriptionBox = createMultiLineBox("Pack description", BOX_H_TALL, false);

        catIdBox = createBox("Category id", BOX_H);
        catNameBox = createBox("Category name", BOX_H);
        catNameBox.setMaxLength(22);
        catIconBox = createBox("Category icon", BOX_H);
        catIconBox.setTextColor(0x00000000);
        catIconBox.setTextColorUneditable(0x00000000);
        catDependencyBox = createBox("Unlock quest", BOX_H);
        catAutoCompleteToggle = createToggle(false);

        subIdBox = createBox("Sub-category id", BOX_H);
        subCategoryBox = createBox("Parent category id", BOX_H);
        subNameBox = createBox("Sub-category name", BOX_H);
        subNameBox.setMaxLength(26);
        subIconBox = createBox("Sub-category icon", BOX_H);
        subIconBox.setTextColor(0x00000000);
        subIconBox.setTextColorUneditable(0x00000000);
        subDefaultOpenToggle = createToggle(false);

        questIdBox = createBox("Quest id", BOX_H);
        questNameBox = createBox("Quest name", BOX_H);
        questIconBox = createBox("Quest icon", BOX_H);
        questIconBox.setTextColor(0x00000000);
        questIconBox.setTextColorUneditable(0x00000000);
        questDescriptionBox = createMultiLineBox("Quest description", BOX_H_TALL, true);
        questCategoryBox = createBox("Quest category", BOX_H);
        questSubCategoryBox = createBox("Quest sub-category", BOX_H);
        questDependenciesBox = createMultiLineBox("Dependencies", BOX_H_TALL, false);
        questDependencyLockToggle = createDependencyLockToggle(false);
        questOptionalToggle = createToggle(false);
        questRepeatableToggle = createToggle(false);
        questAutoCompleteToggle = createToggle(false);
        questHiddenUnderDependencyToggle = createToggle(false);
        questCompletionBox = createMultiLineBox("Completion entries", BOX_H_TALL, false);
        questRewardBox = createMultiLineBox("Reward entries", BOX_H_TALL, false);
        initEntryRowBoxes();
        initDescriptionFormatterButtons();

        attachPackNameSanitizer(packNameBox);
        attachIdSanitizer(packNamespaceBox, false);
        attachIdSanitizer(packIconPathBox, false);
        attachIdSanitizer(catIdBox, false);
        attachIdSanitizer(catDependencyBox, false);
        attachIdSanitizer(subIdBox, false);
        subCategoryBox.setEditable(false);
        attachIdSanitizer(questIdBox, false);
        questCategoryBox.setEditable(false);
        questSubCategoryBox.setEditable(false);
        applyDropdownTextColor(subCategoryBox, false);
        applyDropdownTextColor(questCategoryBox, false);
        applyDropdownTextColor(questSubCategoryBox, false);
    }

    // create inline description tools
    private void initDescriptionFormatterButtons() {
        descriptionFormatButtons.clear();
        descriptionFormatButtons.add(createDescriptionInsertButton("<", "__undo__", 0xFF4A4A4A, 9, 0.75f));
        descriptionFormatButtons.add(createDescriptionInsertButton(">", "__redo__", 0xFF4A4A4A, 9, 0.75f));
        descriptionFormatButtons.add(createDescriptionInsertButton("X", "/x", 0xFF4A4A4A, 9, 0.75f));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/r", 0xFFFF5555, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/g", 0xFF55FF55, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/b", 0xFF5555FF, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/w", 0xFF55FFFF, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/y", 0xFFFFFF55, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/o", 0xFFFFAA00, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/a", 0xFFAAAAAA, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("", "/p", 0xFFAA55FF, 8));
        descriptionFormatButtons.add(createDescriptionInsertButton("B", "/l", 0xFF555555, 9, 0.75f));
        descriptionFormatButtons.add(createDescriptionInsertButton("I", "/i", 0xFF555555, 9, 0.75f));
        descriptionFormatButtons.add(createDescriptionInsertButton("E", "/e", 0xFF555555, 9, 0.75f));
        descriptionFormatButtons.add(createDescriptionInsertButton("H", "/h", 0xFF777733, 9, 0.75f));
    }

    private TextInsertButton createDescriptionInsertButton(String label, String insertText, int fillColor, int width) {
        return createDescriptionInsertButton(label, insertText, fillColor, width, 1.0f);
    }

    private TextInsertButton createDescriptionInsertButton(String label, String insertText, int fillColor, int width, float textScale) {
        TextInsertButton button = new TextInsertButton(0, 0, width, label, insertText, fillColor, textScale, this::applyDescriptionFormat);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    // create a shared single line field
    private EditBox createBox(String hint, int height) {
        EditBox box = new EditBox(font, 0, 0, pw - 4, height, Component.literal(hint));
        box.setMaxLength(1024);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private ScaledMultiLineEditBox createMultiLineBox(String hint, int height) {
        return createMultiLineBox(hint, height, false);
    }

    // create a shared multiline field
    private ScaledMultiLineEditBox createMultiLineBox(String hint, int height, boolean allowColorFormatting) {
        ScaledMultiLineEditBox box = new ScaledMultiLineEditBox(font, 0, 0, pw - 4, height, Component.literal(hint), Component.empty(), INPUT_TEXT_SCALE, allowColorFormatting);
        box.setCharacterLimit(4096);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private ToggleButton createToggle(boolean initial) {
        ToggleButton button = new ToggleButton(0, 0, TOGGLE_SIZE, TOGGLE_SIZE, initial);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    private LockToggleButton createDependencyLockToggle(boolean initial) {
        LockToggleButton button = new LockToggleButton(0, 0, DEP_LOCK_SIZE, DEP_LOCK_SIZE, initial);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    // switch the active editor mode
    private void setMode(Mode next) {
        if (next == Mode.QUEST_LIST && mode != Mode.QUEST_LIST) {
            collapsedQuestCategories.clear();
            collapsedQuestSubCategories.clear();
        }
        closeTransientMenus();
        mode = next;
        statusMessage = "";
        editorScroll = 0f;
        disarmDeleteConfirm();
        refreshLeftList(false);
        clearEditor();
        updateLeftPaneLayout();
        updateBackButtonVisibility();
    }

    private void refreshLeftList() {
        refreshLeftList(true);
    }

    // rebuild the left list for the current mode
    private void refreshLeftList(boolean preserveScroll) {
        float previousScroll = 0f;
        if (preserveScroll && leftList != null) {
            previousScroll = leftList.getScrollY();
        }
        List<EditorEntry> entries = switch (mode) {
            case PACK_LIST, PACK_CREATE -> buildPackEntries();
            case PACK_MENU -> buildPackMenuEntries();
            case CATEGORY_LIST -> buildCategoryEntries();
            case SUBCATEGORY_LIST -> buildSubCategoryEntries();
            case QUEST_LIST -> buildQuestEntries();
        };
        leftList.setEntries(entries);
        if (preserveScroll) {
            leftList.setScrollY(previousScroll);
        }
        leftList.setSelectedId(selectedEntryId);
        refreshPackIdCaches(mode == Mode.QUEST_LIST ? questListIndex() : null);
    }

    // build the pack selection list
    private List<EditorEntry> buildPackEntries() {
        List<EditorEntry> out = new ArrayList<>();
        boolean builtinEnabled = Config.enableBuiltinQuestPack();
        out.add(new EditorEntry(
                ENTRY_BUILTIN_PACK,
                trs("builtin_pack"),
                builtinEnabled ? trs("builtin_pack_enabled_subtitle") : trs("builtin_pack_disabled_subtitle"),
                "",
                builtinEnabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX,
                builtinEnabled ? trs("enabled") : trs("disabled")
        ));

        for (QuestPack pack : QuestEditorPackFiles.listPacks()) {
            PackMeta meta = QuestEditorPackFiles.readPackMeta(pack.root, pack.name);
            String packIconId = QuestEditorPackFiles.normalizePackIconId(meta.iconPath);
            boolean legacy = pack.legacy;
            boolean changed = stagedPacks.containsKey(pack.name);
            boolean enabled = !legacy && pack.enabled;
            ResourceLocation actionIcon = enabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX;
            String actionTooltip = legacy ? trs("legacy_pack") : (enabled ? trs("enabled") : trs("disabled"));
            ResourceLocation rowTexture = legacy ? PACK_ACTION_NEEDED_TEX : (changed ? PACK_CHANGED_TEX : ROW_TEX);
            out.add(new EditorEntry(
                    pack.name,
                    pack.name,
                    "",
                    packIconId,
                    actionIcon,
                    actionTooltip,
                    legacy ? LEGACY_PACK_TOOLTIP : trs("tooltip.right_click_options"),
                    rowTexture
            ));
        }
        return out;
    }

    // build the pack section menu
    private List<EditorEntry> buildPackMenuEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_CATEGORIES, trs("categories"), "", ""));
        out.add(new EditorEntry(ENTRY_SUBCATEGORIES, trs("subcategories"), "", ""));
        out.add(new EditorEntry(ENTRY_QUESTS, trs("quests"), "", ""));
        return out;
    }

    // build category rows
    private List<EditorEntry> buildCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        if (currentPack == null) return out;
        for (NamedEntry entry : QuestEditorJsonFiles.listCategoryEntries(currentPack)) {
            out.add(EditorEntry.movable(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    // build sub category rows
    private List<EditorEntry> buildSubCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        if (currentPack == null) return out;
        for (NamedEntry entry : QuestEditorJsonFiles.listSubCategoryEntries(currentPack)) {
            out.add(EditorEntry.movable(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    // build grouped quest rows
    private List<EditorEntry> buildQuestEntries() {
        List<EditorEntry> out = new ArrayList<>();
        if (currentPack == null) return out;
        QuestListIndex index = questListIndex();
        Map<String, Map<String, List<NamedEntry>>> grouped = new LinkedHashMap<>();
        for (QuestListItem item : index.items) {
            if (!matchesQuestSearch(item.entry)) continue;
            QuestEntryData data = item.data;
            String categoryId = data == null ? "" : safe(data.category);
            String subCategoryId = data == null ? "" : safe(data.subCategory);
            grouped.computeIfAbsent(categoryId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(subCategoryId, k -> new ArrayList<>())
                    .add(item.entry);
        }

        for (Map.Entry<String, Map<String, List<NamedEntry>>> categoryEntry : grouped.entrySet()) {
            String categoryId = safe(categoryEntry.getKey());
            String categoryName = index.categoryNames.getOrDefault(categoryId, categoryId.isBlank() ? trs("unassigned") : categoryId);
            boolean categoryCollapsed = collapsedQuestCategories.contains(categoryId);
            out.add(EditorEntry.categoryHeader(categoryId, categoryName, categoryCollapsed));
            if (categoryCollapsed) continue;

            for (Map.Entry<String, List<NamedEntry>> subEntry : categoryEntry.getValue().entrySet()) {
                String subId = safe(subEntry.getKey());
                String subKey = categoryId + "::" + subId;
                String subName = index.subCategoryNames.getOrDefault(subKey, subId.isBlank() ? trs("no_subcategory") : subId);
                boolean subCollapsed = collapsedQuestSubCategories.contains(subKey);
                out.add(EditorEntry.subCategoryHeader(subKey, subName, subCollapsed));
                if (subCollapsed) continue;
                for (NamedEntry quest : subEntry.getValue()) {
                    QuestEntryData questData = index.questDataByEntryId.get(safe(quest.id));
                    String invalidReason = questEntryInvalidReason(questData, index.categoryIds, index.subCategoryIds, index.questIds, index.duplicateQuestIds);
                    boolean invalid = invalidReason != null;
                    out.add(invalid
                            ? EditorEntry.quest(quest.id, quest.name, quest.id, quest.icon, PACK_ACTION_NEEDED_TEX, invalidReason)
                            : EditorEntry.quest(quest.id, quest.name, quest.id, quest.icon));
                }
            }
        }
        return out;
    }

    private QuestListIndex questListIndex() {
        if (currentPack == null) return QuestListIndex.EMPTY;
        if (questListIndexCache != null && Objects.equals(questListIndexPackRoot, currentPack.root)) {
            return questListIndexCache;
        }
        questListIndexCache = QuestEditorQuestOrdering.buildQuestListIndex(currentPack);
        questListIndexPackRoot = currentPack.root;
        return questListIndexCache;
    }

    private void invalidateQuestListIndex() {
        questListIndexCache = null;
        questListIndexPackRoot = null;
    }

    private boolean isQuestEntryInvalid(QuestEntryData data) {
        return questEntryInvalidReason(data) != null;
    }

    private String questEntryInvalidReason(QuestEntryData data) {
        return questEntryInvalidReason(data, categoryIdCache, subCategoryIdCache, questIdCache, Set.of());
    }

    private String questEntryInvalidReason(QuestEntryData data, Set<String> categoryIds, Set<String> subCategoryIds, Set<String> questIds, Set<String> duplicateQuestIds) {
        if (data == null) return null;
        String id = safe(data.id).trim();
        if (id.isBlank()) return "Missing quest id";
        if (duplicateQuestIds != null && duplicateQuestIds.contains(id)) return "Duplicate quest id";
        String name = safe(data.name).trim();
        if (name.isBlank()) return "Missing quest name";
        String category = safe(data.category).trim();
        if (category.isBlank()) return "Missing quest category";
        boolean categoryKnown = categoryIds != null && categoryIds.contains(category);
        boolean categoryKnownGlobally = categoryIdCache.contains(category) || QuestData.categoryById(category).isPresent();
        if (!categoryKnown && !categoryKnownGlobally) return "Invalid quest category";

        String subCategory = safe(data.subCategory).trim();
        if (!subCategory.isBlank()) {
            boolean hasExact = subCategoryIds != null && subCategoryIds.contains(category + "::" + subCategory);
            boolean hasWildcard = subCategoryIds != null && subCategoryIds.contains("::" + subCategory);
            if (!hasExact && !hasWildcard) return "Invalid quest sub-category";
        }

        for (String dep : QuestEditorEntryCodec.extractEntryLines(safe(data.dependencies))) {
            if (questIds == null || !questIds.contains(dep)) return "Invalid quest dependency";
        }
        return null;
    }

    private boolean matchesQuestSearch(NamedEntry entry) {
        String query = safe(questSearchQuery).trim().toLowerCase(Locale.ROOT);
        if (query.isBlank() || entry == null) return true;
        return safe(entry.id).toLowerCase(Locale.ROOT).contains(query)
                || safe(entry.name).toLowerCase(Locale.ROOT).contains(query)
                || safe(entry.sortKey).toLowerCase(Locale.ROOT).contains(query);
    }

    private void handleLeftClick(EditorEntry entry) {
        if (entry == null) return;
        // keep the current editor open while changes are unsaved
        if (!ENTRY_NEW.equals(entry.id) && Objects.equals(selectedEntryId, entry.id) && hasUnsavedEditorChanges()) {
            leftList.setSelectedId(selectedEntryId);
            return;
        }
        if (shouldWarnForUnsavedChanges(entry)) {
            return;
        }
        clearPendingDiscardState();
        selectedEntryId = entry.id;
        statusMessage = "";
        leftList.setSelectedId(selectedEntryId);

        switch (mode) {
            case PACK_LIST, PACK_CREATE -> handlePackEntry(entry);
            case PACK_MENU -> handlePackMenuEntry(entry);
            case CATEGORY_LIST -> handleCategoryEntry(entry);
            case SUBCATEGORY_LIST -> handleSubCategoryEntry(entry);
            case QUEST_LIST -> handleQuestEntry(entry);
        }
    }

    private void handleLeftSecondaryClick(EditorEntry entry) {
        if (entry == null || mode != Mode.PACK_LIST || ENTRY_BUILTIN_PACK.equals(entry.id)) return;
        // right click opens pack options instead of the pack contents
        if (shouldWarnForUnsavedChanges(entry)) {
            return;
        }
        clearPendingDiscardState();
        QuestPack pack = QuestEditorPackFiles.findPackByName(entry.id);
        if (pack == null) {
            statusMessage = trs("status.pack_not_found");
            statusColor = 0xFF8080;
            return;
        }
        currentPack = pack;
        invalidateQuestListIndex();
        selectedEntryId = pack.name;
        leftList.setSelectedId(selectedEntryId);
        showPackOptions(pack);
    }

    private void handleLeftAction(EditorEntry entry) {
        if (entry == null || mode != Mode.PACK_LIST) return;
        // action clicks toggle pack enabled state
        if (ENTRY_BUILTIN_PACK.equals(entry.id)) {
            boolean next = !Config.enableBuiltinQuestPack();
            Config.ENABLE_BUILTIN_QUEST_PACK.set(next);
            Config.SPEC.save();
            if (isSingleplayerAuthoritySession() || !QuestEditorPackFiles.sendQuestPackEnabledToServer("", next, true)) {
                QuestEditorPackFiles.runBoundlessReloadInBackground();
            }
            QuestPanelClient.applyConfigChanges();
            statusMessage = next ? trs("status.builtin_enabled") : trs("status.builtin_disabled");
            statusColor = 0xA0FFA0;
            refreshLeftList();
            return;
        }
        QuestPack pack = QuestEditorPackFiles.findPackByName(entry.id);
        if (pack == null) return;
        if (pack.legacy) {
            return;
        }
        boolean next = !pack.enabled;
        if (!QuestEditorPackFiles.setQuestPackEnabled(pack, next)) {
            setError(trs("error.update_questpack_failed"));
            return;
        }
        if (isSingleplayerAuthoritySession() || (!QuestEditorPackFiles.sendQuestPackToServer(pack, next) && !QuestEditorPackFiles.sendQuestPackEnabledToServer(pack.name, next, false))) {
            QuestEditorPackFiles.runBoundlessReloadInBackground();
        }
        statusMessage = trs(next ? "status.questpack_enabled" : "status.questpack_disabled", pack.name);
        statusColor = 0xA0FFA0;
        refreshLeftList();
    }

    private void handleEntryMoveAction(EditorEntry entry, MoveDirection direction) {
        if (entry == null || direction == null || currentPack == null) return;
        if (entry.kind == EditorEntryKind.CATEGORY_HEADER || entry.kind == EditorEntryKind.SUBCATEGORY_HEADER) return;

        // move entries inside the active list
        try {
            String successMessageKey = null;
            if (mode == Mode.QUEST_LIST && entry.kind == EditorEntryKind.QUEST) {
                invalidateQuestListIndex();
                QuestOrderResult result = QuestEditorQuestOrdering.moveQuestWithinGroup(currentPack, entry.id, direction, editingPath);
                editingPath = result.editingPath();
                if (!result.questOrderToken().isBlank()) questOrderToken = result.questOrderToken();
                successMessageKey = direction == MoveDirection.UP ? "status.quest_moved_up" : "status.quest_moved_down";
            } else if (mode == Mode.CATEGORY_LIST && entry.kind == EditorEntryKind.NORMAL && !ENTRY_NEW.equals(entry.id)) {
                invalidateQuestListIndex();
                QuestEditorQuestOrdering.moveCategoryByOrder(currentPack, gson, entry.id, direction);
                successMessageKey = direction == MoveDirection.UP ? "status.category_moved_up" : "status.category_moved_down";
            } else if (mode == Mode.SUBCATEGORY_LIST && entry.kind == EditorEntryKind.NORMAL && !ENTRY_NEW.equals(entry.id)) {
                invalidateQuestListIndex();
                QuestEditorQuestOrdering.moveSubCategoryByOrder(currentPack, gson, entry.id, direction);
                successMessageKey = direction == MoveDirection.UP ? "status.subcategory_moved_up" : "status.subcategory_moved_down";
            } else {
                return;
            }

            selectedEntryId = entry.id;
            if (leftList != null) leftList.setSelectedId(selectedEntryId);
            refreshLeftList();
            statusMessage = trs(successMessageKey);
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            if (mode == Mode.CATEGORY_LIST) {
                setError(trs("error.reorder_category_failed"));
            } else if (mode == Mode.SUBCATEGORY_LIST) {
                setError(trs("error.reorder_subcategory_failed"));
            } else {
                setError(trs("error.reorder_quest_failed"));
            }
        }
    }

    // open the pack creation form
    private void openPackCreate() {
        setMode(Mode.PACK_CREATE);
        showPackCreate();
    }

    private void requestCreateEntryForCurrentMode() {
        // create the correct entry type for the active tab
        EditorEntry entry = switch (mode) {
            case CATEGORY_LIST -> new EditorEntry(ENTRY_NEW, trs("create_new_category"), "", "");
            case SUBCATEGORY_LIST -> new EditorEntry(ENTRY_NEW, trs("create_new_subcategory"), "", "");
            case QUEST_LIST -> new EditorEntry(ENTRY_NEW, trs("create_new_quest"), "", "");
            default -> null;
        };
        if (entry != null) handleLeftClick(entry);
    }

    // choose the create action for the current list
    private Component createEntryTooltip() {
        return switch (mode) {
            case CATEGORY_LIST -> tr("tooltip.create_category");
            case SUBCATEGORY_LIST -> tr("tooltip.create_subcategory");
            case QUEST_LIST -> tr("tooltip.create_quest");
            default -> tr("tooltip.create_entry");
        };
    }

    // open the selected pack workspace
    private void handlePackEntry(EditorEntry entry) {
        if (ENTRY_BUILTIN_PACK.equals(entry.id)) {
            statusMessage = trs("status.use_builtin_toggle");
            statusColor = 0xA0A0A0;
            return;
        }

        currentPack = QuestEditorPackFiles.findPackByName(entry.id);
        invalidateQuestListIndex();
        if (currentPack == null) {
            statusMessage = trs("status.pack_not_found");
            statusColor = 0xFF8080;
            return;
        }
        if (currentPack.legacy) {
            return;
        }
        if (!QuestEditorPackFiles.ensurePackWorkspace(currentPack)) {
            setError(trs("error.open_pack_failed"));
            return;
        }
        String refreshedNamespace = QuestEditorPackFiles.findNamespace(currentPack.root);
        if (refreshedNamespace != null && !refreshedNamespace.isBlank()
                && !refreshedNamespace.equals(currentPack.namespace)) {
        currentPack = new QuestPack(currentPack.name, refreshedNamespace, currentPack.root, currentPack.legacy, currentPack.enabled);
        invalidateQuestListIndex();
        }
        setMode(Mode.CATEGORY_LIST);
    }

    // switch between pack sections
    private void handlePackMenuEntry(EditorEntry entry) {
        if (ENTRY_CATEGORIES.equals(entry.id)) {
            setMode(Mode.CATEGORY_LIST);
            return;
        }
        if (ENTRY_SUBCATEGORIES.equals(entry.id)) {
            setMode(Mode.SUBCATEGORY_LIST);
            return;
        }
        if (ENTRY_QUESTS.equals(entry.id)) {
            setMode(Mode.QUEST_LIST);
        }
    }

    // open the selected category form
    private void handleCategoryEntry(EditorEntry entry) {
        if (ENTRY_NEW.equals(entry.id)) {
            showCategoryEditor(new CategoryData(), null);
            return;
        }
        if (currentPack == null) return;
        CategoryData data = QuestEditorJsonFiles.loadCategory(currentPack, entry.id);
        if (data == null) {
            statusMessage = trs("status.category_load_failed");
            statusColor = 0xFF8080;
            return;
        }
        showCategoryEditor(data, data.path);
    }

    // open the selected sub category form
    private void handleSubCategoryEntry(EditorEntry entry) {
        if (ENTRY_NEW.equals(entry.id)) {
            showSubCategoryEditor(new SubCategoryData(), null);
            return;
        }
        if (currentPack == null) return;
        SubCategoryData data = QuestEditorJsonFiles.loadSubCategory(currentPack, entry.id);
        if (data == null) {
            statusMessage = trs("status.subcategory_load_failed");
            statusColor = 0xFF8080;
            return;
        }
        showSubCategoryEditor(data, data.path);
    }

    // open quest rows and toggle grouped headers
    private void handleQuestEntry(EditorEntry entry) {
        if (entry.kind == EditorEntryKind.CATEGORY_HEADER) {
            if (!collapsedQuestCategories.add(entry.id)) collapsedQuestCategories.remove(entry.id);
            refreshLeftList();
            return;
        }
        if (entry.kind == EditorEntryKind.SUBCATEGORY_HEADER) {
            if (!collapsedQuestSubCategories.add(entry.id)) collapsedQuestSubCategories.remove(entry.id);
            refreshLeftList();
            return;
        }
        if (ENTRY_NEW.equals(entry.id)) {
            showQuestEditor(new QuestEntryData(), null);
            return;
        }
        if (currentPack == null) return;
        QuestEntryData data = QuestEditorJsonFiles.loadQuest(currentPack, entry.id);
        if (data == null) {
            statusMessage = trs("status.quest_load_failed");
            statusColor = 0xFF8080;
            return;
        }
        showQuestEditor(data, data.path);
    }

    // show the new pack form
    private void showPackCreate() {
        editorType = EditorType.PACK_CREATE;
        editingPath = null;
        packNameBox.setValue("");
        packIconPathBox.setValue("");

        setActiveFields(List.of(
                field("Display", packNameBox)
        ));
        saveButton.setMessage(tr("create"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorUnsaved();
        updateBackButtonVisibility();
    }

    // show pack rename and icon options
    private void showPackOptions(QuestPack pack) {
        if (pack == null) return;
        editorType = EditorType.PACK_OPTIONS;
        editingPath = null;

        PackMeta meta = QuestEditorPackFiles.readPackMeta(pack.root, pack.name);
        packNameBox.setValue(safe(pack.name));
        packIconPathBox.setValue(safe(meta.iconPath));

        List<FormField> fields = new ArrayList<>();
        fields.add(field("Display", packNameBox));
        setActiveFields(fields);
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        if (exportPackButton != null) {
            exportPackButton.visible = true;
            exportPackButton.active = true;
        }
        markCurrentEditorLoaded(true);
        updateBackButtonVisibility();
    }

    // show category editing fields
    private void showCategoryEditor(CategoryData data, Path sourcePath) {
        editorType = EditorType.CATEGORY;
        editingPath = sourcePath;

        catIdBox.setValue(safe(data.id));
        catNameBox.setValue(safe(data.name));
        catIconBox.setValue(safe(data.icon));
        catDependencyBox.setValue(safe(data.dependency));
        catAutoCompleteToggle.setState(parseBool(data.autoComplete, false));

        setActiveFields(List.of(
                field(trs("field.id"), catIdBox),
                field("Display", catNameBox),
                field("Unlock quest", catDependencyBox),
                field(trs("field.auto_complete") + " (" + trs("tooltip.auto_complete_category") + ")", catAutoCompleteToggle)
        ));
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void showSubCategoryEditor(SubCategoryData data, Path sourcePath) {
        editorType = EditorType.SUBCATEGORY;
        editingPath = sourcePath;

        subIdBox.setValue(safe(data.id));
        setDropdownBoxValue(subCategoryBox, safe(data.category));
        subNameBox.setValue(safe(data.name));
        subIconBox.setValue(safe(data.icon));
        subDefaultOpenToggle.setState(parseBool(data.defaultOpen, true));

        setActiveFields(List.of(
                field(trs("field.id"), subIdBox),
                field(trs("field.category_id"), subCategoryBox),
                field("Display", subNameBox),
                field(trs("field.default_open") + " (" + trs("tooltip.default_open") + ")", subDefaultOpenToggle)
        ));
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void showQuestEditor(QuestEntryData data, Path sourcePath) {
        editorType = EditorType.QUEST;
        editingPath = sourcePath;
        disarmDeleteConfirm();

        // copy quest data into the active form
        questIdBox.setValue(safe(data.id));
        questNameBox.setValue(safe(data.name));
        questIconBox.setValue(safe(data.icon).isBlank() ? "boundless:quest_book" : safe(data.icon));
        questDescriptionBox.setValue(safe(data.description));
        setDropdownBoxValue(questCategoryBox, safe(data.category));
        setDropdownBoxValue(questSubCategoryBox, safe(data.subCategory));
        setEntryRowsFromRaw(EntryRowKind.DEPENDENCY, safe(data.dependencies));
        setDependencyLockState(parseBool(data.lockAfterDependency, false));
        questOptionalToggle.setState(parseBool(data.optional, false));
        questRepeatableToggle.setState(parseBool(data.repeatable, false));
        questAutoCompleteToggle.setState(parseBool(data.autoComplete, false));
        questHiddenUnderDependencyToggle.setState(parseBool(data.hiddenUnderDependency, false));
        setEntryRowsFromRaw(EntryRowKind.COMPLETION, QuestEditorEntryCodec.completionJsonToEntries(safe(data.completionJson)));
        setEntryRowsFromRaw(EntryRowKind.REWARD, QuestEditorEntryCodec.rewardJsonToEntries(safe(data.rewardJson)));
        loadedQuestType = safe(data.type);

        questDescriptionBox.scrollToTop();
        questCompletionBox.scrollToTop();
        questRewardBox.scrollToTop();
        questOrderToken = QuestEditorNaming.questOrderTokenFromPath(sourcePath);

        applyQuestEditorFields();
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void applyQuestEditorFields() {
        float previousScroll = editorScroll;
        List<FormField> fields = new ArrayList<>();
        fields.add(field(trs("field.id_required"), questIdBox));
        fields.add(field("Display *", questNameBox));
        fields.add(field(trs("field.description"), questDescriptionBox));
        fields.add(field(trs("field.category_required"), questCategoryBox));
        if (!dropdownBoxValue(questCategoryBox).isBlank()) {
            fields.add(field(trs("field.subcategory"), questSubCategoryBox));
        } else if (questSubCategoryBox != null) {
            setDropdownBoxValue(questSubCategoryBox, "");
        }
        fields.add(field(trs("field.dependencies"), questDependenciesBox));
        fields.add(field(trs("field.flags"), questOptionalToggle));
        fields.add(field(trs("field.completion"), questCompletionBox));
        fields.add(field(trs("field.reward"), questRewardBox));
        setActiveFields(fields);
        editorScroll = previousScroll;
    }
    private void saveCurrent() {
        if (currentPack == null && editorType != EditorType.PACK_CREATE) return;
        statusMessage = "";
        statusColor = 0xA0A0A0;

        if (editorType == EditorType.PACK_CREATE) {
            savePackCreate();
            return;
        }
        if (editorType == EditorType.PACK_OPTIONS) {
            savePackOptions();
            return;
        }
        if (editorType == EditorType.CATEGORY) {
            saveCategory();
            return;
        }
        if (editorType == EditorType.SUBCATEGORY) {
            saveSubCategory();
            return;
        }
        if (editorType == EditorType.QUEST) {
            saveQuest();
        }
    }

    private void savePackCreate() {
        String name = QuestEditorNaming.normalizePackName(packNameBox.getValue());
        if (!Objects.equals(name, safe(packNameBox.getValue()))) {
            packNameBox.setValue(name);
        }
        String namespace = QuestEditorPackFiles.namespaceFromPackName(name);
        if (!QuestEditorPackFiles.hasPackNameContent(name)) {
            setError("Pack name required");
            return;
        }
        if (QuestEditorPackFiles.isInvalidPackFolderName(name)) {
            setError("Invalid pack name");
            return;
        }
        if (QuestEditorPackFiles.isInvalidNamespace(namespace)) {
            setError("Invalid namespace");
            return;
        }

        Path root = QuestEditorPackFiles.packsRoot().resolve(name);
        if (Files.exists(root)) {
            setError("Pack already exists");
            return;
        }

        try {
            Files.createDirectories(root.getParent());
            Files.createDirectories(root);
            String iconPath = QuestEditorPackFiles.normalizePackIconId(packIconPathBox.getValue());
            QuestEditorPackFiles.writePackMeta(root, name, "Boundless Quest Pack: " + name, iconPath, true);
            QuestPack pack = new QuestPack(name, namespace, root, false, true);
            pack.ensureDirs();
            currentPack = pack;
            QuestEditorPackFiles.backupPack(currentPack, "created");
            setMode(Mode.CATEGORY_LIST);
            stagePackChange(pack, "Pack staged");
        } catch (Exception e) {
            setError("Failed to create pack");
        }
    }

    private void savePackOptions() {
        if (currentPack == null) return;

        String requestedName = QuestEditorNaming.normalizePackName(packNameBox.getValue());
        if (!Objects.equals(requestedName, safe(packNameBox.getValue()))) {
            packNameBox.setValue(requestedName);
        }
        String requestedNamespace = QuestEditorPackFiles.namespaceFromPackName(requestedName);
        if (!QuestEditorPackFiles.hasPackNameContent(requestedName)) {
            setError("Pack name required");
            return;
        }
        if (QuestEditorPackFiles.isInvalidPackFolderName(requestedName)) {
            setError("Invalid pack name");
            return;
        }

        Path oldRoot = currentPack.root;
        String oldName = currentPack.name;
        String oldNamespace = currentPack.namespace;
        Path newRoot = oldRoot;
        if (!requestedName.equals(oldName)) {
            newRoot = QuestEditorPackFiles.packsRoot().resolve(requestedName);
            if (Files.exists(newRoot)) {
                setError("Pack already exists");
                return;
            }
        }

        try {
            if (!requestedName.equals(oldName)) {
                Files.move(oldRoot, newRoot);
            }
            if (!Objects.equals(currentPack.namespace, requestedNamespace) && !currentPack.namespace.isBlank()) {
                Path dataRoot = newRoot.resolve("data");
                Path oldNamespaceDir = dataRoot.resolve(currentPack.namespace);
                Path newNamespaceDir = dataRoot.resolve(requestedNamespace);
                if (Files.exists(oldNamespaceDir) && Files.exists(newNamespaceDir)) {
                    setError("Generated namespace already exists");
                    return;
                }
                if (Files.exists(oldNamespaceDir) && !Files.exists(newNamespaceDir)) {
                    Files.createDirectories(dataRoot);
                    Files.move(oldNamespaceDir, newNamespaceDir);
                }
            }
            String description = "Boundless Quest Pack: " + requestedName;
            String iconPath = QuestEditorPackFiles.normalizePackIconId(packIconPathBox.getValue());
            QuestEditorPackFiles.writePackMeta(newRoot, requestedName, description, iconPath, currentPack.enabled);
            currentPack = new QuestPack(requestedName, requestedNamespace, newRoot, currentPack.legacy, currentPack.enabled);
            invalidateQuestListIndex();
            currentPack.ensureDirs();
            selectedEntryId = currentPack.name;
            leftList.setSelectedId(selectedEntryId);
            if (!requestedName.equals(oldName)) {
                stagedPacks.remove(oldName);
                stagedDeletedPackNames.remove(oldName);
                QuestEditorPackFiles.deleteAppliedPackArtifactsSafe(oldName);
            }
            stagePackChange(currentPack, "Pack options saved");
            QuestEditorPackFiles.backupPack(currentPack, "saved");
            refreshLeftList();
            showPackOptions(currentPack);
            markCurrentEditorSaved();
        } catch (IOException e) {
            setError("Failed to save pack options");
        }
    }

    private void saveCategory() {
        String id = safe(catIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Category id required");
            return;
        }
        selectedEntryId = id;
        JsonObject obj = buildCategoryJson(id);
        if (obj == null) return;
        Path target = currentPack.categoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
    }

    private void saveSubCategory() {
        String id = safe(subIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Sub-category id required");
            return;
        }
        selectedEntryId = id;
        JsonObject obj = buildSubCategoryJson(id);
        if (obj == null) return;
        Path target = currentPack.subCategoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
    }

    private JsonObject buildCategoryJson(String id) {
        return QuestEditorJsonBuilder.buildCategory(
                id,
                catNameBox.getValue(),
                catIconBox.getValue(),
                resolveCategoryOrder(id),
                catDependencyBox.getValue(),
                catAutoCompleteToggle.isOn(),
                this::setError
        );
    }

    private JsonObject buildSubCategoryJson(String id) {
        return QuestEditorJsonBuilder.buildSubCategory(
                id,
                dropdownBoxValue(subCategoryBox),
                subNameBox.getValue(),
                subIconBox.getValue(),
                resolveSubCategoryOrder(id),
                subDefaultOpenToggle.isOn(),
                this::setError
        );
    }

    private int resolveCategoryOrder(String categoryId) {
        int existing = QuestEditorJsonFiles.readOrderFromPath(editingPath, -1);
        if (existing >= 0) return existing;
        if (currentPack == null) return 0;
        int max = -1;
        for (NamedEntry entry : QuestEditorJsonFiles.listCategoryEntries(currentPack)) {
            if (entry == null || entry.id == null || entry.id.equals(categoryId)) continue;
            int order = QuestEditorJsonFiles.readOrderFromPath(entry.path, -1);
            if (order > max) max = order;
        }
        return Math.max(0, max + 1);
    }

    private int resolveSubCategoryOrder(String subCategoryId) {
        int existing = QuestEditorJsonFiles.readOrderFromPath(editingPath, -1);
        if (existing >= 0) return existing;
        if (currentPack == null) return 0;
        int max = -1;
        for (NamedEntry entry : QuestEditorJsonFiles.listSubCategoryEntries(currentPack)) {
            if (entry == null || entry.id == null || entry.id.equals(subCategoryId)) continue;
            int order = QuestEditorJsonFiles.readOrderFromPath(entry.path, -1);
            if (order > max) max = order;
        }
        return Math.max(0, max + 1);
    }

    private void saveQuest() {
        String id = safe(questIdBox.getValue()).trim();
        selectedEntryId = id;
        JsonObject obj = buildQuestJson(id);
        if (obj == null) return;
        String orderToken = questOrderTokenForSave(id);
        Path target = currentPack.questsDir.resolve(QuestEditorQuestOrdering.questFileBaseName(id, orderToken) + ".json");
        boolean creatingNewQuest = editingPath == null;
        questOrderToken = orderToken;
        saveJson(obj, target, editingPath);
        if (creatingNewQuest && editingPath != null) {
            stagedPacks.remove(currentPack.name);
            stagedDeletedPackNames.remove(currentPack.name);
            QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, isSingleplayerAuthoritySession());
            Minecraft.getInstance().execute(() -> QuestData.loadClient(true));
            statusMessage = trs("status.quest_saved");
            statusColor = 0xA0FFA0;
        }
    }

    private JsonObject buildQuestJson(String id) {
        syncEntryBackingValues();
        QuestBuildFields fields = new QuestBuildFields();
        fields.id = id;
        fields.duplicateQuestId = isDuplicateQuestId(safe(id).trim());
        fields.name = questNameBox.getValue();
        fields.icon = questIconBox.getValue();
        fields.description = questDescriptionBox.getValue();
        fields.category = dropdownBoxValue(questCategoryBox);
        fields.subCategory = dropdownBoxValue(questSubCategoryBox);
        fields.optional = questOptionalToggle.isOn();
        fields.repeatable = questRepeatableToggle.isOn();
        fields.autoComplete = questAutoCompleteToggle.isOn();
        fields.hiddenUnderDependency = questHiddenUnderDependencyToggle.isOn();
        fields.lockAfterDependency = dependencyLockState();
        fields.loadedQuestType = loadedQuestType;
        fields.dependencies = collectDependencyEntries();
        fields.completionRaw = questCompletionBox.getValue();
        fields.rewardRaw = questRewardBox.getValue();
        return QuestEditorJsonBuilder.buildQuest(fields, this::setError);
    }

    private void saveJson(JsonObject obj, Path target, Path original) {
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, target);
            if (original != null && !original.equals(target)) {
                QuestPackStorage.archiveReplacedFile(currentPack == null ? null : currentPack.root,
                        currentPack == null ? null : currentPack.name,
                        original,
                        "renamed");
                Files.deleteIfExists(original);
            }
            editingPath = target;
            markCurrentEditorSaved();
            stageCurrentPackChange("Saved to staging");
            QuestEditorPackFiles.backupPack(currentPack, "saved");
            refreshLeftList();
            QuestPanelClient.applyConfigChanges();
        } catch (IOException e) {
            setError("Save failed: " + safe(e.getMessage()));
        }
    }

    private void duplicateCurrentPackAsIs() {
        if (currentPack == null) return;
        try {
            if (!QuestEditorPackFiles.ensurePackWorkspace(currentPack)) {
                setError("Failed to open pack");
                return;
            }
            PackMeta meta = QuestEditorPackFiles.readPackMeta(currentPack.root, currentPack.name);
            String duplicateName = nextAvailablePackName(currentPack.name);
            Path target = QuestEditorPackFiles.packsRoot().resolve(duplicateName);
            QuestEditorPackFiles.mirrorDirectory(currentPack.root, target);
        QuestEditorPackFiles.writePackMeta(target, duplicateName, safe(meta.description), safe(meta.iconPath), meta.enabled);
            QuestPack duplicated = new QuestPack(duplicateName, currentPack.namespace, target, false, currentPack.enabled);
            duplicated.ensureDirs();
            currentPack = duplicated;
            QuestEditorPackFiles.backupPack(currentPack, "duplicated");
            invalidateQuestListIndex();
            selectedEntryId = duplicated.name;
            stagePackChange(duplicated, "Pack duplicated");
            refreshLeftList();
            leftList.setSelectedId(selectedEntryId);
            showPackOptions(duplicated);
            markCurrentEditorSaved();
        } catch (IOException e) {
            setError("Pack duplication failed");
        }
    }

    private void openPackDirectory() {
        try {
            if (currentPack == null || !QuestEditorPackFiles.ensurePackWorkspace(currentPack)) {
                setError("Failed to open pack");
                return;
            }
            Path folder = currentPack.root;
            Files.createDirectories(folder);
            Util.getPlatform().openFile(folder.toFile());
        } catch (Exception e) {
            setError("Failed to open folder");
        }
    }

    private void openImportQuestPackDirectory() {
        Path importRoot = Config.boundlessConfigRoot();
        try {
            Files.createDirectories(importRoot);
            Util.getPlatform().openFile(importRoot.toFile());
            statusMessage = trs("status.place_questpacks");
            statusColor = 0xA0A0A0;
        } catch (Exception e) {
            setError("Failed to open import directory");
        }
    }

    private void duplicateQuest() {
        if (currentPack == null || editorType != EditorType.QUEST) return;
        String baseId = safe(questIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Quest id required");
            return;
        }

        String newId = nextAvailableQuestId(baseId);
        JsonObject obj = buildQuestJson(newId);
        if (obj == null) return;

        selectedEntryId = newId;
        String orderToken = QuestEditorNaming.nextQuestOrderToken(currentPack);
        Path target = currentPack.questsDir.resolve(QuestEditorQuestOrdering.questFileBaseName(newId, orderToken) + ".json");
        saveJson(obj, target, null);
        QuestEntryData data = QuestEditorJsonFiles.loadQuest(currentPack, newId);
        if (data != null) {
            showQuestEditor(data, data.path);
        }
    }

    private void duplicateCategory() {
        if (currentPack == null || editorType != EditorType.CATEGORY) return;
        String baseId = safe(catIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Category id required");
            return;
        }
        String newId = nextAvailableCategoryId(baseId);
        JsonObject obj = buildCategoryJson(newId);
        if (obj == null) return;
        selectedEntryId = newId;
        saveJson(obj, currentPack.categoriesDir.resolve(newId + ".json"), null);
        CategoryData data = QuestEditorJsonFiles.loadCategory(currentPack, newId);
        if (data != null) showCategoryEditor(data, data.path);
    }

    private void duplicateSubCategory() {
        if (currentPack == null || editorType != EditorType.SUBCATEGORY) return;
        String baseId = safe(subIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError("Sub-category id required");
            return;
        }
        String newId = nextAvailableSubCategoryId(baseId);
        JsonObject obj = buildSubCategoryJson(newId);
        if (obj == null) return;
        selectedEntryId = newId;
        saveJson(obj, currentPack.subCategoriesDir.resolve(newId + ".json"), null);
        SubCategoryData data = QuestEditorJsonFiles.loadSubCategory(currentPack, newId);
        if (data != null) showSubCategoryEditor(data, data.path);
    }

    private void duplicateCurrent() {
        if (mode == Mode.PACK_MENU && currentPack != null) {
            duplicateCurrentPackAsIs();
            return;
        }
        switch (editorType) {
            case QUEST -> duplicateQuest();
            case CATEGORY -> duplicateCategory();
            case SUBCATEGORY -> duplicateSubCategory();
            case PACK_OPTIONS -> duplicateCurrentPackAsIs();
            default -> {}
        }
    }

    private void deleteQuest() {
        if (currentPack == null || editorType != EditorType.QUEST) return;
        String id = safe(questIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Quest id required");
            return;
        }

        Path target = editingPath != null ? editingPath : currentPack.questsDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                stageCurrentPackChange("Deletion staged");
            } else {
                statusMessage = trs("status.nothing_to_delete");
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private void deleteCategory() {
        if (currentPack == null || editorType != EditorType.CATEGORY) return;
        String id = safe(catIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Category id required");
            return;
        }

        Path target = editingPath != null ? editingPath : currentPack.categoriesDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                stageCurrentPackChange("Deletion staged");
            } else {
                statusMessage = trs("status.nothing_to_delete");
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private void deleteSubCategory() {
        if (currentPack == null || editorType != EditorType.SUBCATEGORY) return;
        String id = safe(subIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError("Sub-category id required");
            return;
        }

        Path target = editingPath != null ? editingPath : currentPack.subCategoriesDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            refreshLeftList();
            if (deleted) {
                stageCurrentPackChange("Deletion staged");
            } else {
                statusMessage = trs("status.nothing_to_delete");
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private void deleteCurrent() {
        if (mode == Mode.PACK_MENU && currentPack != null) {
            deletePack(currentPack);
            return;
        }
        switch (editorType) {
            case QUEST -> deleteQuest();
            case CATEGORY -> deleteCategory();
            case SUBCATEGORY -> deleteSubCategory();
            case PACK_OPTIONS -> deletePack(resolveCurrentPackForDelete());
            default -> {}
        }
    }

    private QuestPack resolveCurrentPackForDelete() {
        if (currentPack != null) return currentPack;
        String selected = safe(selectedEntryId).trim();
        if (!selected.isBlank()) {
            QuestPack bySelected = QuestEditorPackFiles.findPackByName(selected);
            if (bySelected != null) return bySelected;
        }
        if (packNameBox != null) {
            String byName = safe(packNameBox.getValue()).trim();
            if (!byName.isBlank()) return QuestEditorPackFiles.findPackByName(byName);
        }
        return null;
    }

    private void handleDeleteButtonPress() {
        if (mode != Mode.PACK_MENU
                && editorType != EditorType.QUEST
                && editorType != EditorType.CATEGORY
                && editorType != EditorType.SUBCATEGORY
                && editorType != EditorType.PACK_OPTIONS) {
            disarmDeleteConfirm();
            return;
        }
        if (!deleteConfirmArmed) {
            deleteConfirmArmed = true;
            deleteHoldActive = false;
            updateDeleteButtonTexture();
            statusMessage = trs("status.hold_delete_confirm");
            statusColor = 0xFFD080;
            return;
        }
        deleteHoldActive = true;
        deleteHoldStartMs = Util.getMillis();
    }

    private void disarmDeleteConfirm() {
        deleteHoldActive = false;
        if (!deleteConfirmArmed) return;
        deleteConfirmArmed = false;
        updateDeleteButtonTexture();
    }

    private void updateDeleteButtonTexture() {
        if (deleteQuestButton != null) {
            if (deleteConfirmArmed) {
                deleteQuestButton.setTextures(DELETE_CONFIRM_TEX, DELETE_CONFIRM_TEX_HOVER);
            } else {
                deleteQuestButton.setTextures(DELETE_TEX, DELETE_CONFIRM_TEX_HOVER);
            }
        }
    }

    private void updateDeleteHold(double mouseX, double mouseY) {
        if (!deleteHoldActive) return;
        if (deleteQuestButton == null || !deleteQuestButton.visible || !deleteQuestButton.active || !deleteQuestButton.isMouseOver(mouseX, mouseY)) {
            deleteHoldActive = false;
            return;
        }
        if (Util.getMillis() - deleteHoldStartMs >= DELETE_HOLD_MS) {
            disarmDeleteConfirm();
            deleteCurrent();
        }
    }

    private float deleteHoldProgress() {
        if (!deleteHoldActive) return 0f;
        return Math.min(1f, (Util.getMillis() - deleteHoldStartMs) / (float) DELETE_HOLD_MS);
    }

    private void renderDeleteHoldProgress(GuiGraphics gg) {
        float progress = deleteHoldProgress();
        if (progress <= 0f || deleteQuestButton == null || !deleteQuestButton.visible) return;
        int x0 = deleteQuestButton.getX() + 2;
        int y0 = deleteQuestButton.getY() + deleteQuestButton.getHeight() - 4;
        int w = Math.round((deleteQuestButton.getWidth() - 4) * progress);
        gg.fill(x0, y0, x0 + w, y0 + 2, 0xFFFF3030);
    }

    private String nextAvailableQuestId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextAvailableId(base, currentPack.questsDir);
    }

    private String nextAvailableCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextAvailableId(base, currentPack.categoriesDir);
    }

    private String nextAvailableSubCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextAvailableId(base, currentPack.subCategoriesDir);
    }

    private String nextAvailablePackName(String baseName) {
        return QuestEditorNaming.nextAvailablePackName(baseName);
    }

    private void deletePack(QuestPack pack) {
        if (pack == null) return;
        try {
            if (Files.exists(pack.root)) {
                QuestEditorPackFiles.deleteDirectory(pack.root);
            }
            currentPack = null;
            invalidateQuestListIndex();
            selectedEntryId = "";
            setMode(Mode.PACK_LIST);
            stagePackDeletion(pack, "Pack deletion staged");
        } catch (IOException e) {
            setError("Delete failed");
        }
    }

    private int componentStart(String raw) {
        String value = safe(raw);
        int brace = value.indexOf('{');
        int bracket = value.indexOf('[');
        if (brace < 0) return bracket;
        if (bracket < 0) return brace;
        return Math.min(brace, bracket);
    }

    private boolean applyChanges() {
        ScreenState state = captureState();
        if (state != null) {
            pendingState = state;
            stashPendingInitState(state);
        }
        boolean mirrored = QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, isSingleplayerAuthoritySession());
        Minecraft.getInstance().execute(() -> {
            if (pendingState != null) {
                ScreenState restore = pendingState;
                pendingState = null;
                restoreState(restore);
            }
            QuestData.loadClient(true);
        });
        return mirrored;
    }

    private void stageCurrentPackChange(String message) {
        stagePackChange(currentPack, message);
    }

    private void stagePackChange(QuestPack pack, String message) {
        if (pack == null || pack.name == null || pack.name.isBlank()) return;
        // keep staged pack edits until the screen closes
        invalidateQuestListIndex();
        stagedDeletedPackNames.remove(pack.name);
        stagedPacks.put(pack.name, pack);
        statusMessage = message;
        statusColor = 0xA0FFA0;
    }

    private void stagePackDeletion(QuestPack pack, String message) {
        if (pack == null || pack.name == null || pack.name.isBlank()) return;
        invalidateQuestListIndex();
        stagedPacks.remove(pack.name);
        stagedDeletedPackNames.add(pack.name);
        statusMessage = message;
        statusColor = 0xA0FFA0;
    }

    private boolean hasStagedChanges() {
        return !stagedPacks.isEmpty() || !stagedDeletedPackNames.isEmpty();
    }

    private void applyStagedChangesOnClose() {
        if (!hasStagedChanges()) return;
        Minecraft mc = Minecraft.getInstance();
        QuestPack selectedPack = currentPack;
        boolean changed = false;
        boolean singleplayerAuthority = isSingleplayerAuthoritySession();

        // apply staged deletes before staged writes
        for (String packName : new ArrayList<>(stagedDeletedPackNames)) {
            if (!singleplayerAuthority) {
                QuestEditorPackFiles.sendDeleteQuestPackToServer(packName);
            }
            changed |= QuestEditorPackFiles.deleteAppliedPackArtifactsSafe(packName);
        }
        for (QuestPack pack : new ArrayList<>(stagedPacks.values())) {
            if (pack == null) continue;
            currentPack = pack;
            changed |= QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, singleplayerAuthority);
            if (selectedPack == null) {
                selectedPack = pack;
            }
        }

        currentPack = selectedPack;
        stagedPacks.clear();
        stagedDeletedPackNames.clear();
        if (!changed) return;

        if (singleplayerAuthority) {
            QuestEditorPackFiles.runBoundlessReloadInBackground();
        }
        mc.execute(() -> QuestData.loadClient(true));
    }

    private boolean isSingleplayerAuthoritySession() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getSingleplayerServer() != null;
    }

    private void exportCurrentPack() {
        if (currentPack == null || currentPack.root == null || !Files.isDirectory(currentPack.root)) {
            setError("No questpack to export");
            return;
        }
        try {
            Path zipPath = QuestEditorPackFiles.nextExportPath(currentPack);
            QuestEditorPackFiles.zipDirectory(currentPack.root, zipPath);
            statusMessage = trs("status.exported", zipPath.getFileName());
            statusColor = 0xA0FFA0;
            QuestEditorPackFiles.openParentFolder(zipPath);
        } catch (Exception e) {
            setError("Failed to export questpack");
        }
    }

    private void clearEditor() {
        closeTransientMenus();
        editorType = EditorType.NONE;
        editingPath = null;
        loadedQuestType = "";
        disarmDeleteConfirm();
        clearPendingDiscardState();
        savedEditorState = null;
        setActiveFields(List.of());
        saveButton.visible = false;
        saveButton.active = false;
        if (exportPackButton != null) {
            exportPackButton.visible = false;
            exportPackButton.active = false;
        }
        updateBackButtonVisibility();
    }

    private void closeTransientMenus() {
        popupMenus.close();
        if (isItemPickerOpen()) {
            closeItemPicker();
        }
    }

    private void setActiveFields(List<FormField> fields) {
        for (FormField f : allFields) {
            f.widget.visible = false;
            f.widget.active = false;
        }
        if (questRepeatableToggle != null) {
            questRepeatableToggle.visible = false;
            questRepeatableToggle.active = false;
        }
        if (questAutoCompleteToggle != null) {
            questAutoCompleteToggle.visible = false;
            questAutoCompleteToggle.active = false;
        }
        if (questHiddenUnderDependencyToggle != null) {
            questHiddenUnderDependencyToggle.visible = false;
            questHiddenUnderDependencyToggle.active = false;
        }
        if (questDependencyLockToggle != null) {
            questDependencyLockToggle.visible = false;
            questDependencyLockToggle.active = false;
        }
        if (packIconPathBox != null) {
            packIconPathBox.visible = false;
            packIconPathBox.active = false;
        }
        if (catIconBox != null) {
            catIconBox.visible = false;
            catIconBox.active = false;
        }
        if (subIconBox != null) {
            subIconBox.visible = false;
            subIconBox.active = false;
        }
        if (questIconBox != null) {
            questIconBox.visible = false;
            questIconBox.active = false;
        }
        for (LockToggleButton button : dependencyEntryLockButtons) {
            button.visible = false;
            button.active = false;
        }
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            box.visible = false;
            box.active = false;
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            box.visible = false;
            box.active = false;
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            box.visible = false;
            box.active = false;
        }
        for (EntryRemoveButton button : completionEntryRemoveButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryRemoveButton button : dependencyEntryRemoveButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryRemoveButton button : rewardEntryRemoveButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryTypeButton button : completionEntryTypeButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryTypeButton button : rewardEntryTypeButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryItemPickerButton button : completionEntryItemPickerButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryItemPickerButton button : rewardEntryItemPickerButtons) {
            button.visible = false;
            button.active = false;
        }
        for (EntryCountBox box : completionEntryCountBoxes) {
            box.visible = false;
            box.active = false;
        }
        for (EntryCountBox box : rewardEntryCountBoxes) {
            box.visible = false;
            box.active = false;
        }
        hideDescriptionFormatterButtons();
        activeFields.clear();
        activeFields.addAll(fields);
        for (FormField f : activeFields) {
            if (!allFields.contains(f)) allFields.add(f);
            f.widget.visible = true;
            f.widget.active = true;
        }
        entryRowsDirty = true;
        editorScroll = 0f;
    }

    private void hideDescriptionFormatterButtons() {
        for (TextInsertButton button : descriptionFormatButtons) {
            button.visible = false;
            button.active = false;
        }
    }

    private void initEntryRowBoxes() {
        resetEntryRows(EntryRowKind.DEPENDENCY, List.of());
        resetEntryRows(EntryRowKind.COMPLETION, List.of());
        resetEntryRows(EntryRowKind.REWARD, List.of());
        syncEntryBackingValues();
    }

    private ScaledMultiLineEditBox createEntryRowBox(EntryRowKind kind) {
        String hint = switch (kind) {
            case DEPENDENCY -> "boundless:quest_id";
            case REWARD, COMPLETION -> "";
        };
        ScaledMultiLineEditBox box = new ScaledMultiLineEditBox(font, 0, 0, pw - 4, ENTRY_ROW_H,
                Component.literal(hint), Component.empty(), ENTRY_INPUT_TEXT_SCALE, false);
        box.setCharacterLimit(4096);
        box.setValueListener(v -> {
            if (syncingEntryRows) return;
            entryRowsDirty = true;
            syncEntryBackingValues();
        });
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private List<ScaledMultiLineEditBox> entryRows(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> dependencyEntryBoxes;
            case COMPLETION -> completionEntryBoxes;
            case REWARD -> rewardEntryBoxes;
        };
    }

    private List<EntryRemoveButton> entryRemoveButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> dependencyEntryRemoveButtons;
            case COMPLETION -> completionEntryRemoveButtons;
            case REWARD -> rewardEntryRemoveButtons;
        };
    }

    private List<EntryTypeButton> entryTypeButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryTypeButtons;
            case REWARD -> rewardEntryTypeButtons;
        };
    }

    private List<EntryItemPickerButton> entryItemPickerButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryItemPickerButtons;
            case REWARD -> rewardEntryItemPickerButtons;
        };
    }

    private List<EntryCountBox> entryCountBoxes(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryCountBoxes;
            case REWARD -> rewardEntryCountBoxes;
        };
    }

    private void resetEntryRows(EntryRowKind kind, List<String> lines) {
        List<ScaledMultiLineEditBox> target = entryRows(kind);
        List<EntryRemoveButton> removeButtons = entryRemoveButtons(kind);
        syncingEntryRows = true;
        try {
            for (ScaledMultiLineEditBox box : target) {
                selectedItemIdByBox.remove(box);
                selectedItemIdsByBox.remove(box);
                selectedItemComponentsByBox.remove(box);
                entryCountByBox.remove(box);
                entryTypeByBox.remove(box);
                removeWidget(box);
            }
            target.clear();
            for (EntryRemoveButton button : removeButtons) {
                removeWidget(button);
            }
            removeButtons.clear();
            if (kind == EntryRowKind.DEPENDENCY) {
                for (LockToggleButton button : dependencyEntryLockButtons) {
                    removeWidget(button);
                }
                dependencyEntryLockButtons.clear();
            } else {
                for (EntryTypeButton button : entryTypeButtons(kind)) removeWidget(button);
                entryTypeButtons(kind).clear();
                for (EntryItemPickerButton button : entryItemPickerButtons(kind)) removeWidget(button);
                entryItemPickerButtons(kind).clear();
                for (EntryCountBox box : entryCountBoxes(kind)) removeWidget(box);
                entryCountBoxes(kind).clear();
            }
            List<String> normalized = new ArrayList<>();
            if (lines != null) {
                for (String line : lines) {
                    String v = safe(line).trim();
                    if (!v.isBlank()) normalized.add(v);
                }
            }
            if (normalized.isEmpty()) normalized.add("");
            for (String line : normalized) {
                ScaledMultiLineEditBox box = createEntryRowBox(kind);
                if (kind != EntryRowKind.DEPENDENCY) {
                    ParsedEntry parsed = QuestEditorEntryCodec.parseEntry(line);
                    String normalizedType = QuestEditorEntryTypes.normalize(kind, parsed == null ? "" : parsed.type);
                    entryTypeByBox.put(box, normalizedType);
                    entryCountByBox.put(box, parsed == null ? 1 : Math.max(1, parsed.count));
                    if (parsed != null && QuestEditorEntryTypes.hasRowBrowser(kind, normalizedType)) {
                        List<String> normalizedAcceptedIds = new ArrayList<>();
                        boolean itemSelection = "collect".equals(normalizedType)
                                || "submit".equals(normalizedType)
                                || "item".equals(normalizedType);
                        String components = "";
                        for (String parsedId : parsed.acceptedIds) {
                            String itemId = itemSelection
                                    ? QuestEditorEntryCodec.normalizeItemIdWithComponents(parsedId, true)
                                    : QuestEditorEntryCodec.normalizeNamespacedId(parsedId, false);
                            if (itemId.isBlank() || normalizedAcceptedIds.contains(itemId)) continue;
                            if (itemSelection && parsed.acceptedIds.size() == 1) {
                                int compStart = componentStart(itemId);
                                if (compStart > 0) components = itemId.substring(compStart).trim();
                            }
                            normalizedAcceptedIds.add(itemId);
                        }
                        String itemId = normalizedAcceptedIds.isEmpty() ? "" : QuestItemSpec.stripComponents(normalizedAcceptedIds.get(0));
                        selectedItemIdByBox.put(box, itemId);
                        selectedItemIdsByBox.put(box, List.copyOf(normalizedAcceptedIds));
                        if (!components.isBlank()) selectedItemComponentsByBox.put(box, components);
                        else selectedItemComponentsByBox.remove(box);
                        box.setValue(displayNameForEntrySelection(kind, box, normalizedAcceptedIds));
                    } else {
                        selectedItemComponentsByBox.remove(box);
                        box.setValue(entryRowDisplayValue(kind, normalizedType, parsed, line));
                    }
                } else {
                    box.setValue(line);
                }
                target.add(box);
            }
            ensureTrailingEmptyRow(kind);
            syncEntryRemoveButtons(kind);
            if (kind == EntryRowKind.DEPENDENCY) syncDependencyEntryLockButtons();
        } finally {
            syncingEntryRows = false;
        }
    }

    private void normalizeEntryRows(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        syncingEntryRows = true;
        try {
            for (int i = rows.size() - 1; i >= 0; i--) {
                ScaledMultiLineEditBox box = rows.get(i);
                boolean blank = safe(box.getValue()).trim().isBlank();
                if (!blank) continue;
                boolean isLast = i == rows.size() - 1;
                if (!isLast && !box.isFocused()) {
                    removeWidget(box);
                    rows.remove(i);
                }
            }
            ensureTrailingEmptyRow(kind);
        } finally {
            syncingEntryRows = false;
        }
    }

    private void ensureTrailingEmptyRow(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (rows.isEmpty()) {
            rows.add(createEntryRowBox(kind));
            syncEntryRemoveButtons(kind);
            return;
        }
        ScaledMultiLineEditBox last = rows.get(rows.size() - 1);
        if (!safe(last.getValue()).trim().isBlank()) {
            rows.add(createEntryRowBox(kind));
        }
        while (rows.size() > 1) {
            ScaledMultiLineEditBox prev = rows.get(rows.size() - 2);
            if (!safe(prev.getValue()).trim().isBlank() || prev.isFocused()) break;
            removeWidget(prev);
            rows.remove(rows.size() - 2);
        }
        syncEntryRemoveButtons(kind);
        if (kind == EntryRowKind.DEPENDENCY) syncDependencyEntryLockButtons();
    }

    private String entryRowDisplayValue(EntryRowKind kind, String type, ParsedEntry parsed, String rawLine) {
        return QuestEditorEntryRows.displayValue(kind, type, parsed, rawLine);
    }

    private boolean usesCompactEntryBody(EntryRowKind kind, String type) {
        return QuestEditorEntryRows.usesCompactBody(kind, type);
    }

    private void syncEntryRemoveButtons(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<EntryRemoveButton> buttons = entryRemoveButtons(kind);
        while (buttons.size() < rows.size()) {
            EntryRemoveButton button = new EntryRemoveButton(kind, this::removeEntryRow);
            button.visible = false;
            button.active = false;
            buttons.add(button);
            addRenderableWidget(button);
        }
        while (buttons.size() > rows.size()) {
            EntryRemoveButton last = buttons.remove(buttons.size() - 1);
            removeWidget(last);
        }
        if (kind != EntryRowKind.DEPENDENCY) {
            List<EntryTypeButton> typeButtons = entryTypeButtons(kind);
            while (typeButtons.size() < rows.size()) {
                int rowIndex = typeButtons.size();
                EntryTypeButton button = new EntryTypeButton(kind, rowIndex, this::openTypeMenu, this::entryTypeButtonLabel);
                button.visible = false;
                button.active = false;
                typeButtons.add(button);
                addRenderableWidget(button);
            }
        while (typeButtons.size() > rows.size()) removeWidget(typeButtons.remove(typeButtons.size() - 1));
            for (int i = 0; i < typeButtons.size(); i++) typeButtons.get(i).setRow(i);

            List<EntryItemPickerButton> pickerButtons = entryItemPickerButtons(kind);
            for (EntryItemPickerButton button : pickerButtons) removeWidget(button);
            pickerButtons.clear();
            for (int i = 0; i < rows.size(); i++) {
                EntryItemPickerButton button = new EntryItemPickerButton(kind, i, (entryKind, row) -> {
                    openItemPicker(entryKind, row);
                    return true;
                }, this::selectedItemForRow, this::entryPickerButtonType);
                button.visible = false;
                button.active = false;
                pickerButtons.add(button);
                addRenderableWidget(button);
            }
            List<EntryCountBox> countBoxes = entryCountBoxes(kind);
            while (countBoxes.size() < rows.size()) {
                int rowIndex = countBoxes.size();
                EntryCountBox box = new EntryCountBox(font, kind, rowIndex, this::handleEntryCountChanged);
                box.visible = false;
                box.active = false;
                countBoxes.add(box);
                addRenderableWidget(box);
            }
            while (countBoxes.size() > rows.size()) removeWidget(countBoxes.remove(countBoxes.size() - 1));
            syncEntryCountBoxes(kind);
        }
    }

    private String entryTypeButtonLabel(EntryRowKind kind, int row) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row < 0 || row >= rows.size()) return "";
        return QuestEditorEntryTypes.label(effectiveRowType(kind, rows.get(row)));
    }

    private String entryPickerButtonType(EntryRowKind kind, int row) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        return row >= 0 && row < rows.size() ? effectiveRowType(kind, rows.get(row)) : "";
    }

    private void handleEntryCountChanged(EntryRowKind kind, int row, int count) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row >= 0 && row < rows.size()) {
            entryCountByBox.put(rows.get(row), count);
            entryRowsDirty = true;
            syncEntryBackingValues();
        }
    }

    private void syncEntryCountBoxes(EntryRowKind kind) {
        if (kind == EntryRowKind.DEPENDENCY) return;
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<EntryCountBox> countBoxes = entryCountBoxes(kind);
        int limit = Math.min(rows.size(), countBoxes.size());
        for (int i = 0; i < limit; i++) {
            EntryCountBox countBox = countBoxes.get(i);
            ScaledMultiLineEditBox row = rows.get(i);
            countBox.setRow(i);
            int count = entryCount(row);
            entryCountByBox.put(row, count);
            if (!countBox.isFocused()) {
                countBox.setValueSilently(Integer.toString(count));
            }
        }
    }

    private void syncDependencyEntryLockButtons() {
        while (dependencyEntryLockButtons.size() < dependencyEntryBoxes.size()) {
            LockToggleButton button = createDependencyLockToggle(dependencyLockState());
            dependencyEntryLockButtons.add(button);
        }
        while (dependencyEntryLockButtons.size() > dependencyEntryBoxes.size()) {
            LockToggleButton last = dependencyEntryLockButtons.remove(dependencyEntryLockButtons.size() - 1);
            removeWidget(last);
        }
        boolean state = dependencyLockState();
        for (LockToggleButton button : dependencyEntryLockButtons) {
            button.setState(state);
        }
    }

    private boolean dependencyLockState() {
        if (!dependencyEntryLockButtons.isEmpty()) return dependencyEntryLockButtons.get(0).isOn();
        return questDependencyLockToggle != null && questDependencyLockToggle.isOn();
    }

    private void setDependencyLockState(boolean state) {
        if (questDependencyLockToggle != null) questDependencyLockToggle.setState(state);
        for (LockToggleButton button : dependencyEntryLockButtons) {
            button.setState(state);
        }
    }

    private void removeEntryRow(EntryRowKind kind, EntryRemoveButton button) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<EntryRemoveButton> buttons = entryRemoveButtons(kind);
        int idx = buttons.indexOf(button);
        if (idx < 0 || idx >= rows.size()) return;

        if (rows.size() <= 1) {
            rows.get(0).setValue("");
            rows.get(0).setCursorPosition(0);
            rows.get(0).setFocused(true);
            entryRowsDirty = true;
            syncEntryBackingValues();
            return;
        }

        ScaledMultiLineEditBox removed = rows.remove(idx);
        selectedItemIdByBox.remove(removed);
        selectedItemIdsByBox.remove(removed);
        selectedItemComponentsByBox.remove(removed);
        entryCountByBox.remove(removed);
        entryTypeByBox.remove(removed);
        removeWidget(removed);
        EntryRemoveButton removedButton = buttons.remove(idx);
        removeWidget(removedButton);
        if (kind != EntryRowKind.DEPENDENCY) {
            List<EntryTypeButton> typeButtons = entryTypeButtons(kind);
            if (idx < typeButtons.size()) removeWidget(typeButtons.remove(idx));
            List<EntryItemPickerButton> pickerButtons = entryItemPickerButtons(kind);
            if (idx < pickerButtons.size()) removeWidget(pickerButtons.remove(idx));
            List<EntryCountBox> countBoxes = entryCountBoxes(kind);
            if (idx < countBoxes.size()) removeWidget(countBoxes.remove(idx));
        }

        ensureTrailingEmptyRow(kind);
        if (!rows.isEmpty()) {
            int next = Math.min(idx, rows.size() - 1);
            rows.get(next).setFocused(true);
        }
        entryRowsDirty = true;
        syncEntryBackingValues();
    }

    private void setEntryRowsFromRaw(EntryRowKind kind, String raw) {
        resetEntryRows(kind, QuestEditorEntryCodec.extractEntryLines(raw));
        syncEntryBackingValues();
    }

    private String entryRowsToRaw(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<String> out = new ArrayList<>();
        for (ScaledMultiLineEditBox box : rows) {
            String v = composeEntryRowLine(kind, box).trim();
            if (!v.isBlank()) out.add(v);
        }
        return String.join("\n", out);
    }

    private String composeEntryRowLine(EntryRowKind kind, ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.composeLine(kind, box, rowMaps());
    }

    private ParsedEntry rowParsedBody(EntryRowKind kind, ScaledMultiLineEditBox box, String raw) {
        return QuestEditorEntryRows.parsedBody(kind, box, raw, rowMaps());
    }

    private boolean rowContainsExplicitType(EntryRowKind kind, String raw) {
        return QuestEditorEntryRows.containsExplicitType(kind, raw);
    }

    private String bodyWithEntryCount(String body, int count) {
        return QuestEditorEntryRows.bodyWithCount(body, count);
    }

    private String entryBodyWithoutType(String raw) {
        return QuestEditorEntryRows.bodyWithoutType(raw);
    }

    private String effectiveRowType(EntryRowKind kind, ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.effectiveType(kind, box, rowMaps());
    }

    private int entryCount(ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.count(box, rowMaps());
    }

    private List<String> collectDependencyEntries() {
        List<String> out = new ArrayList<>();
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            String value = safe(box.getValue()).trim();
            if (!value.isBlank()) out.add(value);
        }
        return out;
    }

    private int entryRowsHeight(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (rows.isEmpty()) return ENTRY_ROW_H;
        int total = 0;
        for (ScaledMultiLineEditBox row : rows) {
            if (total > 0) total += ENTRY_ROW_GAP;
            total += entryRowHeight(kind, row);
        }
        return Math.max(ENTRY_ROW_H, total);
    }

    private int entryRowHeight(EntryRowKind kind, ScaledMultiLineEditBox box) {
        if (kind == EntryRowKind.DEPENDENCY || box == null) return ENTRY_ROW_H;
        int content = (int) Math.ceil(Math.max(1, box.getLineCount()) * Math.max(1.0, box.getLineHeight())) + 4;
        int maxHeight = Math.max(ENTRY_ROW_H, ph - (font.lineHeight + FIELD_LABEL_GAP + FIELD_ROW_GAP + 6));
        return Math.max(ENTRY_ROW_H, Math.min(maxHeight, content));
    }

    private void updateEntryRowFocusDisplays(EntryRowKind kind) {
        if (kind == EntryRowKind.DEPENDENCY) return;
        boolean wasSyncing = syncingEntryRows;
        syncingEntryRows = true;
        try {
            for (ScaledMultiLineEditBox box : entryRows(kind)) {
                if (box == null) continue;
                String itemId = safe(selectedItemIdByBox.get(box)).trim();
                if (itemId.isBlank()) continue;
                List<String> acceptedIds = selectedIdsForRow(box);
                String next = box.isFocused()
                        ? (acceptedIds.size() > 1
                            ? "[" + String.join(" | ", acceptedIds) + "]"
                            : itemId + safe(selectedItemComponentsByBox.get(box)).trim())
                        : displayNameForEntrySelection(kind, box, acceptedIds);
                if (safe(box.getValue()).equals(next)) continue;
                int cursor = box.getCursorPosition();
                box.setValue(next);
                box.setCursorPosition(Math.min(cursor, next.length()));
            }
        } finally {
            syncingEntryRows = wasSyncing;
        }
    }

    private String displayNameForEntryRow(EntryRowKind kind, ScaledMultiLineEditBox box, String itemId) {
        return QuestEditorEntryRows.displayNameForRow(kind, box, itemId, rowMaps());
    }

    private String displayNameForEntrySelection(EntryRowKind kind, ScaledMultiLineEditBox box, List<String> ids) {
        return QuestEditorEntryRows.displayNameForSelection(kind, box, ids, rowMaps());
    }

    private List<String> selectedIdsForRow(ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.selectedIds(box, rowMaps());
    }

    private boolean shouldExpandFocusedEntryRow(EntryRowKind kind, ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.shouldExpandFocused(kind, box);
    }

    private RowMaps rowMaps() {
        return new RowMaps(entryTypeByBox, entryCountByBox, selectedItemIdByBox, selectedItemIdsByBox, selectedItemComponentsByBox);
    }

    private int layoutEntryRows(EntryRowKind kind, int x, int y, int width, int clipTop, int clipBottom) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<EntryRemoveButton> removeButtons = entryRemoveButtons(kind);
        boolean dependency = kind == EntryRowKind.DEPENDENCY;
        boolean typed = kind == EntryRowKind.COMPLETION || kind == EntryRowKind.REWARD;
        int iconSpace = dependency ? DEP_ENTRY_ICON_SPACE : 0;
        int typeSpace = typed ? (ENTRY_TYPE_BTN_W + 2) : 0;
        int lockSpace = dependency ? (DEP_LOCK_SIZE + DEP_LOCK_GAP) : 0;
        int cursorY = y;
        List<LockToggleButton> lockButtons = dependency ? dependencyEntryLockButtons : List.of();
        List<EntryTypeButton> typeButtons = typed ? entryTypeButtons(kind) : List.of();
        List<EntryItemPickerButton> pickerButtons = typed ? entryItemPickerButtons(kind) : List.of();
        List<EntryCountBox> countBoxes = typed ? entryCountBoxes(kind) : List.of();
        for (int i = 0; i < rows.size(); i++) {
            ScaledMultiLineEditBox box = rows.get(i);
            EntryRemoveButton removeButton = i < removeButtons.size() ? removeButtons.get(i) : null;
            LockToggleButton lockButton = dependency && i < lockButtons.size() ? lockButtons.get(i) : null;
            EntryTypeButton typeButton = typed && i < typeButtons.size() ? typeButtons.get(i) : null;
            EntryItemPickerButton pickerButton = typed && i < pickerButtons.size() ? pickerButtons.get(i) : null;
            EntryCountBox countBox = typed && i < countBoxes.size() ? countBoxes.get(i) : null;
            String rowType = effectiveRowType(kind, box);
            box.setPlaceholder(Component.literal(QuestEditorEntryTypes.placeholder(kind, rowType)));
            boolean canPickItem = typed && QuestEditorEntryTypes.hasRowBrowser(kind, rowType);
            boolean hasCount = typed && QuestEditorEntryTypes.rowHasCount(kind, rowType);
            boolean expanded = shouldExpandFocusedEntryRow(kind, box);
            int pickerSpace = canPickItem ? (ENTRY_ITEM_PICK_BTN_W + 2) : 0;
            int countSpace = hasCount ? (ENTRY_COUNT_BTN_W + 2) : 0;
            int effectiveTypeSpace = expanded ? 0 : typeSpace;
            int effectivePickerSpace = expanded ? 0 : pickerSpace;
            int effectiveCountSpace = expanded ? 0 : countSpace;
            int effectiveRemoveSpace = expanded ? 0 : ENTRY_REMOVE_BTN_W;
            box.setX(x + iconSpace + effectiveTypeSpace + effectivePickerSpace);
            box.setY(cursorY);
            box.setWidth(Math.max(10, width - effectiveRemoveSpace - 2 - lockSpace - iconSpace - effectiveTypeSpace - effectivePickerSpace - effectiveCountSpace));
            int rowHeight = entryRowHeight(kind, box);
            box.setHeight(rowHeight);
            boolean inside = cursorY + rowHeight > clipTop && cursorY < clipBottom;
            box.visible = inside;
            box.active = inside;
            if (typeButton != null) {
                typeButton.setPosition(x + iconSpace, cursorY);
                typeButton.setWidth(ENTRY_TYPE_BTN_W);
                typeButton.setHeight(ENTRY_ROW_H);
                typeButton.visible = inside && !expanded;
                typeButton.active = inside && !expanded;
            }
            if (pickerButton != null) {
                pickerButton.setRow(i);
                int pickerX = x + iconSpace + typeSpace;
                pickerButton.setPosition(pickerX, cursorY);
                pickerButton.setWidth(ENTRY_ITEM_PICK_BTN_W);
                pickerButton.setHeight(ENTRY_ROW_H);
                pickerButton.visible = inside && canPickItem && !expanded;
                pickerButton.active = inside && canPickItem && !expanded;
            }
            if (countBox != null) {
                countBox.setRow(i);
                int countX = x + width - ENTRY_REMOVE_BTN_W - 2 - lockSpace - ENTRY_COUNT_BTN_W;
                countBox.setPosition(countX, cursorY);
                countBox.setWidth(ENTRY_COUNT_BTN_W);
                countBox.setHeight(ENTRY_ROW_H);
                if (!countBox.isFocused()) {
                    countBox.setValueSilently(Integer.toString(entryCount(box)));
                }
                countBox.visible = inside && hasCount && !expanded;
                countBox.active = inside && hasCount && !expanded;
            }
            if (removeButton != null) {
                removeButton.setPosition(x + width - ENTRY_REMOVE_BTN_W - lockSpace, cursorY);
                removeButton.setWidth(ENTRY_REMOVE_BTN_W);
                removeButton.setHeight(ENTRY_ROW_H);
                removeButton.visible = inside && !expanded;
                removeButton.active = inside && !expanded;
            }
            if (dependency && lockButton != null) {
                int lockX = x + width - DEP_LOCK_SIZE;
                int lockY = cursorY;
                lockButton.setX(lockX);
                lockButton.setY(lockY);
                lockButton.setSize(DEP_LOCK_SIZE, rowHeight);
                lockButton.visible = inside;
                lockButton.active = inside;
            }
            cursorY += rowHeight + ENTRY_ROW_GAP;
        }
        if (dependency) {
            for (int i = rows.size(); i < lockButtons.size(); i++) {
                LockToggleButton lockButton = lockButtons.get(i);
                lockButton.visible = false;
                lockButton.active = false;
            }
        }
        return Math.max(ENTRY_ROW_H, cursorY - y - ENTRY_ROW_GAP);
    }

    private void renderDependencyRowIcons(GuiGraphics gg) {
        if (dependencyEntryBoxes.isEmpty()) return;
        int clipLeft = pxRight + 2;
        int clipRight = pxRight + pw - 2;
        int clipTop = py;
        int clipBottom = py + ph;
        gg.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box == null || !box.visible) continue;
            String dependencyId = safe(box.getValue()).trim();
            if (dependencyId.isBlank()) continue;
            ItemStack icon = dependencyIconStack(dependencyId);
            if (icon.isEmpty()) continue;
            int iconX = box.getX() - DEP_ENTRY_ICON_SPACE + 1;
            int iconY = box.getY() + Math.max(0, (box.getHeight() - Math.round(16f * DEP_ENTRY_ICON_SCALE)) / 2);
            gg.pose().pushPose();
            gg.pose().translate(iconX, iconY, 0.0f);
            gg.pose().scale(DEP_ENTRY_ICON_SCALE, DEP_ENTRY_ICON_SCALE, 1.0f);
            gg.renderItem(icon, 0, 0);
            gg.pose().popPose();
        }
        gg.disableScissor();
    }

    private void setEntryRowsColor(EntryRowKind kind, boolean invalid) {
        for (ScaledMultiLineEditBox box : entryRows(kind)) {
            box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
        }
    }

    private void syncEntryBackingValues() {
        normalizeCommandRewardRows();
        if (questDependenciesBox != null) questDependenciesBox.setValue(entryRowsToRaw(EntryRowKind.DEPENDENCY));
        if (questCompletionBox != null) questCompletionBox.setValue(entryRowsToRaw(EntryRowKind.COMPLETION));
        if (questRewardBox != null) questRewardBox.setValue(entryRowsToRaw(EntryRowKind.REWARD));
        validationSnapshot = null;
        invalidateIdSuggestions();
    }

    private void normalizeCommandRewardRows() {
        if (rewardEntryBoxes.isEmpty()) return;
        boolean wasSyncing = syncingEntryRows;
        syncingEntryRows = true;
        try {
            for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
                if (box == null) continue;
                String raw = safe(box.getValue());
                String normalized = ensureCommandRewardMetadata(raw);
                if (normalized.equals(raw)) continue;
                int cursor = box.getCursorPosition();
                box.setValue(normalized);
                box.setCursorPosition(Math.min(cursor, normalized.length()));
            }
        } finally {
            syncingEntryRows = wasSyncing;
        }
    }

    private String ensureCommandRewardMetadata(String raw) {
        String line = safe(raw).trim();
        if (line.isBlank()) return raw;
        int colon = line.indexOf(':');
        if (colon <= 0) return raw;
        String type = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        if (!"command".equals(type)) return raw;
        String payload = line.substring(colon + 1).trim();
        CommandReward parsed = QuestEditorEntryCodec.parseCommandReward(payload);
        if (parsed.command.isBlank()) return raw;

        boolean hasTitle = hasCommandRewardKey(payload, "title");
        boolean hasIcon = hasCommandRewardKey(payload, "icon");
        if (hasTitle && hasIcon) return raw;

        StringBuilder out = new StringBuilder(line);
        if (!hasTitle) out.append(" | title: \"\"");
        if (!hasIcon) out.append(" | icon: \"\"");
        return out.toString();
    }

    private boolean hasCommandRewardKey(String payload, String key) {
        if (payload == null || payload.isBlank() || key == null || key.isBlank()) return false;
        String keyPrefix = key.toLowerCase(Locale.ROOT) + ":";
        String[] segments = payload.split("\\|");
        for (String segment : segments) {
            String trimmed = safe(segment).trim().toLowerCase(Locale.ROOT);
            if (trimmed.startsWith(keyPrefix)) return true;
        }
        return false;
    }

    private FormField field(String label, AbstractWidget widget) {
        FormField field = new FormField(label, widget);
        if (!allFields.contains(field)) allFields.add(field);
        return field;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("ui.boundless.editor." + key, args);
    }

    private static String trs(String key, Object... args) {
        return tr(key, args).getString();
    }

    private void setError(String msg) {
        statusMessage = msg;
        statusColor = 0xFF8080;
    }

    private void applyDescriptionFormat(String formatCode) {
        if (questDescriptionBox == null) return;
        if ("__undo__".equals(formatCode)) {
            questDescriptionBox.undo();
            questDescriptionBox.setFocused(true);
            markCurrentEditorUnsaved();
            return;
        }
        if ("__redo__".equals(formatCode)) {
            questDescriptionBox.redo();
            questDescriptionBox.setFocused(true);
            markCurrentEditorUnsaved();
            return;
        }
        questDescriptionBox.insertText(formatCode);
        questDescriptionBox.setFocused(true);
        markCurrentEditorUnsaved();
    }

    private boolean shouldWarnForUnsavedChanges(EditorEntry entry) {
        if (entry == null || !hasUnsavedEditorChanges()) return false;
        if ((mode == Mode.PACK_LIST && editorType != EditorType.PACK_OPTIONS) || mode == Mode.PACK_MENU) return false;
        if (!ENTRY_NEW.equals(entry.id) && Objects.equals(selectedEntryId, entry.id)) return false;
        if (pendingDiscardMode == mode && Objects.equals(pendingDiscardEntryId, entry.id)) {
            return false;
        }
        pendingDiscardMode = mode;
        pendingDiscardEntryId = entry.id;
        pendingDiscardEntry = entry;
        statusMessage = "";
        statusColor = 0xFFD080;
        leftList.setSelectedId(selectedEntryId);
        return true;
    }

    private void clearPendingDiscardState() {
        pendingDiscardEntryId = "";
        pendingDiscardMode = null;
        pendingDiscardEntry = null;
    }

    private void markCurrentEditorLoaded(boolean existingEntry) {
        clearPendingDiscardState();
        savedEditorState = currentEditorStateSignature();
    }

    private void markCurrentEditorUnsaved() {
        clearPendingDiscardState();
        savedEditorState = null;
    }

    private void markCurrentEditorSaved() {
        clearPendingDiscardState();
        savedEditorState = currentEditorStateSignature();
    }

    private boolean hasUnsavedEditorChanges() {
        if (editorType == EditorType.NONE) return false;
        String current = currentEditorStateSignature();
        return savedEditorState == null || !savedEditorState.equals(current);
    }

    private String currentEditorStateSignature() {
        SignatureFields fields = new SignatureFields();
        fields.packName = safe(packNameBox == null ? "" : packNameBox.getValue());
        fields.packNamespace = safe(packNamespaceBox == null ? "" : packNamespaceBox.getValue());
        fields.packIconPath = safe(packIconPathBox == null ? "" : packIconPathBox.getValue());
        fields.packDescription = safe(packDescriptionBox == null ? "" : packDescriptionBox.getValue());
        fields.categoryId = safe(catIdBox == null ? "" : catIdBox.getValue());
        fields.categoryName = safe(catNameBox == null ? "" : catNameBox.getValue());
        fields.categoryIcon = safe(catIconBox == null ? "" : catIconBox.getValue());
        fields.categoryDependency = safe(catDependencyBox == null ? "" : catDependencyBox.getValue());
        fields.categoryAutoComplete = catAutoCompleteToggle != null && catAutoCompleteToggle.isOn();
        fields.subCategoryId = safe(subIdBox == null ? "" : subIdBox.getValue());
        fields.subCategoryParent = safe(subCategoryBox == null ? "" : subCategoryBox.getValue());
        fields.subCategoryName = safe(subNameBox == null ? "" : subNameBox.getValue());
        fields.subCategoryIcon = safe(subIconBox == null ? "" : subIconBox.getValue());
        fields.subCategoryDefaultOpen = subDefaultOpenToggle != null && subDefaultOpenToggle.isOn();
        fields.questId = safe(questIdBox == null ? "" : questIdBox.getValue());
        fields.questOrderToken = safe(questOrderToken);
        fields.questName = safe(questNameBox == null ? "" : questNameBox.getValue());
        fields.questIcon = safe(questIconBox == null ? "" : questIconBox.getValue());
        fields.questDescription = safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue());
        fields.questCategory = safe(questCategoryBox == null ? "" : questCategoryBox.getValue());
        fields.questSubCategory = safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue());
        fields.questDependencies = safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue());
        fields.questDependencyLock = dependencyLockState();
        fields.questOptional = questOptionalToggle != null && questOptionalToggle.isOn();
        fields.questRepeatable = questRepeatableToggle != null && questRepeatableToggle.isOn();
        fields.questAutoComplete = questAutoCompleteToggle != null && questAutoCompleteToggle.isOn();
        fields.questHiddenUnderDependency = questHiddenUnderDependencyToggle != null && questHiddenUnderDependencyToggle.isOn();
        fields.questCompletion = safe(questCompletionBox == null ? "" : questCompletionBox.getValue());
        fields.questReward = safe(questRewardBox == null ? "" : questRewardBox.getValue());
        fields.loadedQuestType = safe(loadedQuestType);
        return QuestEditorStateSignatures.signature(editorType, fields);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private void attachIdSanitizer(EditBox box, boolean commaSeparated) {
        if (box == null) return;
        box.setResponder(value -> {
            if (suppressIdSanitizer) return;
            String normalized = QuestEditorNaming.normalizeIdInput(value, commaSeparated);
            if (normalized.equals(value)) return;
            int cursor = box.getCursorPosition();
            suppressIdSanitizer = true;
            box.setValue(normalized);
            box.setCursorPosition(Math.min(cursor, normalized.length()));
            box.setHighlightPos(box.getCursorPosition());
            suppressIdSanitizer = false;
        });
    }

    private void attachPackNameSanitizer(EditBox box) {
        if (box == null) return;
        box.setResponder(value -> {
            if (suppressIdSanitizer) return;
            String normalized = QuestEditorNaming.normalizePackName(value);
            if (normalized.equals(value)) return;
            int cursor = box.getCursorPosition();
            suppressIdSanitizer = true;
            box.setValue(normalized);
            box.setCursorPosition(Math.min(cursor, normalized.length()));
            box.setHighlightPos(box.getCursorPosition());
            suppressIdSanitizer = false;
        });
    }

    private boolean parseBool(String raw, boolean def) {
        if (raw == null) return def;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.isBlank()) return def;
        if (v.equals("true")) return true;
        if (v.equals("false")) return false;
        return def;
    }

    private ItemStack dependencyIconStack(String dependencyId) {
        String id = safe(dependencyId).trim();
        if (id.isBlank()) return ItemStack.EMPTY;
        String iconId = safe(questIconByIdCache.get(id)).trim();
        if (!iconId.isBlank()) {
            ItemStack stack = QuestEditorItemIcons.iconStackFromId(iconId);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private String questOrderTokenForSave(String questId) {
        String token = safe(questOrderToken).trim();
        if (!token.isBlank()) return token;

        String id = safe(questId).trim();
        if (id.isBlank()) return "";

        if (editingPath != null) {
            String fromPath = QuestEditorNaming.questOrderTokenFromPath(editingPath);
            if (!fromPath.isBlank()) return fromPath;
        }

        return QuestEditorNaming.nextQuestOrderToken(currentPack);
    }

    private ScreenState captureState() {
        if (leftList == null) return null;
        syncEntryBackingValues();
        ScreenState state = new ScreenState();
        state.mode = mode;
        state.editorType = editorType;
        state.currentPack = currentPack;
        state.selectedEntryId = selectedEntryId;
        state.editingPath = editingPath;
        state.loadedQuestType = loadedQuestType;
        state.questOrderToken = questOrderToken;
        state.editorScroll = editorScroll;
        state.leftScroll = leftList.getScrollY();
        state.statusMessage = statusMessage;
        state.statusColor = statusColor;
        state.deleteConfirmArmed = deleteConfirmArmed;
        state.savedEditorState = savedEditorState;
        state.pendingDiscardEntryId = pendingDiscardEntryId;
        state.pendingDiscardMode = pendingDiscardMode;
        state.questSearchQuery = questSearchQuery;

        state.packName = safe(packNameBox == null ? "" : packNameBox.getValue());
        state.packNamespace = safe(packNamespaceBox == null ? "" : packNamespaceBox.getValue());
        state.packIconPath = safe(packIconPathBox == null ? "" : packIconPathBox.getValue());
        state.packDescription = safe(packDescriptionBox == null ? "" : packDescriptionBox.getValue());

        state.catId = safe(catIdBox == null ? "" : catIdBox.getValue());
        state.catName = safe(catNameBox == null ? "" : catNameBox.getValue());
        state.catIcon = safe(catIconBox == null ? "" : catIconBox.getValue());
        state.catDependency = safe(catDependencyBox == null ? "" : catDependencyBox.getValue());
        state.catAutoComplete = catAutoCompleteToggle != null && catAutoCompleteToggle.isOn();

        state.subId = safe(subIdBox == null ? "" : subIdBox.getValue());
        state.subCategory = safe(subCategoryBox == null ? "" : subCategoryBox.getValue());
        state.subName = safe(subNameBox == null ? "" : subNameBox.getValue());
        state.subIcon = safe(subIconBox == null ? "" : subIconBox.getValue());
        state.subDefaultOpen = subDefaultOpenToggle != null && subDefaultOpenToggle.isOn();

        state.questId = safe(questIdBox == null ? "" : questIdBox.getValue());
        state.questName = safe(questNameBox == null ? "" : questNameBox.getValue());
        state.questIcon = safe(questIconBox == null ? "" : questIconBox.getValue());
        state.questDescription = safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue());
        state.questCategory = safe(questCategoryBox == null ? "" : questCategoryBox.getValue());
        state.questSubCategory = safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue());
        state.questDependencies = safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue());
        state.questLockAfterDependency = dependencyLockState();
        state.questOptional = questOptionalToggle != null && questOptionalToggle.isOn();
        state.questRepeatable = questRepeatableToggle != null && questRepeatableToggle.isOn();
        state.questAutoComplete = questAutoCompleteToggle != null && questAutoCompleteToggle.isOn();
        state.questHiddenUnderDependency = questHiddenUnderDependencyToggle != null && questHiddenUnderDependencyToggle.isOn();
        state.questCompletion = safe(questCompletionBox == null ? "" : questCompletionBox.getValue());
        state.questReward = safe(questRewardBox == null ? "" : questRewardBox.getValue());
        return state;
    }

    private void restoreState(ScreenState state) {
        if (state == null) {
            setMode(Mode.PACK_LIST);
            return;
        }
        String preservedStatusMessage = statusMessage;
        int preservedStatusColor = statusColor;
        currentPack = state.currentPack;
        selectedEntryId = state.selectedEntryId == null ? "" : state.selectedEntryId;
        mode = state.mode == null ? Mode.PACK_LIST : state.mode;
        questOrderToken = state.questOrderToken == null ? "" : state.questOrderToken;
        if (preservedStatusMessage == null || preservedStatusMessage.isBlank()) {
            statusMessage = state.statusMessage == null ? "" : state.statusMessage;
            statusColor = state.statusColor;
        } else {
            statusMessage = preservedStatusMessage;
            statusColor = preservedStatusColor;
        }
        deleteConfirmArmed = state.deleteConfirmArmed;
        savedEditorState = state.savedEditorState;
        pendingDiscardEntryId = state.pendingDiscardEntryId == null ? "" : state.pendingDiscardEntryId;
        pendingDiscardMode = state.pendingDiscardMode;
        questSearchQuery = state.questSearchQuery == null ? "" : state.questSearchQuery;
        if (questSearchBox != null) questSearchBox.setValue(questSearchQuery);
        updateDeleteButtonTexture();
        refreshLeftList();
        leftList.setScrollY(state.leftScroll);

        switch (state.editorType) {
            case PACK_CREATE -> {
                showPackCreate();
                packNameBox.setValue(state.packName);
                packNamespaceBox.setValue(state.packNamespace);
            }
            case PACK_OPTIONS -> {
                if (currentPack != null) {
                    showPackOptions(currentPack);
                    packNameBox.setValue(state.packName);
                    packIconPathBox.setValue(state.packIconPath);
                    packDescriptionBox.setValue(state.packDescription);
                } else {
                    clearEditor();
                }
            }
            case CATEGORY -> {
                CategoryData data = new CategoryData();
                data.id = state.catId;
                data.name = state.catName;
                data.icon = state.catIcon;
                data.dependency = state.catDependency;
                data.autoComplete = state.catAutoComplete ? "true" : "false";
                showCategoryEditor(data, state.editingPath);
            }
            case SUBCATEGORY -> {
                SubCategoryData data = new SubCategoryData();
                data.id = state.subId;
                data.category = state.subCategory;
                data.name = state.subName;
                data.icon = state.subIcon;
                data.defaultOpen = state.subDefaultOpen ? "true" : "false";
                showSubCategoryEditor(data, state.editingPath);
            }
            case QUEST -> {
                QuestEntryData data = new QuestEntryData();
                data.id = state.questId;
                data.name = state.questName;
                data.icon = state.questIcon;
                data.description = state.questDescription;
                data.category = state.questCategory;
                data.subCategory = state.questSubCategory;
                data.dependencies = state.questDependencies;
                data.lockAfterDependency = state.questLockAfterDependency ? "true" : "false";
                data.optional = state.questOptional ? "true" : "false";
                data.repeatable = state.questRepeatable ? "true" : "false";
                data.autoComplete = state.questAutoComplete ? "true" : "false";
                data.hiddenUnderDependency = state.questHiddenUnderDependency ? "true" : "false";
                data.type = state.loadedQuestType;
                data.completionJson = state.questCompletion;
                data.rewardJson = state.questReward;
                showQuestEditor(data, state.editingPath);
                setEntryRowsFromRaw(EntryRowKind.COMPLETION, safe(state.questCompletion));
                setEntryRowsFromRaw(EntryRowKind.REWARD, safe(state.questReward));
            }
            case NONE -> clearEditor();
        }

        loadedQuestType = state.loadedQuestType == null ? "" : state.loadedQuestType;
        editingPath = state.editingPath;
        editorScroll = state.editorScroll;
        updateBackButtonVisibility();
    }

    private String optString(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) return def;
        return obj.get(key).getAsString();
    }

    private String optStringFlexible(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key)) return def;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return def;
        if (el.isJsonPrimitive()) return el.getAsString();
        return def;
    }

    private static void stashPendingInitState(ScreenState state) {
        pendingInitState = state;
        pendingInitUntil = Util.getMillis() + PENDING_INIT_TTL_MS;
    }

    private static ScreenState takePendingInitState() {
        if (pendingInitState == null) return null;
        long now = Util.getMillis();
        if (now > pendingInitUntil) {
            pendingInitState = null;
            pendingInitUntil = 0L;
            return null;
        }
        ScreenState state = pendingInitState;
        pendingInitState = null;
        pendingInitUntil = 0L;
        return state;
    }

    private static void clearPendingInitState() {
        pendingInitState = null;
        pendingInitUntil = 0L;
    }
    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        pendingEditorTooltip = List.of();
        updateLeftPaneLayout();
        if (editorType == EditorType.PACK_CREATE || editorType == EditorType.PACK_OPTIONS) {
        }
        updateDeleteHold(mouseX, mouseY);
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        renderEditorFields(gg, mouseX, mouseY);
        renderPanelHeader(gg, leftX, panelHeaderTitle());
        renderSideTabs(gg, mouseX, mouseY);

        boolean leftVisible = leftList != null && leftList.visible;
        boolean backVisible = backButton != null && backButton.visible;
        boolean saveVisible = saveButton != null && saveButton.visible;
        boolean exportVisible = exportPackButton != null && exportPackButton.visible;
        boolean duplicateVisible = duplicateButton != null && duplicateButton.visible;
        boolean deleteQuestVisible = deleteQuestButton != null && deleteQuestButton.visible;
        boolean questSearchVisible = questSearchBox != null && questSearchBox.visible;
        boolean createEntryVisible = createEntryButton != null && createEntryButton.visible;
        if (leftList != null) leftList.visible = false;
        if (backButton != null) backButton.visible = false;
        if (saveButton != null) saveButton.visible = false;
        if (exportPackButton != null) exportPackButton.visible = false;
        if (duplicateButton != null) duplicateButton.visible = false;
        if (deleteQuestButton != null) deleteQuestButton.visible = false;
        if (questSearchBox != null) questSearchBox.visible = false;
        if (createEntryButton != null) createEntryButton.visible = false;

        gg.enableScissor(pxRight, py, pxRight + pw, py + ph);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.disableScissor();

        if (leftList != null) leftList.visible = leftVisible;
        if (backButton != null) backButton.visible = backVisible;
        if (saveButton != null) saveButton.visible = saveVisible;
        if (exportPackButton != null) exportPackButton.visible = exportVisible;
        if (duplicateButton != null) duplicateButton.visible = duplicateVisible;
        if (deleteQuestButton != null) deleteQuestButton.visible = deleteQuestVisible;
        if (questSearchBox != null) questSearchBox.visible = questSearchVisible;
        if (createEntryButton != null) createEntryButton.visible = createEntryVisible;

        if (leftList != null && leftList.visible) leftList.render(gg, mouseX, mouseY, partialTick);
        if (backButton != null && backButton.visible) backButton.render(gg, mouseX, mouseY, partialTick);
        if (saveButton != null && saveButton.visible) saveButton.render(gg, mouseX, mouseY, partialTick);
        if (exportPackButton != null && exportPackButton.visible) exportPackButton.render(gg, mouseX, mouseY, partialTick);
        if (duplicateButton != null && duplicateButton.visible) duplicateButton.render(gg, mouseX, mouseY, partialTick);
        if (deleteQuestButton != null && deleteQuestButton.visible) deleteQuestButton.render(gg, mouseX, mouseY, partialTick);
        renderDeleteHoldProgress(gg);
        if (questSearchBox != null && questSearchBox.visible) questSearchBox.render(gg, mouseX, mouseY, partialTick);
        if (createEntryButton != null && createEntryButton.visible) createEntryButton.render(gg, mouseX, mouseY, partialTick);
        if (createPackButton != null && createPackButton.visible) createPackButton.render(gg, mouseX, mouseY, partialTick);
        if (importQuestPackButton != null && importQuestPackButton.visible) importQuestPackButton.render(gg, mouseX, mouseY, partialTick);
        renderIconOverlays(gg);
        if (!isItemPickerOpen() && leftList != null) leftList.renderHoverTooltipOnTop(gg);
        if (!isItemPickerOpen()) renderTabTooltip(gg, mouseX, mouseY);
        renderTypeMenu(gg, mouseX, mouseY);
        renderDropdownMenu(gg, mouseX, mouseY);
        renderItemPicker(gg, mouseX, mouseY);

        if (!statusMessage.isBlank()) {
            int msgW = font.width(statusMessage);
            int msgX = (this.width - msgW) / 2;
            gg.drawString(font, statusMessage, msgX, topY + PANEL_H + 8, statusColor, false);
        }
        renderSavedState(gg);
        renderIdSuggestions(gg, mouseX, mouseY);
        renderUnsavedChangesPopup(gg, mouseX, mouseY);
        renderPendingEditorTooltip(gg);
    }

    private void queueEditorTooltip(Component tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.getString().isBlank()) return;
        queueEditorTooltip(List.of(tooltip), mouseX, mouseY);
    }

    private void queueEditorTooltip(List<Component> tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.isEmpty()) return;
        pendingEditorTooltip = tooltip;
        pendingEditorTooltipX = mouseX;
        pendingEditorTooltipY = mouseY;
    }

    private void renderPendingEditorTooltip(GuiGraphics gg) {
        if (pendingEditorTooltip == null || pendingEditorTooltip.isEmpty()) return;
        gg.pose().pushPose();
        gg.pose().translate(0f, 0f, 650f);
        gg.renderComponentTooltip(font, pendingEditorTooltip, pendingEditorTooltipX, pendingEditorTooltipY);
        gg.pose().popPose();
        pendingEditorTooltip = List.of();
    }

    private void updateLeftPaneLayout() {
        if (leftList != null) {
            leftList.setBounds(pxLeft, py, pw, listH);
        }
        if (backButton != null) {
            backButton.setX(leftX - TAB_W + BACK_TAB_X_OFFSET);
            backButton.setY(topY + PANEL_H - BACK_TAB_H - BACK_TAB_BOTTOM_MARGIN);
        }
        int barY = py + ph + (BOTTOM_BAR_H - 20) / 2;
        if (createPackButton != null) {
            createPackButton.setX(pxLeft + 2);
            createPackButton.setY(barY);
            createPackButton.setWidth(pw - SMALL_BTN_SIZE - SMALL_BTN_GAP - 4);
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
            createEntryButton.setX(pxLeft + 2);
            createEntryButton.setY(barY);
            createEntryButton.visible = createEntryVisible;
            createEntryButton.active = createEntryVisible;
        }
        if (questSearchBox != null) {
            int searchX = pxLeft + 2;
            int searchW = pw - 4;
            if (mode == Mode.QUEST_LIST && createEntryVisible) {
                searchX += SMALL_BTN_SIZE + SMALL_BTN_GAP;
                searchW -= SMALL_BTN_SIZE + SMALL_BTN_GAP;
            }
            questSearchBox.setX(searchX);
            questSearchBox.setY(barY);
            questSearchBox.setWidth(searchW);
        }
        layoutTabButtons();
    }

    private boolean canCreateEntryInCurrentMode() {
        return mode == Mode.CATEGORY_LIST || mode == Mode.SUBCATEGORY_LIST || mode == Mode.QUEST_LIST;
    }

    private void layoutTabButtons() {
        int x = leftX - TAB_W + SIDE_TAB_X_OFFSET;
        int startY = py + 6;
        layoutTabButton(categoriesTabButton, x, startY);
        layoutTabButton(subCategoriesTabButton, x, startY + TAB_H + TAB_GAP);
        layoutTabButton(questsTabButton, x, startY + (TAB_H + TAB_GAP) * 2);
    }

    private void layoutTabButton(EditorTabButton button, int x, int y) {
        if (button == null) return;
        button.setX(x);
        button.setY(y);
        boolean visible = currentPack != null && (mode == Mode.CATEGORY_LIST || mode == Mode.SUBCATEGORY_LIST || mode == Mode.QUEST_LIST);
        button.visible = visible;
        button.active = visible;
    }

    private void renderSideTabs(GuiGraphics gg, int mouseX, int mouseY) {
        if (categoriesTabButton != null && categoriesTabButton.visible) categoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible) subCategoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (questsTabButton != null && questsTabButton.visible) questsTabButton.render(gg, mouseX, mouseY, 0f);
    }

    private void renderTabTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        if (categoriesTabButton != null && categoriesTabButton.visible && categoriesTabButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal(categoriesTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible && subCategoriesTabButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal(subCategoriesTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (questsTabButton != null && questsTabButton.visible && questsTabButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal(questsTabButton.tooltip()), mouseX, mouseY);
            return;
        }
        if (importQuestPackButton != null && importQuestPackButton.visible && importQuestPackButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr("tooltip.import_questpacks"), mouseX, mouseY);
            return;
        }
        if (createEntryButton != null && createEntryButton.visible && createEntryButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, createEntryTooltip(), mouseX, mouseY);
            return;
        }
        if (duplicateButton != null && duplicateButton.visible && duplicateButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr("tooltip.duplicate"), mouseX, mouseY);
            return;
        }
        if (deleteQuestButton != null && deleteQuestButton.visible && deleteQuestButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, deleteConfirmArmed ? tr("tooltip.hold_delete") : tr("tooltip.delete"), mouseX, mouseY);
        }
        if (backButton != null && backButton.visible && backButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr("tooltip.back"), mouseX, mouseY);
        }
    }

    private boolean isUnsavedChangesPopupOpen() {
        return pendingDiscardMode != null && !pendingDiscardEntryId.isBlank();
    }

    private void renderUnsavedChangesPopup(GuiGraphics gg, int mouseX, int mouseY) {
        if (!isUnsavedChangesPopupOpen()) return;
        String message = trs("popup.unsaved_changes");
        String save = trs("popup.save");
        String discard = trs("popup.discard");
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

    private void renderNineSlice(GuiGraphics gg, ResourceLocation texture, int x, int y, int width, int height, int texW, int texH, int slice) {
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

    private boolean isInsideText(double mouseX, double mouseY, int x, int y, String text) {
        return mouseX >= x && mouseX <= x + font.width(text) && mouseY >= y && mouseY <= y + font.lineHeight;
    }

    private void renderPanelHeader(GuiGraphics gg, int panelX, String title) {
        String text = safe(title);
        if (text.isBlank()) return;
        int textW = font.width(text);
        int headerW = Math.max(22, textW + 10);
        int x = panelX + 5;
        int y = topY - 7;
        renderThreeSliceHeader(gg, x, y, headerW);
        gg.drawString(font, text, x + (headerW - textW) / 2, y + 4, 0x404040, false);
    }

    private void renderThreeSliceHeader(GuiGraphics gg, int x, int y, int width) {
        int middleW = Math.max(0, width - HEADER_SLICE * 2);
        gg.blit(HEADER_TEX, x, y, 0, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
        if (middleW > 0) {
            for (int i = 0; i < middleW; i++) {
                gg.blit(HEADER_TEX, x + HEADER_SLICE + i, y, HEADER_SLICE, 0, 1, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
            }
        }
        gg.blit(HEADER_TEX, x + HEADER_SLICE + middleW, y, HEADER_TEX_W - HEADER_SLICE, 0, HEADER_SLICE, HEADER_TEX_H, HEADER_TEX_W, HEADER_TEX_H);
    }

    private String panelHeaderTitle() {
        return switch (mode) {
            case PACK_LIST -> trs("header.quest_packs");
            case PACK_CREATE -> trs("header.create_pack");
            case CATEGORY_LIST -> trs("header.categories");
            case SUBCATEGORY_LIST -> trs("header.subcategories");
            case QUEST_LIST -> trs("header.quests");
            case PACK_MENU -> trs("header.quest_packs");
        };
    }

    private void renderSavedState(GuiGraphics gg) {
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

    private void renderIconOverlays(GuiGraphics gg) {
        int clipLeft = pxRight + 2;
        int clipRight = pxRight + pw - 2;
        int clipTop = py;
        int clipBottom = py + ph;

        gg.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        renderIconOverlay(gg, packIconPathBox);
        renderIconOverlay(gg, questIconBox);
        renderIconOverlay(gg, catIconBox);
        renderIconOverlay(gg, subIconBox);
        // no extra reward fields
        gg.disableScissor();
    }

    private void renderIconOverlay(GuiGraphics gg, EditBox box) {
        if (box == null || !box.visible) return;

        ItemStack stack = QuestEditorItemIcons.iconStackFromId(box.getValue());
        if (!stack.isEmpty()) {
            int iconX = box.getWidth() <= 20 ? box.getX() + 2 : box.getX() + box.getWidth() - 18;
            int iconY = box.getY() + (box.getHeight() - 16) / 2;
            gg.renderItem(stack, iconX, iconY);
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

    private void renderIdSuggestions(GuiGraphics gg, int mouseX, int mouseY) {
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

    private void updateIdSuggestions() {
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

    private void clearIdSuggestions() {
        idSuggestionField = null;
        idSuggestionMultiLineField = null;
        lastIdSuggestionSource = null;
        lastIdSuggestionPrefix = "";
        idSuggestionsDirty = true;
        activeIdSuggestions.clear();
    }

    private void invalidateIdSuggestions() {
        idSuggestionsDirty = true;
    }

    private EditBox focusedIdSuggestionField() {
        if (packIconPathBox != null && packIconPathBox.visible && packIconPathBox.isFocused()) return packIconPathBox;
        if (questIconBox != null && questIconBox.visible && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.visible && catIconBox.isFocused()) return catIconBox;
        if (subIconBox != null && subIconBox.visible && subIconBox.isFocused()) return subIconBox;
        if (catDependencyBox != null && catDependencyBox.visible && catDependencyBox.isFocused()) return catDependencyBox;
        return null;
    }

    private ScaledMultiLineEditBox focusedMultiIdSuggestionField() {
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

    private String idSuggestionPrefix(EditBox field) {
        if (field == null) return "";
        return safe(field.getValue()).trim();
    }

    private List<String> idSuggestionValuesForField(EditBox field) {
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

    private List<String> idSuggestionValuesForMultiField(ScaledMultiLineEditBox field) {
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

    private boolean isCompletionEntryField(ScaledMultiLineEditBox field) {
        return field == questCompletionBox || completionEntryBoxes.contains(field);
    }

    private boolean isRewardEntryField(ScaledMultiLineEditBox field) {
        return field == questRewardBox || rewardEntryBoxes.contains(field);
    }

    private boolean isDependencyEntryField(ScaledMultiLineEditBox field) {
        return field == questDependenciesBox || dependencyEntryBoxes.contains(field);
    }

    private EntryRowKind rowKindForField(ScaledMultiLineEditBox field) {
        if (field == null) return null;
        if (completionEntryBoxes.contains(field)) return EntryRowKind.COMPLETION;
        if (rewardEntryBoxes.contains(field)) return EntryRowKind.REWARD;
        if (dependencyEntryBoxes.contains(field)) return EntryRowKind.DEPENDENCY;
        return null;
    }

    private String multiLineSuggestionPrefix(ScaledMultiLineEditBox field) {
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

    private boolean matchesIdPrefix(String suggestion, String prefix) {
        if (suggestion == null || prefix == null) return false;
        String lowSuggestion = suggestion.toLowerCase(Locale.ROOT);
        String lowPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowSuggestion.startsWith(lowPrefix)) return true;
        int colon = lowSuggestion.indexOf(':');
        return colon >= 0 && colon + 1 < lowSuggestion.length()
                && lowSuggestion.substring(colon + 1).startsWith(lowPrefix);
    }

    private boolean clickIdSuggestion(double mouseX, double mouseY) {
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

    private boolean scrollIdSuggestions(double mouseX, double mouseY, double scrollY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null || activeIdSuggestions.isEmpty()) return false;
        if (mouseX < bounds.x || mouseX > bounds.x + bounds.w || mouseY < bounds.y || mouseY > bounds.y + bounds.h) return false;
        int max = Math.max(0, activeIdSuggestions.size() - ID_SUGGESTION_VISIBLE_ROWS);
        if (max <= 0) return true;
        int next = idSuggestionScroll - (int) Math.signum(scrollY);
        idSuggestionScroll = Mth.clamp(next, 0, max);
        return true;
    }

    private SuggestionBounds suggestionBounds() {
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

    private void applyIdSuggestion(EditBox field, String suggestion) {
        if (field == null || suggestion == null) return;
        field.setValue(suggestion);
        field.setCursorPosition(field.getValue().length());
        field.setHighlightPos(field.getCursorPosition());
        field.setFocused(true);
        updateIdSuggestions();
    }

    private void applyMultiLineIdSuggestion(ScaledMultiLineEditBox field, String suggestion) {
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

    private MultiLineEntryContext parseMultiLineEntryContext(ScaledMultiLineEditBox field) {
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

    private EditBox embeddedIconBoxForDisplayField(AbstractWidget widget) {
        if (widget == packNameBox) return packIconPathBox;
        if (widget == catNameBox) return catIconBox;
        if (widget == subNameBox) return subIconBox;
        if (widget == questNameBox) return questIconBox;
        return null;
    }

    private void renderEditorFields(GuiGraphics gg, int mouseX, int mouseY) {
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
                    } else {
                        eb.setWidth(widgetWidth);
                    }
                } else if (field.widget instanceof ScaledMultiLineEditBox mb) {
                    mb.setWidth(widgetWidth);
                } else if (field.widget instanceof ToggleButton tb) {
                    tb.setSize(TOGGLE_SIZE, TOGGLE_SIZE);
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
                EditBox embeddedIcon = embeddedIconBoxForDisplayField(field.widget);
                if (embeddedIcon != null) {
                    embeddedIcon.setX(boxX);
                    embeddedIcon.setY(boxY);
                    embeddedIcon.setWidth(20);
                    embeddedIcon.visible = inside;
                    embeddedIcon.active = inside;
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
                    String lockTooltip = dependencyLockState()
                            ? "Quest is locked after dependency completed"
                            : "quest is locked until dependency completed";
                    if (!isItemPickerOpen()) queueEditorTooltip(Component.literal(lockTooltip), mouseX, mouseY);
                    break;
                }
            }
            yCursor += font.lineHeight + FIELD_LABEL_GAP + widgetHeight + FIELD_ROW_GAP;
            if (field.widget == questDescriptionBox) {
                yCursor += FORMAT_BAR_GAP + FORMAT_BAR_H;
            }
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

    private void renderEntryComponentOverlays(GuiGraphics gg) {
        int clipLeft = pxRight + 2;
        int clipRight = pxRight + pw - 2;
        int clipTop = py;
        int clipBottom = py + ph;
        gg.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        renderEntryComponentOverlays(gg, completionEntryBoxes);
        renderEntryComponentOverlays(gg, rewardEntryBoxes);
        gg.disableScissor();
    }

    private void renderEntryComponentOverlays(GuiGraphics gg, List<ScaledMultiLineEditBox> boxes) {
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

    private boolean scrollEntryFields(double mouseX, double mouseY, double scrollY) {
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (ScaledMultiLineEditBox box : completionEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) {
            if (box != null && box.visible && box.isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, 0, scrollY)) return true;
        }
        return false;
    }

    private boolean isInsideSuggestionBox(double mouseX, double mouseY) {
        SuggestionBounds bounds = suggestionBounds();
        if (bounds == null) return false;
        return mouseX >= bounds.x && mouseX <= bounds.x + bounds.w
                && mouseY >= bounds.y && mouseY <= bounds.y + bounds.h;
    }

    private boolean isInsideSuggestionTargetField(double mouseX, double mouseY) {
        return isInsideBox(packIconPathBox, mouseX, mouseY)
                || isInsideBox(catIconBox, mouseX, mouseY)
                || isInsideBox(subIconBox, mouseX, mouseY)
                || isInsideBox(questIconBox, mouseX, mouseY)
                || isInsideBox(catDependencyBox, mouseX, mouseY)
                || isInsideBox(subCategoryBox, mouseX, mouseY)
                || isInsideBox(questCategoryBox, mouseX, mouseY)
                || isInsideBox(questSubCategoryBox, mouseX, mouseY)
                || isInsideMultiBox(questCompletionBox, mouseX, mouseY)
                || isInsideMultiBox(questRewardBox, mouseX, mouseY)
                || isInsideMultiBox(questDependenciesBox, mouseX, mouseY)
                || anyVisibleMultiBoxContains(dependencyEntryBoxes, mouseX, mouseY)
                || anyVisibleMultiBoxContains(completionEntryBoxes, mouseX, mouseY)
                || anyVisibleMultiBoxContains(rewardEntryBoxes, mouseX, mouseY);
    }

    private boolean isInsideBox(EditBox box, double mouseX, double mouseY) {
        return box != null && box.visible
                && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
    }

    private EditBox clickedVisibleIconBox(double mouseX, double mouseY) {
        EditBox[] boxes = {packIconPathBox, catIconBox, subIconBox, questIconBox};
        for (EditBox box : boxes) {
            if (isInsideBox(box, mouseX, mouseY)) return box;
        }
        return null;
    }

    private boolean isInsideMultiBox(ScaledMultiLineEditBox box, double mouseX, double mouseY) {
        return box != null && box.visible
                && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
    }

    private boolean anyVisibleMultiBoxContains(List<ScaledMultiLineEditBox> boxes, double mouseX, double mouseY) {
        if (boxes == null || boxes.isEmpty()) return false;
        for (ScaledMultiLineEditBox box : boxes) {
            if (isInsideMultiBox(box, mouseX, mouseY)) return true;
        }
        return false;
    }

    private int contentHeight() {
        int total = 0;
        for (FormField field : activeFields) {
            total += font.lineHeight + FIELD_LABEL_GAP + fieldHeight(field) + FIELD_ROW_GAP;
            if (editorType == EditorType.PACK_OPTIONS && field.widget == packNameBox) {
                total += 20 + FIELD_ROW_GAP;
            }
        }
        return total;
    }

    private int fieldHeight(FormField field) {
        if (field == null || field.widget == null) return 0;
        if (field.widget == questDependenciesBox) return entryRowsHeight(EntryRowKind.DEPENDENCY);
        if (field.widget == questCompletionBox) return entryRowsHeight(EntryRowKind.COMPLETION);
        if (field.widget == questRewardBox) return entryRowsHeight(EntryRowKind.REWARD);
        if (field.widget == questOptionalToggle) return TOGGLE_SIZE + INLINE_FLAG_LABEL_H + 3;
        if (field.widget instanceof ScaledMultiLineEditBox mb) {
            int baseHeight = computeMultilineHeight(mb, BOX_H_TALL);
            if (field.widget == questDescriptionBox) {
                return baseHeight + FORMAT_BAR_GAP + FORMAT_BAR_H;
            }
            return baseHeight;
        }
        return field.widget.getHeight();
    }

    private void updateDynamicFieldSizes() {
        for (FormField field : activeFields) {
            if (field.widget == questDependenciesBox || field.widget == questCompletionBox || field.widget == questRewardBox) continue;
            if (field.widget instanceof ScaledMultiLineEditBox mb) {
                mb.setWidth(pw - 4);
                mb.setHeight(computeMultilineHeight(mb, BOX_H_TALL));
            }
        }
    }

    private void updateInvalidFieldStyles() {
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

    private ValidationSnapshot currentValidationSnapshot() {
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
        request.questEntries = cachedQuestEntriesOrLoad();
        request.errorHandler = this::setError;
        validationSnapshot = QuestEditorValidation.currentSnapshot(request);
        return validationSnapshot;
    }

    private void setIdFieldColor(EditBox box, boolean invalid) {
        if (box == null) return;
        if (isDropdownField(box)) {
            applyDropdownTextColor(box, invalid);
            return;
        }
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    private void applyDropdownTextColor(EditBox box, boolean invalid) {
        if (box == null) return;
        int color = invalid ? INVALID_INPUT_TEXT_COLOR : DROPDOWN_INPUT_TEXT_COLOR;
        box.setTextColor(color);
        box.setTextColorUneditable(color);
    }

    private void setJsonFieldColor(ScaledMultiLineEditBox box, boolean invalid) {
        if (box == null) return;
        box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
    }

    private String tooltipForField(FormField field) {
        if (field == null || field.widget == null) return "";
        if (field.widget == questIdBox && isInvalidQuestIdField()) {
            return isDuplicateQuestId(safe(questIdBox.getValue()).trim()) ? "Quest ID already exists" : "Quest ID required";
        }
        if (field.widget == questNameBox && isInvalidQuestNameField()) return "Quest name required";
        if (field.widget == catDependencyBox && isInvalidCategoryDependency()) return "Invalid quest ID";
        if (field.widget == subCategoryBox && isInvalidParentCategory()) return INVALID_ID_TOOLTIP;
        if (field.widget == questCategoryBox && isInvalidQuestCategory()) {
            return dropdownBoxValue(questCategoryBox).isBlank() ? "Quest category required" : INVALID_ID_TOOLTIP;
        }
        if (field.widget == questSubCategoryBox && isInvalidQuestSubCategory()) return INVALID_ID_TOOLTIP;
        if (field.widget == questDependenciesBox && isInvalidQuestDependencies()) return INVALID_ID_TOOLTIP;
        return field.tooltip == null ? "" : field.tooltip;
    }

    private boolean isInvalidQuestIdField() {
        if (editorType != EditorType.QUEST) return false;
        String questId = safe(questIdBox == null ? "" : questIdBox.getValue()).trim();
        return questId.isBlank() || isDuplicateQuestId(questId);
    }

    private boolean isInvalidQuestNameField() {
        if (editorType != EditorType.QUEST) return false;
        return safe(questNameBox == null ? "" : questNameBox.getValue()).trim().isBlank();
    }

    private int renderInlineQuestFlags(GuiGraphics gg, int mouseX, int mouseY, int x, int y, int width, int clipTop, int clipBottom) {
        ToggleButton[] toggles = new ToggleButton[] { questOptionalToggle, questRepeatableToggle, questAutoCompleteToggle, questHiddenUnderDependencyToggle };
        String[] titles = new String[] { "Optional", "Repeatable", "Redeem", "Hidden" };
        String[][] tooltips = new String[][] {
                { "Optional", "Players can skip this quest without blocking progression." },
                { "Repeatable", "Players can restart this quest after claiming it." },
                { "Redeem", "Automatically claims this quest when it becomes ready." },
                { "Hidden Under Dependency", "Keeps this quest hidden until its dependencies are met." }
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

    private boolean isInvalidCategoryDependency() {
        return isMissingQuestId(safe(catDependencyBox == null ? "" : catDependencyBox.getValue()));
    }

    private boolean isInvalidParentCategory() {
        return isMissingCategoryId(dropdownBoxValue(subCategoryBox));
    }

    private boolean isInvalidQuestCategory() {
        String value = dropdownBoxValue(questCategoryBox);
        return value.isBlank() || isMissingCategoryId(value);
    }

    private boolean isInvalidQuestSubCategory() {
        return isMissingSubCategoryId(dropdownBoxValue(questSubCategoryBox));
    }

    private boolean isInvalidQuestDependencies() {
        return QuestEditorValidation.computeInvalidQuestDependencies(
                String.join("\n", collectDependencyEntries()),
                currentPack != null,
                questPackDependencySuggestionCache
        );
    }

    private boolean isInvalidCompletionEntries() {
        String raw = safe(questCompletionBox == null ? "" : questCompletionBox.getValue()).trim();
        if (raw.isBlank()) return false;
        return QuestEditorEntryCodec.parseCompletionEntries(raw, false, this::setError) == null;
    }

    private boolean isInvalidRewardEntries() {
        String raw = safe(questRewardBox == null ? "" : questRewardBox.getValue()).trim();
        if (raw.isBlank()) return false;
        return QuestEditorEntryCodec.parseRewardEntries(raw, false, this::setError) == null;
    }

    private boolean isMissingCategoryId(String raw) {
        return QuestEditorValidation.isMissingCategoryId(raw, currentPack != null, categoryIdCache);
    }

    private boolean isMissingQuestId(String raw) {
        return QuestEditorValidation.isMissingQuestId(raw, currentPack != null, questIdCache);
    }

    private void setDropdownBoxValue(EditBox box, String value) {
        if (box == null) return;
        String normalized = normalizeDropdownValue(value);
        box.setValue(normalized.isBlank() ? "None" : normalized);
    }

    private String dropdownBoxValue(EditBox box) {
        return normalizeDropdownValue(safe(box == null ? "" : box.getValue()));
    }

    private String normalizeDropdownValue(String raw) {
        String value = safe(raw).trim();
        return value.equalsIgnoreCase("none") ? "" : value;
    }

    private boolean isDropdownField(EditBox box) {
        return box == subCategoryBox || box == questCategoryBox || box == questSubCategoryBox;
    }

    private boolean isOpenDropdownField(EditBox box) {
        return popupMenus.isOpenDropdownField(box, subCategoryBox, questCategoryBox, questSubCategoryBox);
    }

    private boolean isMissingSubCategoryId(String raw) {
        return QuestEditorValidation.isMissingSubCategoryId(raw, dropdownBoxValue(questCategoryBox), currentPack != null, subCategoryIdCache);
    }

    private boolean isDuplicateQuestId(String questIdRaw) {
        return QuestEditorValidation.isDuplicateQuestId(questIdRaw, currentPack != null, cachedQuestEntriesOrLoad(), editingPath);
    }

    private boolean isDuplicateQuestIdAcrossPack(String questIdRaw) {
        return QuestEditorValidation.isDuplicateQuestIdAcrossPack(questIdRaw, currentPack != null, cachedQuestEntriesOrLoad());
    }

    private Iterable<NamedEntry> cachedQuestEntriesOrLoad() {
        if (currentPack != null && questListIndexCache != null && Objects.equals(questListIndexPackRoot, currentPack.root)) {
            List<NamedEntry> entries = new ArrayList<>(questListIndexCache.items.size());
            for (QuestListItem item : questListIndexCache.items) {
                if (item != null && item.entry != null) entries.add(item.entry);
            }
            return entries;
        }
        return currentPack == null ? List.of() : QuestEditorJsonFiles.listQuestEntries(currentPack);
    }

    private void refreshPackIdCaches() {
        refreshPackIdCaches(null);
    }

    private void refreshPackIdCaches(QuestListIndex questIndex) {
        // rebuild editor suggestion caches from the current pack
        validationSnapshot = null;
        invalidateIdSuggestions();
        categoryIdCache.clear();
        subCategoryIdCache.clear();
        questIdCache.clear();
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
                if (item.entry.icon != null && !item.entry.icon.isBlank()) {
                    questIconByIdCache.put(item.entry.id, item.entry.icon);
                }
            }
            for (String id : questIndex.categoryIds) {
                if (!categorySuggestionCache.contains(id)) categorySuggestionCache.add(id);
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
                    questIdCache.add(entry.id);
                    questPackDependencySuggestions.add(entry.id);
                    if (entry.icon != null && !entry.icon.isBlank()) {
                        questIconByIdCache.put(entry.id, entry.icon);
                    }
                }
            }
        }

        List<String> localCategories = sortedIds(categoryIdCache);
        List<String> localSubCategories = sortedIds(subCategoryIdCache);
        List<String> localQuests = sortedIds(questPackDependencySuggestions);
        categorySuggestionCache.addAll(localCategories);
        questSuggestionCache.addAll(localQuests);
        questPackDependencySuggestionCache.addAll(localQuests);

        for (String localSub : localSubCategories) {
            if (!subCategorySuggestionCache.contains(localSub)) subCategorySuggestionCache.add(localSub);
        }

        for (QuestData.Category c : QuestData.categoriesOrdered()) {
            if (c == null || c.id == null || c.id.isBlank()) continue;
            if ("all".equalsIgnoreCase(c.id.trim())) continue;
            categoryIdCache.add(c.id);
            if (!categorySuggestionCache.contains(c.id)) categorySuggestionCache.add(c.id);
        }
        for (QuestData.SubCategory sc : QuestData.subCategoriesAllOrdered()) {
            if (sc == null || sc.id == null || sc.id.isBlank()) continue;
            subCategoryIdCache.add(sc.id);
            addSubCategorySuggestion(sc.id, sc.category);
        }
        for (QuestData.Quest q : QuestData.all()) {
            if (q == null || q.id == null || q.id.isBlank()) continue;
            questIdCache.add(q.id);
            if (q.icon != null && !q.icon.isBlank() && !questIconByIdCache.containsKey(q.id)) {
                questIconByIdCache.put(q.id, q.icon);
            }
            if (!questSuggestionCache.contains(q.id)) questSuggestionCache.add(q.id);
        }
    }

    private void layoutDescriptionFormatterButtons(int startX, int undoY, int formatY, int width, int clipTop, int clipBottom) {
        boolean visible = editorType == EditorType.QUEST
                && questDescriptionBox != null
                && questDescriptionBox.visible;
        layoutDescriptionButtonGroup(startX, undoY, width, clipTop, clipBottom, visible, true);
        layoutDescriptionButtonGroup(startX, formatY, width, clipTop, clipBottom, visible, false);
    }

    private void layoutDescriptionButtonGroup(int startX, int y, int width, int clipTop, int clipBottom, boolean baseVisible, boolean undoRedo) {
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
        int x = startX + Math.max(0, width - totalWidth);
        for (TextInsertButton button : descriptionFormatButtons) {
            if (isDescriptionUndoRedoButton(button) != undoRedo) continue;
            button.setX(x);
            button.setY(y);
            button.visible = visible;
            button.active = visible && descriptionFormatButtonActive(button);
            x += button.getWidth() + FORMAT_BTN_GAP;
        }
    }

    private boolean isDescriptionUndoRedoButton(TextInsertButton button) {
        if (button == null) return false;
        String action = button.insertText();
        return "__undo__".equals(action) || "__redo__".equals(action);
    }

    private boolean descriptionFormatButtonActive(TextInsertButton button) {
        if (button == null) return false;
        String action = button.insertText();
        if ("__undo__".equals(action)) return questDescriptionBox != null && questDescriptionBox.canUndo();
        if ("__redo__".equals(action)) return questDescriptionBox != null && questDescriptionBox.canRedo();
        return true;
    }

    private List<String> sortedIds(Set<String> ids) {
        List<String> out = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return out;
        out.addAll(ids);
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    private void addSubCategorySuggestion(String subId, String categoryId) {
        if (subId == null || subId.isBlank()) return;
        if (!subCategorySuggestionCache.contains(subId)) subCategorySuggestionCache.add(subId);
        String key = safe(categoryId).trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) return;
        List<String> scoped = subCategoryByCategorySuggestion.computeIfAbsent(key, k -> new ArrayList<>());
        if (!scoped.contains(subId)) scoped.add(subId);
    }

    private int computeMultilineHeight(ScaledMultiLineEditBox box, int minHeight) {
        int lines = Math.max(1, box.getLineCount());
        double lineHeight = box.getLineHeight();
        int content = (int) Math.ceil(lines * lineHeight) + 8;
        int maxHeight = Math.max(minHeight, ph - (font.lineHeight + FIELD_LABEL_GAP + FIELD_ROW_GAP + 6));
        return Math.max(minHeight, Math.min(maxHeight, content));
    }

    private void updateBackButtonVisibility() {
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
            questSearchBox.visible = mode == Mode.QUEST_LIST;
            questSearchBox.active = mode == Mode.QUEST_LIST;
        }
    }

    private void goBack() {
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

    private void exitEditor() {
        if (closingEditor) return;
        closingEditor = true;
        clearPendingInitState();
        applyStagedChangesOnClose();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        exitEditor();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        closeTransientMenus();
        pendingState = captureState();
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isUnsavedChangesPopupOpen()) {
            return handleUnsavedChangesPopupClick(mouseX, mouseY, button);
        }
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleUnsavedChangesPopupClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        String save = "Save";
        String discard = "Discard";
        int popupW = Math.max(116, font.width("You have unsaved changes") + 22);
        int popupH = 48;
        int x = (this.width - popupW) / 2;
        int y = (this.height - popupH) / 2;
        int gap = 18;
        int actionY = y + 30;
        int saveX = x + popupW / 2 - font.width(save) - gap / 2;
        int discardX = x + popupW / 2 + gap / 2;
        if (isInsideText(mouseX, mouseY, saveX, actionY, save)) {
            saveCurrent();
            if (!hasUnsavedEditorChanges()) {
                continuePendingDiscardNavigation();
            }
            return true;
        }
        if (isInsideText(mouseX, mouseY, discardX, actionY, discard)) {
            continuePendingDiscardNavigation();
            return true;
        }
        return true;
    }

    private void continuePendingDiscardNavigation() {
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && deleteHoldActive) {
            deleteHoldActive = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clearEntryTextFocus() {
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) box.setFocused(false);
        for (ScaledMultiLineEditBox box : completionEntryBoxes) box.setFocused(false);
        for (ScaledMultiLineEditBox box : rewardEntryBoxes) box.setFocused(false);
        for (EntryCountBox box : completionEntryCountBoxes) box.setFocused(false);
        for (EntryCountBox box : rewardEntryCountBoxes) box.setFocused(false);
    }

    private EntryCountBox focusedEntryCountBox() {
        for (EntryCountBox box : completionEntryCountBoxes) {
            if (box != null && box.visible && box.active && box.isFocused()) return box;
        }
        for (EntryCountBox box : rewardEntryCountBoxes) {
            if (box != null && box.visible && box.active && box.isFocused()) return box;
        }
        return null;
    }

    private boolean clickDependencyRows(double mouseX, double mouseY, int button) {
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

    private boolean clickRowControls(EntryRowKind kind, double mouseX, double mouseY, int button) {
        for (EntryCountBox countBox : entryCountBoxes(kind)) {
            if (countBox != null && countBox.visible && countBox.active && countBox.isMouseOver(mouseX, mouseY)) {
                countBox.mouseClicked(mouseX, mouseY, button);
                countBox.setFocused(true);
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

    private boolean clickDropdownFields(double mouseX, double mouseY) {
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

    private boolean clickToolbarButtons(double mouseX, double mouseY) {
        if (createPackButton != null && createPackButton.visible && createPackButton.active && createPackButton.isMouseOver(mouseX, mouseY)) {
            createPackButton.onPress();
            return true;
        }
        if (createEntryButton != null && createEntryButton.visible && createEntryButton.active && createEntryButton.isMouseOver(mouseX, mouseY)) {
            createEntryButton.onPress();
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

    private void openTypeMenu(EntryRowKind kind, int row, int x, int y, int w) {
        popupMenus.openTypeMenu(kind, row, x, y, w);
    }

    private boolean clickTypeMenu(double mouseX, double mouseY) {
        return popupMenus.clickTypeMenu(mouseX, mouseY, (kind, selection) -> setRowType(kind, selection.row(), selection.type()));
    }

    private void renderTypeMenu(GuiGraphics gg, int mouseX, int mouseY) {
        popupMenus.renderTypeMenu(gg, font, mouseX, mouseY);
    }

    private boolean scrollTypeMenu(double mouseX, double mouseY, double delta) {
        return popupMenus.scrollTypeMenu(mouseX, mouseY, delta);
    }

    private void openDropdownMenu(DropdownMenuTarget target, EditBox box) {
        popupMenus.openDropdownMenu(target, box, py, ph, this::dropdownOptions);
    }

    private boolean clickDropdownMenu(double mouseX, double mouseY) {
        return popupMenus.clickDropdownMenu(mouseX, mouseY, this::dropdownOptions, this::applyDropdownSelection);
    }

    private void renderDropdownMenu(GuiGraphics gg, int mouseX, int mouseY) {
        popupMenus.renderDropdownMenu(gg, font, mouseX, mouseY, this::dropdownOptions);
    }

    private boolean scrollDropdownMenu(double mouseX, double mouseY, double delta) {
        return popupMenus.scrollDropdownMenu(mouseX, mouseY, delta, this::dropdownOptions);
    }

    private List<String> dropdownOptions(DropdownMenuTarget target) {
        List<String> out = new ArrayList<>();
        out.add("None");
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

    private void applyDropdownSelection(DropdownMenuTarget target, String value) {
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
        }
    }

    private void setRowType(EntryRowKind kind, int row, String type) {
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
        entryCountByBox.put(box, 1);
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

    private boolean isItemPickerOpen() {
        return itemPickerIconTarget != null || (itemPickerKind != null && itemPickerRow >= 0);
    }

    private boolean itemPickerAllowsTags() {
        if (pickerMode != PickerMode.ITEMS || itemPickerIconTarget != null || itemPickerKind != EntryRowKind.COMPLETION) return false;
        List<ScaledMultiLineEditBox> rows = entryRows(itemPickerKind);
        if (itemPickerRow < 0 || itemPickerRow >= rows.size()) return false;
        String type = effectiveRowType(itemPickerKind, rows.get(itemPickerRow));
        return "collect".equals(type) || "submit".equals(type) || "item".equals(type);
    }

    private void openIconItemPicker(EditBox target) {
        if (target == null) return;
        itemPickerIconTarget = target;
        itemPickerKind = null;
        itemPickerRow = -1;
        pickerMode = PickerMode.ITEMS;
        itemPickerTab = ItemPickerTab.CREATIVE;
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

    private void openItemPicker(EntryRowKind kind, int row) {
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

    private void closeItemPicker() {
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

    private boolean clickItemPicker(double mouseX, double mouseY) {
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
        if (pickerMode == PickerMode.ITEMS && clickPickerSideTab(mouseX, mouseY, x, y)) {
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
                } else {
                    applyPickedItem(items.get(idx));
                }
                closeItemPicker();
            }
            return true;
        }
        return true;
    }

    private boolean scrollItemPicker(double mouseX, double mouseY, double delta) {
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

    private void renderItemPicker(GuiGraphics gg, int mouseX, int mouseY) {
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
                itemPickerAllowsTags(),
                isItemPickerMultiToggleAvailable(),
                itemPickerMultiSelect,
                itemPickerPendingSelection,
                currentPickerPageData(),
                suggestionCaches.effectSuggestions(),
                this::pickerSelectionId
        ));
    }

    private boolean isItemPickerMultiToggleAvailable() {
        return (pickerMode == PickerMode.ITEMS || pickerMode == PickerMode.MOBS)
                && (itemPickerKind == EntryRowKind.COMPLETION
                || (itemPickerKind == EntryRowKind.REWARD && pickerMode == PickerMode.ITEMS));
    }

    private void togglePendingItemPickerSelection(String id) {
        String normalized = safe(id).trim();
        if (normalized.isBlank()) return;
        if (itemPickerPendingSelection.contains(normalized)) itemPickerPendingSelection.remove(normalized);
        else itemPickerPendingSelection.add(normalized);
    }

    private void applyItemPickerMultiSelection() {
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

    private void invalidateTagPageCache() {
        itemPickerData.invalidate();
    }

    private int itemPickerX() {
        return QuestEditorItemPickerRenderer.pickerX(width);
    }

    private int itemPickerY() {
        return QuestEditorItemPickerRenderer.pickerY(height);
    }

    private boolean clickPickerSideTab(double mouseX, double mouseY, int pickerX, int pickerY) {
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

    private int pickerCloseX(int pickerX) {
        return QuestEditorItemPickerRenderer.closeX(pickerX);
    }

    private int pickerCloseY(int pickerY) {
        return QuestEditorItemPickerRenderer.closeY(pickerY);
    }

    private String pickerSelectionId(ItemStack stack) {
        return itemPickerData.pickerSelectionId(stack, pickerMode, itemPickerTab, minecraft);
    }

    private PickerPageData currentPickerPageData() {
        PickerPageResult result = itemPickerData.currentPickerPageData(new PickerPageRequest(
                pickerMode,
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

    private void applyPickedItem(ItemStack picked) {
        if (!isItemPickerOpen()) return;
        if (itemPickerIconTarget != null) {
            if (picked == null || picked.isEmpty()) return;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(picked.getItem());
            if (id == null) return;
            itemPickerIconTarget.setValue(id.toString());
            itemPickerIconTarget.setCursorPosition(id.toString().length());
            markCurrentEditorUnsaved();
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

    private void applyPickedItemTag(String tagId) {
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

    private void applyPickedMob(String mobId) {
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

    private ItemStack selectedItemForRow(EntryRowKind kind, int row) {
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

    private boolean isIconBox(EditBox box) {
        return box == packIconPathBox || box == questIconBox || box == catIconBox || box == subIconBox;
    }

    private EditBox focusedEditBox() {
        if (packIconPathBox != null && packIconPathBox.isFocused()) return packIconPathBox;
        if (questIconBox != null && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.isFocused()) return catIconBox;
        if (subIconBox != null && subIconBox.isFocused()) return subIconBox;
        return null;
    }


}
