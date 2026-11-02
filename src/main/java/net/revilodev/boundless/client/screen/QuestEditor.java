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
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
import net.revilodev.boundless.BoundlessMod;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.QuestCategoryBackgroundRenderer;
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
import net.revilodev.boundless.client.editor.QuestEditorTooltips;
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
import net.revilodev.boundless.client.screen.QuestEditorScreen.CategoryBackgroundMode;
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

// quest editor state validation and file operations
@OnlyIn(Dist.CLIENT)
public abstract class QuestEditor extends Screen {
    protected static final List<String> QUEST_LANGUAGE_CODES = buildQuestLanguageCodes();
    protected static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    protected static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");
    protected static final ResourceLocation DUPLICATE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/duplicate_button.png");
    protected static final ResourceLocation DUPLICATE_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/duplicate_button-hovered.png");
    protected static final ResourceLocation DELETE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter.png");
    protected static final ResourceLocation DELETE_ENABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter_hovered.png");
    protected static final ResourceLocation MOVE_NODE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/move.png");
    protected static final ResourceLocation CONNECT_NODE_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/connect.png");
    protected static final ResourceLocation DELETE_CONFIRM_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button.png");
    protected static final ResourceLocation DELETE_CONFIRM_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button-hovered.png");
    protected static final ResourceLocation HEADER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/3-slice-header.png");
    protected static final ResourceLocation UNSAVED_POPUP_BG_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/20x20-9-slice.png");
    protected static final ResourceLocation UNSAVED_EXCLAMATION_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/icon/exclamation.png");
    protected static final ResourceLocation FTB_IMPORT_WARNING_TEX = UNSAVED_EXCLAMATION_TEX;
    protected static final ResourceLocation BUILTIN_PACK_ENABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_confirmation.png");
    protected static final ResourceLocation BUILTIN_PACK_DISABLED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/popup_reject.png");
    protected static final ResourceLocation ORIGIN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/origin.png");
    protected static final ResourceLocation ORIGIN_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/origin-hover.png");
    protected static final ResourceLocation CATEGORY_SCROLL_UP_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_up.png");
    protected static final ResourceLocation CATEGORY_SCROLL_UP_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_up-highlighted.png");
    protected static final ResourceLocation CATEGORY_SCROLL_DOWN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_down.png");
    protected static final ResourceLocation CATEGORY_SCROLL_DOWN_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_down-highlighted.png");

    // layout and spacing
    protected static final int TOGGLE_SIZE = 20;
    protected static final int SMALL_BTN_SIZE = 20;
    protected static final int HEADER_ICON_SIZE = 10;
    protected static final int SMALL_BTN_GAP = 4;
    protected static final int TAB_W = 35;
    protected static final int TAB_H = 27;
    protected static final int TAB_GAP = 3;
    protected static final int CATEGORY_TAB_PAGE_SIZE = 4;
    protected static final int CATEGORY_SCROLL_SIZE = 16;
    protected static final int BACK_TAB_H = 17;
    protected static final int SIDE_TAB_X_OFFSET = 4;
    protected static final int BACK_TAB_X_OFFSET = SIDE_TAB_X_OFFSET + 6;
    protected static final int BACK_TAB_BOTTOM_MARGIN = 6;
    protected static final int HEADER_TEX_W = 72;
    protected static final int HEADER_TEX_H = 10;
    protected static final int HEADER_SLICE = 3;
    protected static final int UNSAVED_POPUP_TEX_SIZE = 20;
    protected static final int UNSAVED_POPUP_SLICE = 5;
    protected static final int UNSAVED_EXCLAMATION_W = 4;
    protected static final int UNSAVED_EXCLAMATION_H = 11;
    protected static final int UNSAVED_EXCLAMATION_GAP = 4;
    protected static final int INLINE_FLAG_LABEL_H = 8;
    protected static final int BACKUP_ROW_H = 18;
    protected static final int BACKUP_ROW_GAP = 2;
    protected static final DateTimeFormatter BACKUP_DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    protected static final int PANEL_W = 147;
    protected static final int PANEL_H = 166;
    protected static final int BOTTOM_BAR_H = 24;
    protected static final int FIELD_LABEL_GAP = 2;
    protected static final int FIELD_ROW_GAP = 6;
    protected static final int BOX_H = 20;
    protected static final int BOX_H_TALL = 30;
    protected static final float INPUT_TEXT_SCALE = 0.5f;
    protected static final int FORMAT_BAR_GAP = 3;
    protected static final int FORMAT_BAR_H = 9;
    protected static final int FORMAT_BTN_GAP = 1;
    protected static final int DEP_LOCK_SIZE = 13;
    protected static final int DEP_LOCK_GAP = 2;
    protected static final int DEP_ENTRY_ICON_SPACE = 12;
    protected static final float DEP_ENTRY_ICON_SCALE = 0.625f;

    protected static final ResourceLocation IMPORT_PACK_BUTTON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/import-button.png");
    protected static final ResourceLocation IMPORT_PACK_BUTTON_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/import-button-hovered.png");
    protected static final ResourceLocation PLUS_BUTTON_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/plus_button.png");
    protected static final ResourceLocation PLUS_BUTTON_HOVER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/plus_button-hovered.png");
    protected static final ResourceLocation PACK_ACTION_NEEDED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/quest_widget-action-needed.png");
    protected static final ResourceLocation PACK_CHANGED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_completed.png");
    protected static final int DEFAULT_INPUT_TEXT_COLOR = 0xE0E0E0;
    protected static final int DROPDOWN_INPUT_TEXT_COLOR = 0xFFFFFF;
    protected static final int INVALID_INPUT_TEXT_COLOR = 0xFF4040;

    // id suggestion popup layout
    protected static final int ID_SUGGESTION_MAX = 5000;
    protected static final int ID_SUGGESTION_VISIBLE_ROWS = 6;
    protected static final int ID_SUGGESTION_ROW_H = 8;
    protected static final long DELETE_HOLD_MS = 2000L;
    protected static final int ENTRY_ROW_H = 14;
    protected static final int ENTRY_ROW_GAP = 2;
    protected static final float ENTRY_INPUT_TEXT_SCALE = 0.5f;
    protected static final int ENTRY_REMOVE_BTN_W = 10;
    protected static final int ENTRY_TYPE_BTN_W = 21;
    protected static final int ENTRY_ITEM_PICK_BTN_W = 12;
    protected static final int ENTRY_COUNT_BTN_W = 12;
    protected static final float ENTRY_COUNT_TEXT_SCALE = 0.55f;

    // shared picker layout
    protected static final int ITEM_PICKER_COLS = 9;
    protected static final int ITEM_PICKER_ROWS = 4;
    protected static final int ITEM_PICKER_CELL = 18;
    protected static final int ITEM_PICKER_W = 172;
    protected static final int ITEM_PICKER_H = 112;
    protected static final int ITEM_PICKER_GRID_X = 5;
    protected static final int ITEM_PICKER_GRID_Y = 19;
    protected static final int ITEM_PICKER_GRID_RIGHT = 163;
    protected static final int ITEM_PICKER_GRID_BOTTOM = 95;
    protected static final int ITEM_PICKER_SEARCH_W = 146;
    protected static final int ITEM_PICKER_SEARCH_H = 12;
    protected static final int ITEM_PICKER_CLOSE_SIZE = 13;
    protected static final float ENTRY_TYPE_TEXT_SCALE = 0.45f;

    protected static final String ENTRY_CREATE_PACK = "__create_pack__";
    protected static final String ENTRY_BUILTIN_PACK = "__builtin_pack__";
    protected static final String ENTRY_NEW = "__new__";
    protected static final String ENTRY_CATEGORIES = "__categories__";
    protected static final String ENTRY_SUBCATEGORIES = "__subcategories__";
    protected static final String ENTRY_QUESTS = "__quests__";
    protected static final long PENDING_INIT_TTL_MS = 15000L;
    protected static ScreenState pendingInitState;
    protected static long pendingInitUntil = 0L;

    protected final Screen parent;
    protected boolean returnToParentOnBack;
    protected String initialQuestId = "";
    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    protected final QuestEditorSuggestionCaches suggestionCaches = new QuestEditorSuggestionCaches();

    protected int leftX;
    protected int rightX;
    protected int topY;
    protected int pxLeft;
    protected int pxRight;
    protected int py;
    protected int pw;
    protected int leftPw;
    protected int ph;
    protected int listH;
    protected int panelWidth = PANEL_W;
    protected int editorPanelWidth = PANEL_W;
    protected int panelHeight = PANEL_H;
    protected boolean treePanelExpanded;
    protected BackButton backButton;
    protected EditBox questSearchBox;
    protected Button createPackButton;
    protected IconButton createEntryButton;
    protected QuestEditorScreen.TreeToolButton treeMoveButton;
    protected QuestEditorScreen.TreeToolButton treeDeleteButton;
    protected QuestEditorScreen.TreeToolButton treeConnectorButton;
    protected QuestPanelExpandButton treeExpandButton;
    protected IconButton importQuestPackButton;
    protected EditorTabButton categoriesTabButton;
    protected EditorTabButton subCategoriesTabButton;
    protected EditorTabButton questsTabButton;

    // active editor mode
    protected Mode mode = Mode.PACK_LIST;
    protected EditorType editorType = EditorType.NONE;
    protected QuestPack currentPack;
    protected String selectedEntryId = "";

    // main editor buttons
    protected EditorListWidget leftList;
    protected QuestEditorTreeWidget questTree;
    protected Tool treeTool = Tool.NONE;
    protected final List<QuestEditorScreen.CategoryWorkspaceTab> categoryWorkspaceTabs = new ArrayList<>();
    protected QuestEditorScreen.CategoryScrollButton categoryWorkspaceUpButton;
    protected QuestEditorScreen.CategoryScrollButton categoryWorkspaceDownButton;
    protected int categoryWorkspaceFirstVisibleIndex;
    protected String treeCategoryId = "";
    protected ActionButton saveButton;
    protected Button exportPackButton;
    protected IconButton duplicateButton;
    protected IconButton deleteQuestButton;
    protected QuestEditorScreen.ScaledIconButton originButton;
    protected List<QuestPackStorage.Snapshot> packBackups = List.of();
    protected final List<BackupRowHitbox> backupRowHitboxes = new ArrayList<>();

    // editor runtime state
    protected float editorScroll = 0f;
    protected String statusMessage = "";
    protected int statusColor = 0xA0A0A0;
    protected boolean deleteConfirmArmed = false;
    protected boolean deleteHoldActive = false;
    protected long deleteHoldStartMs = 0L;
    protected ScreenState pendingState;
    protected final List<FormField> activeFields = new ArrayList<>();
    protected final List<FormField> allFields = new ArrayList<>();

    // pack fields
    protected EditBox packNameBox;
    protected EditBox packNamespaceBox;
    protected EditBox packIconPathBox;
    protected ScaledMultiLineEditBox packDescriptionBox;
    protected QuestEditorScreen.FtbImportSection ftbImportSection;
    protected boolean ftbImportRunning;

    // category fields
    protected EditBox catIdBox;
    protected EditBox catNameBox;
    protected EditBox catIconBox;
    protected EditBox catBackgroundBox;
    protected Button catBackgroundModeButton;
    protected Button catBackgroundAssetButton;
    protected QuestEditorScreen.CategoryBackgroundMode catBackgroundMode = QuestEditorScreen.CategoryBackgroundMode.TILEABLE;
    protected ToggleButton catBackgroundDarkeningToggle;
    protected QuestEditorScreen.BackgroundScaleSlider catBackgroundScaleSlider;
    protected QuestEditorScreen.BackgroundOpacitySlider catBackgroundOpacitySlider;
    protected String observedCategoryBackgroundState = "";
    protected String persistedCategoryBackgroundState = "";
    protected long categoryBackgroundSaveAt;
    protected EditBox catDependencyBox;
    protected ToggleButton catAutoCompleteToggle;
    protected QuestEditorScreen.CategoryLayoutSelector catLayoutSelector;
    protected QuestEditorScreen.CategorySubCategorySection categorySubCategorySection;

    // sub category fields
    protected EditBox subIdBox;
    protected EditBox subCategoryBox;
    protected EditBox subNameBox;
    protected EditBox subIconBox;
    protected ToggleButton subDefaultOpenToggle;

    // quest fields
    protected EditBox questIdBox;
    protected EditBox questNameBox;
    protected QuestEditorScreen.LanguageToggleButton questNameLanguageButton;
    protected EditBox questIconBox;
    protected ScaledMultiLineEditBox questDescriptionBox;
    protected QuestEditorScreen.LanguageToggleButton questDescriptionLanguageButton;
    protected final List<QuestEditorScreen.TranslationRow> questNameTranslationRows = new ArrayList<>();
    protected final List<QuestEditorScreen.TranslationRow> questDescriptionTranslationRows = new ArrayList<>();
    protected boolean questNameTranslationsOpen;
    protected boolean questDescriptionTranslationsOpen;
    protected boolean syncingTranslationRows;
    protected QuestEditorScreen.MediaSelector questMediaSelector;
    protected EditBox stickerIconPickerTarget;
    protected int pendingStickerIconX;
    protected int pendingStickerIconY;
    protected EditBox questCategoryBox;
    protected EditBox questSubCategoryBox;
    protected ScaledMultiLineEditBox questDependenciesBox;
    protected LockToggleButton questDependencyLockToggle;
    protected ToggleButton questOptionalToggle;
    protected ToggleButton questRepeatableToggle;
    protected ToggleButton questAutoCompleteToggle;
    protected ToggleButton questHiddenUnderDependencyToggle;
    protected QuestEditorScreen.TreeBackgroundSelector questTreeBackgroundSelector;
    protected int questTreeX = -1;
    protected int questTreeY = -1;
    protected String questIncompatibleDependencies = "";
    protected boolean questTreeOrigin;
    protected ScaledMultiLineEditBox questCompletionBox;
    protected ScaledMultiLineEditBox questRewardBox;
    // entry row widgets
    protected final List<ScaledMultiLineEditBox> dependencyEntryBoxes = new ArrayList<>();
    protected final List<ScaledMultiLineEditBox> completionEntryBoxes = new ArrayList<>();
    protected final List<ScaledMultiLineEditBox> rewardEntryBoxes = new ArrayList<>();
    protected final List<EntryRemoveButton> dependencyEntryRemoveButtons = new ArrayList<>();
    protected final List<LockToggleButton> dependencyEntryLockButtons = new ArrayList<>();
    protected final List<EntryRemoveButton> completionEntryRemoveButtons = new ArrayList<>();
    protected final List<EntryRemoveButton> rewardEntryRemoveButtons = new ArrayList<>();
    protected final List<EntryTypeButton> completionEntryTypeButtons = new ArrayList<>();
    protected final List<EntryTypeButton> rewardEntryTypeButtons = new ArrayList<>();
    protected final List<EntryItemPickerButton> completionEntryItemPickerButtons = new ArrayList<>();
    protected final List<EntryItemPickerButton> rewardEntryItemPickerButtons = new ArrayList<>();
    protected final List<EntryCountBox> completionEntryCountBoxes = new ArrayList<>();
    protected final List<EntryCountBox> rewardEntryCountBoxes = new ArrayList<>();
    // entry row state
    protected final Map<ScaledMultiLineEditBox, String> entryTypeByBox = new HashMap<>();
    protected final Map<ScaledMultiLineEditBox, String> selectedItemIdByBox = new HashMap<>();
    protected final Map<ScaledMultiLineEditBox, List<String>> selectedItemIdsByBox = new HashMap<>();
    protected final Map<ScaledMultiLineEditBox, String> selectedItemComponentsByBox = new HashMap<>();
    protected final Map<ScaledMultiLineEditBox, Integer> entryCountByBox = new HashMap<>();
    protected boolean syncingEntryRows = false;
    protected boolean entryRowsDirty = false;
    protected final List<TextInsertButton> descriptionFormatButtons = new ArrayList<>();
    protected String questOrderToken = "";

