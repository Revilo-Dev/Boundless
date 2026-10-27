
package net.revilodev.boundless.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.QuestPanelBackgroundRenderer;
import net.revilodev.boundless.client.QuestPanelExpandButton;
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
import net.revilodev.boundless.client.editor.FtbQuestImporter;
import net.revilodev.boundless.client.editor.QuestEditorCustomIcons;
import net.revilodev.boundless.client.editor.QuestEditorEmojis;
import net.revilodev.boundless.client.editor.QuestEditorEntryRows;
import net.revilodev.boundless.client.editor.QuestEditorEntryRows.RowMaps;
import net.revilodev.boundless.client.editor.QuestEditorEntryTypes;
import net.revilodev.boundless.client.editor.QuestEditorJsonFiles;
import net.revilodev.boundless.client.editor.QuestEditorItemIcons;
import net.revilodev.boundless.client.editor.QuestEditorTreeWidget;
import net.revilodev.boundless.client.editor.QuestEditorTreeWidget.Tool;
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
import net.revilodev.boundless.client.editor.QuestEditorRowControls.IconPickerBox;
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
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
    private static final ResourceLocation DELETE_ENABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter_hovered.png");
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
    private static final ResourceLocation PACK_LIST_TYPE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/icon-list-quest.png");
    private static final ResourceLocation PACK_TREE_TYPE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/icon-tree-quest.png");
    private static final ResourceLocation ORIGIN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/origin.png");
    private static final ResourceLocation ORIGIN_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/origin-hover.png");

    // layout and spacing
    private static final int TOGGLE_SIZE = 20;
    private static final int SMALL_BTN_SIZE = 20;
    private static final int HEADER_ICON_SIZE = 10;
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
    private static final int BACKUP_ROW_H = 18;
    private static final int BACKUP_ROW_GAP = 2;
    private static final DateTimeFormatter BACKUP_DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

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
    private boolean returnToParentOnBack;
    private String initialQuestId = "";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final QuestEditorSuggestionCaches suggestionCaches = new QuestEditorSuggestionCaches();

    private int leftX;
    private int rightX;
    private int topY;
    private int pxLeft;
    private int pxRight;
    private int py;
    private int pw;
    private int leftPw;
    private int ph;
    private int listH;
    private int panelWidth = PANEL_W;
    private int editorPanelWidth = PANEL_W;
    private int panelHeight = PANEL_H;
    private boolean treePanelExpanded;
    private BackButton backButton;
    private EditBox questSearchBox;
    private Button createPackButton;
    private IconButton createEntryButton;
    private TreeToolButton treeMoveButton;
    private TreeToolButton treeDeleteButton;
    private TreeToolButton treeConnectorButton;
    private QuestPanelExpandButton treeExpandButton;
    private IconButton importQuestPackButton;
    private EditorTabButton categoriesTabButton;
    private EditorTabButton subCategoriesTabButton;
    private EditorTabButton questsTabButton;

    // active editor mode
    private Mode mode = Mode.PACK_LIST;
    private EditorType editorType = EditorType.NONE;
    private QuestPack currentPack;
    private QuestPack cachedLayoutPack;
    private boolean cachedTreeLayout;
    private String selectedEntryId = "";

    // main editor buttons
    private EditorListWidget leftList;
    private QuestEditorTreeWidget questTree;
    private Tool treeTool = Tool.NONE;
    private final List<CategoryWorkspaceTab> categoryWorkspaceTabs = new ArrayList<>();
    private String treeCategoryId = "";
    private ActionButton saveButton;
    private Button exportPackButton;
    private IconButton duplicateButton;
    private IconButton deleteQuestButton;
    private ScaledIconButton originButton;
    private List<QuestPackStorage.Snapshot> packBackups = List.of();
    private final List<BackupRowHitbox> backupRowHitboxes = new ArrayList<>();

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
    private PackLayoutSelector packLayoutSelector;
    private FtbImportSection ftbImportSection;
    private boolean ftbImportRunning;

    // category fields
    private EditBox catIdBox;
    private EditBox catNameBox;
    private EditBox catIconBox;
    private EditBox catBackgroundBox;
    private Button catBackgroundModeButton;
    private Button catBackgroundAssetButton;
    private CategoryBackgroundMode catBackgroundMode = CategoryBackgroundMode.TILEABLE;
    private ToggleButton catBackgroundDarkeningToggle;
    private CategoryBackgroundOpacitySlider catBackgroundOpacitySlider;
    private String observedCategoryBackgroundState = "";
    private String persistedCategoryBackgroundState = "";
    private long categoryBackgroundSaveAt;
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
    private QuestMediaSelector questMediaSelector;
    private EditBox stickerIconPickerTarget;
    private int pendingStickerIconX;
    private int pendingStickerIconY;
    private EditBox questCategoryBox;
    private EditBox questSubCategoryBox;
    private ScaledMultiLineEditBox questDependenciesBox;
    private LockToggleButton questDependencyLockToggle;
    private ToggleButton questOptionalToggle;
    private ToggleButton questRepeatableToggle;
    private ToggleButton questAutoCompleteToggle;
    private ToggleButton questHiddenUnderDependencyToggle;
    private QuestTreeBackgroundSelector questTreeBackgroundSelector;
    private int questTreeX = -1;
    private int questTreeY = -1;
    private String questIncompatibleDependencies = "";
    private boolean questTreeOrigin;
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
    private static final Map<String, Set<String>> QUEST_CATEGORY_COLLAPSE_STATES = new HashMap<>();
    private static final Map<String, Set<String>> QUEST_SUBCATEGORY_COLLAPSE_STATES = new HashMap<>();
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
    private boolean emojiPickerOpen;
    private int emojiPickerPage;
    private static final ResourceLocation EMOJI_BUTTON_TEX = ResourceLocation.fromNamespaceAndPath("boundless", "textures/icon/emoji/emojis.png");
    private static final ResourceLocation EMOJI_GUI_TEX = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/emoji-gui.png");
    private static final int EMOJI_PICKER_COLS = 4;
    private static final int EMOJI_PICKER_ROWS = 5;
    private boolean idSuggestionsDirty = true;
    private boolean suppressIdSuggestions = false;
    private boolean suppressIdSanitizer = false;
    private boolean closingEditor = false;
    // unsaved change state
    private String savedEditorState;
    private String pendingDiscardEntryId = "";
    private Mode pendingDiscardMode;
    private EditorEntry pendingDiscardEntry;
    private boolean pendingEditorExit;
    private boolean pendingBackNavigation;
    private Runnable pendingTreeDeleteAction;
    private String pendingTreeDeleteMessage = "";
    private static final Map<String, QuestCategorySelection> LAST_QUEST_CATEGORY_SELECTIONS = new HashMap<>();
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
    private float uiScale = 1.0f;

    private record BackupRowHitbox(QuestPackStorage.Snapshot snapshot, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    // create the editor screen
    public QuestEditorScreen(Screen parent) {
        super(tr("title"));
        this.parent = parent;
        this.returnToParentOnBack = false;
    }

    public QuestEditorScreen(Screen parent, QuestData.Quest quest) {
        this(parent);
        this.initialQuestId = quest == null ? "" : safe(quest.id);
        this.returnToParentOnBack = true;
    }

    @Override
    // build the editor layout
    protected void init() {
        uiScale = Config.editorScale();
        width = Math.max(1, Math.round(width / uiScale));
        height = Math.max(1, Math.round(height / uiScale));
        closeTransientMenus();
        int maxPanelWidth = Math.max(100, (width - TAB_W - 12) / 2);
        panelWidth = Math.min(treePanelExpanded ? 220 : PANEL_W, maxPanelWidth);
        editorPanelWidth = Math.min(PANEL_W, maxPanelWidth);
        panelHeight = Math.min(PANEL_H, Math.max(120, height - 26));
        int centerX = width / 2 + (TAB_W - SIDE_TAB_X_OFFSET) / 2;
        leftX = centerX - panelWidth - 2;
        rightX = centerX + 2;
        topY = (height / 2) - panelHeight / 2;
        pxLeft = leftX + 10;
        pxRight = rightX + 10;
        py = topY + 10;
        leftPw = panelWidth - 20;
        pw = editorPanelWidth - 20;
        int interiorH = panelHeight - 20;
        listH = interiorH - BOTTOM_BAR_H;
        ph = listH;

        leftList = new EditorListWidget(pxLeft, py, leftPw, listH, this::handleLeftClick, this::handleLeftAction, this::handleLeftSecondaryClick, this::handleEntryMoveAction, QuestEditorItemIcons::iconStackFromId);
        addRenderableWidget(leftList);
        questTree = new QuestEditorTreeWidget(pxLeft, py, leftPw, listH, this::openTreeQuest,
                this::placeTreeQuest, this::saveTreePosition, this::requestDeleteTreeNode,
                this::requestTreeDependencyChange, this::toggleTreeIncompatibility,
                this::saveTreeStickers, this::requestStickerIcon);
        questTree.visible = false;
        questTree.active = false;
        addRenderableWidget(questTree);

        initFormFields();
        stickerIconPickerTarget = new EditBox(font, 0, 0, 1, 1, Component.empty());

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

        backButton = new BackButton(leftX - TAB_W + BACK_TAB_X_OFFSET, topY + panelHeight - BACK_TAB_H - BACK_TAB_BOTTOM_MARGIN, this::goBack);
        addRenderableWidget(backButton);
        createPackButton = Button.builder(tr("create_new"), button -> openPackCreate())
                .bounds(pxLeft + 2, barY, leftPw - SMALL_BTN_SIZE - SMALL_BTN_GAP - 4, 20)
                .build();
        addRenderableWidget(createPackButton);
        createEntryButton = new IconButton(pxLeft + 2, barY, SMALL_BTN_SIZE, PLUS_BUTTON_TEX, PLUS_BUTTON_HOVER_TEX, this::requestCreateEntryForCurrentMode);
        createEntryButton.visible = false;
        createEntryButton.active = false;
        addRenderableWidget(createEntryButton);
        treeMoveButton = new TreeToolButton(Tool.MOVE, this::toggleTreeMoveMode);
        treeMoveButton.visible = false;
        treeMoveButton.active = false;
        addRenderableWidget(treeMoveButton);
        treeDeleteButton = new TreeToolButton(Tool.DELETE, this::toggleTreeDeleteMode);
        treeDeleteButton.visible = false;
        treeDeleteButton.active = false;
        addRenderableWidget(treeDeleteButton);
        treeConnectorButton = new TreeToolButton(Tool.CONNECT, this::toggleTreeConnectorMode);
        treeConnectorButton.visible = false;
        treeConnectorButton.active = false;
        addRenderableWidget(treeConnectorButton);
        treeExpandButton = new QuestPanelExpandButton(0, 0, () -> treePanelExpanded, () -> {
            treePanelExpanded = !treePanelExpanded;
            resize(minecraft, Math.round(width * uiScale), Math.round(height * uiScale));
        });
        treeExpandButton.visible = false;
        treeExpandButton.active = false;
        addRenderableWidget(treeExpandButton);
        originButton = new ScaledIconButton(pxRight + pw - HEADER_ICON_SIZE, py,
                ORIGIN_TEX, ORIGIN_HOVER_TEX, this::makeCurrentQuestOrigin);
        originButton.visible = false;
        originButton.active = false;
        addRenderableWidget(originButton);
        setTreeTool(treeTool);
        importQuestPackButton = new IconButton(createPackButton.getX() + createPackButton.getWidth() + 2, barY, SMALL_BTN_SIZE, IMPORT_PACK_BUTTON_TEX, IMPORT_PACK_BUTTON_TEX_HOVER, this::openImportQuestPackDirectory);
        addRenderableWidget(importQuestPackButton);
        categoriesTabButton = new EditorTabButton(trs("categories"), "boundless:quest_book", Mode.CATEGORY_LIST, () -> currentPack != null, () -> mode, this::setMode);
        subCategoriesTabButton = new EditorTabButton(trs("subcategories"), "minecraft:book", Mode.SUBCATEGORY_LIST, () -> currentPack != null, () -> mode, this::setMode);
        questsTabButton = new EditorTabButton(trs("quests"), QUEST_TAB_SCROLL_ICON_TEX, Mode.QUEST_LIST, () -> currentPack != null, () -> mode, this::setMode);
        questSearchBox = new EditBox(font, pxLeft + 2, barY, leftPw - 4, 20, tr("search_quests"));
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
        if (!initialQuestId.isBlank() && openInitialQuest()) return;
        setMode(Mode.PACK_LIST);
    }

    private boolean openInitialQuest() {
        String questId = initialQuestId;
        initialQuestId = "";
        for (QuestPack pack : QuestEditorPackFiles.listPacks()) {
            QuestEntryData data = QuestEditorJsonFiles.loadQuest(pack, questId);
            if (data == null) continue;
            currentPack = pack;
            selectedEntryId = questId;
            setMode(Mode.QUEST_LIST);
            showQuestEditor(data, data.path);
            if (leftList != null) leftList.setSelectedId(questId);
            return true;
        }
        return false;
    }

    // create all reusable form widgets
    private void initFormFields() {
        packNameBox = createBox("Pack name", BOX_H);
        packNamespaceBox = createBox("Namespace", BOX_H);
        packIconPathBox = createIconPickerBox("Pack icon path");
        packDescriptionBox = makeMultiBox("Pack description", BOX_H_TALL, false);
        packLayoutSelector = new PackLayoutSelector();
        addRenderableWidget(packLayoutSelector);
        ftbImportSection = new FtbImportSection();
        addRenderableWidget(ftbImportSection);

        catIdBox = createBox("Category id", BOX_H);
        catNameBox = createBox("Category name", BOX_H);
        catNameBox.setMaxLength(22);
        catIconBox = createIconPickerBox("Category icon");
        catBackgroundBox = createIconPickerBox("Category background");
        catBackgroundModeButton = Button.builder(Component.empty(), button -> cycleCategoryBackgroundMode())
                .bounds(0, 0, 40, BOX_H).build();
        catBackgroundModeButton.visible = false;
        catBackgroundModeButton.active = false;
        addRenderableWidget(catBackgroundModeButton);
        catBackgroundAssetButton = Button.builder(Component.literal("Select"), button -> openCategoryBackgroundPicker())
                .bounds(0, 0, 38, BOX_H).build();
        catBackgroundAssetButton.visible = false;
        catBackgroundAssetButton.active = false;
        addRenderableWidget(catBackgroundAssetButton);
        catBackgroundDarkeningToggle = createToggle(false);
        catBackgroundOpacitySlider = new CategoryBackgroundOpacitySlider(0, 0, 100, BOX_H, 0.45f);
        addRenderableWidget(catBackgroundOpacitySlider);
        catDependencyBox = createBox("Unlock quest", BOX_H);
        catAutoCompleteToggle = createToggle(false);

        subIdBox = createBox("Sub-category id", BOX_H);
        subCategoryBox = createBox("Parent category id", BOX_H);
        subNameBox = createBox("Sub-category name", BOX_H);
        subNameBox.setMaxLength(26);
        subIconBox = createIconPickerBox("Sub-category icon");
        subDefaultOpenToggle = createToggle(false);

        questIdBox = createBox("Quest id", BOX_H);
        questNameBox = createBox("Quest name", BOX_H);
        questIconBox = createIconPickerBox("Quest icon");
        questDescriptionBox = makeMultiBox("Quest description", BOX_H_TALL, true);
        questMediaSelector = new QuestMediaSelector();
        addRenderableWidget(questMediaSelector);
        questCategoryBox = createBox("Quest category", BOX_H);
        questSubCategoryBox = createBox("Quest sub-category", BOX_H);
        questDependenciesBox = makeMultiBox("Dependencies", BOX_H_TALL, false);
        questDependencyLockToggle = makeDepLock(false);
        questOptionalToggle = createToggle(false);
        questRepeatableToggle = createToggle(false);
        questAutoCompleteToggle = createToggle(false);
        questHiddenUnderDependencyToggle = createToggle(false);
        questTreeBackgroundSelector = new QuestTreeBackgroundSelector();
        addRenderableWidget(questTreeBackgroundSelector);
        questCompletionBox = makeMultiBox("Completion entries", BOX_H_TALL, false);
        questRewardBox = makeMultiBox("Reward entries", BOX_H_TALL, false);
        initEntryRowBoxes();
        initFormatBtns();

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
    private void initFormatBtns() {
        descriptionFormatButtons.clear();
        descriptionFormatButtons.add(createDescriptionInsertButton("", "__emoji__", 0xFF4A4A4A, 12));
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

    private EditBox createIconPickerBox(String hint) {
        EditBox box = new IconPickerBox(font, Component.literal(hint));
        box.setMaxLength(1024);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    private ScaledMultiLineEditBox makeMultiBox(String hint, int height) {
        return makeMultiBox(hint, height, false);
    }

    // create a shared multiline field
    private ScaledMultiLineEditBox makeMultiBox(String hint, int height, boolean allowColorFormatting) {
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

    private LockToggleButton makeDepLock(boolean initial) {
        LockToggleButton button = new LockToggleButton(0, 0, DEP_LOCK_SIZE, DEP_LOCK_SIZE, initial);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    // switch the active editor mode
    private void setMode(Mode next) {
        if (next != mode) flushCategoryBackgroundAutosave();
        if (next != Mode.QUEST_LIST) setTreeTool(Tool.NONE);
        if (next == Mode.QUEST_LIST && mode != Mode.QUEST_LIST) {
            restoreQuestListCollapseState();
        }
        closeTransientMenus();
        mode = next;
        statusMessage = "";
        editorScroll = 0f;
        disarmDeleteConfirm();
        refreshLeftList(false);
        clearEditor();
        if (next == Mode.QUEST_LIST && isTreePack() && currentPack != null) {
            List<NamedEntry> categories = QuestEditorJsonFiles.listCategoryEntries(currentPack);
            if (!categories.isEmpty()) {
                treeCategoryId = categories.getFirst().id;
                refreshQuestTree();
            }
        }
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
        // Quest files can be created, renamed, or deleted while this screen remains open.
        // Rebuild the file-backed index before rendering the quest list so stale rows cannot
        // survive a deletion and later fail to load when selected.
        if (mode == Mode.QUEST_LIST) {
            invalidateQuestListIndex();
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
        refreshQuestTree();
        refreshCategoryWorkspaceTabs();
    }

    private boolean isTreePack() {
        if (currentPack == null) return false;
        if (cachedLayoutPack != currentPack) {
            cachedLayoutPack = currentPack;
            cachedTreeLayout = "tree".equalsIgnoreCase(
                    QuestEditorPackFiles.readPackMeta(currentPack.root, currentPack.name).layout);
        }
        return cachedTreeLayout;
    }

    private void refreshQuestTree() {
        if (questTree == null) return;
        if (mode != Mode.QUEST_LIST || !isTreePack()) {
            questTree.setQuests(List.of());
            questTree.setBackground("", false, 0.45f);
            return;
        }
        List<QuestEntryData> quests = new ArrayList<>();
        for (QuestListItem item : questListIndex().items) {
            if (item.data != null && (treeCategoryId.isBlank() || treeCategoryId.equals(item.data.category))) quests.add(item.data);
        }
        questTree.setQuests(quests);
        CategoryData category = QuestEditorJsonFiles.loadCategory(currentPack, treeCategoryId);
        questTree.setBackground(category == null ? "" : category.background,
                category != null && parseBool(category.backgroundDarkening, false),
                category == null ? 0.45f : parseFloat(category.backgroundDarkeningOpacity, 0.45f));
        questTree.setStickers(readCategoryStickers(category == null ? null : category.path));
    }

    private void refreshCategoryWorkspaceTabs() {
        for (CategoryWorkspaceTab tab : categoryWorkspaceTabs) {
            tab.visible = false;
            tab.active = false;
        }
        categoryWorkspaceTabs.clear();
        if (mode != Mode.QUEST_LIST || !isTreePack() || currentPack == null) return;
        List<NamedEntry> categories = QuestEditorJsonFiles.listCategoryEntries(currentPack);
        boolean currentExists = categories.stream().anyMatch(category -> category.id.equals(treeCategoryId));
        if (!currentExists && !categories.isEmpty()) treeCategoryId = categories.getFirst().id;
        int tabY = py;
        for (NamedEntry category : categories) {
            CategoryWorkspaceTab tab = new CategoryWorkspaceTab(category, tabY);
            categoryWorkspaceTabs.add(tab);
            addRenderableWidget(tab);
            tabY += TAB_H + TAB_GAP;
        }
        CategoryWorkspaceTab createTab = new CategoryWorkspaceTab(null, tabY);
        categoryWorkspaceTabs.add(createTab);
        addRenderableWidget(createTab);
    }

    private void selectTreeCategory(NamedEntry entry) {
        if (entry == null || currentPack == null) return;
        treeCategoryId = entry.id;
        refreshQuestTree();
        CategoryData category = QuestEditorJsonFiles.loadCategory(currentPack, entry.id);
        if (category != null) showCategoryEditor(category, category.path);
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
            String packIconId = QuestEditorPackFiles.cleanIconId(meta.iconPath);
            boolean legacy = pack.legacy;
            boolean changed = stagedPacks.containsKey(pack.name);
            boolean enabled = !legacy && pack.enabled;
            ResourceLocation actionIcon = enabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX;
            String actionTooltip = legacy ? trs("legacy_pack") : (enabled ? trs("enabled") : trs("disabled"));
            ResourceLocation rowTexture = legacy ? PACK_ACTION_NEEDED_TEX : (changed ? PACK_CHANGED_TEX : ROW_TEX);
            ResourceLocation typeIcon = "tree".equalsIgnoreCase(meta.layout) ? PACK_TREE_TYPE_TEX : PACK_LIST_TYPE_TEX;
            out.add(EditorEntry.pack(pack.name, pack.name, packIconId, typeIcon, actionIcon, actionTooltip,
                    legacy ? LEGACY_PACK_TOOLTIP : trs("tooltip.right_click_options"), rowTexture));
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
            if (isSingleplayerAuthoritySession()) {
                Config.ENABLE_BUILTIN_QUEST_PACK.set(next);
                Config.SPEC.save();
                QuestEditorPackFiles.reloadLater();
            } else if (!QuestEditorPackFiles.sendPackEnabled("", next, true)) {
                setError(trs("error.update_questpack_failed"));
                return;
            }
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
        if (isSingleplayerAuthoritySession()) {
            if (!QuestEditorPackFiles.setQuestPackEnabled(pack, next)) {
                setError(trs("error.update_questpack_failed"));
                return;
            }
            QuestEditorPackFiles.reloadLater();
        } else if (!QuestEditorPackFiles.sendPackEnabled(pack.name, next, false)) {
            setError(trs("error.update_questpack_failed"));
            return;
        }
        if (isSingleplayerAuthoritySession()) {
            Config.setQuestPackApplied(pack.name, next);
            QuestEditorPackFiles.reloadLater();
        } else {
            Config.setSyncedQuestPackApplied(pack.name, next);
            if (!QuestEditorPackFiles.sendPack(pack, next)
                    && !QuestEditorPackFiles.sendPackEnabled(pack.name, next, false)) {
                Config.setSyncedQuestPackApplied(pack.name, !next);
                QuestEditorPackFiles.setQuestPackEnabled(pack, !next);
                setError(trs("error.update_questpack_failed"));
                return;
            }
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
        if (mode == Mode.QUEST_LIST && isTreePack()) {
            setTreeTool(treeTool == Tool.CREATE ? Tool.NONE : Tool.CREATE);
            return;
        }
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
        if (mode == Mode.QUEST_LIST && isTreePack()) {
            return Component.literal("Create nodes: click empty space to place a blank quest. "
                    + "Creation stays active until you select a node or toggle this button.");
        }
        return switch (mode) {
            case CATEGORY_LIST -> tr("tooltip.create_category");
            case SUBCATEGORY_LIST -> tr("tooltip.create_subcategory");
            case QUEST_LIST -> tr("tooltip.create_quest");
            default -> tr("tooltip.create_entry");
        };
    }

    private void openTreeQuest(QuestEntryData treeQuest) {
        if (treeQuest == null || currentPack == null) return;
        setTreeTool(Tool.NONE);
        QuestEntryData data = QuestEditorJsonFiles.loadQuest(currentPack, treeQuest.id);
        if (data == null) return;
        selectedEntryId = data.id;
        if (leftList != null) leftList.setSelectedId(data.id);
        showQuestEditor(data, data.path);
    }

    private void placeTreeQuest(Integer x, Integer y) {
        if (questTree == null || currentPack == null || treeCategoryId.isBlank()) return;
        int number = nextTreeQuestNumber();
        String name = String.format(Locale.ROOT, "New Quest %02d", number);
        String id = questIdFromName(name);
        QuestBuildFields fields = new QuestBuildFields();
        fields.id = id;
        fields.name = name;
        fields.category = treeCategoryId;
        fields.treeX = x == null ? 0 : x;
        fields.treeY = y == null ? 0 : y;
        fields.treeBackground = "square";
        fields.treeOrigin = !categoryHasTreeNodes(treeCategoryId);
        JsonObject obj = QuestEditorJsonBuilder.buildQuest(fields, this::setError);
        if (obj == null) return;
        Path target = currentPack.questsDir.resolve(id + ".json");
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, target);
            stageCurrentPackChange("Quest node created");
            QuestEditorPackFiles.backupPack(currentPack, "created");
            refreshLeftList();
            statusMessage = name + " created";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError("Failed to create quest node");
        }
    }

    private int nextTreeQuestNumber() {
        Set<String> ids = new HashSet<>();
        for (QuestListItem item : questListIndex().items) {
            if (item != null && item.data != null) ids.add(safe(item.data.id));
        }
        int number = 1;
        while (ids.contains(String.format(Locale.ROOT, "new-quest-%02d", number))) number++;
        return number;
    }

    private void toggleTreeDeleteMode() {
        setTreeTool(treeTool == Tool.DELETE ? Tool.NONE : Tool.DELETE);
    }

    private void toggleTreeMoveMode() {
        setTreeTool(treeTool == Tool.MOVE ? Tool.NONE : Tool.MOVE);
    }

    private void toggleTreeConnectorMode() {
        setTreeTool(treeTool == Tool.CONNECT ? Tool.NONE : Tool.CONNECT);
    }

    private void setTreeTool(Tool tool) {
        treeTool = tool == null ? Tool.NONE : tool;
        if (questTree != null) questTree.setTool(treeTool);
        if (createEntryButton != null) {
            boolean selected = treeTool == Tool.CREATE;
            createEntryButton.setTextures(selected ? PLUS_BUTTON_HOVER_TEX : PLUS_BUTTON_TEX, PLUS_BUTTON_HOVER_TEX);
        }
        if (treeDeleteButton != null) treeDeleteButton.setSelected(treeTool == Tool.DELETE);
        if (treeConnectorButton != null) treeConnectorButton.setSelected(treeTool == Tool.CONNECT);
        if (treeMoveButton != null) treeMoveButton.setSelected(treeTool == Tool.MOVE);
    }

    private boolean categoryHasTreeNodes(String categoryId) {
        for (QuestListItem item : questListIndex().items) {
            if (item != null && item.data != null && categoryId.equals(item.data.category)) return true;
        }
        return false;
    }

    private void makeCurrentQuestOrigin() {
        if (currentPack == null || editorType != EditorType.QUEST || editingPath == null) return;
        String questId = safe(questIdBox.getValue()).trim();
        String categoryId = dropdownBoxValue(questCategoryBox);
        if (questId.isBlank() || categoryId.isBlank()) return;
        try {
            for (QuestListItem item : questListIndex().items) {
                QuestEntryData data = item == null ? null : item.data;
                if (data == null || data.path == null || !categoryId.equals(data.category)) continue;
                JsonObject obj = QuestEditorJsonFiles.readJson(data.path);
                if (obj == null) continue;
                if (questId.equals(data.id)) obj.addProperty("tree_origin", true);
                else obj.remove("tree_origin");
                QuestPackStorage.writeJsonAtomically(gson, obj, data.path);
            }
            questTreeOrigin = true;
            stageCurrentPackChange("Tree origin changed");
            QuestEditorPackFiles.backupPack(currentPack, "origin");
            refreshLeftList();
            statusMessage = "Tree origin set";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError("Failed to set tree origin");
        }
    }

    private void saveTreePosition(QuestEntryData quest, int x, int y) {
        if (quest == null || quest.path == null) return;
        JsonObject obj = QuestEditorJsonFiles.readJson(quest.path);
        if (obj == null) return;
        obj.addProperty("tree_x", x);
        obj.addProperty("tree_y", y);
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, quest.path);
            stageCurrentPackChange("Quest position staged");
        } catch (IOException e) {
            setError("Failed to save quest position");
        }
    }

    private List<QuestData.Sticker> readCategoryStickers(Path categoryPath) {
        if (categoryPath == null) return List.of();
        JsonObject object = QuestEditorJsonFiles.readJson(categoryPath);
        if (object == null || !object.has("stickers") || !object.get("stickers").isJsonArray()) return List.of();
        List<QuestData.Sticker> stickers = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("stickers")) {
            if (!element.isJsonObject()) continue;
            JsonObject sticker = element.getAsJsonObject();
            String value = optStringFlexible(sticker, "value", "");
            if (value.isBlank()) continue;
            stickers.add(new QuestData.Sticker(optStringFlexible(sticker, "type", "text"), value,
                    parseInt(optStringFlexible(sticker, "x", "0"), 0),
                    parseInt(optStringFlexible(sticker, "y", "0"), 0)));
        }
        return List.copyOf(stickers);
    }

    private void saveTreeStickers(List<QuestData.Sticker> stickers) {
        if (currentPack == null || treeCategoryId.isBlank()) return;
        CategoryData category = QuestEditorJsonFiles.loadCategory(currentPack, treeCategoryId);
        if (category == null || category.path == null) return;
        JsonObject object = QuestEditorJsonFiles.readJson(category.path);
        if (object == null) return;
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (QuestData.Sticker sticker : stickers) {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", sticker.type());
            entry.addProperty("value", sticker.value());
            entry.addProperty("x", sticker.x());
            entry.addProperty("y", sticker.y());
            array.add(entry);
        }
        object.add("stickers", array);
        try {
            QuestPackStorage.writeJsonAtomically(gson, object, category.path);
            stageCurrentPackChange("Tree sticker saved");
            QuestEditorPackFiles.backupPack(currentPack, "stickers");
            QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, isSingleplayerAuthoritySession());
            statusMessage = "Tree sticker saved";
            statusColor = 0xA0FFA0;
        } catch (IOException exception) {
            setError("Failed to save tree sticker");
        }
    }

    private void requestStickerIcon(Integer x, Integer y) {
        pendingStickerIconX = x == null ? 0 : x;
        pendingStickerIconY = y == null ? 0 : y;
        openIconItemPicker(stickerIconPickerTarget);
    }

    private void autosaveTreeBackground(String background, boolean alreadyUnsaved) {
        if (editorType != EditorType.QUEST || editingPath == null || currentPack == null) {
            markCurrentEditorUnsaved();
            return;
        }
        JsonObject obj = QuestEditorJsonFiles.readJson(editingPath);
        if (obj == null) return;
        obj.addProperty("tree_background", background);
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, editingPath);
            stageCurrentPackChange("Quest node style saved");
            QuestEditorPackFiles.backupPack(currentPack, "node-style");
            invalidateQuestListIndex();
            refreshQuestTree();
            if (!alreadyUnsaved) markCurrentEditorSaved();
            statusMessage = "Quest node style saved";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError("Failed to save quest node style");
        }
    }

    private void resetCategoryBackgroundAutosaveState() {
        String state = categoryBackgroundAutosaveState();
        observedCategoryBackgroundState = state;
        persistedCategoryBackgroundState = state;
        categoryBackgroundSaveAt = 0L;
    }

    private String categoryBackgroundAutosaveState() {
        return encodedCategoryBackground() + "\n"
                + (catBackgroundDarkeningToggle != null && catBackgroundDarkeningToggle.isOn()) + "\n"
                + (catBackgroundOpacitySlider == null ? 0.45f : catBackgroundOpacitySlider.getOpacity());
    }

    private void tickCategoryBackgroundAutosave() {
        if (editorType != EditorType.CATEGORY || editingPath == null || currentPack == null) return;
        String current = categoryBackgroundAutosaveState();
        long now = Util.getMillis();
        if (!current.equals(observedCategoryBackgroundState)) queueCategoryBackgroundAutosave(false);
        if (categoryBackgroundSaveAt > 0L && now >= categoryBackgroundSaveAt
                && !current.equals(persistedCategoryBackgroundState)) {
            autosaveCategoryBackground(current);
        }
    }

    private void queueCategoryBackgroundAutosave(boolean immediate) {
        observedCategoryBackgroundState = categoryBackgroundAutosaveState();
        categoryBackgroundSaveAt = Util.getMillis() + (immediate ? 0L : 200L);
        applyCategoryBackgroundPreview();
    }

    private void flushCategoryBackgroundAutosave() {
        if (editorType != EditorType.CATEGORY || editingPath == null || currentPack == null) return;
        String current = categoryBackgroundAutosaveState();
        if (!current.equals(persistedCategoryBackgroundState)) autosaveCategoryBackground(current);
    }

    private void applyCategoryBackgroundPreview() {
        if (questTree == null || catIdBox == null || !treeCategoryId.equals(catIdBox.getValue().trim())) return;
        questTree.setBackground(encodedCategoryBackground(), catBackgroundDarkeningToggle.isOn(),
                catBackgroundOpacitySlider.getOpacity());
    }

    private void autosaveCategoryBackground(String state) {
        JsonObject obj = QuestEditorJsonFiles.readJson(editingPath);
        if (obj == null) return;
        String background = encodedCategoryBackground();
        if (background.isBlank()) obj.remove("background");
        else obj.addProperty("background", background);
        obj.addProperty("background_darkening", catBackgroundDarkeningToggle.isOn());
        obj.addProperty("background_darkening_opacity", catBackgroundOpacitySlider.getOpacity());
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, editingPath);
            persistedCategoryBackgroundState = state;
            categoryBackgroundSaveAt = 0L;
            stageCurrentPackChange("Category background saved");
            QuestEditorPackFiles.backupPack(currentPack, "category-background");
            refreshQuestTree();
            QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, isSingleplayerAuthoritySession());
            Minecraft.getInstance().execute(() -> {
                QuestData.loadClient(true);
                QuestPanelClient.applyConfigChanges();
            });
            statusMessage = "Category background saved";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            categoryBackgroundSaveAt = Util.getMillis() + 1_000L;
            setError("Failed to save category background");
        }
    }

    private void requestDeleteTreeNode(QuestEntryData quest, boolean bypassConfirmation) {
        if (quest == null || quest.path == null) return;
        Runnable delete = () -> deleteTreeNode(quest);
        if (bypassConfirmation) {
            delete.run();
            return;
        }
        confirmTreeAction("Delete quest node?", "Delete " + safe(quest.name) + " and its dependency links?", delete);
    }

    private void requestTreeDependencyChange(QuestEntryData quest, String dependencyId, boolean add,
                                             boolean bypassConfirmation) {
        if (quest == null || quest.path == null || dependencyId == null || dependencyId.isBlank()) return;
        Runnable change = () -> changeTreeDependency(quest, dependencyId, add);
        if (add || bypassConfirmation) {
            change.run();
            return;
        }
        QuestEntryData dependency = questListIndex().questDataByEntryId.get(dependencyId);
        String dependencyName = dependency == null ? dependencyId : safe(dependency.name);
        confirmTreeAction("Delete dependency line?",
                "Remove the connection from " + dependencyName + " to " + safe(quest.name) + "?", change);
    }

    private void confirmTreeAction(String title, String message, Runnable action) {
        pendingTreeDeleteMessage = message == null || message.isBlank() ? title : message;
        pendingTreeDeleteAction = action;
    }

    private void toggleTreeIncompatibility(QuestEntryData quest, String dependencyId) {
        if (currentPack == null || quest == null || quest.path == null || dependencyId == null || dependencyId.isBlank()) return;
        JsonObject obj = QuestEditorJsonFiles.readJson(quest.path);
        if (obj == null) return;
        LinkedHashSet<String> incompatibleDependencies = new LinkedHashSet<>(
                QuestEditorEntryCodec.extractEntryLines(quest.incompatibleDependencies));
        if (incompatibleDependencies.isEmpty() && parseBool(quest.lockAfterDependency, false)) {
            incompatibleDependencies.addAll(QuestEditorEntryCodec.extractEntryLines(quest.dependencies));
        }
        boolean incompatible = incompatibleDependencies.add(dependencyId);
        if (!incompatible) incompatibleDependencies.remove(dependencyId);
        writeStringArray(obj, "incompatible_dependencies", incompatibleDependencies);
        obj.remove("lock_after_dependency");
        obj.remove("lockAfterDependency");
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, quest.path);
            quest.incompatibleDependencies = String.join("\n", incompatibleDependencies);
            quest.lockAfterDependency = "false";
            stageCurrentPackChange(incompatible ? "Incompatibility added" : "Incompatibility removed");
            QuestEditorPackFiles.backupPack(currentPack, incompatible ? "incompatible" : "compatible");
            refreshLeftList();
            statusMessage = incompatible ? "Incompatibility line created" : "Dependency line restored";
            statusColor = incompatible ? 0xFFFF6060 : 0xA0FFA0;
        } catch (IOException e) {
            setError("Failed to update incompatibility line");
        }
    }

    private void deleteTreeNode(QuestEntryData quest) {
        if (currentPack == null || quest == null || quest.path == null) return;
        try {
            for (QuestListItem item : QuestEditorQuestOrdering.listQuestListItems(currentPack)) {
                QuestEntryData dependent = item == null ? null : item.data;
                if (dependent == null || dependent.path == null || dependent.id.equals(quest.id)) continue;
                List<String> dependencies = new ArrayList<>(QuestEditorEntryCodec.extractEntryLines(dependent.dependencies));
                if (!dependencies.removeIf(quest.id::equals)) continue;
                JsonObject obj = QuestEditorJsonFiles.readJson(dependent.path);
                if (obj != null) {
                    writeDependencies(obj, dependencies);
                    LinkedHashSet<String> incompatibleDependencies = new LinkedHashSet<>(
                            QuestEditorEntryCodec.extractEntryLines(dependent.incompatibleDependencies));
                    if (incompatibleDependencies.remove(quest.id)) {
                        writeStringArray(obj, "incompatible_dependencies", incompatibleDependencies);
                    }
                    QuestPackStorage.writeJsonAtomically(gson, obj, dependent.path);
                }
            }
            Files.deleteIfExists(quest.path);
            if (editingPath != null && editingPath.equals(quest.path)) clearEditor();
            selectedEntryId = "";
            stageCurrentPackChange("Quest node deleted");
            QuestEditorPackFiles.backupPack(currentPack, "deleted");
            refreshLeftList();
            statusMessage = "Quest node deleted";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError("Failed to delete quest node");
        }
    }

    private void changeTreeDependency(QuestEntryData quest, String dependencyId, boolean add) {
        if (currentPack == null || quest == null || quest.path == null || quest.id.equals(dependencyId)) return;
        JsonObject obj = QuestEditorJsonFiles.readJson(quest.path);
        if (obj == null) return;
        List<String> dependencies = new ArrayList<>(QuestEditorEntryCodec.extractEntryLines(quest.dependencies));
        boolean changed = add ? !dependencies.contains(dependencyId) && dependencies.add(dependencyId)
                : dependencies.removeIf(dependencyId::equals);
        if (!changed) return;
        writeDependencies(obj, dependencies);
        if (!add) {
            LinkedHashSet<String> incompatibleDependencies = new LinkedHashSet<>(
                    QuestEditorEntryCodec.extractEntryLines(quest.incompatibleDependencies));
            if (incompatibleDependencies.remove(dependencyId)) {
                writeStringArray(obj, "incompatible_dependencies", incompatibleDependencies);
                quest.incompatibleDependencies = String.join("\n", incompatibleDependencies);
            }
        }
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, quest.path);
            quest.dependencies = String.join("\n", dependencies);
            stageCurrentPackChange(add ? "Dependency connected" : "Dependency removed");
            QuestEditorPackFiles.backupPack(currentPack, add ? "connected" : "disconnected");
            refreshLeftList();
            statusMessage = add ? "Dependency connected" : "Dependency removed";
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError(add ? "Failed to connect quests" : "Failed to remove dependency");
        }
    }

    private static void writeDependencies(JsonObject obj, List<String> dependencies) {
        obj.remove("dependencies");
        if (dependencies == null || dependencies.isEmpty()) return;
        if (dependencies.size() == 1) {
            obj.addProperty("dependencies", dependencies.getFirst());
            return;
        }
        JsonArray array = new JsonArray();
        for (String dependency : dependencies) array.add(dependency);
        obj.add("dependencies", array);
    }

    private static void writeStringArray(JsonObject obj, String key, Iterable<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) array.add(value);
            }
        }
        if (array.isEmpty()) obj.remove(key);
        else obj.add(key, array);
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
        if (!QuestEditorPackFiles.ensurePack(currentPack)) {
            setError(trs("error.open_pack_failed"));
            return;
        }
        String refreshedNamespace = QuestEditorPackFiles.findNamespace(currentPack.root);
        if (refreshedNamespace != null && !refreshedNamespace.isBlank()
                && !refreshedNamespace.equals(currentPack.namespace)) {
        currentPack = new QuestPack(currentPack.name, refreshedNamespace, currentPack.root, currentPack.legacy, currentPack.enabled);
        invalidateQuestListIndex();
        }
        setMode(isTreePack() ? Mode.QUEST_LIST : Mode.CATEGORY_LIST);
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
            rememberQuestListCollapseState();
            refreshLeftList();
            return;
        }
        if (entry.kind == EditorEntryKind.SUBCATEGORY_HEADER) {
            if (!collapsedQuestSubCategories.add(entry.id)) collapsedQuestSubCategories.remove(entry.id);
            rememberQuestListCollapseState();
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
        packLayoutSelector.setTreeLayout(false);

        setActiveFields(List.of(
                field("Display", packNameBox),
                field("Questpack type", packLayoutSelector),
                field("Import existing questline", ftbImportSection)
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
        refreshPackBackups(pack);

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

    private String questListCollapseStateKey() {
        if (currentPack == null || currentPack.root == null) return "";
        return currentPack.root.toAbsolutePath().normalize().toString();
    }

    private void restoreQuestListCollapseState() {
        String key = questListCollapseStateKey();
        collapsedQuestCategories.clear();
        collapsedQuestSubCategories.clear();
        if (key.isBlank()) return;
        collapsedQuestCategories.addAll(QUEST_CATEGORY_COLLAPSE_STATES.getOrDefault(key, Set.of()));
        collapsedQuestSubCategories.addAll(QUEST_SUBCATEGORY_COLLAPSE_STATES.getOrDefault(key, Set.of()));
    }

    private void rememberQuestListCollapseState() {
        String key = questListCollapseStateKey();
        if (key.isBlank()) return;
        QUEST_CATEGORY_COLLAPSE_STATES.put(key, new HashSet<>(collapsedQuestCategories));
        QUEST_SUBCATEGORY_COLLAPSE_STATES.put(key, new HashSet<>(collapsedQuestSubCategories));
    }

    // show category editing fields
    private void showCategoryEditor(CategoryData data, Path sourcePath) {
        editorType = EditorType.CATEGORY;
        editingPath = sourcePath;

        catIdBox.setValue(safe(data.id));
        catNameBox.setValue(safe(data.name));
        catIconBox.setValue(safe(data.icon));
        setCategoryBackgroundValue(data.background);
        catBackgroundDarkeningToggle.setState(parseBool(data.backgroundDarkening, false));
        catBackgroundOpacitySlider.setOpacity(parseFloat(data.backgroundDarkeningOpacity, 0.45f));
        resetCategoryBackgroundAutosaveState();
        catDependencyBox.setValue(safe(data.dependency));
        catAutoCompleteToggle.setState(parseBool(data.autoComplete, false));

        if (isTreePack()) {
            setActiveFields(List.of(
                    field(trs("field.id"), catIdBox),
                    field("Display", catNameBox),
                    field("Background (Tile a full block texture, tile a custom asset, or use a full 121x150 image.)", catBackgroundBox),
                    field("Enable background darkening", catBackgroundDarkeningToggle),
                    field("Background darkening opacity", catBackgroundOpacitySlider)
            ));
        } else {
            setActiveFields(List.of(
                    field(trs("field.id"), catIdBox),
                    field("Display", catNameBox),
                    field("Background (Tile a full block texture, tile a custom asset, or use a full 121x150 image.)", catBackgroundBox),
                    field("Enable background darkening", catBackgroundDarkeningToggle),
                    field("Background darkening opacity", catBackgroundOpacitySlider),
                    field("Unlock quest", catDependencyBox),
                    field(trs("field.auto_complete") + " (" + trs("tooltip.auto_complete_category") + ")", catAutoCompleteToggle)
            ));
        }
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    private void setCategoryBackgroundValue(String encoded) {
        String value = safe(encoded).trim();
        if (value.startsWith("custom_tile:")) {
            catBackgroundMode = CategoryBackgroundMode.CUSTOM_TILEABLE;
            value = value.substring("custom_tile:".length());
        } else if (value.startsWith("full:")) {
            catBackgroundMode = CategoryBackgroundMode.FULL_IMAGE;
            value = value.substring("full:".length());
        } else {
            catBackgroundMode = CategoryBackgroundMode.TILEABLE;
            if (value.startsWith("tile:")) value = value.substring("tile:".length());
        }
        if (catBackgroundBox != null) catBackgroundBox.setValue(value);
    }

    private String encodedCategoryBackground() {
        String value = safe(catBackgroundBox == null ? "" : catBackgroundBox.getValue()).trim();
        if (value.isBlank()) return "";
        return switch (catBackgroundMode) {
            case TILEABLE -> value;
            case CUSTOM_TILEABLE -> "custom_tile:" + value;
            case FULL_IMAGE -> "full:" + value;
        };
    }

    private void cycleCategoryBackgroundMode() {
        catBackgroundMode = switch (catBackgroundMode) {
            case TILEABLE -> CategoryBackgroundMode.CUSTOM_TILEABLE;
            case CUSTOM_TILEABLE -> CategoryBackgroundMode.FULL_IMAGE;
            case FULL_IMAGE -> CategoryBackgroundMode.TILEABLE;
        };
        if (catBackgroundBox != null) catBackgroundBox.setValue("");
        queueCategoryBackgroundAutosave(true);
    }

    private void openCategoryBackgroundPicker() {
        openIconItemPicker(catBackgroundBox);
        itemPickerTab = catBackgroundMode == CategoryBackgroundMode.TILEABLE
                ? ItemPickerTab.CREATIVE : ItemPickerTab.CUSTOM;
        if (catBackgroundMode == CategoryBackgroundMode.FULL_IMAGE) {
            statusMessage = "Use a 121x150 PNG for a full-image category background.";
            statusColor = 0xFFFFFFFF;
        } else if (catBackgroundMode == CategoryBackgroundMode.CUSTOM_TILEABLE) {
            statusMessage = "Choose a seamless PNG to tile across the category background.";
            statusColor = 0xFFFFFFFF;
        }
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
        questMediaSelector.setValue(safe(data.media));
        QuestCategorySelection previousSelection = sourcePath == null && currentPack != null
                ? LAST_QUEST_CATEGORY_SELECTIONS.get(currentPack.name) : null;
        setDropdownBoxValue(questCategoryBox, previousSelection == null ? safe(data.category) : previousSelection.category());
        setDropdownBoxValue(questSubCategoryBox, previousSelection == null ? safe(data.subCategory) : previousSelection.subCategory());
        setEntryRowsFromRaw(EntryRowKind.DEPENDENCY, safe(data.dependencies));
        questIncompatibleDependencies = safe(data.incompatibleDependencies);
        setDependencyLockState(parseBool(data.lockAfterDependency, false));
        questOptionalToggle.setState(parseBool(data.optional, false));
        questRepeatableToggle.setState(parseBool(data.repeatable, false));
        questAutoCompleteToggle.setState(parseBool(data.autoComplete, false));
        questHiddenUnderDependencyToggle.setState(parseBool(data.hiddenUnderDependency, false));
        questTreeX = parseInt(data.treeX, -1);
        questTreeY = parseInt(data.treeY, -1);
        questTreeBackgroundSelector.setBackground(data.treeBackground);
        questTreeOrigin = parseBool(data.treeOrigin, false);
        setEntryRowsFromRaw(EntryRowKind.COMPLETION, QuestEditorEntryCodec.completionJsonToEntries(safe(data.completionJson)));
        setEntryRowsFromRaw(EntryRowKind.REWARD, QuestEditorEntryCodec.rewardJsonToEntries(safe(data.rewardJson)));
        loadedQuestType = safe(data.type);

        questDescriptionBox.scrollToTop();
        questCompletionBox.scrollToTop();
        questRewardBox.scrollToTop();
        questOrderToken = QuestEditorNaming.orderFromPath(sourcePath);

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
        if (Config.showIdField()) fields.add(field(trs("field.id"), questIdBox));
        fields.add(field("Display", questNameBox));
        fields.add(field(trs("field.description"), questDescriptionBox));
        fields.add(field("Image or looping GIF", questMediaSelector));
        fields.add(field(trs("field.category_required"), questCategoryBox));
        if (!dropdownBoxValue(questCategoryBox).isBlank()) {
            fields.add(field(trs("field.subcategory"), questSubCategoryBox));
        } else if (questSubCategoryBox != null) {
            setDropdownBoxValue(questSubCategoryBox, "");
        }
        fields.add(field(trs("field.dependencies"), questDependenciesBox));
        if (isTreePack()) fields.add(field("Quest node background", questTreeBackgroundSelector));
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
        String name = QuestEditorNaming.cleanPackName(packNameBox.getValue());
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
            String iconPath = QuestEditorPackFiles.cleanIconId(packIconPathBox.getValue());
            String layout = packLayoutSelector.isTreeLayout() ? "tree" : "list";
            QuestEditorPackFiles.writePackMeta(root, name, "Boundless Quest Pack: " + name, iconPath, layout, true);
            QuestPack pack = new QuestPack(name, namespace, root, false, true);
            pack.ensureDirs();
            currentPack = pack;
            JsonObject firstCategory = QuestEditorJsonBuilder.buildCategory(
                    namespace, name, iconPath, 0, "", false, "", false, 0.45f, this::setError);
            if (firstCategory == null) return;
            if ("tree".equals(layout)) firstCategory.addProperty("layout", "tree");
            QuestPackStorage.writeJsonAtomically(gson, firstCategory, pack.categoriesDir.resolve(namespace + ".json"));
            QuestEditorPackFiles.backupPack(currentPack, "created");
            if ("tree".equals(layout)) {
                treeCategoryId = namespace;
                setMode(Mode.QUEST_LIST);
            } else {
                setMode(Mode.CATEGORY_LIST);
            }
            stagePackChange(pack, "Pack staged");
        } catch (Exception e) {
            setError("Failed to create pack");
        }
    }

    private void selectFtbQuestImportFolder() {
        if (ftbImportRunning) return;
        Path defaultFolder = minecraft.gameDirectory.toPath().resolve("config").resolve("ftbquests").resolve("quests");
        if (!Files.isDirectory(defaultFolder)) defaultFolder = minecraft.gameDirectory.toPath().resolve("config");
        String selected = TinyFileDialogs.tinyfd_selectFolderDialog(
                "Select config/ftbquests/quests", defaultFolder.toAbsolutePath().toString());
        if (selected == null || selected.isBlank()) return;
        beginFtbQuestImport(Path.of(selected));
    }

    private void beginFtbQuestImport(Path sourceFolder) {
        String requestedName = QuestEditorNaming.cleanPackName(packNameBox.getValue());
        if (requestedName.isBlank()) {
            requestedName = QuestEditorNaming.nextCopyPack("ftb-quests-import");
            packNameBox.setValue(requestedName);
        }
        if (QuestEditorPackFiles.isInvalidPackFolderName(requestedName)) {
            setError("Enter a valid pack name before importing");
            return;
        }
        if (Files.exists(QuestEditorPackFiles.packsRoot().resolve(requestedName))) {
            setError("Pack already exists");
            return;
        }
        packLayoutSelector.setTreeLayout(true);
        ftbImportRunning = true;
        statusMessage = "Importing FTB Quests...";
        statusColor = 0xFFFFFFFF;
        String packName = requestedName;
        CompletableFuture.supplyAsync(() -> {
            try {
                return FtbQuestImporter.importPack(sourceFolder, QuestEditorPackFiles.packsRoot(), packName);
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, Util.ioPool()).whenComplete((result, error) -> minecraft.execute(() -> finishFtbQuestImport(result, error)));
    }

    private void finishFtbQuestImport(FtbQuestImporter.ImportResult result, Throwable error) {
        ftbImportRunning = false;
        if (error != null || result == null) {
            Throwable cause = error;
            while (cause != null && cause.getCause() != null
                    && (cause instanceof CompletionException || cause.getMessage() == null)) {
                cause = cause.getCause();
            }
            String message = cause == null || cause.getMessage() == null ? "Unknown error" : cause.getMessage();
            setError("FTB import failed: " + message);
            return;
        }
        try {
            currentPack = result.pack();
            currentPack.ensureDirs();
            cachedLayoutPack = null;
            selectedEntryId = currentPack.name;
            try {
                QuestEditorPackFiles.backupPack(currentPack, "ftb-imported");
            } catch (IOException ignored) {
            }
            setMode(Mode.QUEST_LIST);
            stagePackChange(currentPack, "Imported " + result.quests() + " quests from " + result.chapters()
                    + " chapters (" + result.warnings().size() + " warning"
                    + (result.warnings().size() == 1 ? "" : "s") + ")");
        } catch (IOException exception) {
            setError("FTB import finished, but the pack could not be opened: " + exception.getMessage());
        }
    }

    private void savePackOptions() {
        if (currentPack == null) return;

        String requestedName = QuestEditorNaming.cleanPackName(packNameBox.getValue());
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
            String iconPath = QuestEditorPackFiles.cleanIconId(packIconPathBox.getValue());
            PackMeta meta = QuestEditorPackFiles.readPackMeta(newRoot, requestedName);
            QuestEditorPackFiles.writePackMeta(newRoot, requestedName, description, iconPath, meta.layout, currentPack.enabled);
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
        if (isTreePack()) treeCategoryId = id;
        JsonObject obj = buildCategoryJson(id);
        if (obj == null) return;
        Path target = currentPack.categoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
        if (editingPath != null) {
            QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, isSingleplayerAuthoritySession());
            Minecraft.getInstance().execute(() -> {
                QuestData.loadClient(true);
                QuestPanelClient.applyConfigChanges();
            });
        }
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
        JsonObject category = QuestEditorJsonBuilder.buildCategory(
                id,
                catNameBox.getValue(),
                catIconBox.getValue(),
                resolveCategoryOrder(id),
                catDependencyBox.getValue(),
                catAutoCompleteToggle.isOn(),
                encodedCategoryBackground(),
                catBackgroundDarkeningToggle.isOn(),
                catBackgroundOpacitySlider.getOpacity(),
                this::setError
        );
        if (category != null && isTreePack()) category.addProperty("layout", "tree");
        return category;
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
        String id = Config.showIdField() ? safe(questIdBox.getValue()).trim() : questIdFromName(questNameBox.getValue());
        questIdBox.setValue(id);
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
            Minecraft.getInstance().execute(() -> {
                QuestData.loadClient(true);
                QuestPanelClient.applyConfigChanges();
            });
            statusMessage = trs("status.quest_saved");
            statusColor = 0xA0FFA0;
        }
        if (editingPath != null) rememberQuestCategorySelection();
    }

    private static String questIdFromName(String name) {
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized;
    }

    private void rememberQuestCategorySelection() {
        if (currentPack == null) return;
        LAST_QUEST_CATEGORY_SELECTIONS.put(currentPack.name, new QuestCategorySelection(
                dropdownBoxValue(questCategoryBox), dropdownBoxValue(questSubCategoryBox)));
    }

    private JsonObject buildQuestJson(String id) {
        syncEntryBackingValues();
        QuestBuildFields fields = new QuestBuildFields();
        fields.id = id;
        fields.duplicateQuestId = isDuplicateQuestId(safe(id).trim());
        fields.name = questNameBox.getValue();
        fields.icon = questIconBox.getValue();
        fields.description = questDescriptionBox.getValue();
        fields.media = questMediaSelector.value();
        fields.category = dropdownBoxValue(questCategoryBox);
        fields.subCategory = dropdownBoxValue(questSubCategoryBox);
        fields.optional = questOptionalToggle.isOn();
        fields.repeatable = questRepeatableToggle.isOn();
        fields.autoComplete = questAutoCompleteToggle.isOn();
        fields.hiddenUnderDependency = questHiddenUnderDependencyToggle.isOn();
        fields.lockAfterDependency = dependencyLockState();
        fields.loadedQuestType = loadedQuestType;
        fields.dependencies = collectDependencyEntries();
        fields.incompatibleDependencies = QuestEditorEntryCodec.extractEntryLines(questIncompatibleDependencies);
        fields.completionRaw = questCompletionBox.getValue();
        fields.rewardRaw = questRewardBox.getValue();
        fields.treeX = questTreeX;
        fields.treeY = questTreeY;
        fields.treeBackground = questTreeBackgroundSelector.background();
        fields.treeOrigin = questTreeOrigin;
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
            if (!QuestEditorPackFiles.ensurePack(currentPack)) {
                setError("Failed to open pack");
                return;
            }
            PackMeta meta = QuestEditorPackFiles.readPackMeta(currentPack.root, currentPack.name);
            String duplicateName = nextAvailablePackName(currentPack.name);
            Path target = QuestEditorPackFiles.packsRoot().resolve(duplicateName);
            QuestEditorPackFiles.mirrorDirectory(currentPack.root, target);
            QuestEditorPackFiles.writePackMeta(target, duplicateName, safe(meta.description), safe(meta.iconPath), meta.layout, meta.enabled);
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
            if (currentPack == null || !QuestEditorPackFiles.ensurePack(currentPack)) {
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
        String orderToken = QuestEditorNaming.nextOrder(currentPack);
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
        return QuestEditorNaming.nextCopyId(base, currentPack.questsDir);
    }

    private String nextAvailableCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextCopyId(base, currentPack.categoriesDir);
    }

    private String nextAvailableSubCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextCopyId(base, currentPack.subCategoriesDir);
    }

    private String nextAvailablePackName(String baseName) {
        return QuestEditorNaming.nextCopyPack(baseName);
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
        boolean singleplayerAuthority = isSingleplayerAuthoritySession();

        // apply staged deletes before staged writes
        for (String packName : new ArrayList<>(stagedDeletedPackNames)) {
            if (!singleplayerAuthority) {
                QuestEditorPackFiles.sendDeleteQuestPackToServer(packName);
            }
            QuestEditorPackFiles.deleteAppliedPackArtifactsSafe(packName);
        }
        for (QuestPack pack : new ArrayList<>(stagedPacks.values())) {
            if (pack == null) continue;
            currentPack = pack;
            QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, singleplayerAuthority);
            if (!singleplayerAuthority) {
                QuestEditorPackFiles.sendPack(currentPack, currentPack.enabled);
            }
            if (selectedPack == null) {
                selectedPack = pack;
            }
        }

        currentPack = selectedPack;
        stagedPacks.clear();
        stagedDeletedPackNames.clear();

        // An edited pack can already be at the applied path, so no file copy is needed.
        // It still needs a quest-data reload after the editor is closed.
        if (singleplayerAuthority) {
            QuestEditorPackFiles.reloadLater();
        }
        mc.execute(() -> QuestData.loadClient(true));
    }

    private boolean isSingleplayerAuthoritySession() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getSingleplayerServer() != null;
    }

    // this refreshes an open editor after a server sync
    public void refreshSyncedData() {
        if (mode == Mode.PACK_LIST) refreshLeftList();
    }

    private void exportCurrentPack() {
        if (currentPack == null || currentPack.root == null || !Files.isDirectory(currentPack.root)) {
            setError("No questpack to export");
            return;
        }
        try {
            Path zipPath = QuestEditorPackFiles.nextZipPath(currentPack);
            QuestEditorPackFiles.zipDirectory(currentPack.root, zipPath);
            statusMessage = trs("status.exported", zipPath.getFileName());
            statusColor = 0xA0FFA0;
            QuestEditorPackFiles.openParentFolder(zipPath);
        } catch (Exception e) {
            setError("Failed to export questpack");
        }
    }

    private void refreshPackBackups(QuestPack pack) {
        if (pack == null || pack.name == null || pack.name.isBlank()) {
            packBackups = List.of();
            return;
        }
        try {
            packBackups = QuestPackStorage.listSnapshotDetails(pack.name);
        } catch (IOException ignored) {
            packBackups = List.of();
        }
    }

    private void confirmRestoreBackup(QuestPackStorage.Snapshot snapshot) {
        if (snapshot == null || currentPack == null) return;
        ScreenState returnState = captureState();
        String timestamp = BACKUP_DISPLAY_TIME.format(snapshot.createdAt());
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            pendingState = returnState;
            minecraft.setScreen(this);
            if (confirmed) restoreBackup(snapshot);
        }, Component.literal(trs("backup.restore_confirm_title")),
                Component.literal(trs("backup.restore_confirm_message", timestamp))));
    }

    private void restoreBackup(QuestPackStorage.Snapshot snapshot) {
        QuestPack pack = currentPack;
        if (pack == null || snapshot == null) return;
        boolean previousEnabled = pack.enabled;
        try {
            QuestPackStorage.restoreSnapshot(pack.root, pack.name, snapshot.path());
            PackMeta restoredMeta = QuestEditorPackFiles.readPackMeta(pack.root, pack.name);
            if (isSingleplayerAuthoritySession()) {
                Config.setQuestPackApplied(pack.name, restoredMeta.enabled);
                QuestEditorPackFiles.reloadLater();
            } else {
                Config.setSyncedQuestPackApplied(pack.name, restoredMeta.enabled);
                if (!QuestEditorPackFiles.sendPack(pack, restoredMeta.enabled)) {
                    Config.setSyncedQuestPackApplied(pack.name, previousEnabled);
                    throw new IOException("Failed to upload restored questpack");
                }
            }
            currentPack = QuestEditorPackFiles.findPackByName(pack.name);
            if (currentPack == null) currentPack = pack;
            invalidateQuestListIndex();
            QuestData.loadClient(true);
            QuestPanelClient.applyConfigChanges();
            showPackOptions(currentPack);
            refreshLeftList();
            statusMessage = trs("status.backup_restored", BACKUP_DISPLAY_TIME.format(snapshot.createdAt()));
            statusColor = 0xA0FFA0;
        } catch (Exception ignored) {
            setError(trs("error.restore_backup_failed"));
        }
    }

    private void clearEditor() {
        closeTransientMenus();
        editorType = EditorType.NONE;
        editingPath = null;
        loadedQuestType = "";
        questIncompatibleDependencies = "";
        questTreeOrigin = false;
        disarmDeleteConfirm();
        clearPendingDiscardState();
        savedEditorState = null;
        packBackups = List.of();
        backupRowHitboxes.clear();
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
        emojiPickerOpen = false;
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
            LockToggleButton button = makeDepLock(dependencyLockState());
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
        if ("__emoji__".equals(formatCode)) {
            emojiPickerOpen = !emojiPickerOpen;
            emojiPickerPage = 0;
            return;
        }
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
        questDescriptionBox.wrapSelection(formatCode, "/x");
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

    private record QuestCategorySelection(String category, String subCategory) {
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
        fields.packTreeLayout = packLayoutSelector != null && packLayoutSelector.isTreeLayout();
        fields.categoryId = safe(catIdBox == null ? "" : catIdBox.getValue());
        fields.categoryName = safe(catNameBox == null ? "" : catNameBox.getValue());
        fields.categoryIcon = safe(catIconBox == null ? "" : catIconBox.getValue());
        fields.categoryBackground = encodedCategoryBackground();
        fields.categoryBackgroundDarkening = catBackgroundDarkeningToggle != null && catBackgroundDarkeningToggle.isOn();
        fields.categoryBackgroundDarkeningOpacity = catBackgroundOpacitySlider == null ? 0.45f : catBackgroundOpacitySlider.getOpacity();
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
        fields.questMedia = questMediaSelector == null ? "" : questMediaSelector.value();
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
            String normalized = QuestEditorNaming.cleanId(value, commaSeparated);
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
            String normalized = QuestEditorNaming.cleanPackName(value);
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

    private float parseFloat(String raw, float fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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
            String fromPath = QuestEditorNaming.orderFromPath(editingPath);
            if (!fromPath.isBlank()) return fromPath;
        }

        return QuestEditorNaming.nextOrder(currentPack);
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
        state.packTreeLayout = packLayoutSelector != null && packLayoutSelector.isTreeLayout();

        state.catId = safe(catIdBox == null ? "" : catIdBox.getValue());
        state.catName = safe(catNameBox == null ? "" : catNameBox.getValue());
        state.catIcon = safe(catIconBox == null ? "" : catIconBox.getValue());
        state.catBackground = encodedCategoryBackground();
        state.catBackgroundDarkening = catBackgroundDarkeningToggle != null && catBackgroundDarkeningToggle.isOn();
        state.catBackgroundDarkeningOpacity = catBackgroundOpacitySlider == null ? 0.45f : catBackgroundOpacitySlider.getOpacity();
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
        state.questMedia = questMediaSelector == null ? "" : questMediaSelector.value();
        state.questCategory = safe(questCategoryBox == null ? "" : questCategoryBox.getValue());
        state.questSubCategory = safe(questSubCategoryBox == null ? "" : questSubCategoryBox.getValue());
        state.questDependencies = safe(questDependenciesBox == null ? "" : questDependenciesBox.getValue());
        state.questLockAfterDependency = dependencyLockState();
        state.questIncompatibleDependencies = questIncompatibleDependencies;
        state.questTreeOrigin = questTreeOrigin;
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
                packLayoutSelector.setTreeLayout(state.packTreeLayout);
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
                data.background = state.catBackground;
                data.backgroundDarkening = state.catBackgroundDarkening ? "true" : "false";
                data.backgroundDarkeningOpacity = Float.toString(state.catBackgroundDarkeningOpacity);
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
                data.media = state.questMedia;
                data.category = state.questCategory;
                data.subCategory = state.questSubCategory;
                data.dependencies = state.questDependencies;
                data.lockAfterDependency = state.questLockAfterDependency ? "true" : "false";
                data.incompatibleDependencies = state.questIncompatibleDependencies;
                data.treeOrigin = state.questTreeOrigin ? "true" : "false";
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
            questTree.setTooltipSuppressed(isTreeTooltipCovered(mouseX, mouseY));
            questTree.render(gg, mouseX, mouseY, partialTick);
        }
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 500);
        for (CategoryWorkspaceTab tab : categoryWorkspaceTabs) {
            if (tab.visible) tab.render(gg, mouseX, mouseY, partialTick);
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
        if (!isItemPickerOpen() && leftList != null) leftList.renderHoverTooltipOnTop(gg);
        if (!isItemPickerOpen()) renderTabTooltip(gg, mouseX, mouseY);
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
        renderPendingEditorTooltip(gg);
        gg.pose().popPose();
    }

    private void queueEditorTooltip(Component tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.getString().isBlank()) return;
        queueEditorTooltip(List.of(tooltip), mouseX, mouseY);
    }

    private void queueEditorTooltip(List<Component> tooltip, int mouseX, int mouseY) {
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
            treeExpandButton.setX(questTree.getX() + questTree.getWidth() - 41);
            treeExpandButton.setY(questTree.getY() + 3);
            treeExpandButton.visible = showTree;
            treeExpandButton.active = showTree;
        }
        if (originButton != null) {
            originButton.setX(pxRight + pw - HEADER_ICON_SIZE - 2);
            originButton.setY(py + 1);
            originButton.visible = editorType == EditorType.QUEST && isTreePack();
            originButton.active = originButton.visible;
            originButton.setTextures(questTreeOrigin ? ORIGIN_HOVER_TEX : ORIGIN_TEX, ORIGIN_HOVER_TEX);
        }
        if (questSearchBox != null) {
            boolean showQuestSearch = mode == Mode.QUEST_LIST && currentPack != null && !isTreePack();
            int searchX = pxLeft + 2 + SMALL_BTN_SIZE + SMALL_BTN_GAP;
            questSearchBox.setX(searchX);
            questSearchBox.setY(barY);
            questSearchBox.setWidth(leftPw - 4 - SMALL_BTN_SIZE - SMALL_BTN_GAP);
            questSearchBox.visible = showQuestSearch;
            questSearchBox.active = showQuestSearch;
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

    private boolean isTreeTooltipCovered(int mouseX, int mouseY) {
        if (isHovered(treeMoveButton, mouseX, mouseY)
                || isHovered(createEntryButton, mouseX, mouseY)
                || isHovered(treeDeleteButton, mouseX, mouseY)
                || isHovered(treeConnectorButton, mouseX, mouseY)
                || isHovered(treeExpandButton, mouseX, mouseY)) return true;
        for (CategoryWorkspaceTab tab : categoryWorkspaceTabs) {
            if (isHovered(tab, mouseX, mouseY)) return true;
        }
        return false;
    }

    private boolean isHovered(AbstractWidget widget, double mouseX, double mouseY) {
        return widget != null && widget.visible && widget.isMouseOver(mouseX, mouseY);
    }

    private void layoutTabButton(EditorTabButton button, int x, int y) {
        if (button == null) return;
        button.setX(x);
        button.setY(y);
        boolean visible = currentPack != null && !isTreePack()
                && (mode == Mode.CATEGORY_LIST || mode == Mode.SUBCATEGORY_LIST || mode == Mode.QUEST_LIST);
        button.visible = visible;
        button.active = visible;
    }

    private void renderSideTabs(GuiGraphics gg, int mouseX, int mouseY) {
        if (categoriesTabButton != null && categoriesTabButton.visible) categoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (subCategoriesTabButton != null && subCategoriesTabButton.visible) subCategoriesTabButton.render(gg, mouseX, mouseY, 0f);
        if (questsTabButton != null && questsTabButton.visible) questsTabButton.render(gg, mouseX, mouseY, 0f);
    }

    private void renderTabTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        boolean directTooltipHovered = isHovered(categoriesTabButton, mouseX, mouseY)
                || isHovered(subCategoriesTabButton, mouseX, mouseY)
                || isHovered(questsTabButton, mouseX, mouseY)
                || isHovered(importQuestPackButton, mouseX, mouseY)
                || isHovered(createEntryButton, mouseX, mouseY)
                || isHovered(treeMoveButton, mouseX, mouseY)
                || isHovered(treeDeleteButton, mouseX, mouseY)
                || isHovered(treeConnectorButton, mouseX, mouseY)
                || isHovered(originButton, mouseX, mouseY)
                || isHovered(packLayoutSelector, mouseX, mouseY)
                || isHovered(duplicateButton, mouseX, mouseY)
                || isHovered(deleteQuestButton, mouseX, mouseY)
                || isHovered(backButton, mouseX, mouseY);
        if (directTooltipHovered) pendingEditorTooltip = List.of();
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
            gg.renderTooltip(font, font.split(createEntryTooltip(), 170), mouseX, mouseY);
            return;
        }
        if (treeMoveButton != null && treeMoveButton.visible && treeMoveButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal("Move nodes or drag empty space to pan the tree."), mouseX, mouseY);
            return;
        }
        if (treeDeleteButton != null && treeDeleteButton.visible && treeDeleteButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, font.split(Component.literal("Delete nodes or dependency lines. "
                    + "Hold Shift while clicking to bypass the confirmation prompt."), 170), mouseX, mouseY);
            return;
        }
        if (treeConnectorButton != null && treeConnectorButton.visible && treeConnectorButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, font.split(Component.literal("Connect nodes: select the dependency first, "
                    + "then select the quest that should depend on it. Shift-double-click a line to toggle "
                    + "a red incompatibility."), 190), mouseX, mouseY);
            return;
        }
        if (originButton != null && originButton.visible && originButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, Component.literal(questTreeOrigin
                    ? "This quest is the tree origin."
                    : "Make this quest the tree origin."), mouseX, mouseY);
            return;
        }
        if (packLayoutSelector != null && packLayoutSelector.visible && packLayoutSelector.isMouseOver(mouseX, mouseY)) {
            Component tooltip = packLayoutSelector.hoveredTooltip(mouseX, mouseY);
            if (tooltip != null) gg.renderTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
        if (duplicateButton != null && duplicateButton.visible && duplicateButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr("tooltip.duplicate"), mouseX, mouseY);
            return;
        }
        if (deleteQuestButton != null && deleteQuestButton.visible && deleteQuestButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, deleteConfirmArmed ? tr("tooltip.hold_delete") : tr("tooltip.delete"), mouseX, mouseY);
            return;
        }
        if (backButton != null && backButton.visible && backButton.isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(font, tr("tooltip.back"), mouseX, mouseY);
        }
    }

    private boolean isUnsavedChangesPopupOpen() {
        return pendingTreeDeleteAction != null
                || pendingEditorExit
                || pendingBackNavigation
                || (pendingDiscardMode != null && !pendingDiscardEntryId.isBlank());
    }

    private void renderUnsavedChangesPopup(GuiGraphics gg, int mouseX, int mouseY) {
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
        renderIconOverlay(gg, catBackgroundBox);
        renderIconOverlay(gg, subIconBox);
        // no extra reward fields
        gg.disableScissor();
    }

    private void renderIconOverlay(GuiGraphics gg, EditBox box) {
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
        if (catBackgroundBox != null && catBackgroundBox.visible && catBackgroundBox.isFocused()) return catBackgroundBox;
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
        for (FormField field : activeFields) {
            if (field.widget == catBackgroundOpacitySlider && !catBackgroundDarkeningToggle.isOn()) {
                field.widget.visible = false;
                field.widget.active = false;
                continue;
            }
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
                } else if (field.widget instanceof PackLayoutSelector selector) {
                    selector.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestTreeBackgroundSelector selector) {
                    selector.setWidth(widgetWidth);
                } else if (field.widget instanceof CategoryBackgroundOpacitySlider slider) {
                    slider.setWidth(widgetWidth);
                } else if (field.widget instanceof FtbImportSection section) {
                    section.setWidth(widgetWidth);
                } else if (field.widget instanceof QuestMediaSelector selector) {
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
                    catBackgroundModeButton.setMessage(Component.literal(catBackgroundMode.label));
                    catBackgroundModeButton.visible = inside;
                    catBackgroundModeButton.active = inside;
                    catBackgroundAssetButton.setPosition(modeX + modeWidth + gap, boxY);
                    catBackgroundAssetButton.setWidth(assetWidth);
                    catBackgroundAssetButton.setHeight(BOX_H);
                    catBackgroundAssetButton.setMessage(Component.literal(catBackgroundMode == CategoryBackgroundMode.TILEABLE ? "Blocks" : "Asset"));
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
            if (!isItemPickerOpen() && catBackgroundModeButton != null && catBackgroundModeButton.visible
                    && catBackgroundModeButton.isMouseOver(mouseX, mouseY)) {
                queueEditorTooltip(Component.literal(switch (catBackgroundMode) {
                    case TILEABLE -> "Tile one full block texture across the category background.";
                    case CUSTOM_TILEABLE -> "Tile a custom PNG asset across the category background.";
                    case FULL_IMAGE -> "Stretch one custom PNG across the background. Use a 121x150 image.";
                }), mouseX, mouseY);
            } else if (!isItemPickerOpen() && catBackgroundAssetButton != null && catBackgroundAssetButton.visible
                    && catBackgroundAssetButton.isMouseOver(mouseX, mouseY)) {
                queueEditorTooltip(Component.literal(catBackgroundMode == CategoryBackgroundMode.TILEABLE
                        ? "Choose a full block texture." : "Choose or add a custom PNG asset."), mouseX, mouseY);
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

    private void renderPackBackups(GuiGraphics gg, int mouseX, int mouseY, int yCursor, int clipTop, int clipBottom) {
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

    private void drawScaledText(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0f);
        gg.pose().scale(scale, scale, 1f);
        gg.drawString(font, text, 0, 0, color, false);
        gg.pose().popPose();
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
        mouseX /= uiScale;
        mouseY /= uiScale;
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
                || anyVisibleMultiBoxContains(rewardEntryBoxes, mouseX, mouseY);
    }

    private boolean isInsideBox(EditBox box, double mouseX, double mouseY) {
        return box != null && box.visible
                && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
    }

    private EditBox clickedVisibleIconBox(double mouseX, double mouseY) {
        EditBox[] boxes = {packIconPathBox, catIconBox, catBackgroundBox, subIconBox, questIconBox};
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
            if (field.widget == catBackgroundOpacitySlider && !catBackgroundDarkeningToggle.isOn()) continue;
            total += font.lineHeight + FIELD_LABEL_GAP + fieldHeight(field) + FIELD_ROW_GAP;
            if (editorType == EditorType.PACK_OPTIONS && field.widget == packNameBox) {
                total += 20 + FIELD_ROW_GAP;
                total += backupSectionHeight();
            }
        }
        return total;
    }

    private int backupSectionHeight() {
        int rowsHeight = packBackups.isEmpty()
                ? font.lineHeight
                : packBackups.size() * (BACKUP_ROW_H + BACKUP_ROW_GAP) - BACKUP_ROW_GAP;
        return font.lineHeight + FIELD_LABEL_GAP + rowsHeight + FIELD_ROW_GAP;
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
            button.setY("__emoji__".equals(button.insertText()) ? y - 2 : y);
            button.visible = visible;
            button.active = visible && descriptionFormatButtonActive(button);
            x += button.getWidth() + FORMAT_BTN_GAP;
        }
    }

    private boolean isDescriptionUndoRedoButton(TextInsertButton button) {
        if (button == null) return false;
        String action = button.insertText();
        return "__emoji__".equals(action) || "__undo__".equals(action) || "__redo__".equals(action);
    }

    private boolean descriptionFormatButtonActive(TextInsertButton button) {
        if (button == null) return false;
        String action = button.insertText();
        if ("__emoji__".equals(action)) return true;
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
            boolean visible = mode == Mode.QUEST_LIST && currentPack != null && !isTreePack();
            questSearchBox.visible = visible;
            questSearchBox.active = visible;
        }
    }

    private void goBack() {
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

    private void performBackNavigation() {
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

    private void exitEditor() {
        if (closingEditor) return;
        flushCategoryBackgroundAutosave();
        closingEditor = true;
        clearPendingInitState();
        applyStagedChangesOnClose();
        Minecraft.getInstance().setScreen(parent);
    }

    private void requestExitEditor() {
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
        super.tick();
        tickCategoryBackgroundAutosave();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX /= uiScale;
        mouseY /= uiScale;
        if (isUnsavedChangesPopupOpen()) {
            return handleUnsavedChangesPopupClick(mouseX, mouseY, button);
        }
        if (button == 0 && clickPackBackup(mouseX, mouseY)) return true;
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
            CategoryData category = QuestEditorJsonFiles.loadCategory(currentPack, treeCategoryId);
            if (category != null) showCategoryEditor(category, category.path);
        }
        boolean categoryBackgroundToggleClicked = button == 0 && catBackgroundDarkeningToggle != null
                && catBackgroundDarkeningToggle.visible && catBackgroundDarkeningToggle.isMouseOver(mouseX, mouseY);
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled && categoryBackgroundToggleClicked) queueCategoryBackgroundAutosave(true);
        return handled;
    }

    private boolean clickPackBackup(double mouseX, double mouseY) {
        if (editorType != EditorType.PACK_OPTIONS) return false;
        for (BackupRowHitbox hitbox : backupRowHitboxes) {
            if (!hitbox.contains(mouseX, mouseY)) continue;
            confirmRestoreBackup(hitbox.snapshot());
            return true;
        }
        return false;
    }

    private boolean handleUnsavedChangesPopupClick(double mouseX, double mouseY, int button) {
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

    private boolean clickEmojiPicker(double mouseX, double mouseY, int button) {
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

    private void renderEmojiButtonIcon(GuiGraphics gg) {
        for (TextInsertButton button : descriptionFormatButtons) {
            if ("__emoji__".equals(button.insertText()) && button.visible) {
                gg.blit(EMOJI_BUTTON_TEX, button.getX() + 1, button.getY() + 1, 0, 0, 10, 10, 10, 10);
                return;
            }
        }
    }

    private void renderEmojiPicker(GuiGraphics gg, int mouseX, int mouseY) {
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
        return super.mouseDragged(mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
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
                statusMessage = "use custom assets, 16x16 icons recommended, upload to config/boundless/assets";
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

    private boolean isFullBackgroundBlock(Block block) {
        if (block == null) return false;
        var state = block.defaultBlockState();
        return !state.isAir()
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                && state.getRenderShape() != net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    private void applyPickedItem(ItemStack picked) {
        if (!isItemPickerOpen()) return;
        if (itemPickerIconTarget != null) {
            if (picked == null || picked.isEmpty()) return;
            ResourceLocation id;
            if (itemPickerIconTarget == catBackgroundBox) {
                if (catBackgroundMode != CategoryBackgroundMode.TILEABLE) return;
                if (!(picked.getItem() instanceof BlockItem blockItem)) {
                    setError("Category background must be a block");
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
            if (itemPickerIconTarget == catBackgroundBox) queueCategoryBackgroundAutosave(true);
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

    private void applyPickedIcon(String id) {
        if (itemPickerIconTarget == null || id == null || id.isBlank()) return;
        if (itemPickerIconTarget == stickerIconPickerTarget) {
            if (questTree != null) questTree.addIconSticker(pendingStickerIconX, pendingStickerIconY, id);
            return;
        }
        if (itemPickerIconTarget == catBackgroundBox) {
            if (catBackgroundMode == CategoryBackgroundMode.TILEABLE) {
                setError("Category background must be a full block");
                return;
            }
            itemPickerIconTarget.setValue(id);
            itemPickerIconTarget.setCursorPosition(id.length());
            queueCategoryBackgroundAutosave(true);
            return;
        }
        itemPickerIconTarget.setValue(id);
        itemPickerIconTarget.setCursorPosition(id.length());
        markCurrentEditorUnsaved();
    }

    private void openCustomAssetsDirectory() {
        try {
            java.nio.file.Files.createDirectories(QuestEditorCustomIcons.directory());
            Util.getPlatform().openUri(QuestEditorCustomIcons.directory().toUri());
        } catch (Exception e) {
            setError("Could not open custom assets folder");
        }
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
        return box == packIconPathBox || box == questIconBox || box == catIconBox
                || box == catBackgroundBox || box == subIconBox;
    }

    private EditBox focusedEditBox() {
        if (packIconPathBox != null && packIconPathBox.isFocused()) return packIconPathBox;
        if (questIconBox != null && questIconBox.isFocused()) return questIconBox;
        if (catIconBox != null && catIconBox.isFocused()) return catIconBox;
        if (catBackgroundBox != null && catBackgroundBox.isFocused()) return catBackgroundBox;
        if (subIconBox != null && subIconBox.isFocused()) return subIconBox;
        return null;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt((raw == null ? "" : raw).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void selectQuestMediaFile() {
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
            statusMessage = "Media copied to config/boundless/media";
            statusColor = 0xA0FFA0;
        } catch (Exception exception) {
            setError("Could not import quest media");
        }
    }

    private final class QuestMediaSelector extends AbstractWidget {
        private String value = "";

        QuestMediaSelector() {
            super(0, 0, 1, BOX_H, Component.empty());
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
                markCurrentEditorUnsaved();
                return true;
            }
            if (button != 0) return false;
            selectQuestMediaFile();
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation sprite = isMouseOver(mouseX, mouseY)
                    ? ResourceLocation.withDefaultNamespace("widget/button_highlighted")
                    : ResourceLocation.withDefaultNamespace("widget/button");
            graphics.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
            String label = value.isBlank() ? "Browse config/boundless/media..." : value;
            String clipped = font.plainSubstrByWidth(label, Math.max(1, getWidth() - 8));
            graphics.drawString(font, clipped, getX() + 4, getY() + 6, 0xFFFFFFFF, false);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private final class FtbImportSection extends AbstractWidget {
        private static final int HEIGHT = 138;
        private static final int BUTTON_HEIGHT = 20;
        private static final String DESCRIPTION = "Select config/ftbquests/quests. Chapters become tree categories. "
                + "Quest positions, dependencies, icons, text, tasks and rewards are converted. Unsupported entries "
                + "are replaced where possible and listed in ftb-import-report.txt. Your FTB files are never changed.";

        FtbImportSection() {
            super(0, 0, 1, HEIGHT, Component.empty());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isButtonHovered(mouseX, mouseY)) return false;
            selectFtbQuestImportFolder();
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
                    : (isButtonHovered(mouseX, mouseY)
                    ? ResourceLocation.withDefaultNamespace("widget/button_highlighted")
                    : ResourceLocation.withDefaultNamespace("widget/button"));
            graphics.blitSprite(sprite, getX(), getY(), getWidth(), BUTTON_HEIGHT);
            String label = ftbImportRunning ? "Importing..." : "Import from FTB Quests";
            graphics.drawString(font, label, getX() + (getWidth() - font.width(label)) / 2,
                    getY() + 6, active ? 0xFFFFFFFF : 0xFF808080, false);
            int lineY = getY() + BUTTON_HEIGHT + 6;
            for (var line : font.split(Component.literal(DESCRIPTION), Math.max(20, getWidth()))) {
                if (lineY + font.lineHeight > getY() + getHeight()) break;
                graphics.drawString(font, line, getX(), lineY, 0xFFA0A0A0, false);
                lineY += font.lineHeight;
            }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private final class PackLayoutSelector extends AbstractWidget {
        private static final int HEIGHT = 22;
        private boolean treeLayout;

        PackLayoutSelector() {
            super(0, 0, 1, HEIGHT, Component.empty());
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
                markCurrentEditorUnsaved();
            }
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int buttonY = getY() + 1;
            int halfWidth = Math.max(1, (getWidth() - 3) / 2);
            renderChoice(gg, getX(), buttonY, halfWidth, "List", !treeLayout, mouseX, mouseY);
            renderChoice(gg, getX() + halfWidth + 3, buttonY, halfWidth, "Tree", treeLayout, mouseX, mouseY);
        }

        private void renderChoice(GuiGraphics gg, int x, int y, int width, String label, boolean selected, int mouseX, int mouseY) {
            ResourceLocation sprite = selected
                    ? ResourceLocation.withDefaultNamespace("widget/button")
                    : ResourceLocation.withDefaultNamespace("widget/button_disabled");
            gg.blitSprite(sprite, x, y, width, 20);
            int textColor = selected ? 0xFFFFFFFF : 0xFF808080;
            gg.drawString(font, label, x + (width - font.width(label)) / 2, y + 6, textColor, false);
        }

        Component hoveredTooltip(double mouseX, double mouseY) {
            int halfWidth = Math.max(1, (getWidth() - 3) / 2);
            int buttonY = getY() + 1;
            if (mouseY < buttonY || mouseY > buttonY + 20) return null;
            return mouseX <= getX() + halfWidth
                    ? Component.literal("Use a linear quest list.")
                    : Component.literal("Use a visual quest tree.");
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private final class CategoryBackgroundOpacitySlider extends AbstractSliderButton {
        CategoryBackgroundOpacitySlider(int x, int y, int width, int height, float opacity) {
            super(x, y, width, height, Component.empty(), Math.max(0d, Math.min(1d, opacity)));
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
            queueCategoryBackgroundAutosave(false);
        }
    }

    private final class QuestTreeBackgroundSelector extends AbstractWidget {
        private static final int HEIGHT = 34;
        private static final int ICON_SIZE = 8;
        private static final int BUTTON_GAP = 2;
        private static final int BUTTON_Y = 1;
        private static final int BUTTON_HEIGHT = 32;
        private String background = "square";

        QuestTreeBackgroundSelector() {
            super(0, 0, 1, HEIGHT, Component.empty());
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
            int buttonWidth = Math.max(1, (getWidth() - BUTTON_GAP * 3) / 4);
            int index = (int) ((mouseX - getX()) / (buttonWidth + BUTTON_GAP));
            String[] values = {"circle", "diamond", "rounded_square", "square"};
            int buttonX = getX() + index * (buttonWidth + BUTTON_GAP);
            if (index < 0 || index >= values.length || mouseX > buttonX + buttonWidth
                    || mouseY < getY() + BUTTON_Y || mouseY > getY() + BUTTON_Y + BUTTON_HEIGHT) return false;
            if (background.equals(values[index])) return true;
            boolean alreadyUnsaved = hasUnsavedEditorChanges();
            background = values[index];
            autosaveTreeBackground(background, alreadyUnsaved);
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            String[] values = {"circle", "diamond", "rounded_square", "square"};
            int buttonWidth = Math.max(1, (getWidth() - BUTTON_GAP * 3) / 4);
            for (int index = 0; index < values.length; index++) {
                int x = getX() + index * (buttonWidth + BUTTON_GAP);
                int y = getY() + BUTTON_Y;
                boolean selected = background.equals(values[index]);
                boolean hovered = mouseX >= x && mouseX <= x + buttonWidth
                        && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
                ResourceLocation buttonTexture = selected || hovered
                        ? ResourceLocation.withDefaultNamespace("widget/button_highlighted")
                        : ResourceLocation.withDefaultNamespace("widget/button");
                gg.blitSprite(buttonTexture, x, y, buttonWidth, BUTTON_HEIGHT);
                gg.pose().pushPose();
                gg.pose().translate(x + (buttonWidth - ICON_SIZE) / 2f, y + (BUTTON_HEIGHT - ICON_SIZE) / 2f, 0);
                gg.pose().scale(0.25f, 0.25f, 1f);
                gg.blit(ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/tree/" + values[index].replace('_', '-') + "-incomplete.png"),
                        0, 0, 0, 0, 32, 32, 32, 32);
                gg.pose().popPose();
            }
        }

        private String normalize(String value) {
            return switch (safe(value).trim().toLowerCase(Locale.ROOT)) {
                case "circle", "diamond", "rounded_square", "square" -> safe(value).trim().toLowerCase(Locale.ROOT);
                default -> "square";
            };
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private void renderCategoryBackgroundPreview(GuiGraphics gg, EditBox box) {
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

    private final class TreeToolButton extends AbstractWidget {
        private final Tool tool;
        private final Runnable onPress;
        private boolean selected;

        TreeToolButton(Tool tool, Runnable onPress) {
            super(0, 0, SMALL_BTN_SIZE, SMALL_BTN_SIZE, Component.empty());
            this.tool = tool;
            this.onPress = onPress;
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
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation background = selected || isMouseOver(mouseX, mouseY)
                    ? ResourceLocation.withDefaultNamespace("widget/button_highlighted")
                    : ResourceLocation.withDefaultNamespace("widget/button");
            gg.blitSprite(background, getX(), getY(), getWidth(), getHeight());
            if (tool == Tool.DELETE) {
                ResourceLocation texture = selected ? DELETE_ENABLED_TEX : DELETE_TEX;
                gg.blit(texture, getX(), getY(), 0, 0, SMALL_BTN_SIZE, SMALL_BTN_SIZE, SMALL_BTN_SIZE, SMALL_BTN_SIZE);
                return;
            }
            if (tool == Tool.MOVE) {
                int centerX = getX() + 10;
                int centerY = getY() + 10;
                gg.fill(centerX - 1, getY() + 4, centerX + 2, getY() + 16, 0xFFFFFFFF);
                gg.fill(getX() + 4, centerY - 1, getX() + 16, centerY + 2, 0xFFFFFFFF);
                gg.fill(centerX - 3, getY() + 5, centerX + 4, getY() + 7, 0xFFFFFFFF);
                gg.fill(centerX - 3, getY() + 13, centerX + 4, getY() + 15, 0xFFFFFFFF);
                gg.fill(getX() + 5, centerY - 3, getX() + 7, centerY + 4, 0xFFFFFFFF);
                gg.fill(getX() + 13, centerY - 3, getX() + 15, centerY + 4, 0xFFFFFFFF);
                return;
            }
            int left = getX() + 4;
            int right = getX() + 13;
            int top = getY() + 6;
            int bottom = getY() + 13;
            gg.fill(left, top, left + 4, top + 4, 0xFF000000);
            gg.fill(left + 1, top + 1, left + 3, top + 3, 0xFFFFFFFF);
            gg.fill(right, bottom, right + 4, bottom + 4, 0xFF000000);
            gg.fill(right + 1, bottom + 1, right + 3, bottom + 3, 0xFFFFFFFF);
            gg.fill(left + 3, top + 1, right + 2, top + 4, 0xFF000000);
            gg.fill(left + 3, top + 2, right + 1, top + 3, 0xFFFFFFFF);
            gg.fill(right, top + 3, right + 3, bottom + 1, 0xFF000000);
            gg.fill(right + 1, top + 3, right + 2, bottom + 1, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private enum CategoryBackgroundMode {
        TILEABLE("Tile block"),
        CUSTOM_TILEABLE("Tile asset"),
        FULL_IMAGE("Full image");

        private final String label;

        CategoryBackgroundMode(String label) {
            this.label = label;
        }
    }

    private static final class ScaledIconButton extends net.minecraft.client.gui.components.AbstractButton {
        private ResourceLocation texture;
        private ResourceLocation hoverTexture;
        private final Runnable onPress;

        private ScaledIconButton(int x, int y, ResourceLocation texture, ResourceLocation hoverTexture, Runnable onPress) {
            super(x, y, HEADER_ICON_SIZE, HEADER_ICON_SIZE, Component.empty());
            this.texture = texture;
            this.hoverTexture = hoverTexture;
            this.onPress = onPress;
        }

        private void setTextures(ResourceLocation texture, ResourceLocation hoverTexture) {
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

    private final class CategoryWorkspaceTab extends AbstractWidget {
        private final NamedEntry category;

        CategoryWorkspaceTab(NamedEntry category, int y) {
            super(leftX - TAB_W + SIDE_TAB_X_OFFSET, y, TAB_W, TAB_H, Component.empty());
            this.category = category;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            if (category == null) {
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
                gg.pose().translate(x + 7, getY() + 3, 0);
                gg.pose().scale(3f, 3f, 1f);
                gg.blit(plus, 0, 0, 0, 0, 7, 7, 7, 7);
                gg.pose().popPose();
                if (isMouseOver(mouseX, mouseY)) gg.renderTooltip(font, Component.literal("Create category tree"), mouseX, mouseY);
                return;
            }
            ItemStack icon = QuestEditorItemIcons.iconStackFromId(category.icon);
            if (!icon.isEmpty()) gg.renderItem(icon, x + (TAB_W - 16) / 2, getY() + 5);
            if (isMouseOver(mouseX, mouseY)) gg.renderTooltip(font, Component.literal(category.name), mouseX, mouseY);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }


}
