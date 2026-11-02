package net.revilodev.boundless.client.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuestEditorNamingTest {
    @Test
    void ignoresTitleFormattingWhenGeneratingIds() {
        assertEquals("main-category", QuestEditorNaming.idFromDisplayName("/rMain /lCategory/x/x"));
        assertEquals("side-quests", QuestEditorNaming.idFromDisplayName("/hSide/x /pQuests/x"));
        assertEquals("vanilla-title", QuestEditorNaming.idFromDisplayName("\u00a7aVanilla \u00a7lTitle\u00a7r"));
    }
}
