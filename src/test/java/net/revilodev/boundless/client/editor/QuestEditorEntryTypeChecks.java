package net.revilodev.boundless.client.editor;

import com.google.gson.JsonObject;
import net.revilodev.boundless.client.editor.QuestEditorModels.EntryRowKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestEditorEntryTypeChecks {
    @Test
    void usesReadableCompactLabels() {
        assertEquals("collect", QuestEditorEntryTypes.label("collect"));
        assertEquals("item", QuestEditorEntryTypes.label("item"));
        assertEquals("advance", QuestEditorEntryTypes.label("advancement"));
    }

    @Test
    void usesAdvanceForAdvancementRewards() {
        assertEquals("advance", QuestEditorEntryTypes.normalize(EntryRowKind.REWARD, "advancement"));

        JsonObject reward = QuestEditorEntryCodec.parseRewardEntries(
                "advance: minecraft:story/root", true, message -> {
                    throw new AssertionError(message);
                });
        assertEquals("minecraft:story/root",
                reward.getAsJsonArray("advancements").get(0).getAsJsonObject().get("advancement").getAsString());
        assertTrue(QuestEditorEntryCodec.rewardJsonToEntries(reward.toString()).contains("advance: minecraft:story/root"));
    }

    @Test
    void providesCompactExperienceValueOptions() {
        assertEquals(java.util.List.of("points", "levels"),
                QuestEditorEntryTypes.valueOptions(EntryRowKind.COMPLETION, "xp"));
        assertEquals(java.util.List.of("levels"),
                QuestEditorEntryTypes.valueOptions(EntryRowKind.COMPLETION, "levelup"));
        assertEquals(java.util.List.of("xp", "levels"),
                QuestEditorEntryTypes.valueOptions(EntryRowKind.REWARD, "levelup"));
    }
}
