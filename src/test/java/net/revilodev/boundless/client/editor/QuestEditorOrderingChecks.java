package net.revilodev.boundless.client.editor;

import com.google.gson.JsonObject;
import net.revilodev.boundless.client.editor.QuestEditorModels.MoveDirection;
import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestPack;
import net.revilodev.boundless.quest.QuestPackStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuestEditorOrderingChecks {
    @Test
    void reordersSubCategoriesOnlyInsideTheirParent(@TempDir Path tempDir) throws Exception {
        QuestPack pack = new QuestPack("ordering", "ordering", tempDir.resolve("ordering"));
        pack.ensureDirs();
        writeSubCategory(pack, "first", "main", 0);
        writeSubCategory(pack, "second", "main", 1);
        writeSubCategory(pack, "unrelated", "other", 7);

        List<NamedEntry> reordered = QuestEditorQuestOrdering.reorderedSubCategories(
                pack, "second", MoveDirection.UP);

        assertEquals(List.of("second", "first"), reordered.stream().map(entry -> entry.id).toList());
    }

    private Path writeSubCategory(QuestPack pack, String id, String category, int order) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("category", category);
        json.addProperty("name", id);
        json.addProperty("order", order);
        Path path = pack.subCategoriesDir.resolve(id + ".json");
        QuestPackStorage.writeJsonAtomically(new com.google.gson.Gson(), json, path);
        return path;
    }
}
