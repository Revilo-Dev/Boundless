package net.revilodev.boundless.client.editor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.revilodev.boundless.client.editor.QuestEditorModels.MoveDirection;
import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListIndex;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListItem;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestMoveEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;
import net.revilodev.boundless.quest.QuestPackStorage;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// quest list ordering
public final class QuestEditorQuestOrdering {
    private QuestEditorQuestOrdering() {
    }

    // quest order result
    public record QuestOrderResult(Path editingPath, String questOrderToken) {}

    // build quest list index
    public static QuestListIndex buildQuestListIndex(QuestPack pack) {
        if (pack == null) return QuestListIndex.EMPTY;

        // build a stable view of quests and group names
        QuestListIndex index = new QuestListIndex();
        for (NamedEntry category : QuestEditorJsonFiles.listCategoryEntries(pack)) {
            String id = safe(category.id);
            index.categoryNames.put(id, safe(category.name));
            if (!id.isBlank() && !"all".equalsIgnoreCase(id)) index.categoryIds.add(id);
        }
        for (NamedEntry subCategory : QuestEditorJsonFiles.listSubCategoryEntries(pack)) {
            QuestEditorModels.SubCategoryData data = QuestEditorJsonFiles.loadSubCategory(subCategory.path, subCategory.id);
            String parent = data == null ? "" : safe(data.category);
            String id = safe(subCategory.id);
            index.subCategoryNames.put(parent + "::" + id, safe(subCategory.name));
            if (!id.isBlank()) {
                index.subCategoryIds.add(id);
                if (!parent.isBlank()) index.subCategoryIds.add(parent + "::" + id);
            }
        }

        for (QuestListItem item : listQuestListItems(pack)) {
            QuestEntryData data = item.data;
            if (data == null) continue;
            String id = safe(data.id).trim();
            if (!id.isBlank() && !index.questIds.add(id)) index.duplicateQuestIds.add(id);
            index.questDataByEntryId.put(safe(item.entry.id), data);
            index.items.add(item);
        }

        return index;
    }

