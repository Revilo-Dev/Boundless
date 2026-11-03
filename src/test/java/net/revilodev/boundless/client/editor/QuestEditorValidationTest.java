package net.revilodev.boundless.client.editor;

import net.revilodev.boundless.client.editor.QuestEditorModels.NamedEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestEditorValidationTest {
    @Test
    void existingQuestDoesNotConflictWithItsNormalizedPath() {
        Path editingPath = Path.of("questpacks", "test", "quests", "quest.json");
        Path indexedPath = editingPath.toAbsolutePath().resolveSibling(".").resolve("quest.json");
        NamedEntry quest = new NamedEntry("quest", "Quest", "", indexedPath, "");

        assertFalse(QuestEditorValidation.isDuplicateQuestId("quest", true, List.of(quest), editingPath));
    }

    @Test
    void duplicateQuestStillConflictsWhileEditing() {
        Path editingPath = Path.of("questpacks", "test", "quests", "quest.json");
        NamedEntry current = new NamedEntry("quest", "Quest", "", editingPath.toAbsolutePath(), "");
        NamedEntry duplicate = new NamedEntry("quest", "Duplicate", "", Path.of("other.json"), "");

        assertTrue(QuestEditorValidation.isDuplicateQuestId("quest", true, List.of(current, duplicate), editingPath));
    }
}
