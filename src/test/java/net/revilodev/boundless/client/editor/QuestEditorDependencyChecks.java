package net.revilodev.boundless.client.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.revilodev.boundless.client.editor.QuestEditorJsonBuilder.QuestBuildFields;
import net.revilodev.boundless.client.editor.QuestEditorModels.EditorType;
import net.revilodev.boundless.client.editor.QuestEditorStateSignatures.SignatureFields;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class QuestEditorDependencyChecks {
    @Test
    void writesLocksForIndividualDependencies() {
        QuestBuildFields fields = new QuestBuildFields();
        fields.id = "branch";
        fields.name = "Branch";
        fields.category = "main";
        fields.dependencies = List.of("start", "alternate");
        fields.incompatibleDependencies = List.of("alternate");

        JsonObject json = QuestEditorJsonBuilder.buildQuest(fields, message -> {
            throw new AssertionError(message);
        });
        JsonArray locked = json.getAsJsonArray("incompatible_dependencies");

        assertEquals(1, locked.size());
        assertEquals("alternate", locked.get(0).getAsString());
        assertFalse(json.get("lock_after_dependency").getAsBoolean());
    }

    @Test
    void dependencyLockRowsChangeUnsavedSignature() {
        SignatureFields first = new SignatureFields();
        first.questDependencies = "start\nalternate";
        first.questIncompatibleDependencies = "start";
        SignatureFields second = new SignatureFields();
        second.questDependencies = first.questDependencies;
        second.questIncompatibleDependencies = "alternate";

        assertNotEquals(QuestEditorStateSignatures.signature(EditorType.QUEST, first),
                QuestEditorStateSignatures.signature(EditorType.QUEST, second));
    }
}
