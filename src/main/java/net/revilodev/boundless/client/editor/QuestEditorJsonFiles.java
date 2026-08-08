package net.revilodev.boundless.client.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.revilodev.boundless.client.editor.QuestEditorModels.CategoryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;
import net.revilodev.boundless.client.editor.QuestEditorModels.SubCategoryData;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// quest json file loading
public final class QuestEditorJsonFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuestEditorJsonFiles() {
    }

    // list category entries
    public static List<NamedEntry> listCategoryEntries(QuestPack pack) {
        List<NamedEntry> entries = listEntries(pack.categoriesDir);
        entries.sort(Comparator.comparing(a -> safe(a.sortKey).toLowerCase(Locale.ROOT)));
        return entries;
    }

    // list sub category entries
    public static List<NamedEntry> listSubCategoryEntries(QuestPack pack) {
        List<NamedEntry> entries = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();
        for (Path dir : subCategoryDirectories(pack)) {
            for (NamedEntry entry : listEntries(dir)) {
                if (entry == null || entry.path == null) continue;
                String pathKey = entry.path.toString();
                if (seenPaths.add(pathKey)) entries.add(entry);
            }
        }
        entries.sort(Comparator.comparing(a -> safe(a.sortKey).toLowerCase(Locale.ROOT)));
        return entries;
    }

    // sub category directories
    public static List<Path> subCategoryDirectories(QuestPack pack) {
        if (pack == null || pack.questsDir == null) return List.of();
        List<Path> dirs = new ArrayList<>();
        dirs.add(pack.questsDir.resolve("sub-category"));
        dirs.add(pack.questsDir.resolve("subcategories"));
        dirs.add(pack.questsDir.resolve("sub_category"));
        dirs.add(pack.questsDir.resolve("subcategory"));
        return dirs;
    }

    // list quest entries
    public static List<NamedEntry> listQuestEntries(QuestPack pack) {
        return listEntries(pack.questsDir);
    }

    // list entries
    public static List<NamedEntry> listEntries(Path dir) {
        List<NamedEntry> entries = new ArrayList<>();
        if (dir == null || !Files.isDirectory(dir)) return entries;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path path : stream) {
                JsonObject obj = readJson(path);
                String id = obj == null ? fileId(path) : optString(obj, "id", fileId(path));
                String name = obj == null ? id : optString(obj, "name", id);
                String icon = obj == null ? "" : optString(obj, "icon", "");
                int order = QuestEditorEntryCodec.parseIntFlexible(obj, "order", 0);
                String sortKey = String.format(Locale.ROOT, "%06d_%s", order, fileId(path));
                entries.add(new NamedEntry(id, name, icon, path, sortKey));
            }
        } catch (IOException ignored) {
        }
        return entries;
    }

    // load category
    public static CategoryData loadCategory(QuestPack pack, String id) {
        Path path = pack.categoriesDir.resolve(id + ".json");
        if (!Files.exists(path)) {
            for (NamedEntry entry : listCategoryEntries(pack)) {
                if (entry.id.equals(id)) {
                    path = entry.path;
                    break;
                }
            }
        }
        JsonObject obj = readJson(path);
        if (obj == null) return null;

        CategoryData data = new CategoryData();
        data.path = path;
        data.id = optString(obj, "id", id);
        data.name = optString(obj, "name", "");
        data.icon = optString(obj, "icon", "");
        data.order = optStringFlexible(obj, "order", "");
        data.dependency = optString(obj, "dependency", "");
        data.autoComplete = optStringFlexible(obj, "auto_complete",
                optStringFlexible(obj, "autoComplete", ""));
        return data;
    }

    // load sub category
    public static SubCategoryData loadSubCategory(QuestPack pack, String id) {
        Path path = pack.subCategoriesDir.resolve(id + ".json");
        if (!Files.exists(path)) {
            for (NamedEntry entry : listSubCategoryEntries(pack)) {
                if (entry.id.equals(id)) {
                    path = entry.path;
                    break;
                }
            }
        }
        return loadSubCategory(path, id);
    }

    // load sub category
    public static SubCategoryData loadSubCategory(Path path, String id) {
        JsonObject obj = readJson(path);
        if (obj == null) return null;

        SubCategoryData data = new SubCategoryData();
        data.path = path;
        data.id = optString(obj, "id", id);
        data.category = optString(obj, "category", "");
        data.name = optString(obj, "name", "");
        data.icon = optString(obj, "icon", "");
        data.order = optStringFlexible(obj, "order", "");
        data.defaultOpen = optStringFlexible(obj, "default_open", "");
        return data;
    }

    // load quest
    public static QuestEntryData loadQuest(QuestPack pack, String id) {
        Path path = pack.questsDir.resolve(id + ".json");
        if (!Files.exists(path)) {
            for (NamedEntry entry : listQuestEntries(pack)) {
                if (entry.id.equals(id)) {
                    path = entry.path;
                    break;
                }
            }
        }
        return loadQuest(path, id);
    }

    // load quest
    public static QuestEntryData loadQuest(Path path, String id) {
        JsonObject obj = readJson(path);
        return loadQuest(path, id, obj);
    }

    // load quest
    public static QuestEntryData loadQuest(Path path, String id, JsonObject obj) {
        if (obj == null) return null;

        QuestEntryData data = new QuestEntryData();
        data.path = path;
        data.id = optString(obj, "id", id);
        data.name = optString(obj, "name", "");
        data.icon = optString(obj, "icon", "");
        data.description = optString(obj, "description", "");
        data.category = optString(obj, "category", "");
        data.subCategory = optString(obj, "sub-category", optString(obj, "subCategory", ""));
        data.dependencies = formatDependenciesLines(obj.get("dependencies"));
        data.lockAfterDependency = optStringFlexible(obj, "lock_after_dependency",
                optStringFlexible(obj, "lockAfterDependency", ""));
        data.optional = optStringFlexible(obj, "optional", "");
        data.repeatable = optStringFlexible(obj, "repeatable", "");
        data.autoComplete = optStringFlexible(obj, "auto_complete",
                optStringFlexible(obj, "autoComplete", ""));
        data.hiddenUnderDependency = optStringFlexible(obj, "hiddenUnderDependency",
                optStringFlexible(obj, "hidden_under_dependency", ""));
        data.type = optString(obj, "type", "");
        data.completionJson = obj.has("completion") ? GSON.toJson(obj.get("completion")) : "";
        data.rewardJson = obj.has("reward") ? GSON.toJson(obj.get("reward")) : "";
        return data;
    }

    // format dependencies
    public static String formatDependencies(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString();
        if (el.isJsonArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonElement e : el.getAsJsonArray()) {
                if (e != null && e.isJsonPrimitive()) parts.add(e.getAsString());
            }
            return String.join(", ", parts);
        }
        return "";
    }

    // format dependencies lines
    public static String formatDependenciesLines(JsonElement el) {
        String value = formatDependencies(el);
        if (value.isBlank()) return "";
        String[] parts = value.split(",");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String trimmed = safe(part).trim();
            if (!trimmed.isBlank()) out.add(trimmed);
        }
        return String.join("\n", out);
    }

    // read json
    public static JsonObject readJson(Path path) {
        if (path == null || !Files.exists(path)) return null;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
        }
        return null;
    }

    // read order from path
    public static int readOrderFromPath(Path path, int fallback) {
        JsonObject obj = readJson(path);
        return QuestEditorEntryCodec.parseIntFlexible(obj, "order", fallback);
    }

    // opt string
    public static String optString(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) return def;
        return obj.get(key).getAsString();
    }

    // opt string flexible
    public static String optStringFlexible(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key)) return def;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return def;
        if (el.isJsonPrimitive()) return el.getAsString();
        return def;
    }

    // file id
    public static String fileId(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