    // list quest list items
    public static List<QuestListItem> listQuestListItems(QuestPack pack) {
        List<QuestListItem> items = new ArrayList<>();
        if (pack == null || pack.questsDir == null || !Files.isDirectory(pack.questsDir)) return items;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pack.questsDir, "*.json")) {
            for (Path path : stream) {
                JsonObject obj = QuestEditorJsonFiles.readJson(path);
                if (obj == null) continue;
                String fallbackId = QuestEditorJsonFiles.fileId(path);
                QuestEntryData data = QuestEditorJsonFiles.loadQuest(path, fallbackId, obj);
                if (data == null) continue;
                int order = QuestEditorEntryCodec.parseIntFlexible(obj, "order", 0);
                String sortKey = String.format(Locale.ROOT, "%06d_%s", order, fallbackId);
                NamedEntry entry = new NamedEntry(data.id, data.name, data.icon, path, sortKey);
                items.add(new QuestListItem(entry, data));
            }
        } catch (IOException ignored) {
        }
        items.sort(Comparator.comparing(item -> safe(item.entry.sortKey).toLowerCase(Locale.ROOT)));
        return items;
    }

    // move quest within group
    public static QuestOrderResult moveQuestWithinGroup(QuestPack pack, String questId, MoveDirection direction, Path editingPath) throws IOException {
        if (pack == null || questId == null || questId.isBlank()) return new QuestOrderResult(editingPath, "");

        // keep quest moves inside the current quest group
        List<NamedEntry> all = QuestEditorJsonFiles.listQuestEntries(pack);
        List<QuestMoveEntry> group = new ArrayList<>();
        for (NamedEntry entry : all) {
            QuestEntryData data = QuestEditorJsonFiles.loadQuest(entry.path, entry.id);
            if (data == null) continue;
            group.add(new QuestMoveEntry(entry.id, entry.path, safe(data.category), safe(data.subCategory)));
        }

        QuestMoveEntry current = null;
        for (QuestMoveEntry entry : group) {
            if (entry.id.equals(questId)) {
                current = entry;
                break;
            }
        }
        if (current == null) return new QuestOrderResult(editingPath, "");

        List<QuestMoveEntry> siblings = new ArrayList<>();
        for (QuestMoveEntry entry : group) {
            if (entry.category.equals(current.category) && entry.subCategory.equals(current.subCategory)) {
                siblings.add(entry);
            }
        }
        if (siblings.size() < 2) return new QuestOrderResult(editingPath, "");

        int index = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).id.equals(questId)) {
                index = i;
                break;
            }
        }
        if (index < 0) return new QuestOrderResult(editingPath, "");

        int targetIndex = direction == MoveDirection.UP ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= siblings.size()) return new QuestOrderResult(editingPath, "");

        QuestMoveEntry moving = siblings.remove(index);
        siblings.add(targetIndex, moving);

        return applyQuestOrder(pack, siblings, editingPath);
    }

    // move category by order
    public static void moveCategoryByOrder(QuestPack pack, Gson gson, String categoryId, MoveDirection direction) throws IOException {
        if (pack == null || categoryId == null || categoryId.isBlank()) return;
        List<NamedEntry> categories = QuestEditorJsonFiles.listCategoryEntries(pack);
        moveOrderedEntry(pack, gson, categories, categoryId, direction, "order");
    }

    // move sub category by order
    public static void moveSubCategoryByOrder(QuestPack pack, Gson gson, String subCategoryId, MoveDirection direction) throws IOException {
        if (pack == null || subCategoryId == null || subCategoryId.isBlank()) return;
        List<NamedEntry> subCategories = QuestEditorJsonFiles.listSubCategoryEntries(pack);
        moveOrderedEntry(pack, gson, subCategories, subCategoryId, direction, "order");
    }

    // quest file base name
    public static String questFileBaseName(String questId, String indexRaw) {
        String id = safe(questId).trim();
        if (id.isBlank()) return id;
        String index = safe(indexRaw).trim();
        if (index.isBlank()) return id;
        return index + "-" + id;
    }

    private static QuestOrderResult applyQuestOrder(QuestPack pack, List<QuestMoveEntry> orderedEntries, Path editingPath) throws IOException {
        if (pack == null || orderedEntries == null || orderedEntries.isEmpty()) return new QuestOrderResult(editingPath, "");
        Map<Path, Path> stagedMoves = new LinkedHashMap<>();
        Path updatedEditingPath = editingPath;
        String updatedQuestOrderToken = "";
        // stage temporary names before final order names
        for (int i = 0; i < orderedEntries.size(); i++) {
            QuestMoveEntry entry = orderedEntries.get(i);
            String orderToken = String.format(Locale.ROOT, "%02d", i + 1);
            Path target = pack.questsDir.resolve(questFileBaseName(entry.id, orderToken) + ".json");
            if (entry.path.equals(target)) continue;

            Path temp = entry.path.resolveSibling(entry.path.getFileName().toString() + ".reorder_tmp");
            int guard = 0;
            while (Files.exists(temp) || stagedMoves.containsKey(temp)) {
                guard++;
                temp = entry.path.resolveSibling(entry.path.getFileName().toString() + ".reorder_tmp_" + guard);
            }
            Files.move(entry.path, temp, StandardCopyOption.REPLACE_EXISTING);
            stagedMoves.put(temp, target);

            if (editingPath != null && editingPath.equals(entry.path)) {
                updatedEditingPath = target;
                updatedQuestOrderToken = orderToken;
            }
        }

        for (Map.Entry<Path, Path> move : stagedMoves.entrySet()) {
            Files.move(move.getKey(), move.getValue(), StandardCopyOption.REPLACE_EXISTING);
        }
        return new QuestOrderResult(updatedEditingPath, updatedQuestOrderToken);
    }

    // move list entries by saved order
    private static void moveOrderedEntry(QuestPack pack, Gson gson, List<NamedEntry> entries, String id, MoveDirection direction, String key) throws IOException {
        if (entries == null || entries.size() < 2 || id == null || id.isBlank()) return;
        int index = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.get(i).id)) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        int targetIndex = direction == MoveDirection.UP ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= entries.size()) return;

        NamedEntry moving = entries.remove(index);
        entries.add(targetIndex, moving);
        applyExplicitOrder(pack, gson, entries, key);
    }

    // write explicit order values back to disk
    private static void applyExplicitOrder(QuestPack pack, Gson gson, List<NamedEntry> orderedEntries, String key) throws IOException {
        if (pack == null || orderedEntries == null || orderedEntries.isEmpty() || key == null || key.isBlank()) return;
        for (int i = 0; i < orderedEntries.size(); i++) {
            NamedEntry entry = orderedEntries.get(i);
            if (entry == null || entry.path == null) continue;
            JsonObject obj = QuestEditorJsonFiles.readJson(entry.path);
            if (obj == null) continue;
            obj.addProperty(key, i);
            QuestPackStorage.writeJsonAtomically(gson, obj, entry.path);
        }
        QuestEditorPackFiles.backupPack(pack, "reordered");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
