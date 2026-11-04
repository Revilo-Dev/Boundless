package net.revilodev.boundless.client.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.revilodev.boundless.client.editor.QuestEditorJsonBuilder.QuestBuildFields;
import net.revilodev.boundless.client.editor.QuestEditorModels.CategoryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestListIndex;
import net.revilodev.boundless.client.editor.QuestEditorModels.SubCategoryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestEditorTranslationChecks {
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
    void writesAndLoadsCategoryTitleTranslations() throws Exception {
        JsonObject category = QuestEditorJsonBuilder.buildCategory("main", "/rMain", Map.of("pl_pl", "Glowna"),
                "minecraft:book", 0, "", false, "", 0.5f, false, 0.45f,
                message -> { throw new AssertionError(message); });
        Path categoryPath = temporaryDirectory.resolve("main.json");
        Files.writeString(categoryPath, category.toString());
        CategoryData loadedCategory = QuestEditorJsonFiles.loadCategory(categoryPath, "main");

        JsonObject subCategory = QuestEditorJsonBuilder.buildSubCategory("start", "main", "/gStart",
                Map.of("pl_pl", "Poczatek"), "minecraft:book", 0, true,
                message -> { throw new AssertionError(message); });
        Path subCategoryPath = temporaryDirectory.resolve("start.json");
        Files.writeString(subCategoryPath, subCategory.toString());
        SubCategoryData loadedSubCategory = QuestEditorJsonFiles.loadSubCategory(subCategoryPath, "start");

        assertEquals("Glowna", loadedCategory.nameTranslations.get("pl_pl"));
        assertEquals("Poczatek", loadedSubCategory.nameTranslations.get("pl_pl"));
    }

    @Test
    void categoryRebuildPreservesStickers() {
        JsonObject existing = new JsonObject();
        JsonArray stickers = new JsonArray();
        JsonObject sticker = new JsonObject();
        sticker.addProperty("type", "text");
        sticker.addProperty("value", "Keep me");
        stickers.add(sticker);
        existing.add("stickers", stickers);
        JsonObject rebuilt = QuestEditorJsonBuilder.buildCategory("main", "Main", Map.of(),
                "", 0, "", false, "", 0.5f, false, 0.45f,
                message -> { throw new AssertionError(message); });

        QuestEditorJsonBuilder.preserveCategoryStickers(rebuilt, existing);
        existing.getAsJsonArray("stickers").get(0).getAsJsonObject().addProperty("value", "Changed");

        assertTrue(rebuilt.has("stickers"));
        assertEquals("Keep me", rebuilt.getAsJsonArray("stickers").get(0).getAsJsonObject().get("value").getAsString());
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
        assertEquals(1, index.questsByCategory.get("main").size());

        JsonObject moved = quest("large", "Moved");
        moved.addProperty("category", "second");
        QuestEditorQuestOrdering.upsertQuestListIndex(index, moved, path, path);

        assertEquals(0, index.questsByCategory.getOrDefault("main", java.util.List.of()).size());
        assertEquals(1, index.questsByCategory.get("second").size());
    }

    private static JsonObject quest(String id, String name) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("category", "main");
        return json;
    }
}