    // loaded file state
    protected Path editingPath;
    protected String loadedQuestType = "";
    protected List<String> questImportWarnings = List.of();
    // editor id caches
    protected final Set<String> categoryIdCache = new HashSet<>();
    protected final Set<String> subCategoryIdCache = new HashSet<>();
    protected final Set<String> questIdCache = new HashSet<>();
    protected final Map<String, Integer> questIdCountCache = new HashMap<>();
    protected final Map<String, Set<Path>> questPathsByIdCache = new HashMap<>();
    protected boolean localQuestIdCacheReady;
    protected Path idCachePackRoot;
    protected final Map<String, String> questIconByIdCache = new HashMap<>();
    protected final List<String> categorySuggestionCache = new ArrayList<>();
    protected final List<String> subCategorySuggestionCache = new ArrayList<>();
    protected final List<String> questSuggestionCache = new ArrayList<>();
    protected final List<String> questPackDependencySuggestionCache = new ArrayList<>();
    protected final Map<String, List<String>> subCategoryByCategorySuggestion = new HashMap<>();
    // staged pack changes
    protected final Map<String, QuestPack> stagedPacks = new LinkedHashMap<>();
    protected final Set<String> stagedDeletedPackNames = new LinkedHashSet<>();
    protected final Set<String> collapsedQuestCategories = new HashSet<>();
    protected final Set<String> collapsedQuestSubCategories = new HashSet<>();
    protected static final Map<String, Set<String>> QUEST_CATEGORY_COLLAPSE_STATES = new HashMap<>();
    protected static final Map<String, Set<String>> QUEST_SUBCATEGORY_COLLAPSE_STATES = new HashMap<>();
    // quest list cache
    protected QuestListIndex questListIndexCache;
    protected Path questListIndexPackRoot;
    protected Path categoryDataCachePackRoot;
    protected List<NamedEntry> categoryEntryCache = List.of();
    protected final Map<String, CategoryData> categoryDataCache = new HashMap<>();
    // id suggestion state
    protected final List<String> activeIdSuggestions = new ArrayList<>();
    protected EditBox idSuggestionField;
    protected ScaledMultiLineEditBox idSuggestionMultiLineField;
    protected int idSuggestionScroll = 0;
    protected Object lastIdSuggestionSource;
    protected String lastIdSuggestionPrefix = "";
    protected boolean emojiPickerOpen;
    protected int emojiPickerPage;
    protected static final ResourceLocation EMOJI_BUTTON_TEX = ResourceLocation.fromNamespaceAndPath("boundless", "textures/icon/emoji/emojis.png");
    protected static final ResourceLocation EMOJI_GUI_TEX = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/emoji-gui.png");
    protected static final int EMOJI_PICKER_COLS = 4;
    protected static final int EMOJI_PICKER_ROWS = 5;
    protected boolean idSuggestionsDirty = true;
    protected boolean suppressIdSuggestions = false;
    protected boolean suppressIdSanitizer = false;
    protected boolean closingEditor = false;
    // unsaved change state
    protected String savedEditorState;
    protected String pendingDiscardEntryId = "";
    protected Mode pendingDiscardMode;
    protected EditorEntry pendingDiscardEntry;
    protected boolean pendingEditorExit;
    protected boolean pendingBackNavigation;
    protected Runnable pendingTreeDeleteAction;
    protected String pendingTreeDeleteMessage = "";
    protected static final Map<String, QuestCategorySelection> LAST_QUEST_CATEGORY_SELECTIONS = new HashMap<>();
    protected String questSearchQuery = "";
    protected String categorySearchQuery = "";
    protected String subCategorySearchQuery = "";
    protected boolean updatingListSearchBox;
    // popup menu state
    protected final QuestEditorPopupMenus popupMenus = new QuestEditorPopupMenus();
    protected QuestEditorScreen.TranslationRow openTranslationLanguageRow;
    // item picker state
    protected EntryRowKind itemPickerKind;
    protected int itemPickerRow = -1;
    protected EditBox itemPickerIconTarget;
    protected ItemPickerTab itemPickerTab = ItemPickerTab.CREATIVE;
    protected ValidationSnapshot validationSnapshot;
    protected final QuestEditorItemPickerData itemPickerData = new QuestEditorItemPickerData();
    protected PickerMode pickerMode = PickerMode.ITEMS;
    protected int itemPickerPage = 0;
    protected EditBox itemPickerSearchBox;
    protected boolean itemPickerMultiSelect = false;
    protected final LinkedHashSet<String> itemPickerPendingSelection = new LinkedHashSet<>();
    protected final LinkedHashSet<String> itemPickerOriginalSelection = new LinkedHashSet<>();
    protected String itemPickerSearchQuery = "";

    // pending tooltip state
    protected List<Component> pendingEditorTooltip = List.of();
    protected int pendingEditorTooltipX;
    protected int pendingEditorTooltipY;
    protected float uiScale = 1.0f;

