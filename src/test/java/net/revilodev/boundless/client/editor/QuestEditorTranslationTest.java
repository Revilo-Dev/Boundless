package net.revilodev.boundless.client.editor;

import com.google.gson.JsonObject;
import net.revilodev.boundless.client.editor.QuestEditorJsonBuilder.QuestBuildFields;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class QuestEditorTranslationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesAndLoadsQuestTranslations() {
        QuestBuildFields fields = new QuestBuildFields();
        fields.id = "translated";
        fields.name = "Fallback title";
        fields.description = "Fallback description";
        fields.category = "main";
        fields.nameTranslations = Map.of("pl-PL", "Polski tytul", "es", "Titulo");
        fields.descriptionTranslations = Map.of("pl-PL", "Polski opis", "es", "Descripcion");

        JsonObject json = QuestEditorJsonBuilder.buildQuest(fields, message -> {
            throw new AssertionError(message);
        });
        QuestEntryData loaded = QuestEditorJsonFiles.loadQuest(temporaryDirectory.resolve("translated.json"),
                "translated", json);

        assertEquals("Polski tytul", loaded.nameTranslations.get("pl_pl"));
        assertEquals("Descripcion", loaded.descriptionTranslations.get("es"));
    }

    @Test
    void updatesLargeQuestIndexWithoutReparsingPack() {
        QuestListIndex index = new QuestListIndex();
        JsonObject original = quest("large", "Before");
        Path path = temporaryDirectory.resolve("01-large.json");
        QuestEditorQuestOrdering.upsertQuestListIndex(index, original, path, null);
        QuestEntryData retained = index.items.getFirst().data;

        JsonObject updated = quest("large", "After");
        QuestEditorQuestOrdering.upsertQuestListIndex(index, updated, path, path);

        assertEquals(1, index.items.size());
        assertSame(retained, index.items.getFirst().data);
        assertEquals("After", retained.name);
    }

    private static JsonObject quest(String id, String name) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("category", "main");
        return json;
    }
}
