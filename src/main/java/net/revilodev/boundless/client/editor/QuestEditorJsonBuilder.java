package net.revilodev.boundless.client.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.function.Consumer;

// editor json builders
public final class QuestEditorJsonBuilder {
    private QuestEditorJsonBuilder() {
    }

    // build category
    public static JsonObject buildCategory(String id, String name, String icon, int order, String dependency, boolean autoComplete, Consumer<String> onError) {
        String categoryId = safe(id).trim();
        if (categoryId.isBlank()) {
            setError(onError, "Category id required");
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("id", categoryId);
        addOptional(obj, "name", name);
        addOptional(obj, "icon", icon);
        obj.addProperty("order", order);
        addOptional(obj, "dependency", dependency);
        obj.addProperty("auto_complete", autoComplete);
        return obj;
    }

    // build sub category
    public static JsonObject buildSubCategory(String id, String category, String name, String icon, int order, boolean defaultOpen, Consumer<String> onError) {
        String subId = safe(id).trim();
        if (subId.isBlank()) {
            setError(onError, "Sub-category id required");
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("id", subId);
        addOptional(obj, "category", category);
        addOptional(obj, "name", name);
        addOptional(obj, "icon", icon);
        obj.addProperty("order", order);
        obj.addProperty("default_open", defaultOpen);
        return obj;
    }

    // build quest
    public static JsonObject buildQuest(QuestBuildFields fields, Consumer<String> onError) {
        if (fields == null) return null;
        String questId = safe(fields.id).trim();
        if (questId.isBlank()) {
            setError(onError, "Quest id required");
            return null;
        }
        if (fields.duplicateQuestId) {
            setError(onError, "Quest id already exists");
            return null;
        }
        String nameRaw = safe(fields.name).trim();
        if (nameRaw.isBlank()) {
            setError(onError, "Quest name required");
            return null;
        }
        String categoryRaw = safe(fields.category).trim();
        if (categoryRaw.isBlank()) {
            setError(onError, "Quest category required");
            return null;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("id", questId);
        addOptional(obj, "name", nameRaw);
        addOptional(obj, "icon", fields.icon);
        addOptional(obj, "description", fields.description);
        addOptional(obj, "category", categoryRaw);
        addOptional(obj, "sub-category", fields.subCategory);
        obj.addProperty("optional", fields.optional);
        obj.addProperty("repeatable", fields.repeatable);
        obj.addProperty("auto_complete", fields.autoComplete);
        obj.addProperty("hiddenUnderDependency", fields.hiddenUnderDependency);
        obj.addProperty("lock_after_dependency", fields.lockAfterDependency);
        if (!safe(fields.loadedQuestType).isBlank()) {
            obj.addProperty("type", fields.loadedQuestType);
        }

        if (fields.dependencies != null && fields.dependencies.size() == 1) {
            obj.addProperty("dependencies", fields.dependencies.get(0));
        } else if (fields.dependencies != null && !fields.dependencies.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (String dependency : fields.dependencies) arr.add(dependency);
            obj.add("dependencies", arr);
        }

        String completionRaw = safe(fields.completionRaw).trim();
        if (!completionRaw.isBlank()) {
            JsonObject completion = QuestEditorEntryCodec.parseCompletionEntries(completionRaw, true, onError);
            if (completion == null) return null;
            obj.add("completion", completion);
        }

        String rewardRaw = safe(fields.rewardRaw).trim();
        if (!rewardRaw.isBlank()) {
            JsonObject reward = QuestEditorEntryCodec.parseRewardEntries(rewardRaw, true, onError);
            if (reward == null) return null;
            obj.add("reward", reward);
        }

        return obj;
    }

    private static void addOptional(JsonObject obj, String key, String value) {
        String v = safe(value).trim();
        if (!v.isBlank()) obj.addProperty(key, v);
    }

    private static void setError(Consumer<String> onError, String message) {
        if (onError != null) onError.accept(message);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // quest build fields
    public static final class QuestBuildFields {
        public String id = "";
        public boolean duplicateQuestId;
        public String name = "";
        public String icon = "";
        public String description = "";
        public String category = "";
        public String subCategory = "";
        public boolean optional;
        public boolean repeatable;
        public boolean autoComplete;
        public boolean hiddenUnderDependency;
        public boolean lockAfterDependency;
        public String loadedQuestType = "";
        public List<String> dependencies = List.of();
        public String completionRaw = "";
        public String rewardRaw = "";
    }
}
