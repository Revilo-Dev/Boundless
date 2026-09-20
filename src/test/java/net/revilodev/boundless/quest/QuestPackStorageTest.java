package net.revilodev.boundless.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestPackStorageTest {
    @Test
    void uploadFoldersAreNeverQuestPacks() {
        assertFalse(QuestPackStorage.isQuestPackDirectoryName(".example.upload"));
        assertFalse(QuestPackStorage.isQuestPackDirectoryName("example.upload"));
        assertFalse(QuestPackStorage.isQuestPackDirectoryName(".example"));
        assertTrue(QuestPackStorage.isQuestPackDirectoryName("example"));
    }
}