    protected record BackupRowHitbox(QuestPackStorage.Snapshot snapshot, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    // create the editor screen
    protected QuestEditor(Screen parent) {
        super(tr("title"));
        this.parent = parent;
        this.returnToParentOnBack = false;
    }

    protected QuestEditor(Screen parent, QuestData.Quest quest) {
        this(parent);
        this.initialQuestId = quest == null ? "" : safe(quest.id);
        this.returnToParentOnBack = true;
    }

    @Override
    // build the editor layout
    protected void init() {
        uiScale = 1.0f;
        width = Math.max(1, Math.round(width / uiScale));
        height = Math.max(1, Math.round(height / uiScale));
        closeTransientMenus();
        int availableWidth = Math.max(202, width - TAB_W - 12);
        editorPanelWidth = Math.min(PANEL_W, Math.max(100, availableWidth / 2));
        int maxTreePanelWidth = Math.max(100, availableWidth - editorPanelWidth - 2);
        if (!Config.expandTreeToggle()) treePanelExpanded = false;
        panelWidth = Math.min(treePanelExpanded ? 220 : PANEL_W, maxTreePanelWidth);
        panelHeight = Math.min(PANEL_H, Math.max(120, height - 26));
        int sideTabSpace = TAB_W - SIDE_TAB_X_OFFSET;
        int fullLayoutWidth = sideTabSpace + panelWidth + 2 + editorPanelWidth;
        leftX = Math.max(sideTabSpace + 2, (width - fullLayoutWidth) / 2 + sideTabSpace);
        rightX = leftX + panelWidth + 2;
        topY = (height / 2) - panelHeight / 2;
        pxLeft = leftX + 10;
        pxRight = rightX + 10;
        py = topY + 10;
        leftPw = panelWidth - 20;
        pw = editorPanelWidth - 20;
        int interiorH = panelHeight - 20;
        listH = interiorH - BOTTOM_BAR_H;
        ph = listH;

        leftList = new EditorListWidget(pxLeft, py, leftPw, listH, this::handleLeftClick, this::handleLeftAction,
                this::handleLeftSecondaryClick, this::handleEntryMoveAction, this::openCurrentCategorySettings,
                QuestEditorItemIcons::iconStackFromId);
        addRenderableWidget(leftList);
        questTree = new QuestEditorTreeWidget(pxLeft, py, leftPw, listH, this::openTreeQuest,
                this::placeTreeQuest, this::saveTreePosition, this::requestDeleteTreeNode,
                this::requestTreeDependencyChange, this::toggleTreeIncompatibility,
                this::saveTreeStickers, this::requestStickerIcon, this::requestStickerTextEdit);
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
        treeMoveButton = new QuestEditorScreen.TreeToolButton(Tool.MOVE, this::toggleTreeMoveMode,
                SMALL_BTN_SIZE, MOVE_NODE_TEX, MOVE_NODE_TEX);
        treeMoveButton.visible = false;
        treeMoveButton.active = false;
        addRenderableWidget(treeMoveButton);
        treeDeleteButton = new QuestEditorScreen.TreeToolButton(Tool.DELETE, this::toggleTreeDeleteMode,
                SMALL_BTN_SIZE, DELETE_TEX, DELETE_ENABLED_TEX);
        treeDeleteButton.visible = false;
        treeDeleteButton.active = false;
        addRenderableWidget(treeDeleteButton);
        treeConnectorButton = new QuestEditorScreen.TreeToolButton(Tool.CONNECT, this::toggleTreeConnectorMode,
                SMALL_BTN_SIZE, CONNECT_NODE_TEX, CONNECT_NODE_TEX);
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
        originButton = new QuestEditorScreen.ScaledIconButton(pxRight + pw - HEADER_ICON_SIZE, py,
                HEADER_ICON_SIZE, ORIGIN_TEX, ORIGIN_HOVER_TEX, this::makeCurrentQuestOrigin);
        originButton.visible = false;
        originButton.active = false;
        addRenderableWidget(originButton);
        setTreeTool(treeTool);
        importQuestPackButton = new IconButton(createPackButton.getX() + createPackButton.getWidth() + 2, barY, SMALL_BTN_SIZE, IMPORT_PACK_BUTTON_TEX, IMPORT_PACK_BUTTON_TEX_HOVER, this::openImportQuestPackDirectory);
        addRenderableWidget(importQuestPackButton);
        questSearchBox = new EditBox(font, pxLeft + 2, barY, leftPw - 4, 20, tr("search"));
        questSearchBox.setMaxLength(128);
        questSearchBox.setHint(tr("search"));
        questSearchBox.setValue(searchQueryForMode());
        questSearchBox.setResponder(value -> {
            if (updatingListSearchBox) return;
            setSearchQueryForMode(safe(value));
            if (isListSearchMode()) refreshLeftList();
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

    protected boolean openInitialQuest() {
        String questId = initialQuestId;
        initialQuestId = "";
        for (QuestPack pack : QuestEditorPackFiles.listPacks()) {
            QuestListIndex index = QuestEditorQuestOrdering.buildQuestListIndex(pack);
            QuestEntryData data = index.questDataByEntryId.get(questId);
            if (data == null) continue;
            currentPack = pack;
            questListIndexCache = index;
            questListIndexPackRoot = pack.root;
            selectedEntryId = questId;
            treeCategoryId = safe(data.category).trim();
            setMode(Mode.QUEST_LIST);
            showQuestEditor(data, data.path);
            if (leftList != null) leftList.setSelectedId(questId);
            return true;
        }
        return false;
    }

    // create all reusable form widgets
    protected void initFormFields() {
        packNameBox = createBox(trs("field.pack_name"), BOX_H);
        packNamespaceBox = createBox(trs("field.namespace"), BOX_H);
        packIconPathBox = createIconPickerBox(trs("field.pack_icon_path"));
        packDescriptionBox = makeMultiBox(trs("field.pack_description"), BOX_H_TALL, false);
        ftbImportSection = new QuestEditorScreen.FtbImportSection(
                font, this::selectFtbQuestImportFolder, () -> ftbImportRunning);
        addRenderableWidget(ftbImportSection);

        catIdBox = createBox(trs("field.category_id"), BOX_H);
        catNameBox = createBox(trs("field.category_name"), BOX_H);
        catNameBox.setMaxLength(128);
        catIconBox = createIconPickerBox(trs("field.category_icon"));
        catBackgroundBox = createIconPickerBox(trs("field.category_background"));
        catBackgroundModeButton = Button.builder(Component.empty(), button -> cycleCategoryBackgroundMode())
                .bounds(0, 0, 40, BOX_H).build();
        catBackgroundModeButton.visible = false;
        catBackgroundModeButton.active = false;
        addRenderableWidget(catBackgroundModeButton);
        catBackgroundAssetButton = Button.builder(tr("select"), button -> openCategoryBackgroundPicker())
                .bounds(0, 0, 38, BOX_H).build();
        catBackgroundAssetButton.visible = false;
        catBackgroundAssetButton.active = false;
        addRenderableWidget(catBackgroundAssetButton);
        catBackgroundDarkeningToggle = createToggle(false);
        catBackgroundScaleSlider = new QuestEditorScreen.BackgroundScaleSlider(
                0, 0, 100, BOX_H, 0.5f, () -> queueCategoryBackgroundAutosave(false));
        addRenderableWidget(catBackgroundScaleSlider);
        catBackgroundOpacitySlider = new QuestEditorScreen.BackgroundOpacitySlider(
                0, 0, 100, BOX_H, 0.45f, () -> queueCategoryBackgroundAutosave(false));
        addRenderableWidget(catBackgroundOpacitySlider);
        catDependencyBox = createBox(trs("field.unlock_quest"), BOX_H);
        catAutoCompleteToggle = createToggle(false);
        catLayoutSelector = new QuestEditorScreen.CategoryLayoutSelector(font, this::applyCategoryEditorFields);
        catLayoutSelector.visible = false;
        catLayoutSelector.active = false;
        addRenderableWidget(catLayoutSelector);
        categorySubCategorySection = new QuestEditorScreen.CategorySubCategorySection(
                font, this::openCategorySubCategory, this::createCategorySubCategory);
        categorySubCategorySection.visible = false;
        categorySubCategorySection.active = false;
        addRenderableWidget(categorySubCategorySection);

        subIdBox = createBox(trs("field.subcategory_id"), BOX_H);
        subCategoryBox = createBox(trs("field.parent_category_id"), BOX_H);
        subNameBox = createBox(trs("field.subcategory_name"), BOX_H);
        subNameBox.setMaxLength(128);
        subIconBox = createIconPickerBox(trs("field.subcategory_icon"));
        subDefaultOpenToggle = createToggle(false);

        questIdBox = createBox(trs("field.quest_id"), BOX_H);
        questNameBox = createBox(trs("field.quest_name"), BOX_H);
        questNameLanguageButton = addRenderableWidget(createLanguageToggleButton(false));
        questIconBox = createIconPickerBox(trs("field.quest_icon"));
        questDescriptionBox = makeMultiBox(trs("field.quest_description"), BOX_H_TALL, true);
        questDescriptionLanguageButton = addRenderableWidget(createLanguageToggleButton(true));
        questMediaSelector = new QuestEditorScreen.MediaSelector(
                font, this::selectQuestMediaFile, this::markCurrentEditorUnsaved);
        addRenderableWidget(questMediaSelector);
        questCategoryBox = createBox(trs("field.quest_category"), BOX_H);
        questSubCategoryBox = createBox(trs("field.quest_subcategory"), BOX_H);
        questDependenciesBox = makeMultiBox(trs("field.dependencies"), BOX_H_TALL, false);
        questDependencyLockToggle = makeDepLock(false);
        questOptionalToggle = createToggle(false);
        questRepeatableToggle = createToggle(false);
        questAutoCompleteToggle = createToggle(false);
        questHiddenUnderDependencyToggle = createToggle(false);
        questTreeBackgroundSelector = new QuestEditorScreen.TreeBackgroundSelector(
                this::hasUnsavedEditorChanges, this::autosaveTreeBackground);
        addRenderableWidget(questTreeBackgroundSelector);
        questCompletionBox = makeMultiBox(trs("field.completion_entries"), BOX_H_TALL, false);
        questRewardBox = makeMultiBox(trs("field.reward_entries"), BOX_H_TALL, false);
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
    protected void initFormatBtns() {
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

    protected TextInsertButton createDescriptionInsertButton(String label, String insertText, int fillColor, int width) {
        return createDescriptionInsertButton(label, insertText, fillColor, width, 1.0f);
    }

    protected TextInsertButton createDescriptionInsertButton(String label, String insertText, int fillColor, int width, float textScale) {
        TextInsertButton button = new TextInsertButton(0, 0, width, label, insertText, fillColor, textScale, this::applyDescriptionFormat);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    // create a shared single line field
    protected EditBox createBox(String hint, int height) {
        EditBox box = new EditBox(font, 0, 0, pw - 4, height, Component.literal(hint));
        box.setMaxLength(1024);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    protected EditBox createIconPickerBox(String hint) {
        EditBox box = new IconPickerBox(font, Component.literal(hint));
        box.setMaxLength(1024);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    protected ScaledMultiLineEditBox makeMultiBox(String hint, int height) {
        return makeMultiBox(hint, height, false);
    }

    // create a shared multiline field
    protected ScaledMultiLineEditBox makeMultiBox(String hint, int height, boolean allowColorFormatting) {
        ScaledMultiLineEditBox box = new ScaledMultiLineEditBox(font, 0, 0, pw - 4, height, Component.literal(hint), Component.empty(), INPUT_TEXT_SCALE, allowColorFormatting);
        box.setCharacterLimit(4096);
        box.visible = false;
        box.active = false;
        addRenderableWidget(box);
        return box;
    }

    protected ToggleButton createToggle(boolean initial) {
        ToggleButton button = new ToggleButton(0, 0, TOGGLE_SIZE, TOGGLE_SIZE, initial);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    protected LockToggleButton makeDepLock(boolean initial) {
        LockToggleButton button = new LockToggleButton(0, 0, DEP_LOCK_SIZE, DEP_LOCK_SIZE, initial);
        button.visible = false;
        button.active = false;
        addRenderableWidget(button);
        return button;
    }

    // switch the active editor mode
    protected void setMode(Mode next) {
        if (next != mode) flushCategoryBackgroundAutosave();
        if (next != Mode.QUEST_LIST) setTreeTool(Tool.NONE);
        if (next == Mode.QUEST_LIST && mode != Mode.QUEST_LIST) {
            restoreQuestListCollapseState();
        }
        closeTransientMenus();
        mode = next;
        if (next == Mode.QUEST_LIST && currentPack != null) {
            List<NamedEntry> categories = categoryEntries();
            boolean categoryExists = categories.stream().anyMatch(category -> category.id.equals(treeCategoryId));
            if (!categoryExists) treeCategoryId = categories.isEmpty() ? "" : categories.getFirst().id;
        }
        syncListSearchBox();
        statusMessage = "";
        editorScroll = 0f;
        disarmDeleteConfirm();
        refreshLeftList(false);
        clearEditor();
        if (next == Mode.QUEST_LIST && currentPack != null && !treeCategoryId.isBlank()) {
            CategoryData category = categoryData(treeCategoryId);
            if (category != null) showCategoryEditor(category, category.path);
        }
        updateLeftPaneLayout();
        updateBackButtonVisibility();
        if (treePanelExpanded && (next != Mode.QUEST_LIST || !isTreePack())) {
            treePanelExpanded = false;
            if (minecraft != null) {
                minecraft.execute(() -> resize(minecraft, Math.round(width * uiScale), Math.round(height * uiScale)));
            }
        }
    }

    protected void refreshLeftList() {
        refreshLeftList(true);
    }

    // rebuild the left list for the current mode
    protected void refreshLeftList(boolean preserveScroll) {
        refreshLeftList(preserveScroll, true);
    }

    protected void refreshLeftList(boolean preserveScroll, boolean refreshTree) {
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
        QuestListIndex index = mode == Mode.QUEST_LIST ? questListIndex() : null;
        if (!localQuestIdCacheReady || currentPack == null || !Objects.equals(idCachePackRoot, currentPack.root)) {
            refreshPackIdCaches(index);
            idCachePackRoot = currentPack == null ? null : currentPack.root;
        }
        if (refreshTree) refreshQuestTree();
        refreshCategoryWorkspaceTabs();
    }

    protected boolean isTreePack() {
        return isTreeCategory(treeCategoryId);
    }

    protected boolean isTreeCategory(String categoryId) {
        CategoryData category = categoryData(categoryId);
        return category != null && "tree".equalsIgnoreCase(category.layout);
    }

    protected void refreshQuestTree() {
        if (questTree == null) return;
        if (mode != Mode.QUEST_LIST || !isTreePack()) {
            questTree.setQuests(List.of());
            questTree.setBackground("", false, 0.45f);
            return;
        }
        List<QuestEntryData> quests = new ArrayList<>();
        for (QuestListItem item : questListIndex().questsByCategory.getOrDefault(treeCategoryId, List.of())) {
            if (item.data != null) quests.add(item.data);
        }
        questTree.setQuests(quests);
        CategoryData category = categoryData(treeCategoryId);
        questTree.setBackground(category == null ? "" : category.background,
                category != null && parseBool(category.backgroundDarkening, false),
                category == null ? 0.45f : parseFloat(category.backgroundDarkeningOpacity, 0.45f),
                category == null ? 0.5f : parseFloat(category.backgroundTileScale, 0.5f));
        questTree.setStickers(readCategoryStickers(category == null ? null : category.path));
    }

    protected void refreshCategoryWorkspaceTabs() {
        refreshCategoryWorkspaceTabs(true);
    }

    protected void refreshCategoryWorkspaceTabs(boolean revealSelected) {
        for (QuestEditorScreen.CategoryWorkspaceTab tab : categoryWorkspaceTabs) {
            removeWidget(tab);
        }
        if (categoryWorkspaceUpButton != null) {
            removeWidget(categoryWorkspaceUpButton);
            categoryWorkspaceUpButton = null;
        }
        if (categoryWorkspaceDownButton != null) {
            removeWidget(categoryWorkspaceDownButton);
            categoryWorkspaceDownButton = null;
        }
        categoryWorkspaceTabs.clear();
        if (mode != Mode.QUEST_LIST || currentPack == null) return;
        List<NamedEntry> categories = categoryEntries();
        boolean currentExists = categories.stream().anyMatch(category -> category.id.equals(treeCategoryId));
        if (!currentExists && !categories.isEmpty()) treeCategoryId = categories.getFirst().id;
        int totalTabs = categories.size() + 1;
        int maxFirst = Math.max(0, totalTabs - CATEGORY_TAB_PAGE_SIZE);
        categoryWorkspaceFirstVisibleIndex = Math.min(categoryWorkspaceFirstVisibleIndex, maxFirst);
        if (revealSelected && !treeCategoryId.isBlank()) {
            int selectedIndex = -1;
            for (int index = 0; index < categories.size(); index++) {
                if (treeCategoryId.equals(categories.get(index).id)) {
                    selectedIndex = index;
                    break;
                }
            }
            if (selectedIndex >= 0) {
                if (selectedIndex < categoryWorkspaceFirstVisibleIndex) {
                    categoryWorkspaceFirstVisibleIndex = selectedIndex;
                } else if (selectedIndex >= categoryWorkspaceFirstVisibleIndex + CATEGORY_TAB_PAGE_SIZE) {
                    categoryWorkspaceFirstVisibleIndex = selectedIndex - CATEGORY_TAB_PAGE_SIZE + 1;
                }
            }
        }
        categoryWorkspaceFirstVisibleIndex = Math.max(0, Math.min(categoryWorkspaceFirstVisibleIndex, maxFirst));
        int tabY = py;
        int end = Math.min(categories.size(), categoryWorkspaceFirstVisibleIndex + CATEGORY_TAB_PAGE_SIZE);
        for (int index = categoryWorkspaceFirstVisibleIndex; index < end; index++) {
            NamedEntry category = categories.get(index);
            QuestEditorScreen.CategoryWorkspaceTab tab = createCategoryWorkspaceTab(category, tabY);
            categoryWorkspaceTabs.add(tab);
            addRenderableWidget(tab);
            tabY += TAB_H + TAB_GAP;
        }
        int visibleCategoryCount = end - categoryWorkspaceFirstVisibleIndex;
        if (end == categories.size() && visibleCategoryCount < CATEGORY_TAB_PAGE_SIZE) {
            QuestEditorScreen.CategoryWorkspaceTab createTab = createCategoryWorkspaceTab(null, tabY);
            categoryWorkspaceTabs.add(createTab);
            addRenderableWidget(createTab);
        }
        int scrollX = leftX - TAB_W + SIDE_TAB_X_OFFSET + (TAB_W - CATEGORY_SCROLL_SIZE) / 2;
        if (categoryWorkspaceFirstVisibleIndex > 0) {
            categoryWorkspaceUpButton = addRenderableWidget(new QuestEditorScreen.CategoryScrollButton(
                    scrollX, py - CATEGORY_SCROLL_SIZE - TAB_GAP, true,
                    CATEGORY_SCROLL_SIZE, this::scrollCategoryWorkspace,
                    CATEGORY_SCROLL_UP_TEX, CATEGORY_SCROLL_UP_HOVER_TEX,
                    CATEGORY_SCROLL_DOWN_TEX, CATEGORY_SCROLL_DOWN_HOVER_TEX));
        }
        if (categoryWorkspaceFirstVisibleIndex + CATEGORY_TAB_PAGE_SIZE < totalTabs) {
            categoryWorkspaceDownButton = addRenderableWidget(new QuestEditorScreen.CategoryScrollButton(
                    scrollX, tabY + 1, false,
                    CATEGORY_SCROLL_SIZE, this::scrollCategoryWorkspace,
                    CATEGORY_SCROLL_UP_TEX, CATEGORY_SCROLL_UP_HOVER_TEX,
                    CATEGORY_SCROLL_DOWN_TEX, CATEGORY_SCROLL_DOWN_HOVER_TEX));
        }
    }

    protected void scrollCategoryWorkspace(int direction) {
        if (currentPack == null || direction == 0) return;
        int categoryCount = categoryEntries().size();
        int maxFirst = Math.max(0, categoryCount + 1 - CATEGORY_TAB_PAGE_SIZE);
        int next = Math.max(0, Math.min(maxFirst, categoryWorkspaceFirstVisibleIndex + direction));
        if (next == categoryWorkspaceFirstVisibleIndex) return;
        categoryWorkspaceFirstVisibleIndex = next;
        refreshCategoryWorkspaceTabs(false);
    }

    protected void selectTreeCategory(NamedEntry entry) {
        if (entry == null || currentPack == null) return;
        if (hasUnsavedEditorChanges()) {
            setError(trs("error.save_before_navigation"));
            return;
        }
        treeCategoryId = entry.id;
        selectedEntryId = "";
        refreshLeftList(false);
        CategoryData category = categoryData(entry.id);
        if (category != null) showCategoryEditor(category, category.path);
    }

    protected void openCurrentCategorySettings() {
        if (currentPack == null || treeCategoryId.isBlank()) return;
        if (hasUnsavedEditorChanges() && editorType != EditorType.CATEGORY) {
            setError(trs("error.save_before_navigation"));
            return;
        }
        CategoryData category = categoryData(treeCategoryId);
        if (category == null) return;
        selectedEntryId = "";
        if (leftList != null) leftList.setSelectedId("");
        showCategoryEditor(category, category.path);
    }

    protected List<SubCategoryData> categorySubCategories(String categoryId) {
        if (currentPack == null || safe(categoryId).isBlank()) return List.of();
        return questListIndex().subCategoriesByCategory.getOrDefault(safe(categoryId).trim(), List.of());
    }

    protected void openCategorySubCategory(SubCategoryData subCategory) {
        if (subCategory == null) return;
        if (hasUnsavedEditorChanges()) {
            setError(trs("error.save_before_navigation"));
            return;
        }
        selectedEntryId = subCategory.id;
        showSubCategoryEditor(subCategory, subCategory.path);
    }

    protected void createCategorySubCategory() {
        if (hasUnsavedEditorChanges()) {
            setError(trs("error.save_before_navigation"));
            return;
        }
        SubCategoryData subCategory = new SubCategoryData();
        subCategory.category = treeCategoryId;
        showSubCategoryEditor(subCategory, null);
    }

    // build the pack selection list
    protected List<EditorEntry> buildPackEntries() {
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
            if (QuestEditorPackFiles.isBuiltinPack(pack)) continue;
            PackMeta meta = QuestEditorPackFiles.readPackMeta(pack.root, pack.name);
            String packIconId = QuestEditorPackFiles.cleanIconId(meta.iconPath);
            boolean legacy = pack.legacy;
            boolean changed = stagedPacks.containsKey(pack.name);
            boolean enabled = !legacy && pack.enabled;
            ResourceLocation actionIcon = enabled ? BUILTIN_PACK_ENABLED_TEX : BUILTIN_PACK_DISABLED_TEX;
            String actionTooltip = legacy ? trs("legacy_pack") : (enabled ? trs("enabled") : trs("disabled"));
            ResourceLocation rowTexture = legacy ? PACK_ACTION_NEEDED_TEX : (changed ? PACK_CHANGED_TEX : ROW_TEX);
            out.add(EditorEntry.pack(pack.name, pack.name, packIconId, null, actionIcon, actionTooltip,
                    legacy ? trs("tooltip.incompatible_pack") : trs("tooltip.right_click_options"), rowTexture));
        }
        return out;
    }

    // build the pack section menu
    protected List<EditorEntry> buildPackMenuEntries() {
        List<EditorEntry> out = new ArrayList<>();
        out.add(new EditorEntry(ENTRY_CATEGORIES, trs("categories"), "", ""));
        out.add(new EditorEntry(ENTRY_SUBCATEGORIES, trs("subcategories"), "", ""));
        out.add(new EditorEntry(ENTRY_QUESTS, trs("quests"), "", ""));
        return out;
    }

    // build category rows
    protected List<EditorEntry> buildCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        if (currentPack == null) return out;
        for (NamedEntry entry : categoryEntries()) {
            if (!matchesListSearch(entry, categorySearchQuery)) continue;
            out.add(EditorEntry.movable(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    // build sub category rows
    protected List<EditorEntry> buildSubCategoryEntries() {
        List<EditorEntry> out = new ArrayList<>();
        if (currentPack == null) return out;
        for (NamedEntry entry : QuestEditorJsonFiles.listSubCategoryEntries(currentPack)) {
            if (!matchesListSearch(entry, subCategorySearchQuery)) continue;
            out.add(EditorEntry.movable(entry.id, entry.name, entry.id, entry.icon));
        }
        return out;
    }

    // build grouped quest rows
    protected List<EditorEntry> buildQuestEntries() {
        // build only the selected category without reopening quest files
        List<EditorEntry> out = new ArrayList<>();
        if (currentPack == null) return out;
        QuestListIndex index = questListIndex();
        Map<String, List<NamedEntry>> grouped = new LinkedHashMap<>();
        for (QuestListItem item : index.questsByCategory.getOrDefault(safe(treeCategoryId).trim(), List.of())) {
            if (!matchesQuestSearch(item.entry)) continue;
            QuestEntryData data = item.data;
            String subCategoryId = data == null ? "" : safe(data.subCategory);
            grouped.computeIfAbsent(subCategoryId, ignored -> new ArrayList<>()).add(item.entry);
        }

        for (Map.Entry<String, List<NamedEntry>> subEntry : grouped.entrySet()) {
            String subId = safe(subEntry.getKey()).trim();
            String subKey = treeCategoryId + "::" + subId;
            String subName = index.subCategoryNames.getOrDefault(subKey,
                    subId.isBlank() ? trs("no_subcategory") : subId);
            boolean subCollapsed = collapsedQuestSubCategories.contains(subKey);
            if (!subId.isBlank()) out.add(EditorEntry.subCategoryHeader(subKey, subName, subCollapsed));
            if (subCollapsed) continue;
            for (NamedEntry quest : subEntry.getValue()) {
                QuestEntryData questData = index.questDataByEntryId.get(safe(quest.id));
                String invalidReason = questEntryInvalidReason(questData, index.categoryIds,
                        index.subCategoryIds, index.questIds, index.duplicateQuestIds);
                boolean invalid = invalidReason != null;
                String importWarning = questData == null ? "" : String.join(" | ", questData.importWarnings);
                if (!importWarning.isBlank()) {
                    out.add(EditorEntry.questWarning(quest.id, quest.name, quest.id, quest.icon,
                            FTB_IMPORT_WARNING_TEX, importWarning,
                            invalid ? PACK_ACTION_NEEDED_TEX : ROW_TEX, invalid ? invalidReason : ""));
                } else {
                    out.add(invalid
                            ? EditorEntry.quest(quest.id, quest.name, quest.id, quest.icon,
                            PACK_ACTION_NEEDED_TEX, invalidReason)
                            : EditorEntry.quest(quest.id, quest.name, quest.id, quest.icon));
                }
            }
        }
        return out;
    }

    protected QuestListIndex questListIndex() {
        // build the quest index once and reuse it until pack data changes
        if (currentPack == null) return QuestListIndex.EMPTY;
        if (questListIndexCache != null && Objects.equals(questListIndexPackRoot, currentPack.root)) {
            return questListIndexCache;
        }
        questListIndexCache = QuestEditorQuestOrdering.buildQuestListIndex(currentPack);
        questListIndexPackRoot = currentPack.root;
        return questListIndexCache;
    }

    protected void invalidateQuestListIndex() {
        // clear every cache derived from the current quest index
        questListIndexCache = null;
        questListIndexPackRoot = null;
        localQuestIdCacheReady = false;
        idCachePackRoot = null;
        invalidateCategoryDataCache();
    }

    protected void invalidateCategoryDataCache() {
        categoryDataCachePackRoot = null;
        categoryEntryCache = List.of();
        categoryDataCache.clear();
        if (questListIndexCache != null) questListIndexCache.categoryDataById.clear();
    }

    protected List<NamedEntry> categoryEntries() {
        if (currentPack == null || currentPack.root == null) return List.of();
        if (!Objects.equals(categoryDataCachePackRoot, currentPack.root)) {
            categoryDataCachePackRoot = currentPack.root;
            categoryEntryCache = List.copyOf(QuestEditorJsonFiles.listCategoryEntries(currentPack));
            categoryDataCache.clear();
        }
        return categoryEntryCache;
    }

    protected CategoryData categoryData(String id) {
        // lazily read category data and keep it for later list rebuilds
        String normalized = safe(id).trim();
        if (currentPack == null || normalized.isBlank()) return null;
        categoryEntries();
        CategoryData cached = categoryDataCache.get(normalized);
        if (cached != null) return cached;
        if (questListIndexCache != null && Objects.equals(questListIndexPackRoot, currentPack.root)) {
            CategoryData indexed = questListIndexCache.categoryDataById.get(normalized);
            if (indexed != null) {
                categoryDataCache.put(normalized, indexed);
                return indexed;
            }
        }
        Path path = null;
        for (NamedEntry entry : categoryEntryCache) {
            if (entry != null && normalized.equals(entry.id)) {
                path = entry.path;
                break;
            }
        }
        CategoryData loaded = path == null
                ? QuestEditorJsonFiles.loadCategory(currentPack, normalized)
                : QuestEditorJsonFiles.loadCategory(path, normalized);
        if (loaded != null) categoryDataCache.put(normalized, loaded);
        return loaded;
    }

    protected boolean isQuestEntryInvalid(QuestEntryData data) {
        return questEntryInvalidReason(data) != null;
    }

    protected String questEntryInvalidReason(QuestEntryData data) {
        return questEntryInvalidReason(data, categoryIdCache, subCategoryIdCache, questIdCache, Set.of());
    }

    protected String questEntryInvalidReason(QuestEntryData data, Set<String> categoryIds, Set<String> subCategoryIds, Set<String> questIds, Set<String> duplicateQuestIds) {
        // return the first import or reference problem shown by the warning icon
        if (data == null) return null;
        String id = safe(data.id).trim();
        if (id.isBlank()) return trs("validation.missing_quest_id");
        if (duplicateQuestIds != null && duplicateQuestIds.contains(id)) return trs("validation.duplicate_quest_id");
        String name = safe(data.name).trim();
        if (name.isBlank()) return trs("validation.missing_quest_name");
        String category = safe(data.category).trim();
        if (category.isBlank()) return trs("validation.missing_quest_category");
        boolean categoryKnown = categoryIds != null && categoryIds.contains(category);
        boolean categoryKnownGlobally = categoryIdCache.contains(category) || QuestData.categoryById(category).isPresent();
        if (!categoryKnown && !categoryKnownGlobally) return trs("validation.invalid_quest_category");

        String subCategory = safe(data.subCategory).trim();
        if (!subCategory.isBlank()) {
            boolean hasExact = subCategoryIds != null && subCategoryIds.contains(category + "::" + subCategory);
            boolean hasWildcard = subCategoryIds != null && subCategoryIds.contains("::" + subCategory);
            if (!hasExact && !hasWildcard) return trs("validation.invalid_quest_subcategory");
        }

        for (String dep : QuestEditorEntryCodec.extractEntryLines(safe(data.dependencies))) {
            if (questIds == null || !questIds.contains(dep)) return trs("validation.invalid_quest_dependency");
        }
        return null;
    }

    protected boolean matchesQuestSearch(NamedEntry entry) {
        return matchesListSearch(entry, questSearchQuery);
    }

    protected boolean matchesListSearch(NamedEntry entry, String rawQuery) {
        String query = safe(rawQuery).trim().toLowerCase(Locale.ROOT);
        if (query.isBlank() || entry == null) return true;
        return safe(entry.id).toLowerCase(Locale.ROOT).contains(query)
                || safe(entry.name).toLowerCase(Locale.ROOT).contains(query)
                || safe(entry.sortKey).toLowerCase(Locale.ROOT).contains(query);
    }

    protected boolean isListSearchMode() {
        return mode == Mode.CATEGORY_LIST || mode == Mode.SUBCATEGORY_LIST || mode == Mode.QUEST_LIST;
    }

    protected String searchQueryForMode() {
        return switch (mode) {
            case CATEGORY_LIST -> categorySearchQuery;
            case SUBCATEGORY_LIST -> subCategorySearchQuery;
            case QUEST_LIST -> questSearchQuery;
            default -> "";
        };
    }

    protected void setSearchQueryForMode(String query) {
        switch (mode) {
            case CATEGORY_LIST -> categorySearchQuery = query;
            case SUBCATEGORY_LIST -> subCategorySearchQuery = query;
            case QUEST_LIST -> questSearchQuery = query;
            default -> {
            }
        }
    }

    protected void syncListSearchBox() {
        // restore the search text owned by the active list mode
        if (questSearchBox == null) return;
        updatingListSearchBox = true;
        questSearchBox.setValue(searchQueryForMode());
        questSearchBox.setHint(tr("search"));
        updatingListSearchBox = false;
    }

    protected void handleLeftClick(EditorEntry entry) {
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

    protected void handleLeftSecondaryClick(EditorEntry entry) {
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

    protected void handleLeftAction(EditorEntry entry) {
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

    protected void handleEntryMoveAction(EditorEntry entry, MoveDirection direction) {
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
    protected void openPackCreate() {
        setMode(Mode.PACK_CREATE);
        showPackCreate();
    }

    protected void requestCreateEntryForCurrentMode() {
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
    protected Component createEntryTooltip() {
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

    protected void openTreeQuest(QuestEntryData treeQuest) {
        // open the clicked tree quest without losing the selected category
        if (treeQuest == null || currentPack == null) return;
        setTreeTool(Tool.NONE);
        QuestEntryData data = questListIndex().questDataByEntryId.get(treeQuest.id);
        if (data == null) return;
        selectedEntryId = data.id;
        if (leftList != null) leftList.setSelectedId(data.id);
        showQuestEditor(data, data.path);
    }

    protected void placeTreeQuest(Integer x, Integer y) {
        // create a quest at the requested tree position with a unique id
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
            stageCurrentPackChange(trs("status.quest_node_created"));
            QuestEditorPackFiles.queueBackupPack(currentPack, "created");
            refreshLeftList();
            statusMessage = trs("status.quest_created", name);
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError(trs("error.create_quest_node_failed"));
        }
    }

    protected int nextTreeQuestNumber() {
        Set<String> ids = new HashSet<>();
        for (QuestListItem item : questListIndex().items) {
            if (item != null && item.data != null) ids.add(safe(item.data.id));
        }
        int number = 1;
        while (ids.contains(String.format(Locale.ROOT, "new-quest-%02d", number))) number++;
        return number;
    }

    protected void toggleTreeDeleteMode() {
        setTreeTool(treeTool == Tool.DELETE ? Tool.NONE : Tool.DELETE);
    }

    protected void toggleTreeMoveMode() {
        setTreeTool(treeTool == Tool.MOVE ? Tool.NONE : Tool.MOVE);
    }

    protected void toggleTreeConnectorMode() {
        setTreeTool(treeTool == Tool.CONNECT ? Tool.NONE : Tool.CONNECT);
    }

    protected void setTreeTool(Tool tool) {
        // keep only one tree editing tool active at a time
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

    protected boolean categoryHasTreeNodes(String categoryId) {
        return !questListIndex().questsByCategory.getOrDefault(safe(categoryId).trim(), List.of()).isEmpty();
    }

    protected void makeCurrentQuestOrigin() {
        // make the current quest the only origin in its category
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
            stageCurrentPackChange(trs("status.tree_origin_changed"));
            QuestEditorPackFiles.queueBackupPack(currentPack, "origin");
            refreshLeftList();
            statusMessage = trs("status.tree_origin_set");
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError(trs("error.set_tree_origin_failed"));
        }
    }

    protected void saveTreePosition(QuestEntryData quest, int x, int y) {
        // update only the moved quest instead of rebuilding the whole pack
        if (quest == null || quest.path == null) return;
        JsonObject obj = QuestEditorJsonFiles.readJson(quest.path);
        if (obj == null) return;
        obj.addProperty("tree_x", x);
        obj.addProperty("tree_y", y);
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, quest.path);
            quest.treeX = Integer.toString(x);
            quest.treeY = Integer.toString(y);
            stageCurrentPackChange(trs("status.quest_position_staged"), false);
        } catch (IOException e) {
            setError(trs("error.save_quest_position_failed"));
        }
    }

    protected List<QuestData.Sticker> readCategoryStickers(Path categoryPath) {
        // keep malformed sticker data from breaking the tree editor
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
                    parseInt(optStringFlexible(sticker, "y", "0"), 0),
                    parseFloat(optStringFlexible(sticker, "scale", "1"), 1f)));
        }
        return List.copyOf(stickers);
    }

    protected void saveTreeStickers(List<QuestData.Sticker> stickers) {
        // write sticker changes back to the active category file
        if (currentPack == null || treeCategoryId.isBlank()) return;
        CategoryData category = categoryData(treeCategoryId);
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
            entry.addProperty("scale", sticker.scale());
            array.add(entry);
        }
        object.add("stickers", array);
        try {
            QuestPackStorage.writeJsonAtomically(gson, object, category.path);
            stageCurrentPackChange(trs("status.tree_sticker_saved"), false);
            QuestEditorPackFiles.queueBackupPack(currentPack, "stickers");
            QuestEditorPackFiles.mirrorPackDirectorySafe(currentPack, isSingleplayerAuthoritySession());
            statusMessage = trs("status.tree_sticker_saved");
            statusColor = 0xA0FFA0;
        } catch (IOException exception) {
            setError(trs("error.save_tree_sticker_failed"));
        }
    }

    protected void requestStickerIcon(Integer x, Integer y) {
        pendingStickerIconX = x == null ? 0 : x;
        pendingStickerIconY = y == null ? 0 : y;
        openIconItemPicker(stickerIconPickerTarget);
    }

    protected void requestStickerTextEdit(Integer index, QuestData.Sticker sticker) {
        if (index == null || sticker == null || "icon".equals(sticker.type())) return;
        pendingState = captureState();
        Minecraft.getInstance().setScreen(new StickerTextEditorScreen(this, sticker.value(), updatedText -> {
            if (currentPack == null || treeCategoryId.isBlank()) return;
            CategoryData category = categoryData(treeCategoryId);
            List<QuestData.Sticker> updated = new ArrayList<>(readCategoryStickers(category == null ? null : category.path));
            if (index < 0) {
                if (updatedText != null && !updatedText.isBlank()) {
                    updated.add(new QuestData.Sticker("text", updatedText, sticker.x(), sticker.y(), sticker.scale()));
                    saveTreeStickers(updated);
                }
                return;
            }
            if (index >= updated.size()) return;
            QuestData.Sticker current = updated.get(index);
            if ("icon".equals(current.type())) return;
            if (updatedText == null || updatedText.isBlank()) updated.remove((int) index);
            else updated.set(index, new QuestData.Sticker("text", updatedText, current.x(), current.y(), current.scale()));
            saveTreeStickers(updated);
        }));
    }

    protected void autosaveTreeBackground(String background, boolean alreadyUnsaved) {
        if (editorType != EditorType.QUEST || editingPath == null || currentPack == null) {
            markCurrentEditorUnsaved();
            return;
        }
        JsonObject obj = QuestEditorJsonFiles.readJson(editingPath);
        if (obj == null) return;
        obj.addProperty("tree_background", background);
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, editingPath);
            boolean indexed = questListIndexCache != null && Objects.equals(questListIndexPackRoot, currentPack.root);
            if (indexed) QuestEditorQuestOrdering.upsertQuestListIndex(questListIndexCache, obj, editingPath, editingPath);
            stagePackChange(currentPack, trs("status.quest_node_style_saved"), !indexed);
            QuestEditorPackFiles.queueBackupPack(currentPack, "node-style");
            refreshQuestTree();
            if (!alreadyUnsaved) markCurrentEditorSaved();
            statusMessage = trs("status.quest_node_style_saved");
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError(trs("error.save_quest_node_style_failed"));
        }
    }

    protected void resetCategoryBackgroundAutosaveState() {
        String state = categoryBackgroundAutosaveState();
        observedCategoryBackgroundState = state;
        persistedCategoryBackgroundState = state;
        categoryBackgroundSaveAt = 0L;
    }

    protected String categoryBackgroundAutosaveState() {
        return encodedCategoryBackground() + "\n"
                + (catBackgroundScaleSlider == null ? 0.5f : catBackgroundScaleSlider.getScale()) + "\n"
                + categoryBackgroundDarkeningEnabled() + "\n"
                + (catBackgroundOpacitySlider == null ? 0.45f : catBackgroundOpacitySlider.getOpacity());
    }

    protected boolean categoryBackgroundDarkeningEnabled() {
        return catBackgroundOpacitySlider != null && catBackgroundOpacitySlider.getOpacity() > 0.001f;
    }

    protected void tickCategoryBackgroundAutosave() {
        // wait for slider movement to settle before touching the category file
        if (editorType != EditorType.CATEGORY || editingPath == null || currentPack == null) return;
        String current = categoryBackgroundAutosaveState();
        long now = Util.getMillis();
        if (!current.equals(observedCategoryBackgroundState)) queueCategoryBackgroundAutosave(false);
        if (categoryBackgroundSaveAt > 0L && now >= categoryBackgroundSaveAt
                && !current.equals(persistedCategoryBackgroundState)) {
            autosaveCategoryBackground(current);
        }
    }

    protected void queueCategoryBackgroundAutosave(boolean immediate) {
        // merge repeated slider changes into one pending save
        observedCategoryBackgroundState = categoryBackgroundAutosaveState();
        categoryBackgroundSaveAt = Util.getMillis() + (immediate ? 0L : 500L);
        applyCategoryBackgroundPreview();
    }

    protected void flushCategoryBackgroundAutosave() {
        if (editorType != EditorType.CATEGORY || editingPath == null || currentPack == null) return;
        String current = categoryBackgroundAutosaveState();
        if (!current.equals(persistedCategoryBackgroundState)) autosaveCategoryBackground(current);
    }

    protected void applyCategoryBackgroundPreview() {
        if (questTree == null || catIdBox == null || !treeCategoryId.equals(catIdBox.getValue().trim())) return;
        questTree.setBackground(encodedCategoryBackground(), categoryBackgroundDarkeningEnabled(),
                catBackgroundOpacitySlider.getOpacity(), catBackgroundScaleSlider.getScale());
    }

    protected void autosaveCategoryBackground(String state) {
        // skip writes when the encoded background state has not changed
        JsonObject obj = QuestEditorJsonFiles.readJson(editingPath);
        if (obj == null) return;
        String background = encodedCategoryBackground();
        if (background.isBlank()) obj.remove("background");
        else obj.addProperty("background", background);
        obj.addProperty("background_tile_scale", catBackgroundScaleSlider.getScale());
        obj.addProperty("background_darkening", categoryBackgroundDarkeningEnabled());
        obj.addProperty("background_darkening_opacity", catBackgroundOpacitySlider.getOpacity());
        try {
            QuestPackStorage.writeJsonAtomically(gson, obj, editingPath);
            persistedCategoryBackgroundState = state;
            categoryBackgroundSaveAt = 0L;
            stagePackChange(currentPack, trs("status.category_background_saved"), false);
            QuestEditorPackFiles.queueBackupPack(currentPack, "category-background");
            invalidateCategoryDataCache();
            statusMessage = trs("status.category_background_saved");
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            categoryBackgroundSaveAt = Util.getMillis() + 1_000L;
            setError(trs("error.save_category_background_failed"));
        }
    }

    protected void requestDeleteTreeNode(QuestEntryData quest, boolean bypassConfirmation) {
        // require confirmation unless the delete tool is already armed
        if (quest == null || quest.path == null) return;
        Runnable delete = () -> deleteTreeNode(quest);
        if (bypassConfirmation) {
            delete.run();
            return;
        }
        confirmTreeAction(trs("confirm.delete_quest_node_title"),
                trs("confirm.delete_quest_node_message", safe(quest.name)), delete);
    }

    protected void requestTreeDependencyChange(QuestEntryData quest, String dependencyId, boolean add,
                                             boolean bypassConfirmation) {
        if (quest == null || quest.path == null || dependencyId == null || dependencyId.isBlank()) return;
        Runnable change = () -> changeTreeDependency(quest, dependencyId, add);
        if (add || bypassConfirmation) {
            change.run();
            return;
        }
        QuestEntryData dependency = questListIndex().questDataByEntryId.get(dependencyId);
        String dependencyName = dependency == null ? dependencyId : safe(dependency.name);
        confirmTreeAction(trs("confirm.delete_dependency_title"),
                trs("confirm.delete_dependency_message", dependencyName, safe(quest.name)), change);
    }

    protected void confirmTreeAction(String title, String message, Runnable action) {
        pendingTreeDeleteMessage = message == null || message.isBlank() ? title : message;
        pendingTreeDeleteAction = action;
    }

    protected void toggleTreeIncompatibility(QuestEntryData quest, String dependencyId) {
        // swap a dependency between normal and incompatible states
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
            stageCurrentPackChange(trs(incompatible ? "status.incompatibility_added" : "status.incompatibility_removed"), false);
            QuestEditorPackFiles.queueBackupPack(currentPack, incompatible ? "incompatible" : "compatible");
            refreshLeftList();
            statusMessage = trs(incompatible ? "status.incompatibility_created" : "status.dependency_restored");
            statusColor = incompatible ? 0xFFFF6060 : 0xA0FFA0;
        } catch (IOException e) {
            setError(trs("error.update_incompatibility_failed"));
        }
    }

    protected void deleteTreeNode(QuestEntryData quest) {
        // remove the quest and clean references from remaining nodes
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
            boolean deletedOpenQuest = editingPath != null && editingPath.equals(quest.path);
            if (deletedOpenQuest) clearEditor();
            selectedEntryId = "";
            stageCurrentPackChange(trs("status.quest_node_deleted"));
            QuestEditorPackFiles.queueBackupPack(currentPack, "deleted");
            refreshLeftList();
            if (deletedOpenQuest) openCurrentCategorySettings();
            statusMessage = trs("status.quest_node_deleted");
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError(trs("error.delete_quest_node_failed"));
        }
    }

    protected void changeTreeDependency(QuestEntryData quest, String dependencyId, boolean add) {
        // update one dependency edge and preserve the rest of the quest json
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
            stageCurrentPackChange(trs(add ? "status.dependency_connected" : "status.dependency_removed"), false);
            QuestEditorPackFiles.queueBackupPack(currentPack, add ? "connected" : "disconnected");
            refreshLeftList();
            statusMessage = trs(add ? "status.dependency_connected" : "status.dependency_removed");
            statusColor = 0xA0FFA0;
        } catch (IOException e) {
            setError(trs(add ? "error.connect_quests_failed" : "error.remove_dependency_failed"));
        }
    }

    protected static void writeDependencies(JsonObject obj, List<String> dependencies) {
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

    protected static void writeStringArray(JsonObject obj, String key, Iterable<String> values) {
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
    protected void handlePackEntry(EditorEntry entry) {
        if (ENTRY_BUILTIN_PACK.equals(entry.id)) {
            currentPack = QuestEditorPackFiles.builtinPack();
            invalidateQuestListIndex();
            selectedEntryId = ENTRY_BUILTIN_PACK;
            if (!QuestEditorPackFiles.ensurePack(currentPack)) {
                setError(trs("error.open_pack_failed"));
                return;
            }
            setMode(Mode.QUEST_LIST);
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
        setMode(Mode.QUEST_LIST);
    }

    // switch between pack sections
    protected void handlePackMenuEntry(EditorEntry entry) {
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
    protected void handleCategoryEntry(EditorEntry entry) {
        if (ENTRY_NEW.equals(entry.id)) {
            showCategoryEditor(new CategoryData(), null);
            return;
        }
        if (currentPack == null) return;
        CategoryData data = categoryData(entry.id);
        if (data == null) {
            statusMessage = trs("status.category_load_failed");
            statusColor = 0xFF8080;
            return;
        }
        showCategoryEditor(data, data.path);
    }

    // open the selected sub category form
    protected void handleSubCategoryEntry(EditorEntry entry) {
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
    protected void handleQuestEntry(EditorEntry entry) {
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
            QuestEntryData data = new QuestEntryData();
            data.category = treeCategoryId;
            showQuestEditor(data, null);
            return;
        }
        if (currentPack == null) return;
        QuestEntryData data = questListIndex().questDataByEntryId.get(entry.id);
        if (data == null) {
            statusMessage = trs("status.quest_load_failed");
            statusColor = 0xFF8080;
            return;
        }
        showQuestEditor(data, data.path);
    }

    // show the new pack form
    protected void showPackCreate() {
        editorType = EditorType.PACK_CREATE;
        editingPath = null;
        packNameBox.setValue("");
        packIconPathBox.setValue("");

        setActiveFields(List.of(
                field(trs("field.display"), packNameBox),
                field(trs("field.import_existing_questline"), ftbImportSection)
        ));
        saveButton.setMessage(tr("create"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorUnsaved();
        updateBackButtonVisibility();
    }

    // show pack rename and icon options
    protected void showPackOptions(QuestPack pack) {
        if (pack == null) return;
        editorType = EditorType.PACK_OPTIONS;
        editingPath = null;

        PackMeta meta = QuestEditorPackFiles.readPackMeta(pack.root, pack.name);
        packNameBox.setValue(safe(pack.name));
        packIconPathBox.setValue(safe(meta.iconPath));
        refreshPackBackups(pack);

        List<FormField> fields = new ArrayList<>();
        fields.add(field(trs("field.display"), packNameBox));
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

    protected String questListCollapseStateKey() {
        if (currentPack == null || currentPack.root == null) return "";
        return currentPack.root.toAbsolutePath().normalize().toString();
    }

    protected void restoreQuestListCollapseState() {
        String key = questListCollapseStateKey();
        collapsedQuestCategories.clear();
        collapsedQuestSubCategories.clear();
        if (key.isBlank()) return;
        collapsedQuestCategories.addAll(QUEST_CATEGORY_COLLAPSE_STATES.getOrDefault(key, Set.of()));
        collapsedQuestSubCategories.addAll(QUEST_SUBCATEGORY_COLLAPSE_STATES.getOrDefault(key, Set.of()));
    }

    protected void rememberQuestListCollapseState() {
        String key = questListCollapseStateKey();
        if (key.isBlank()) return;
        QUEST_CATEGORY_COLLAPSE_STATES.put(key, new HashSet<>(collapsedQuestCategories));
        QUEST_SUBCATEGORY_COLLAPSE_STATES.put(key, new HashSet<>(collapsedQuestSubCategories));
    }

    // show category editing fields
    protected void showCategoryEditor(CategoryData data, Path sourcePath) {
        editorType = EditorType.CATEGORY;
        editingPath = sourcePath;

        catIdBox.setValue(safe(data.id));
        catNameBox.setValue(safe(data.name));
        catIconBox.setValue(safe(data.icon));
        setCategoryBackgroundValue(data.background);
        catBackgroundScaleSlider.setScale(parseFloat(data.backgroundTileScale, 0.5f));
        boolean darkeningEnabled = parseBool(data.backgroundDarkening, false);
        catBackgroundDarkeningToggle.setState(darkeningEnabled);
        catBackgroundOpacitySlider.setOpacity(darkeningEnabled ? parseFloat(data.backgroundDarkeningOpacity, 0.45f) : 0f);
        resetCategoryBackgroundAutosaveState();
        catDependencyBox.setValue(safe(data.dependency));
        catAutoCompleteToggle.setState(parseBool(data.autoComplete, false));
        catLayoutSelector.setTreeLayout("tree".equalsIgnoreCase(data.layout));
        categorySubCategorySection.setEntries(categorySubCategories(data.id));

        applyCategoryEditorFields();
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    protected void applyCategoryEditorFields() {
        if (editorType != EditorType.CATEGORY) return;
        float previousScroll = editorScroll;
        List<FormField> fields = new ArrayList<>();
        if (Config.showIdField()) fields.add(field(trs("field.id"), catIdBox));
        fields.add(field(trs("field.display"), catNameBox));
        fields.add(field(trs("field.category_type"), catLayoutSelector));
        fields.add(field(trs("field.category_background_help"), catBackgroundBox));
        if (!encodedCategoryBackground().isBlank() && catBackgroundMode != CategoryBackgroundMode.FULL_IMAGE) {
            fields.add(field(trs("field.background_tile_scale"), catBackgroundScaleSlider));
        }
        fields.add(field(trs("field.background_darkening"), catBackgroundOpacitySlider));
        if (!catLayoutSelector.isTreeLayout()) {
            fields.add(field(trs("field.unlock_quest"), catDependencyBox));
            fields.add(field(trs("field.auto_complete") + " (" + trs("tooltip.auto_complete_category") + ")", catAutoCompleteToggle));
            fields.add(field(trs("field.subcategories"), categorySubCategorySection));
        }
        setActiveFields(fields);
        editorScroll = previousScroll;
    }

    protected void setCategoryBackgroundValue(String encoded) {
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

    protected String encodedCategoryBackground() {
        String value = safe(catBackgroundBox == null ? "" : catBackgroundBox.getValue()).trim();
        if (value.isBlank()) return "";
        return switch (catBackgroundMode) {
            case TILEABLE -> value;
            case CUSTOM_TILEABLE -> "custom_tile:" + value;
            case FULL_IMAGE -> "full:" + value;
        };
    }

    protected void cycleCategoryBackgroundMode() {
        catBackgroundMode = switch (catBackgroundMode) {
            case TILEABLE -> CategoryBackgroundMode.CUSTOM_TILEABLE;
            case CUSTOM_TILEABLE -> CategoryBackgroundMode.FULL_IMAGE;
            case FULL_IMAGE -> CategoryBackgroundMode.TILEABLE;
        };
        if (catBackgroundBox != null) catBackgroundBox.setValue("");
        if (catBackgroundScaleSlider != null) {
            catBackgroundScaleSlider.active = catBackgroundMode != CategoryBackgroundMode.FULL_IMAGE;
        }
        applyCategoryEditorFields();
        queueCategoryBackgroundAutosave(true);
    }

    protected void openCategoryBackgroundPicker() {
        openIconItemPicker(catBackgroundBox);
        itemPickerTab = catBackgroundMode == CategoryBackgroundMode.TILEABLE
                ? ItemPickerTab.CREATIVE : ItemPickerTab.CUSTOM;
        if (catBackgroundMode == CategoryBackgroundMode.FULL_IMAGE) {
            statusMessage = trs("status.full_image_background_help");
            statusColor = 0xFFFFFFFF;
        } else if (catBackgroundMode == CategoryBackgroundMode.CUSTOM_TILEABLE) {
            statusMessage = trs("status.tile_background_help");
            statusColor = 0xFFFFFFFF;
        }
    }

    protected void showSubCategoryEditor(SubCategoryData data, Path sourcePath) {
        editorType = EditorType.SUBCATEGORY;
        editingPath = sourcePath;

        subIdBox.setValue(safe(data.id));
        setDropdownBoxValue(subCategoryBox, safe(data.category));
        subNameBox.setValue(safe(data.name));
        subIconBox.setValue(safe(data.icon));
        subDefaultOpenToggle.setState(parseBool(data.defaultOpen, true));

        List<FormField> fields = new ArrayList<>();
        if (Config.showIdField()) fields.add(field(trs("field.id"), subIdBox));
        fields.add(field(trs("field.category_id"), subCategoryBox));
        fields.add(field(trs("field.display"), subNameBox));
        fields.add(field(trs("field.default_open") + " (" + trs("tooltip.default_open") + ")", subDefaultOpenToggle));
        setActiveFields(fields);
        saveButton.setMessage(tr("save"));
        saveButton.visible = true;
        saveButton.active = true;
        markCurrentEditorLoaded(sourcePath != null);
        updateBackButtonVisibility();
    }

    protected void showQuestEditor(QuestEntryData data, Path sourcePath) {
        editorType = EditorType.QUEST;
        editingPath = sourcePath;
        disarmDeleteConfirm();

        // copy quest data into the active form
        questIdBox.setValue(safe(data.id));
        questNameBox.setValue(safe(data.name));
        resetTranslationRows(false, data.nameTranslations);
        questIconBox.setValue(safe(data.icon).isBlank() ? "boundless:quest_book" : safe(data.icon));
        questDescriptionBox.setValue(safe(data.description));
        resetTranslationRows(true, data.descriptionTranslations);
        questMediaSelector.setValue(safe(data.media));
        QuestCategorySelection previousSelection = sourcePath == null && safe(data.category).isBlank() && currentPack != null
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
        questImportWarnings = data.importWarnings == null ? List.of() : List.copyOf(data.importWarnings);
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

    protected void applyQuestEditorFields() {
        // copy visible quest controls into the loaded quest model
        float previousScroll = editorScroll;
        List<FormField> fields = new ArrayList<>();
        if (Config.showIdField()) fields.add(field(trs("field.id"), questIdBox));
        fields.add(field(trs("field.display"), questNameBox));
        fields.add(field(trs("field.description"), questDescriptionBox));
        fields.add(field(trs("field.image_or_media"), questMediaSelector));
        fields.add(field(trs("field.category_required"), questCategoryBox));
        if (!dropdownBoxValue(questCategoryBox).isBlank()) {
            fields.add(field(trs("field.subcategory"), questSubCategoryBox));
        } else if (questSubCategoryBox != null) {
            setDropdownBoxValue(questSubCategoryBox, "");
        }
        fields.add(field(trs("field.dependencies"), questDependenciesBox));
        if (isTreePack()) fields.add(field(trs("field.quest_node_background"), questTreeBackgroundSelector));
        fields.add(field(trs("field.flags"), questOptionalToggle));
        fields.add(field(trs("field.completion"), questCompletionBox));
        fields.add(field(trs("field.reward"), questRewardBox));
        setActiveFields(fields);
        editorScroll = previousScroll;
    }
    protected void saveCurrent() {
        // route save through the active editor type
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

    protected void savePackCreate() {
        // create the pack structure before opening its workspace
        String name = QuestEditorNaming.cleanPackName(packNameBox.getValue());
        if (!Objects.equals(name, safe(packNameBox.getValue()))) {
            packNameBox.setValue(name);
        }
        String namespace = QuestEditorPackFiles.namespaceFromPackName(name);
        if (!QuestEditorPackFiles.hasPackNameContent(name)) {
            setError(trs("error.pack_name_required"));
            return;
        }
        if (QuestEditorPackFiles.isInvalidPackFolderName(name)) {
            setError(trs("error.invalid_pack_name"));
            return;
        }
        if (QuestEditorPackFiles.isInvalidNamespace(namespace)) {
            setError(trs("error.invalid_namespace"));
            return;
        }

        Path root = QuestEditorPackFiles.packsRoot().resolve(name);
        if (Files.exists(root)) {
            setError(trs("error.pack_exists"));
            return;
        }

        try {
            Files.createDirectories(root.getParent());
            Files.createDirectories(root);
            String iconPath = QuestEditorPackFiles.cleanIconId(packIconPathBox.getValue());
            QuestEditorPackFiles.writePackMeta(root, name, "Boundless Quest Pack: " + name, iconPath, true);
            QuestPack pack = new QuestPack(name, namespace, root, false, true);
            pack.ensureDirs();
            currentPack = pack;
            QuestEditorPackFiles.backupPack(currentPack, "created");
            treeCategoryId = "";
            setMode(Mode.QUEST_LIST);
            stagePackChange(pack, trs("status.pack_staged"));
        } catch (Exception e) {
            setError(trs("error.create_pack_failed"));
        }
    }

    protected void selectFtbQuestImportFolder() {
        // open the native folder picker at the last useful location
        if (ftbImportRunning) return;
        Path defaultFolder = minecraft.gameDirectory.toPath().resolve("config").resolve("ftbquests").resolve("quests");
        if (!Files.isDirectory(defaultFolder)) defaultFolder = minecraft.gameDirectory.toPath().resolve("config");
        String selected;
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer filters = stack.mallocPointer(3);
            filters.put(stack.UTF8("*.snbt"));
            filters.put(stack.UTF8("*.json5"));
            filters.put(stack.UTF8("*.json"));
            filters.flip();
            selected = TinyFileDialogs.tinyfd_openFileDialog(
                    "Select any FTB Quests data or chapter file",
                    defaultFolder.toAbsolutePath().toString(),
                    filters,
                    "FTB Quests data",
                    false);
        }
        if (selected == null || selected.isBlank()) return;
        beginFtbQuestImport(Path.of(selected));
    }

    protected void beginFtbQuestImport(Path sourceFolder) {
        // import large packs away from the render thread
        String requestedName = QuestEditorNaming.cleanPackName(packNameBox.getValue());
        if (requestedName.isBlank()) {
            requestedName = QuestEditorNaming.nextCopyPack("ftb-quests-import");
            packNameBox.setValue(requestedName);
        }
        if (QuestEditorPackFiles.isInvalidPackFolderName(requestedName)) {
            setError(trs("error.valid_pack_name_before_import"));
            return;
        }
        if (Files.exists(QuestEditorPackFiles.packsRoot().resolve(requestedName))) {
            setError(trs("error.pack_exists"));
            return;
        }
        ftbImportRunning = true;
        statusMessage = trs("status.importing_ftb");
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

    protected void finishFtbQuestImport(FtbQuestImporter.ImportResult result, Throwable error) {
        // publish the imported pack and surface any conversion warnings
        ftbImportRunning = false;
        if (error != null || result == null) {
            Throwable cause = error;
            while (cause != null && cause.getCause() != null
                    && (cause instanceof CompletionException || cause.getMessage() == null)) {
                cause = cause.getCause();
            }
            String message = cause == null || cause.getMessage() == null ? "Unknown error" : cause.getMessage();
            BoundlessMod.LOGGER.error("Failed to import FTB Quests: {}", message, cause);
            setError(trs("error.ftb_import_failed", message));
            return;
        }
        try {
            currentPack = result.pack();
            currentPack.ensureDirs();
            selectedEntryId = currentPack.name;
            String importStatus = "Imported " + result.quests() + " quests from " + result.chapters()
                    + " chapters (" + result.warnings().size() + " warning"
                    + (result.warnings().size() == 1 ? "" : "s") + ")";
            stagePackChange(currentPack, importStatus);
            questListIndexCache = result.questIndex();
            questListIndexPackRoot = currentPack.root;
            QuestEditorPackFiles.queueBackupPack(currentPack, "ftb-imported");
            setMode(Mode.QUEST_LIST);
            statusMessage = importStatus;
            statusColor = 0xA0FFA0;
        } catch (IOException exception) {
            setError(trs("error.ftb_pack_open_failed", safe(exception.getMessage())));
        }
    }

    protected void savePackOptions() {
        // rename or update the pack without dropping its existing data
        if (currentPack == null) return;

        String requestedName = QuestEditorNaming.cleanPackName(packNameBox.getValue());
        if (!Objects.equals(requestedName, safe(packNameBox.getValue()))) {
            packNameBox.setValue(requestedName);
        }
        String requestedNamespace = QuestEditorPackFiles.namespaceFromPackName(requestedName);
        if (!QuestEditorPackFiles.hasPackNameContent(requestedName)) {
            setError(trs("error.pack_name_required"));
            return;
        }
        if (QuestEditorPackFiles.isInvalidPackFolderName(requestedName)) {
            setError(trs("error.invalid_pack_name"));
            return;
        }

        Path oldRoot = currentPack.root;
        String oldName = currentPack.name;
        String oldNamespace = currentPack.namespace;
        Path newRoot = oldRoot;
        if (!requestedName.equals(oldName)) {
            newRoot = QuestEditorPackFiles.packsRoot().resolve(requestedName);
            if (Files.exists(newRoot)) {
                setError(trs("error.pack_exists"));
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
                    setError(trs("error.namespace_exists"));
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
            stagePackChange(currentPack, trs("status.pack_options_saved"));
            QuestEditorPackFiles.backupPack(currentPack, "saved");
            refreshLeftList();
            showPackOptions(currentPack);
            markCurrentEditorSaved();
        } catch (IOException e) {
            setError(trs("error.save_pack_options_failed"));
        }
    }

    protected void saveCategory() {
        // preserve category order while writing edited values
        String id = Config.showIdField() ? safe(catIdBox.getValue()).trim() : questIdFromName(catNameBox.getValue());
        catIdBox.setValue(id);
        if (id.isBlank()) {
            setError(trs("error.category_id_required"));
            return;
        }
        selectedEntryId = id;
        treeCategoryId = id;
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

    protected void saveSubCategory() {
        // keep subcategory placement stable across edits
        String id = Config.showIdField() ? safe(subIdBox.getValue()).trim() : questIdFromName(subNameBox.getValue());
        subIdBox.setValue(id);
        if (id.isBlank()) {
            setError(trs("error.subcategory_id_required"));
            return;
        }
        selectedEntryId = id;
        JsonObject obj = buildSubCategoryJson(id);
        if (obj == null) return;
        Path target = currentPack.subCategoriesDir.resolve(id + ".json");
        saveJson(obj, target, editingPath);
    }

    protected JsonObject buildCategoryJson(String id) {
        // build category json from the current fields and background controls
        JsonObject category = QuestEditorJsonBuilder.buildCategory(
                id,
                catNameBox.getValue(),
                catIconBox.getValue(),
                resolveCategoryOrder(id),
                catDependencyBox.getValue(),
                catAutoCompleteToggle.isOn(),
                encodedCategoryBackground(),
                catBackgroundScaleSlider.getScale(),
                categoryBackgroundDarkeningEnabled(),
                catBackgroundOpacitySlider.getOpacity(),
                this::setError
        );
        if (category != null && catLayoutSelector.isTreeLayout()) category.addProperty("layout", "tree");
        return category;
    }

    protected JsonObject buildSubCategoryJson(String id) {
        // build subcategory json from the current editor state
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

    protected int resolveCategoryOrder(String categoryId) {
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

    protected int resolveSubCategoryOrder(String subCategoryId) {
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

    protected void saveQuest() {
        // validate and write the quest using its current category selection
        String id = Config.showIdField() ? safe(questIdBox.getValue()).trim() : questIdFromName(questNameBox.getValue());
        questIdBox.setValue(id);
        selectedEntryId = id;
        JsonObject obj = buildQuestJson(id);
        if (obj == null) return;
        String orderToken = questOrderTokenForSave(id);
        Path target = currentPack.questsDir.resolve(QuestEditorQuestOrdering.questFileBaseName(id, orderToken) + ".json");
        questOrderToken = orderToken;
        saveJson(obj, target, editingPath);
        if (editingPath != null) rememberQuestCategorySelection();
    }

    protected static String questIdFromName(String name) {
        return QuestEditorNaming.idFromDisplayName(name);
    }

    protected void rememberQuestCategorySelection() {
        if (currentPack == null) return;
        LAST_QUEST_CATEGORY_SELECTIONS.put(currentPack.name, new QuestCategorySelection(
                dropdownBoxValue(questCategoryBox), dropdownBoxValue(questSubCategoryBox)));
    }

    protected JsonObject buildQuestJson(String id) {
        // merge edited values with fields that the editor does not own
        syncEntryBackingValues();
        QuestBuildFields fields = new QuestBuildFields();
        fields.id = id;
        fields.duplicateQuestId = isDuplicateQuestId(safe(id).trim());
        fields.name = questNameBox.getValue();
        fields.nameTranslations = translationValues(false);
        fields.icon = questIconBox.getValue();
        fields.description = questDescriptionBox.getValue();
        fields.descriptionTranslations = translationValues(true);
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
        fields.importWarnings = questImportWarnings;
        return QuestEditorJsonBuilder.buildQuest(fields, this::setError);
    }

    protected void saveJson(JsonObject obj, Path target, Path original) {
        // write through a temporary file before replacing the original
        try {
            QuestEntryData previousQuest = editorType == EditorType.QUEST && questListIndexCache != null
                    ? questListIndexCache.questDataByEntryId.get(selectedEntryId) : null;
            String previousTreeSignature = treeQuestSignature(previousQuest);
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
            boolean questSave = editorType == EditorType.QUEST && questListIndexCache != null
                    && Objects.equals(questListIndexPackRoot, currentPack.root);
            if (questSave) QuestEditorQuestOrdering.upsertQuestListIndex(questListIndexCache, obj, target, original);
            stagePackChange(currentPack, trs("status.saved_to_staging"), !questSave);
            QuestEditorPackFiles.queueBackupPack(currentPack, "saved");
            QuestEntryData updatedQuest = questSave ? questListIndexCache.questDataByEntryId.get(selectedEntryId) : null;
            refreshLeftList(true, !questSave || !Objects.equals(previousTreeSignature, treeQuestSignature(updatedQuest)));
        } catch (IOException e) {
            setError(trs("error.save_failed", safe(e.getMessage())));
        }
    }

    protected void duplicateCurrentPackAsIs() {
        // copy the pack folder without rebuilding its quest data
        if (currentPack == null) return;
        try {
            if (!QuestEditorPackFiles.ensurePack(currentPack)) {
                setError(trs("error.open_pack_failed"));
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
            stagePackChange(duplicated, trs("status.pack_duplicated"));
            refreshLeftList();
            leftList.setSelectedId(selectedEntryId);
            showPackOptions(duplicated);
            markCurrentEditorSaved();
        } catch (IOException e) {
            setError(trs("error.duplicate_pack_failed"));
        }
    }

    protected void openPackDirectory() {
        try {
            if (currentPack == null || !QuestEditorPackFiles.ensurePack(currentPack)) {
                setError(trs("error.open_pack_failed"));
                return;
            }
            Path folder = currentPack.root;
            Files.createDirectories(folder);
            Util.getPlatform().openFile(folder.toFile());
        } catch (Exception e) {
            setError(trs("error.open_folder_failed"));
        }
    }

    protected void openImportQuestPackDirectory() {
        Path importRoot = Config.boundlessConfigRoot();
        try {
            Files.createDirectories(importRoot);
            Util.getPlatform().openFile(importRoot.toFile());
            statusMessage = trs("status.place_questpacks");
            statusColor = 0xA0A0A0;
        } catch (Exception e) {
            setError(trs("error.open_import_directory_failed"));
        }
    }

    protected void duplicateQuest() {
        // duplicate the quest with a unique id and nearby tree position
        if (currentPack == null || editorType != EditorType.QUEST) return;
        String baseId = safe(questIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError(trs("error.quest_id_required"));
            return;
        }

        String newId = nextAvailableQuestId(baseId);
        JsonObject obj = buildQuestJson(newId);
        if (obj == null) return;

        selectedEntryId = newId;
        String orderToken = QuestEditorNaming.nextOrder(currentPack);
        Path target = currentPack.questsDir.resolve(QuestEditorQuestOrdering.questFileBaseName(newId, orderToken) + ".json");
        saveJson(obj, target, null);
        QuestEntryData data = questListIndex().questDataByEntryId.get(newId);
        if (data != null) {
            showQuestEditor(data, data.path);
        }
    }

    protected void duplicateCategory() {
        // duplicate category settings without copying unrelated quests
        if (currentPack == null || editorType != EditorType.CATEGORY) return;
        String baseId = safe(catIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError(trs("error.category_id_required"));
            return;
        }
        String newId = nextAvailableCategoryId(baseId);
        JsonObject obj = buildCategoryJson(newId);
        if (obj == null) return;
        selectedEntryId = newId;
        saveJson(obj, currentPack.categoriesDir.resolve(newId + ".json"), null);
        CategoryData data = categoryData(newId);
        if (data != null) showCategoryEditor(data, data.path);
    }

    protected void duplicateSubCategory() {
        // duplicate the selected subcategory with a unique id
        if (currentPack == null || editorType != EditorType.SUBCATEGORY) return;
        String baseId = safe(subIdBox.getValue()).trim();
        if (baseId.isBlank()) {
            setError(trs("error.subcategory_id_required"));
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

    protected void duplicateCurrent() {
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

    protected void deleteQuest() {
        // remove the quest and any references that point to it
        if (currentPack == null || editorType != EditorType.QUEST) return;
        String id = safe(questIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError(trs("error.quest_id_required"));
            return;
        }

        Path target = editingPath != null ? editingPath : currentPack.questsDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            if (deleted) {
                stageCurrentPackChange(trs("status.deletion_staged"));
                refreshLeftList();
                openCurrentCategorySettings();
            } else {
                statusMessage = trs("status.nothing_to_delete");
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError(trs("error.delete_failed"));
        }
    }

    protected void deleteCategory() {
        // delete the category and its owned editor data
        if (currentPack == null || editorType != EditorType.CATEGORY) return;
        String id = safe(catIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError(trs("error.category_id_required"));
            return;
        }

        Path target = editingPath != null ? editingPath : currentPack.categoriesDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            if (deleted) {
                stageCurrentPackChange(trs("status.deletion_staged"));
                List<NamedEntry> categories = categoryEntries();
                treeCategoryId = categories.isEmpty() ? "" : categories.getFirst().id;
                refreshLeftList();
                openCurrentCategorySettings();
            } else {
                statusMessage = trs("status.nothing_to_delete");
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError(trs("error.delete_failed"));
        }
    }

    protected void deleteSubCategory() {
        // delete the subcategory without changing other category entries
        if (currentPack == null || editorType != EditorType.SUBCATEGORY) return;
        String id = safe(subIdBox.getValue()).trim();
        if (id.isBlank()) {
            setError(trs("error.subcategory_id_required"));
            return;
        }

        Path target = editingPath != null ? editingPath : currentPack.subCategoriesDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(target);
            disarmDeleteConfirm();
            selectedEntryId = "";
            clearEditor();
            if (deleted) {
                stageCurrentPackChange(trs("status.deletion_staged"));
                refreshLeftList();
                openCurrentCategorySettings();
            } else {
                statusMessage = trs("status.nothing_to_delete");
                statusColor = 0xA0A0A0;
            }
        } catch (IOException e) {
            setError(trs("error.delete_failed"));
        }
    }

    protected void deleteCurrent() {
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

    protected QuestPack resolveCurrentPackForDelete() {
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

    protected void handleDeleteButtonPress() {
        // require the second press or completed hold before deletion
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

    protected void disarmDeleteConfirm() {
        deleteHoldActive = false;
        if (!deleteConfirmArmed) return;
        deleteConfirmArmed = false;
        updateDeleteButtonTexture();
    }

    protected void updateDeleteButtonTexture() {
        if (deleteQuestButton != null) {
            if (deleteConfirmArmed) {
                deleteQuestButton.setTextures(DELETE_CONFIRM_TEX, DELETE_CONFIRM_TEX_HOVER);
            } else {
                deleteQuestButton.setTextures(DELETE_TEX, DELETE_CONFIRM_TEX_HOVER);
            }
        }
    }

    protected void updateDeleteHold(double mouseX, double mouseY) {
        // cancel the hold when the cursor leaves the delete control
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

    protected float deleteHoldProgress() {
        if (!deleteHoldActive) return 0f;
        return Math.min(1f, (Util.getMillis() - deleteHoldStartMs) / (float) DELETE_HOLD_MS);
    }

    protected void renderDeleteHoldProgress(GuiGraphics gg) {
        float progress = deleteHoldProgress();
        if (progress <= 0f || deleteQuestButton == null || !deleteQuestButton.visible) return;
        int x0 = deleteQuestButton.getX() + 2;
        int y0 = deleteQuestButton.getY() + deleteQuestButton.getHeight() - 4;
        int w = Math.round((deleteQuestButton.getWidth() - 4) * progress);
        gg.fill(x0, y0, x0 + w, y0 + 2, 0xFFFF3030);
    }

    protected String nextAvailableQuestId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextCopyId(base, currentPack.questsDir);
    }

    protected String nextAvailableCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextCopyId(base, currentPack.categoriesDir);
    }

    protected String nextAvailableSubCategoryId(String baseId) {
        if (currentPack == null) return baseId;
        String base = safe(baseId).trim();
        if (base.isBlank()) return baseId;
        return QuestEditorNaming.nextCopyId(base, currentPack.subCategoriesDir);
    }

    protected String nextAvailablePackName(String baseName) {
        return QuestEditorNaming.nextCopyPack(baseName);
    }

    protected void deletePack(QuestPack pack) {
        // protect the built in pack and remove backups with deleted packs
        if (pack == null) return;
        if (QuestEditorPackFiles.isBuiltinPack(pack)) {
            setError(trs("error.builtin_pack_delete"));
            return;
        }
        try {
            if (Files.exists(pack.root)) {
                QuestEditorPackFiles.deleteDirectory(pack.root);
            }
            try {
                QuestPackStorage.deleteQuestPackBackups(pack.name);
            } catch (IOException ignored) {
            }
            currentPack = null;
            invalidateQuestListIndex();
            selectedEntryId = "";
            setMode(Mode.PACK_LIST);
            stagePackDeletion(pack, trs("status.pack_deletion_staged"));
        } catch (IOException e) {
            setError(trs("error.delete_failed"));
        }
    }

    protected int componentStart(String raw) {
        String value = safe(raw);
        int brace = value.indexOf('{');
        int bracket = value.indexOf('[');
        if (brace < 0) return bracket;
        if (bracket < 0) return brace;
        return Math.min(brace, bracket);
    }

    protected boolean applyChanges() {
        // mirror staged files then reload quest data on the client thread
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

    protected void stageCurrentPackChange(String message) {
        stagePackChange(currentPack, message);
    }

    protected void stageCurrentPackChange(String message, boolean invalidateIndex) {
        stagePackChange(currentPack, message, invalidateIndex);
    }

    protected void stagePackChange(QuestPack pack, String message) {
        stagePackChange(pack, message, true);
    }

    protected void stagePackChange(QuestPack pack, String message, boolean invalidateIndex) {
        if (pack == null || pack.name == null || pack.name.isBlank()) return;
        // keep staged pack edits until the screen closes
        if (invalidateIndex) invalidateQuestListIndex();
        stagedDeletedPackNames.remove(pack.name);
        stagedPacks.put(pack.name, pack);
        statusMessage = message;
        statusColor = 0xA0FFA0;
    }

    protected String treeQuestSignature(QuestEntryData quest) {
        if (quest == null) return "";
        return safe(quest.id) + '\n' + safe(quest.icon) + '\n' + safe(quest.category) + '\n'
                + safe(quest.dependencies) + '\n' + safe(quest.treeX) + '\n' + safe(quest.treeY) + '\n'
                + safe(quest.treeBackground) + '\n' + safe(quest.treeOrigin);
    }

    protected void stagePackDeletion(QuestPack pack, String message) {
        if (pack == null || pack.name == null || pack.name.isBlank()) return;
        invalidateQuestListIndex();
        stagedPacks.remove(pack.name);
        stagedDeletedPackNames.add(pack.name);
        statusMessage = message;
        statusColor = 0xA0FFA0;
    }

    protected boolean hasStagedChanges() {
        return !stagedPacks.isEmpty() || !stagedDeletedPackNames.isEmpty();
    }

    protected void applyStagedChangesOnClose() {
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

        // edited packs may already be applied but still need a data reload
        if (singleplayerAuthority) {
            QuestEditorPackFiles.reloadLater();
        }
        mc.execute(() -> QuestData.loadClient(true));
    }

    protected boolean isSingleplayerAuthoritySession() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getSingleplayerServer() != null;
    }

    // this refreshes an open editor after a server sync
    public void refreshSyncedData() {
        if (mode == Mode.PACK_LIST) refreshLeftList();
    }

    protected void exportCurrentPack() {
        // export the current pack after flushing its staged changes
        if (currentPack == null || currentPack.root == null || !Files.isDirectory(currentPack.root)) {
            setError(trs("error.no_pack_to_export"));
            return;
        }
        try {
            Path zipPath = QuestEditorPackFiles.nextZipPath(currentPack);
            QuestEditorPackFiles.zipDirectory(currentPack.root, zipPath);
            statusMessage = trs("status.exported", zipPath.getFileName());
            statusColor = 0xA0FFA0;
            QuestEditorPackFiles.openParentFolder(zipPath);
        } catch (Exception e) {
            setError(trs("error.export_pack_failed"));
        }
    }

    protected void refreshPackBackups(QuestPack pack) {
        // reload backup rows only when the selected pack changes
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

    protected void confirmRestoreBackup(QuestPackStorage.Snapshot snapshot) {
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

    protected void restoreBackup(QuestPackStorage.Snapshot snapshot) {
        // restore the snapshot then rebuild editor caches from disk
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

    protected void clearEditor() {
        // reset transient state before another editor form is shown
        closeTransientMenus();
        editorType = EditorType.NONE;
        editingPath = null;
        loadedQuestType = "";
        questImportWarnings = List.of();
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

    protected void closeTransientMenus() {
        // close every popup that should not survive a mode change
        popupMenus.close();
        emojiPickerOpen = false;
        if (isItemPickerOpen()) {
            closeItemPicker();
        }
    }

    protected void setActiveFields(List<FormField> fields) {
        // hide old fields before laying out the next editor form
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
        if (questNameLanguageButton != null) questNameLanguageButton.setVisible(false);
        if (questDescriptionLanguageButton != null) questDescriptionLanguageButton.setVisible(false);
        for (QuestEditorScreen.TranslationRow row : questNameTranslationRows) row.setVisible(false);
        for (QuestEditorScreen.TranslationRow row : questDescriptionTranslationRows) row.setVisible(false);
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

    protected void hideDescriptionFormatterButtons() {
        for (TextInsertButton button : descriptionFormatButtons) {
            button.visible = false;
            button.active = false;
        }
    }

    protected void initEntryRowBoxes() {
        // create the first editable row for each repeatable quest section
        resetEntryRows(EntryRowKind.DEPENDENCY, List.of());
        resetEntryRows(EntryRowKind.COMPLETION, List.of());
        resetEntryRows(EntryRowKind.REWARD, List.of());
        syncEntryBackingValues();
    }

    protected static List<String> buildQuestLanguageCodes() {
        Set<String> codes = new LinkedHashSet<>(List.of("en_us", "en", "es_es", "es", "pl_pl", "pl"));
        for (Locale locale : Locale.getAvailableLocales()) {
            String tag = locale.toLanguageTag();
            if (tag == null || tag.isBlank() || "und".equalsIgnoreCase(tag)) continue;
            codes.add(tag.toLowerCase(Locale.ROOT).replace('-', '_'));
            if (!locale.getLanguage().isBlank()) codes.add(locale.getLanguage().toLowerCase(Locale.ROOT));
        }
        return codes.stream().sorted().toList();
    }

    protected void resetTranslationRows(boolean description, Map<String, String> values) {
        // rebuild translation rows from saved locale values
        List<QuestEditorScreen.TranslationRow> rows = translationRows(description);
        syncingTranslationRows = true;
        try {
            for (QuestEditorScreen.TranslationRow row : rows) row.removeWidgets();
            rows.clear();
            if (values != null) values.forEach((locale, value) -> addTranslationRow(description, locale, value));
            addTranslationRow(description, nextTranslationLocale(rows), "");
            if (description) questDescriptionTranslationsOpen = values != null && !values.isEmpty();
            else questNameTranslationsOpen = values != null && !values.isEmpty();
        } finally {
            syncingTranslationRows = false;
        }
    }

    protected List<QuestEditorScreen.TranslationRow> translationRows(boolean description) {
        return description ? questDescriptionTranslationRows : questNameTranslationRows;
    }

    protected void addTranslationRow(boolean description, String locale, String value) {
        // add one locale row and wire its controls to the shared state
        QuestEditorScreen.TranslationRow row = createTranslationRow(description, normalizeLanguageCode(locale));
        translationRows(description).add(row);
        row.box.setValue(value == null ? "" : value);
    }

    protected String normalizeLanguageCode(String locale) {
        String normalized = safe(locale).trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.isBlank() ? "en_us" : normalized;
    }

    protected String nextTranslationLocale(List<QuestEditorScreen.TranslationRow> rows) {
        Set<String> used = new HashSet<>();
        for (QuestEditorScreen.TranslationRow row : rows) used.add(row.locale);
        for (String locale : QUEST_LANGUAGE_CODES) if (!used.contains(locale)) return locale;
        return "en_us";
    }

    protected void ensureTrailingTranslationRow(boolean description) {
        // keep one empty row ready for the next translation
        if (syncingTranslationRows) return;
        List<QuestEditorScreen.TranslationRow> rows = translationRows(description);
        if (rows.isEmpty() || !safe(rows.getLast().box.getValue()).isBlank()) {
            syncingTranslationRows = true;
            try {
                addTranslationRow(description, nextTranslationLocale(rows), "");
            } finally {
                syncingTranslationRows = false;
            }
        }
    }

    protected Map<String, String> translationValues(boolean description) {
        // ignore empty locale rows when saving translations
        Map<String, String> values = new LinkedHashMap<>();
        for (QuestEditorScreen.TranslationRow row : translationRows(description)) {
            String value = safe(row.box.getValue()).trim();
            if (!value.isBlank()) values.put(normalizeLanguageCode(row.locale), value);
        }
        return values.isEmpty() ? Map.of() : Map.copyOf(values);
    }

    protected void removeTranslationRow(QuestEditorScreen.TranslationRow row) {
        if (row == null) return;
        List<QuestEditorScreen.TranslationRow> rows = translationRows(row.description);
        row.removeWidgets();
        rows.remove(row);
        ensureTrailingTranslationRow(row.description);
    }

    protected int translationRowsHeight(boolean description) {
        if (!(description ? questDescriptionTranslationsOpen : questNameTranslationsOpen)) return 0;
        int height = 0;
        for (QuestEditorScreen.TranslationRow row : translationRows(description)) {
            if (height > 0) height += ENTRY_ROW_GAP;
            height += row.height();
        }
        return height + 2;
    }

    protected int layoutTranslationRows(boolean description, int x, int y, int width, int clipTop, int clipBottom) {
        // place translation controls inside the same clipped field column
        boolean open = description ? questDescriptionTranslationsOpen : questNameTranslationsOpen;
        List<QuestEditorScreen.TranslationRow> rows = translationRows(description);
        for (QuestEditorScreen.TranslationRow row : rows) row.setVisible(false);
        if (!open) return 0;
        int cursorY = y + 2;
        for (QuestEditorScreen.TranslationRow row : rows) {
            int height = row.height();
            boolean visible = cursorY + height > clipTop && cursorY < clipBottom;
            int inputWidth = Math.max(10, width - 26);
            int languageSize = Math.min(12, height);
            row.language.setPosition(x, cursorY + Math.max(0, (height - languageSize) / 2));
            row.language.setWidth(languageSize);
            row.language.setHeight(languageSize);
            row.box.setX(x + 14);
            row.box.setY(cursorY);
            row.box.setWidth(inputWidth);
            row.box.setHeight(height);
            row.remove.setPosition(x + width - 10, cursorY);
            row.remove.setWidth(10);
            row.remove.setHeight(ENTRY_ROW_H);
            row.setVisible(visible);
            cursorY += height + ENTRY_ROW_GAP;
        }
        return cursorY - y - ENTRY_ROW_GAP;
    }

    protected ScaledMultiLineEditBox createEntryRowBox(EntryRowKind kind) {
        // use the same multiline control for dependencies tasks and rewards
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

    protected List<ScaledMultiLineEditBox> entryRows(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> dependencyEntryBoxes;
            case COMPLETION -> completionEntryBoxes;
            case REWARD -> rewardEntryBoxes;
        };
    }

    protected List<EntryRemoveButton> entryRemoveButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> dependencyEntryRemoveButtons;
            case COMPLETION -> completionEntryRemoveButtons;
            case REWARD -> rewardEntryRemoveButtons;
        };
    }

    protected List<EntryTypeButton> entryTypeButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryTypeButtons;
            case REWARD -> rewardEntryTypeButtons;
        };
    }

    protected List<EntryItemPickerButton> entryItemPickerButtons(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryItemPickerButtons;
            case REWARD -> rewardEntryItemPickerButtons;
        };
    }

    protected List<EntryCountBox> entryCountBoxes(EntryRowKind kind) {
        return switch (kind) {
            case DEPENDENCY -> List.of();
            case COMPLETION -> completionEntryCountBoxes;
            case REWARD -> rewardEntryCountBoxes;
        };
    }

    protected void resetEntryRows(EntryRowKind kind, List<String> lines) {
        // rebuild row widgets from their stored compact values
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

    protected void normalizeEntryRows(EntryRowKind kind) {
        // remove unused rows while preserving one empty trailing row
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

    protected void ensureTrailingEmptyRow(EntryRowKind kind) {
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

    protected String entryRowDisplayValue(EntryRowKind kind, String type, ParsedEntry parsed, String rawLine) {
        return QuestEditorEntryRows.displayValue(kind, type, parsed, rawLine);
    }

    protected boolean usesCompactEntryBody(EntryRowKind kind, String type) {
        return QuestEditorEntryRows.usesCompactBody(kind, type);
    }

    protected void syncEntryRemoveButtons(EntryRowKind kind) {
        // align remove and type controls with their current row
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

    protected String entryTypeButtonLabel(EntryRowKind kind, int row) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row < 0 || row >= rows.size()) return "";
        return QuestEditorEntryTypes.label(effectiveRowType(kind, rows.get(row)));
    }

    protected String entryPickerButtonType(EntryRowKind kind, int row) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        return row >= 0 && row < rows.size() ? effectiveRowType(kind, rows.get(row)) : "";
    }

    protected void handleEntryCountChanged(EntryRowKind kind, int row, int count) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (row >= 0 && row < rows.size()) {
            entryCountByBox.put(rows.get(row), count);
            entryRowsDirty = true;
            syncEntryBackingValues();
        }
    }

    protected void syncEntryCountBoxes(EntryRowKind kind) {
        // show count controls only for entry types that support counts
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

    protected void syncDependencyEntryLockButtons() {
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

    protected boolean dependencyLockState() {
        if (!dependencyEntryLockButtons.isEmpty()) return dependencyEntryLockButtons.get(0).isOn();
        return questDependencyLockToggle != null && questDependencyLockToggle.isOn();
    }

    protected void setDependencyLockState(boolean state) {
        if (questDependencyLockToggle != null) questDependencyLockToggle.setState(state);
        for (LockToggleButton button : dependencyEntryLockButtons) {
            button.setState(state);
        }
    }

    protected void removeEntryRow(EntryRowKind kind, EntryRemoveButton button) {
        // remove matching row state and keep later controls aligned
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

    protected void setEntryRowsFromRaw(EntryRowKind kind, String raw) {
        resetEntryRows(kind, QuestEditorEntryCodec.extractEntryLines(raw));
        syncEntryBackingValues();
    }

    protected String entryRowsToRaw(EntryRowKind kind) {
        // convert visible rows back into the compact stored format
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        List<String> out = new ArrayList<>();
        for (ScaledMultiLineEditBox box : rows) {
            String v = composeEntryRowLine(kind, box).trim();
            if (!v.isBlank()) out.add(v);
        }
        return String.join("\n", out);
    }

    protected String composeEntryRowLine(EntryRowKind kind, ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.composeLine(kind, box, rowMaps());
    }

    protected ParsedEntry rowParsedBody(EntryRowKind kind, ScaledMultiLineEditBox box, String raw) {
        return QuestEditorEntryRows.parsedBody(kind, box, raw, rowMaps());
    }

    protected boolean rowContainsExplicitType(EntryRowKind kind, String raw) {
        return QuestEditorEntryRows.containsExplicitType(kind, raw);
    }

    protected String bodyWithEntryCount(String body, int count) {
        return QuestEditorEntryRows.bodyWithCount(body, count);
    }

    protected String entryBodyWithoutType(String raw) {
        return QuestEditorEntryRows.bodyWithoutType(raw);
    }

    protected String effectiveRowType(EntryRowKind kind, ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.effectiveType(kind, box, rowMaps());
    }

    protected int entryCount(ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.count(box, rowMaps());
    }

    protected List<String> collectDependencyEntries() {
        List<String> out = new ArrayList<>();
        for (ScaledMultiLineEditBox box : dependencyEntryBoxes) {
            String value = safe(box.getValue()).trim();
            if (!value.isBlank()) out.add(value);
        }
        return out;
    }

    protected int entryRowsHeight(EntryRowKind kind) {
        List<ScaledMultiLineEditBox> rows = entryRows(kind);
        if (rows.isEmpty()) return ENTRY_ROW_H;
        int total = 0;
        for (ScaledMultiLineEditBox row : rows) {
            if (total > 0) total += ENTRY_ROW_GAP;
            total += entryRowHeight(kind, row);
        }
        return Math.max(ENTRY_ROW_H, total);
    }

    protected int entryRowHeight(EntryRowKind kind, ScaledMultiLineEditBox box) {
        if (kind == EntryRowKind.DEPENDENCY || box == null) return ENTRY_ROW_H;
        int content = (int) Math.ceil(Math.max(1, box.getLineCount()) * Math.max(1.0, box.getLineHeight())) + 4;
        int maxHeight = Math.max(ENTRY_ROW_H, ph - (font.lineHeight + FIELD_LABEL_GAP + FIELD_ROW_GAP + 6));
        return Math.max(ENTRY_ROW_H, Math.min(maxHeight, content));
    }

    protected void updateEntryRowFocusDisplays(EntryRowKind kind) {
        // expand focused rows and shorten inactive item displays
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

    protected String displayNameForEntryRow(EntryRowKind kind, ScaledMultiLineEditBox box, String itemId) {
        return QuestEditorEntryRows.displayNameForRow(kind, box, itemId, rowMaps());
    }

    protected String displayNameForEntrySelection(EntryRowKind kind, ScaledMultiLineEditBox box, List<String> ids) {
        return QuestEditorEntryRows.displayNameForSelection(kind, box, ids, rowMaps());
    }

    protected List<String> selectedIdsForRow(ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.selectedIds(box, rowMaps());
    }

    protected boolean shouldExpandFocusedEntryRow(EntryRowKind kind, ScaledMultiLineEditBox box) {
        return QuestEditorEntryRows.shouldExpandFocused(kind, box);
    }

    protected RowMaps rowMaps() {
        return new RowMaps(entryTypeByBox, entryCountByBox, selectedItemIdByBox, selectedItemIdsByBox, selectedItemComponentsByBox);
    }

    protected int layoutEntryRows(EntryRowKind kind, int x, int y, int width, int clipTop, int clipBottom) {
        // lay out row bodies and their compact side controls together
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

    protected void renderDependencyRowIcons(GuiGraphics gg) {
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

    protected void setEntryRowsColor(EntryRowKind kind, boolean invalid) {
        for (ScaledMultiLineEditBox box : entryRows(kind)) {
            box.setTextColor(invalid ? INVALID_INPUT_TEXT_COLOR : DEFAULT_INPUT_TEXT_COLOR);
        }
    }

    protected void syncEntryBackingValues() {
        // keep hidden backing fields in sync before validation and save
        normalizeCommandRewardRows();
        if (questDependenciesBox != null) questDependenciesBox.setValue(entryRowsToRaw(EntryRowKind.DEPENDENCY));
        if (questCompletionBox != null) questCompletionBox.setValue(entryRowsToRaw(EntryRowKind.COMPLETION));
        if (questRewardBox != null) questRewardBox.setValue(entryRowsToRaw(EntryRowKind.REWARD));
        validationSnapshot = null;
        invalidateIdSuggestions();
    }

    protected void normalizeCommandRewardRows() {
        // add missing command metadata without changing entered commands
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

    protected String ensureCommandRewardMetadata(String raw) {
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

    protected boolean hasCommandRewardKey(String payload, String key) {
        if (payload == null || payload.isBlank() || key == null || key.isBlank()) return false;
        String keyPrefix = key.toLowerCase(Locale.ROOT) + ":";
        String[] segments = payload.split("\\|");
        for (String segment : segments) {
            String trimmed = safe(segment).trim().toLowerCase(Locale.ROOT);
            if (trimmed.startsWith(keyPrefix)) return true;
        }
        return false;
    }

    protected FormField field(String label, AbstractWidget widget) {
        FormField field = new FormField(label, widget);
        if (!allFields.contains(field)) allFields.add(field);
        return field;
    }

    protected static Component tr(String key, Object... args) {
        return Component.translatable("ui.boundless.editor." + key, args);
    }

    protected static String trs(String key, Object... args) {
        return tr(key, args).getString();
    }

    protected void setError(String msg) {
        statusMessage = msg;
        statusColor = 0xFF8080;
    }

    protected void applyDescriptionFormat(String formatCode) {
        EditBox titleBox = focusedTitleFormatBox();
        if (titleBox != null) {
            if (!formatCode.startsWith("__")) wrapTitleSelection(titleBox, formatCode);
            return;
        }
        // wrap the current selection and return focus to the description
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

    protected EditBox focusedTitleFormatBox() {
        if (editorType == EditorType.CATEGORY && catNameBox != null && catNameBox.isFocused()) return catNameBox;
        if (editorType == EditorType.SUBCATEGORY && subNameBox != null && subNameBox.isFocused()) return subNameBox;
        return null;
    }

    protected void wrapTitleSelection(EditBox box, String formatCode) {
        String selected = box.getHighlighted();
        int cursor = box.getCursorPosition();
        box.insertText(formatCode + selected + "/x");
        if (selected.isEmpty()) box.setCursorPosition(cursor + formatCode.length());
        box.setHighlightPos(box.getCursorPosition());
        setFocused(box);
        markCurrentEditorUnsaved();
    }

    protected boolean shouldWarnForUnsavedChanges(EditorEntry entry) {
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

    protected void clearPendingDiscardState() {
        pendingDiscardEntryId = "";
        pendingDiscardMode = null;
        pendingDiscardEntry = null;
    }

    protected record QuestCategorySelection(String category, String subCategory) {
    }

    protected void markCurrentEditorLoaded(boolean existingEntry) {
        clearPendingDiscardState();
        savedEditorState = currentEditorStateSignature();
    }

    protected void markCurrentEditorUnsaved() {
        clearPendingDiscardState();
        savedEditorState = null;
    }

    protected void markCurrentEditorSaved() {
        clearPendingDiscardState();
        savedEditorState = currentEditorStateSignature();
    }

    protected boolean hasUnsavedEditorChanges() {
        if (editorType == EditorType.NONE) return false;
        String current = currentEditorStateSignature();
        return savedEditorState == null || !savedEditorState.equals(current);
    }

    protected String currentEditorStateSignature() {
        // compare one stable signature instead of every widget separately
        SignatureFields fields = new SignatureFields();
        fields.packName = safe(packNameBox == null ? "" : packNameBox.getValue());
        fields.packNamespace = safe(packNamespaceBox == null ? "" : packNamespaceBox.getValue());
        fields.packIconPath = safe(packIconPathBox == null ? "" : packIconPathBox.getValue());
        fields.packDescription = safe(packDescriptionBox == null ? "" : packDescriptionBox.getValue());
        fields.categoryId = safe(catIdBox == null ? "" : catIdBox.getValue());
        fields.categoryName = safe(catNameBox == null ? "" : catNameBox.getValue());
        fields.categoryIcon = safe(catIconBox == null ? "" : catIconBox.getValue());
        fields.categoryBackground = encodedCategoryBackground();
        fields.categoryBackgroundTileScale = catBackgroundScaleSlider == null ? 0.5f : catBackgroundScaleSlider.getScale();
        fields.categoryBackgroundDarkening = categoryBackgroundDarkeningEnabled();
        fields.categoryBackgroundDarkeningOpacity = catBackgroundOpacitySlider == null ? 0.45f : catBackgroundOpacitySlider.getOpacity();
        fields.categoryDependency = safe(catDependencyBox == null ? "" : catDependencyBox.getValue());
        fields.categoryAutoComplete = catAutoCompleteToggle != null && catAutoCompleteToggle.isOn();
        fields.categoryTreeLayout = catLayoutSelector != null && catLayoutSelector.isTreeLayout();
        fields.subCategoryId = safe(subIdBox == null ? "" : subIdBox.getValue());
        fields.subCategoryParent = safe(subCategoryBox == null ? "" : subCategoryBox.getValue());
        fields.subCategoryName = safe(subNameBox == null ? "" : subNameBox.getValue());
        fields.subCategoryIcon = safe(subIconBox == null ? "" : subIconBox.getValue());
        fields.subCategoryDefaultOpen = subDefaultOpenToggle != null && subDefaultOpenToggle.isOn();
        fields.questId = safe(questIdBox == null ? "" : questIdBox.getValue());
        fields.questOrderToken = safe(questOrderToken);
        fields.questName = safe(questNameBox == null ? "" : questNameBox.getValue());
        fields.questNameTranslations = translationValues(false).toString();
        fields.questIcon = safe(questIconBox == null ? "" : questIconBox.getValue());
        fields.questDescription = safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue());
        fields.questDescriptionTranslations = translationValues(true).toString();
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

    protected String safe(String v) {
        return v == null ? "" : v;
    }

    protected void attachIdSanitizer(EditBox box, boolean commaSeparated) {
        // normalise ids as they are typed without moving the cursor
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

    protected void attachPackNameSanitizer(EditBox box) {
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

    protected boolean parseBool(String raw, boolean def) {
        if (raw == null) return def;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.isBlank()) return def;
        if (v.equals("true")) return true;
        if (v.equals("false")) return false;
        return def;
    }

    protected float parseFloat(String raw, float fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    protected ItemStack dependencyIconStack(String dependencyId) {
        String id = safe(dependencyId).trim();
        if (id.isBlank()) return ItemStack.EMPTY;
        String iconId = safe(questIconByIdCache.get(id)).trim();
        if (!iconId.isBlank()) {
            ItemStack stack = QuestEditorItemIcons.iconStackFromId(iconId);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    protected String questOrderTokenForSave(String questId) {
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

    protected ScreenState captureState() {
        // capture editor navigation and form values before a data reload
        if (leftList == null) return null;
        syncEntryBackingValues();
        ScreenState state = new ScreenState();
        state.mode = mode;
        state.editorType = editorType;
        state.currentPack = currentPack;
        state.selectedEntryId = selectedEntryId;
        state.editingPath = editingPath;
        state.loadedQuestType = loadedQuestType;
        state.questImportWarnings = questImportWarnings;
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
        state.categorySearchQuery = categorySearchQuery;
        state.subCategorySearchQuery = subCategorySearchQuery;

        state.packName = safe(packNameBox == null ? "" : packNameBox.getValue());
        state.packNamespace = safe(packNamespaceBox == null ? "" : packNamespaceBox.getValue());
        state.packIconPath = safe(packIconPathBox == null ? "" : packIconPathBox.getValue());
        state.packDescription = safe(packDescriptionBox == null ? "" : packDescriptionBox.getValue());

        state.catId = safe(catIdBox == null ? "" : catIdBox.getValue());
        state.catName = safe(catNameBox == null ? "" : catNameBox.getValue());
        state.catIcon = safe(catIconBox == null ? "" : catIconBox.getValue());
        state.catBackground = encodedCategoryBackground();
        state.catBackgroundTileScale = catBackgroundScaleSlider == null ? 0.5f : catBackgroundScaleSlider.getScale();
        state.catBackgroundDarkening = categoryBackgroundDarkeningEnabled();
        state.catBackgroundDarkeningOpacity = catBackgroundOpacitySlider == null ? 0.45f : catBackgroundOpacitySlider.getOpacity();
        state.catDependency = safe(catDependencyBox == null ? "" : catDependencyBox.getValue());
        state.catAutoComplete = catAutoCompleteToggle != null && catAutoCompleteToggle.isOn();
        state.categoryTreeLayout = catLayoutSelector != null && catLayoutSelector.isTreeLayout();

        state.subId = safe(subIdBox == null ? "" : subIdBox.getValue());
        state.subCategory = safe(subCategoryBox == null ? "" : subCategoryBox.getValue());
        state.subName = safe(subNameBox == null ? "" : subNameBox.getValue());
        state.subIcon = safe(subIconBox == null ? "" : subIconBox.getValue());
        state.subDefaultOpen = subDefaultOpenToggle != null && subDefaultOpenToggle.isOn();

        state.questId = safe(questIdBox == null ? "" : questIdBox.getValue());
        state.questName = safe(questNameBox == null ? "" : questNameBox.getValue());
        state.questNameTranslations = translationValues(false);
        state.questIcon = safe(questIconBox == null ? "" : questIconBox.getValue());
        state.questDescription = safe(questDescriptionBox == null ? "" : questDescriptionBox.getValue());
        state.questDescriptionTranslations = translationValues(true);
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

    protected void restoreState(ScreenState state) {
        // restore the editor only after refreshed pack data is available
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
        categorySearchQuery = state.categorySearchQuery == null ? "" : state.categorySearchQuery;
        subCategorySearchQuery = state.subCategorySearchQuery == null ? "" : state.subCategorySearchQuery;
        syncListSearchBox();
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
                data.background = state.catBackground;
                data.backgroundTileScale = Float.toString(state.catBackgroundTileScale);
                data.backgroundDarkening = state.catBackgroundDarkening ? "true" : "false";
                data.backgroundDarkeningOpacity = Float.toString(state.catBackgroundDarkeningOpacity);
                data.dependency = state.catDependency;
                data.autoComplete = state.catAutoComplete ? "true" : "false";
                data.layout = state.categoryTreeLayout ? "tree" : "list";
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
                data.nameTranslations = state.questNameTranslations;
                data.icon = state.questIcon;
                data.description = state.questDescription;
                data.descriptionTranslations = state.questDescriptionTranslations;
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
                data.importWarnings = state.questImportWarnings == null ? List.of() : state.questImportWarnings;
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

    protected String optString(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) return def;
        return obj.get(key).getAsString();
    }

    protected String optStringFlexible(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key)) return def;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return def;
        if (el.isJsonPrimitive()) return el.getAsString();
        return def;
    }

    protected static void stashPendingInitState(ScreenState state) {
        // hold reload state briefly while the replacement screen is created
        pendingInitState = state;
        pendingInitUntil = Util.getMillis() + PENDING_INIT_TTL_MS;
    }

    protected static ScreenState takePendingInitState() {
        // discard reload state when it has waited too long to be restored
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

    protected static void clearPendingInitState() {
        pendingInitState = null;
        pendingInitUntil = 0L;
    }

    // screen callbacks keep concrete gui components out of the controller
    protected abstract QuestEditorScreen.CategoryWorkspaceTab createCategoryWorkspaceTab(NamedEntry category, int y);

    protected abstract QuestEditorScreen.LanguageToggleButton createLanguageToggleButton(boolean description);

    protected abstract QuestEditorScreen.TranslationRow createTranslationRow(boolean description, String locale);

    protected abstract void applyDropdownTextColor(EditBox box, boolean invalid);

    protected abstract void updateLeftPaneLayout();

    protected abstract void updateBackButtonVisibility();

    protected abstract void refreshPackIdCaches(QuestListIndex questIndex);

    protected abstract String dropdownBoxValue(EditBox box);

    protected abstract void setDropdownBoxValue(EditBox box, String value);

    protected static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt((raw == null ? "" : raw).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    protected abstract void openIconItemPicker(EditBox target);

    protected abstract boolean isDuplicateQuestId(String questIdRaw);

    protected abstract void goBack();

    protected abstract boolean isItemPickerOpen();

    protected abstract void closeItemPicker();

    protected abstract void openTypeMenu(EntryRowKind kind, int row, int x, int y, int width);

    protected abstract void openItemPicker(EntryRowKind kind, int row);

    protected abstract ItemStack selectedItemForRow(EntryRowKind kind, int row);

    protected abstract void invalidateIdSuggestions();

    protected abstract void queueEditorTooltip(Component tooltip, int mouseX, int mouseY);

    protected abstract void openDropdownMenu(DropdownMenuTarget target, AbstractWidget box);

    protected abstract void selectQuestMediaFile();
}
