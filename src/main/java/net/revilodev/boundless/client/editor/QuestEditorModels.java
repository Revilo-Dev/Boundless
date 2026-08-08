package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// shared editor data models
public final class QuestEditorModels {
    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");

    private QuestEditorModels() {
    }

    // editor entry
    public static final class EditorEntry {
        public final String id;
        public final String label;
        public final String subtitle;
        public final String icon;
        public final ResourceLocation actionIcon;
        public final ResourceLocation rowTexture;
        public final String actionTooltip;
        public final String rowTooltip;
        public final EditorEntryKind kind;
        public final int indent;
        public final boolean showMoveArrows;

        public EditorEntry(String id, String label, String subtitle, String icon) {
            this(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.NORMAL, 0);
        }

        public EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, "", ROW_TEX, EditorEntryKind.NORMAL, 0);
        }

        public EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, rowTooltip, ROW_TEX, EditorEntryKind.NORMAL, 0);
        }

        public EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip, ResourceLocation rowTexture) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, rowTooltip, rowTexture, EditorEntryKind.NORMAL, 0);
        }

        public EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip, ResourceLocation rowTexture, EditorEntryKind kind, int indent) {
            this(id, label, subtitle, icon, actionIcon, actionTooltip, rowTooltip, rowTexture, kind, indent, false);
        }

        public EditorEntry(String id, String label, String subtitle, String icon, ResourceLocation actionIcon, String actionTooltip, String rowTooltip, ResourceLocation rowTexture, EditorEntryKind kind, int indent, boolean showMoveArrows) {
            this.id = id;
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon == null ? "" : icon;
            this.actionIcon = actionIcon;
            this.rowTexture = rowTexture == null ? ROW_TEX : rowTexture;
            this.actionTooltip = actionTooltip == null ? "" : actionTooltip;
            this.rowTooltip = rowTooltip == null ? "" : rowTooltip;
            this.kind = kind == null ? EditorEntryKind.NORMAL : kind;
            this.indent = Math.max(0, indent);
            this.showMoveArrows = showMoveArrows;
        }

        // movable
        public static EditorEntry movable(String id, String label, String subtitle, String icon) {
            return new EditorEntry(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.NORMAL, 0, true);
        }

        // normal
        public static EditorEntry normal(String id, String label, String subtitle, String icon) {
            return new EditorEntry(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.NORMAL, 0, false);
        }

        // quest
        public static EditorEntry quest(String id, String label, String subtitle, String icon) {
            return new EditorEntry(id, label, subtitle, icon, null, "", "", ROW_TEX, EditorEntryKind.QUEST, 0, true);
        }

        // quest
        public static EditorEntry quest(String id, String label, String subtitle, String icon, ResourceLocation rowTexture, String rowTooltip) {
            return new EditorEntry(id, label, subtitle, icon, null, "", rowTooltip, rowTexture, EditorEntryKind.QUEST, 0, true);
        }

        // category header
        public static EditorEntry categoryHeader(String id, String label, boolean collapsed) {
            return new EditorEntry(id, (collapsed ? "+ " : "- ") + label, "", "", null, "", "", ROW_TEX, EditorEntryKind.CATEGORY_HEADER, 0);
        }

        // sub category header
        public static EditorEntry subCategoryHeader(String id, String label, boolean collapsed) {
            return new EditorEntry(id, (collapsed ? "+ " : "- ") + label, "", "", null, "", "", ROW_TEX, EditorEntryKind.SUBCATEGORY_HEADER, 10);
        }
    }

    // editor entry kind
    public enum EditorEntryKind {
        NORMAL,
        QUEST,
        CATEGORY_HEADER,
        SUBCATEGORY_HEADER
    }

    // move direction
    public enum MoveDirection {
        UP,
        DOWN
    }

    // named entry
    public static final class NamedEntry {
        public final String id;
        public final String name;
        public final Path path;
        public final String icon;
        public final String sortKey;

        public NamedEntry(String id, String name, String icon, Path path, String sortKey) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.icon = icon == null ? "" : icon;
            this.sortKey = sortKey == null ? "" : sortKey;
        }
    }

    // quest list item
    public static final class QuestListItem {
        public final NamedEntry entry;
        public final QuestEntryData data;

        public QuestListItem(NamedEntry entry, QuestEntryData data) {
            this.entry = entry;
            this.data = data;
        }
    }

    // quest list index
    public static final class QuestListIndex {
        public static final QuestListIndex EMPTY = new QuestListIndex();

        public final Map<String, String> categoryNames = new HashMap<>();
        public final Map<String, String> subCategoryNames = new HashMap<>();
        public final Set<String> categoryIds = new HashSet<>();
        public final Set<String> subCategoryIds = new HashSet<>();
        public final Set<String> questIds = new HashSet<>();
        public final Set<String> duplicateQuestIds = new HashSet<>();
        public final Map<String, QuestEntryData> questDataByEntryId = new HashMap<>();
        public final List<QuestListItem> items = new ArrayList<>();
    }

    // quest move entry
    public static final class QuestMoveEntry {
        public final String id;
        public final Path path;
        public final String category;
        public final String subCategory;

        public QuestMoveEntry(String id, Path path, String category, String subCategory) {
            this.id = id == null ? "" : id;
            this.path = path;
            this.category = category == null ? "" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
        }
    }

    // quest pack
    public static final class QuestPack {
        public final String name;
        public final String namespace;
        public final Path root;
        public final boolean legacy;
        public final boolean enabled;
        public final Path dataDir;
        public final Path questsDir;
        public final Path categoriesDir;
        public final Path subCategoriesDir;

        public QuestPack(String name, String namespace, Path root) {
            this(name, namespace, root, false, true);
        }

        public QuestPack(String name, String namespace, Path root, boolean legacy) {
            this(name, namespace, root, legacy, true);
        }

        public QuestPack(String name, String namespace, Path root, boolean legacy, boolean enabled) {
            this.name = name;
            this.namespace = namespace == null ? "" : namespace;
            this.root = root;
            this.legacy = legacy;
            this.enabled = enabled;
            this.dataDir = root.resolve("data").resolve(this.namespace);
            this.questsDir = dataDir.resolve("quests");
            this.categoriesDir = questsDir.resolve("categories");
            this.subCategoriesDir = questsDir.resolve("sub-category");
        }

        public void ensureDirs() throws IOException {
            Files.createDirectories(root);
            Files.createDirectories(questsDir);
            Files.createDirectories(categoriesDir);
            Files.createDirectories(subCategoriesDir);
        }
    }

    // pack meta
    public static final class PackMeta {
        public String description = "";
        public String iconPath = "";
        public boolean enabled = true;
    }

    // category data
    public static final class CategoryData {
        public Path path;
        public String id = "";
        public String name = "";
        public String icon = "";
        public String order = "";
        public String dependency = "";
        public String autoComplete = "";
    }

    // sub category data
    public static final class SubCategoryData {
        public Path path;
        public String id = "";
        public String category = "";
        public String name = "";
        public String icon = "";
        public String order = "";
        public String defaultOpen = "";
    }

    // quest entry data
    public static final class QuestEntryData {
        public Path path;
        public String id = "";
        public String name = "";
        public String icon = "";
        public String description = "";
        public String category = "";
        public String subCategory = "";
        public String dependencies = "";
        public String lockAfterDependency = "";
        public String optional = "";
        public String repeatable = "";
        public String autoComplete = "";
        public String hiddenUnderDependency = "";
        public String type = "";
        public String completionJson = "";
        public String rewardJson = "";
    }

    // index name
    public static final class IndexName {
        public final String index;
        public final String name;

        public IndexName(String index, String name) {
            this.index = index == null ? "" : index;
            this.name = name == null ? "" : name;
        }
    }

    // parsed entry
    public static final class ParsedEntry {
        public final String type;
        public final String id;
        public final List<String> acceptedIds;
        public final int count;
        public final String hint;

        public ParsedEntry(String type, String id, int count) {
            this(type, id, count, "", List.of(id));
        }

        public ParsedEntry(String type, String id, int count, String hint) {
            this(type, id, count, hint, List.of(id));
        }

        public ParsedEntry(String type, String id, int count, String hint, List<String> acceptedIds) {
            this.type = type == null ? "" : type;
            this.id = id == null ? "" : id;
            List<String> normalized = new ArrayList<>();
            if (acceptedIds != null) {
                for (String acceptedId : acceptedIds) {
                    String value = acceptedId == null ? "" : acceptedId.trim();
                    if (!value.isBlank() && !normalized.contains(value)) normalized.add(value);
                }
            }
            if (normalized.isEmpty() && !this.id.isBlank()) normalized.add(this.id);
            this.acceptedIds = List.copyOf(normalized);
            this.count = count;
            this.hint = hint == null ? "" : hint;
        }
    }

    // toast editor reward
    public static final class ToastEditorReward {
        public final String title;
        public final String description;
        public final String icon;

        public ToastEditorReward(String title, String description, String icon) {
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
            this.icon = icon == null ? "" : icon;
        }
    }

    // command reward
    public static final class CommandReward {
        public final String command;
        public final String icon;
        public final String title;

        public CommandReward(String command, String icon, String title) {
            this.command = command == null ? "" : command;
            this.icon = icon == null ? "" : icon;
            this.title = title == null ? "" : title;
        }
    }

    // tag page entry
    public record TagPageEntry(String tagId, ItemStack icon, Component tooltip) {}

    // tag page data
    public record TagPageData(int totalCount, List<TagPageEntry> entries) {}

    // picker page data
    public record PickerPageData(int totalCount, List<ItemStack> items, List<String> ids) {}

    // multi line entry context
    public static final class MultiLineEntryContext {
        public final boolean hasTypeSeparator;
        public final String type;
        public final String typePrefix;
        public final int typeStart;
        public final int typeEnd;
        public final int idStart;
        public final int idEnd;
        public final String idPrefix;

        public MultiLineEntryContext(boolean hasTypeSeparator, String type, String typePrefix,
                              int typeStart, int typeEnd, int idStart, int idEnd, String idPrefix) {
            this.hasTypeSeparator = hasTypeSeparator;
            this.type = type == null ? "" : type;
            this.typePrefix = typePrefix == null ? "" : typePrefix;
            this.typeStart = typeStart;
            this.typeEnd = typeEnd;
            this.idStart = idStart;
            this.idEnd = idEnd;
            this.idPrefix = idPrefix == null ? "" : idPrefix;
        }
    }

    // suggestion bounds
    public static final class SuggestionBounds {
        public final int x;
        public final int y;
        public final int w;
        public final int h;

        public SuggestionBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    // form field
    public static final class FormField {
        public final String displayLabel;
        public final String tooltip;
        public final AbstractWidget widget;

        public FormField(String label, AbstractWidget widget) {
            String raw = label == null ? "" : label;
            String display = raw;
            String tip = "";
            int start = raw.indexOf('(');
            int end = raw.lastIndexOf(')');
            if (start >= 0 && end > start) {
                display = raw.substring(0, start).trim();
                tip = raw.substring(start + 1, end).trim();
            }
            this.displayLabel = display.isBlank() ? raw : display;
            this.tooltip = tip == null ? "" : tip;
            this.widget = widget;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FormField other)) return false;
            return widget == other.widget;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(widget);
        }
    }

    // screen state
    public static final class ScreenState {

        public Mode mode = Mode.PACK_LIST;

        public EditorType editorType = EditorType.NONE;

        public QuestPack currentPack;

        public String selectedEntryId = "";

        public Path editingPath;

        public String loadedQuestType = "";

        public String questOrderToken = "";

        public float editorScroll = 0f;

        public float leftScroll = 0f;

        public String statusMessage = "";

        public int statusColor = 0xA0A0A0;

        public boolean deleteConfirmArmed = false;

        public String savedEditorState = null;

        public String pendingDiscardEntryId = "";

        public Mode pendingDiscardMode = null;

        public String questSearchQuery = "";

        public String packName = "";

        public String packNamespace = "";

        public String packIconPath = "";

        public String packDescription = "";

        public String catId = "";

        public String catName = "";

        public String catIcon = "";

        public String catDependency = "";

        public boolean catAutoComplete = false;

        public String subId = "";

        public String subCategory = "";

        public String subName = "";

        public String subIcon = "";

        public boolean subDefaultOpen = false;

        public String questId = "";

        public String questName = "";

        public String questIcon = "";

        public String questDescription = "";

        public String questCategory = "";

        public String questSubCategory = "";

        public String questDependencies = "";

        public boolean questLockAfterDependency = false;

        public boolean questOptional = false;

        public boolean questRepeatable = false;

        public boolean questAutoComplete = false;

        public boolean questHiddenUnderDependency = false;

        public String questCompletion = "";

        public String questReward = "";

    }

    // entry row kind
    public enum EntryRowKind {

        DEPENDENCY,

        COMPLETION,

        REWARD

    }

    // dropdown menu target
    public enum DropdownMenuTarget {

        SUBCATEGORY_PARENT,

        QUEST_CATEGORY,

        QUEST_SUBCATEGORY

    }

    // item picker tab
    public enum ItemPickerTab {

        CREATIVE,

        TAGS,

        INVENTORY

    }

    // picker mode
    public enum PickerMode {

        NONE,

        ITEMS,

        EFFECTS,

        MOBS

    }

    // mode
    public enum Mode {

        PACK_LIST,

        PACK_CREATE,

        PACK_MENU,

        CATEGORY_LIST,

        SUBCATEGORY_LIST,

        QUEST_LIST

    }

    // editor type
    public enum EditorType {

        NONE,

        PACK_CREATE,

        PACK_OPTIONS,

        CATEGORY,

        SUBCATEGORY,

        QUEST

    }

    // validation snapshot
    public static final class ValidationSnapshot {

        public final EditorType editorType;

        public final String packName;

        public final String editingPath;

        public final String questId;

        public final String questName;

        public final String categoryDependency;

        public final String parentCategory;

        public final String questCategory;

        public final String questSubCategory;

        public final String dependencyRaw;

        public final String completionRaw;

        public final String rewardRaw;

        public final boolean invalidQuestId;

        public final boolean invalidQuestName;

        public final boolean invalidCategoryDependency;

        public final boolean invalidParentCategory;

        public final boolean invalidQuestCategory;

        public final boolean invalidQuestSubCategory;

        public final boolean invalidQuestDependencies;

        public final boolean invalidCompletionEntries;

        public final boolean invalidRewardEntries;

        public ValidationSnapshot(EditorType editorType, String packName, String editingPath, String questId, String questName,

                                   String categoryDependency, String parentCategory, String questCategory, String questSubCategory,

                                   String dependencyRaw, String completionRaw, String rewardRaw, boolean invalidQuestId,

                                   boolean invalidQuestName, boolean invalidCategoryDependency, boolean invalidParentCategory,

                                   boolean invalidQuestCategory, boolean invalidQuestSubCategory, boolean invalidQuestDependencies,

                                   boolean invalidCompletionEntries, boolean invalidRewardEntries) {

            this.editorType = editorType;

            this.packName = packName;

            this.editingPath = editingPath;

            this.questId = questId;

            this.questName = questName;

            this.categoryDependency = categoryDependency;

            this.parentCategory = parentCategory;

            this.questCategory = questCategory;

            this.questSubCategory = questSubCategory;

            this.dependencyRaw = dependencyRaw;

            this.completionRaw = completionRaw;

            this.rewardRaw = rewardRaw;

            this.invalidQuestId = invalidQuestId;

            this.invalidQuestName = invalidQuestName;

            this.invalidCategoryDependency = invalidCategoryDependency;

            this.invalidParentCategory = invalidParentCategory;

            this.invalidQuestCategory = invalidQuestCategory;

            this.invalidQuestSubCategory = invalidQuestSubCategory;

            this.invalidQuestDependencies = invalidQuestDependencies;

            this.invalidCompletionEntries = invalidCompletionEntries;

            this.invalidRewardEntries = invalidRewardEntries;

        }

        public boolean matches(EditorType editorType, String packName, String editingPath, String questId, String questName,

                                String categoryDependency, String parentCategory, String questCategory, String questSubCategory,

                                String dependencyRaw, String completionRaw, String rewardRaw) {

            return this.editorType == editorType

                    && Objects.equals(this.packName, packName)

                    && Objects.equals(this.editingPath, editingPath)

                    && Objects.equals(this.questId, questId)

                    && Objects.equals(this.questName, questName)

                    && Objects.equals(this.categoryDependency, categoryDependency)

                    && Objects.equals(this.parentCategory, parentCategory)

                    && Objects.equals(this.questCategory, questCategory)

                    && Objects.equals(this.questSubCategory, questSubCategory)

                    && Objects.equals(this.dependencyRaw, dependencyRaw)

                    && Objects.equals(this.completionRaw, completionRaw)

                    && Objects.equals(this.rewardRaw, rewardRaw);

        }

    }

}
