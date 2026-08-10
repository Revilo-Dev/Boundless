package net.revilodev.boundless.client.editor;

import net.revilodev.boundless.client.editor.QuestEditorModels.EditorType;
import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.ValidationSnapshot;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

// editor validation checks
public final class QuestEditorValidation {
    private QuestEditorValidation() {
    }

    // current snapshot
    public static ValidationSnapshot currentSnapshot(ValidationRequest request) {
        if (request == null) return null;
        if (request.previousSnapshot != null
                && request.previousSnapshot.matches(
                request.editorType,
                request.packName,
                request.editingPathString,
                request.questId,
                request.questName,
                request.categoryDependency,
                request.parentCategory,
                request.questCategory,
                request.questSubCategory,
                request.dependencyRaw,
                request.completionRaw,
                request.rewardRaw)) {
            return request.previousSnapshot;
        }

        return new ValidationSnapshot(
                request.editorType,
                request.packName,
                request.editingPathString,
                request.questId,
                request.questName,
                request.categoryDependency,
                request.parentCategory,
                request.questCategory,
                request.questSubCategory,
                request.dependencyRaw,
                request.completionRaw,
                request.rewardRaw,
                computeInvalidQuestId(request.editorType, request.questId, request.hasCurrentPack, request.questEntries, request.editingPath),
                computeInvalidQuestName(request.editorType, request.questName),
                isMissingQuestId(request.categoryDependency, request.hasCurrentPack, request.questIdCache),
                isMissingCategoryId(request.parentCategory, request.hasCurrentPack, request.categoryIdCache),
                request.questCategory.isBlank() || isMissingCategoryId(request.questCategory, request.hasCurrentPack, request.categoryIdCache),
                isMissingSubCategoryId(request.questSubCategory, request.parentQuestCategory, request.hasCurrentPack, request.subCategoryIdCache),
                computeInvalidQuestDependencies(request.dependencyRaw, request.hasCurrentPack, request.questPackDependencySuggestionCache),
                !request.completionRaw.isBlank() && QuestEditorEntryCodec.parseCompletionEntries(request.completionRaw, false, request.errorHandler) == null,
                !request.rewardRaw.isBlank() && QuestEditorEntryCodec.parseRewardEntries(request.rewardRaw, false, request.errorHandler) == null
        );
    }

    // determine invalid quest id
    public static boolean computeInvalidQuestId(EditorType editorType, String questId, boolean hasCurrentPack, Iterable<NamedEntry> questEntries, Path editingPath) {
        return editorType == EditorType.QUEST && (safe(questId).trim().isBlank() || isDuplicateQuestId(questId, hasCurrentPack, questEntries, editingPath));
    }

    // determine invalid quest name
    public static boolean computeInvalidQuestName(EditorType editorType, String questName) {
        return editorType == EditorType.QUEST && safe(questName).trim().isBlank();
    }

    // determine invalid quest dependencies
    public static boolean computeInvalidQuestDependencies(String dependencyRaw, boolean hasCurrentPack, Collection<String> dependencyCache) {
        if (!hasCurrentPack || safe(dependencyRaw).trim().isBlank()) return false;
        for (String dependency : QuestEditorEntryCodec.extractEntryLines(dependencyRaw)) {
            if (!dependency.isBlank() && !dependencyCache.contains(dependency)) return true;
        }
        return false;
    }

    // is missing category id
    public static boolean isMissingCategoryId(String raw, boolean hasCurrentPack, Set<String> categoryIdCache) {
        if (!hasCurrentPack) return false;
        String id = safe(raw).trim();
        if (id.isBlank()) return false;
        return !categoryIdCache.contains(id);
    }

    // is missing quest id
    public static boolean isMissingQuestId(String raw, boolean hasCurrentPack, Set<String> questIdCache) {
        if (!hasCurrentPack) return false;
        String id = safe(raw).trim();
        if (id.isBlank()) return false;
        return !questIdCache.contains(id);
    }

    // is missing sub category id
    public static boolean isMissingSubCategoryId(String raw, String category, boolean hasCurrentPack, Set<String> subCategoryIdCache) {
        if (!hasCurrentPack) return false;
        String id = safe(raw).trim();
        if (id.isBlank()) return false;
        String categoryId = safe(category).trim();
        if (!categoryId.isBlank()) {
            boolean hasExact = subCategoryIdCache.contains(categoryId + "::" + id);
            boolean hasWildcard = subCategoryIdCache.contains("::" + id);
            if (hasExact || hasWildcard) return false;
        }
        return !subCategoryIdCache.contains(id);
    }

    // is duplicate quest id
    public static boolean isDuplicateQuestId(String questIdRaw, boolean hasCurrentPack, Iterable<NamedEntry> questEntries, Path editingPath) {
        if (!hasCurrentPack) return false;
        String questId = safe(questIdRaw).trim();
        if (questId.isBlank()) return false;
        int matches = 0;
        boolean matchedEditingPath = false;
        for (NamedEntry entry : questEntries) {
            if (entry == null || entry.id == null || !questId.equals(entry.id)) continue;
            matches++;
            if (editingPath != null && entry.path != null && editingPath.equals(entry.path)) {
                matchedEditingPath = true;
            }
        }
        if (editingPath != null && matchedEditingPath) return matches > 1;
        return matches > 0;
    }

    // is duplicate quest id across pack
    public static boolean isDuplicateQuestIdAcrossPack(String questIdRaw, boolean hasCurrentPack, Iterable<NamedEntry> questEntries) {
        if (!hasCurrentPack) return false;
        String questId = safe(questIdRaw).trim();
        if (questId.isBlank()) return false;
        int matches = 0;
        for (NamedEntry entry : questEntries) {
            if (entry == null || entry.id == null || !questId.equals(entry.id)) continue;
            matches++;
            if (matches > 1) return true;
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // validation request
    public static final class ValidationRequest {
        public ValidationSnapshot previousSnapshot;
        public EditorType editorType;
        public boolean hasCurrentPack;
        public String packName = "";
        public String editingPathString = "";
        public Path editingPath;
        public String questId = "";
        public String questName = "";
        public String categoryDependency = "";
        public String parentCategory = "";
        public String questCategory = "";
        public String questSubCategory = "";
        public String parentQuestCategory = "";
        public String dependencyRaw = "";
        public String completionRaw = "";
        public String rewardRaw = "";
        public Set<String> categoryIdCache = Set.of();
        public Set<String> subCategoryIdCache = Set.of();
        public Set<String> questIdCache = Set.of();
        public Collection<String> questPackDependencySuggestionCache = Set.of();
        public Iterable<NamedEntry> questEntries = Set.of();
        public Consumer<String> errorHandler;
    }
}
