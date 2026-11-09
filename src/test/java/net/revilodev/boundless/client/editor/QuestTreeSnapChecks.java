package net.revilodev.boundless.client.editor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuestTreeSnapChecks {
    @Test
    void leavesFreePlacementAwayFromAlignment() {
        assertEquals(new QuestTreeSnap.Position(13, 27),
                QuestTreeSnap.snap(13, 27, List.of(new QuestTreeSnap.Position(100, 100))));
    }

    @Test
    void softlyAlignsHorizontalAndVerticalPlacement() {
        QuestTreeSnap.Position anchor = new QuestTreeSnap.Position(100, 20);
        assertEquals(new QuestTreeSnap.Position(100, 60), QuestTreeSnap.snap(97, 60, List.of(anchor)));
        assertEquals(new QuestTreeSnap.Position(60, 20), QuestTreeSnap.snap(60, 23, List.of(anchor)));
        assertEquals(new QuestTreeSnap.Position(95, 60), QuestTreeSnap.snap(95, 60, List.of(anchor)));
    }

    @Test
    void softlyAlignsPerfectDiagonals() {
        QuestTreeSnap.Position anchor = new QuestTreeSnap.Position(0, 0);
        assertEquals(new QuestTreeSnap.Position(32, 32), QuestTreeSnap.snap(30, 33, List.of(anchor)));
        assertEquals(new QuestTreeSnap.Position(33, -33), QuestTreeSnap.snap(31, -34, List.of(anchor)));
    }

    @Test
    void shiftDragLocksToPerfectAngles() {
        assertEquals(new QuestTreeSnap.Position(40, 0),
                QuestTreeSnap.snapDrag(40, 5, 0, 0, List.of()));
        assertEquals(new QuestTreeSnap.Position(0, 40),
                QuestTreeSnap.snapDrag(5, 40, 0, 0, List.of()));
        assertEquals(new QuestTreeSnap.Position(36, 36),
                QuestTreeSnap.snapDrag(40, 31, 0, 0, List.of()));
        assertEquals(new QuestTreeSnap.Position(-36, 36),
                QuestTreeSnap.snapDrag(-40, 31, 0, 0, List.of()));
    }

    @Test
    void nodeAlignmentTakesPriorityDuringShiftDrag() {
        QuestTreeSnap.Position anchor = new QuestTreeSnap.Position(100, 70);
        assertEquals(new QuestTreeSnap.Position(100, 61),
                QuestTreeSnap.snapDrag(97, 61, 0, 0, List.of(anchor)));
    }
}
