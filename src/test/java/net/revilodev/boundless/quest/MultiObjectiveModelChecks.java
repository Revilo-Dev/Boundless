package net.revilodev.boundless.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MultiObjectiveModelChecks {

    @Test
    void targetRetainsAcceptedIdsForMultiItemObjective() {
        QuestData.Target target = new QuestData.Target("item",
                List.of("minecraft:oak_log", "minecraft:birch_log", "minecraft:spruce_log"),
                16);

        assertEquals("minecraft:oak_log", target.id);
        assertEquals(List.of("minecraft:oak_log", "minecraft:birch_log", "minecraft:spruce_log"), target.acceptedIdsOrLegacy());
        assertTrue(target.hasMultipleAcceptedIds());
        assertEquals(16, target.count);
    }

    @Test
    void singleTargetRemainsBackwardCompatible() {
        QuestData.Target target = new QuestData.Target("entity", "minecraft:zombie", 10);

        assertEquals(List.of("minecraft:zombie"), target.acceptedIdsOrLegacy());
        assertFalse(target.hasMultipleAcceptedIds());
    }

    @Test
    void multiProgressKeyIsStableAcrossOrdering() {
        QuestData.Quest quest = new QuestData.Quest(
                "quest.logs",
                "Logs",
                "minecraft:oak_log",
                "",
                List.of(),
                false,
                false,
                false,
                false,
                false,
                new QuestData.Rewards(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "", 0),
                "collection",
                new QuestData.Completion(List.of()),
                "misc",
                "",
                ""
        );

        QuestData.Target first = new QuestData.Target("item",
                List.of("minecraft:oak_log", "minecraft:birch_log", "minecraft:spruce_log"),
                16);
        QuestData.Target second = new QuestData.Target("item",
                List.of("minecraft:spruce_log", "minecraft:oak_log", "minecraft:birch_log"),
                16);

        assertEquals(QuestTracker.itemProgressKey(quest, first), QuestTracker.itemProgressKey(quest, second));
    }

    @Test
    void malformedObjectivesFailClosed() {
        assertFalse(QuestTracker.isSupportedTarget(null));
        assertFalse(QuestTracker.isSupportedTarget(new QuestData.Target("broken", "missing:id", 1)));
        assertTrue(QuestTracker.isSupportedTarget(new QuestData.Target("item", "minecraft:stone", 1)));
    }

    @Test
    void storedCompletionCannotBypassDependencyLock() {
        assertFalse(QuestTracker.isStoredCompletionRedeemable(QuestTracker.Status.COMPLETED, false));
        assertTrue(QuestTracker.isStoredCompletionRedeemable(QuestTracker.Status.COMPLETED, true));
        assertFalse(QuestTracker.isStoredCompletionRedeemable(QuestTracker.Status.INCOMPLETE, true));
    }

    @Test
    void completionToastWaitsForRequirementsAndDependencies() {
        assertFalse(QuestTracker.shouldShowCompletionToast(QuestTracker.Status.INCOMPLETE, false, true));
        assertFalse(QuestTracker.shouldShowCompletionToast(QuestTracker.Status.INCOMPLETE, true, false));
        assertFalse(QuestTracker.shouldShowCompletionToast(QuestTracker.Status.COMPLETED, true, true));
        assertTrue(QuestTracker.shouldShowCompletionToast(QuestTracker.Status.INCOMPLETE, true, true));
    }
}
