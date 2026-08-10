package net.revilodev.boundless.client.editor;

import java.util.List;

final class QuestTreeSnap {
    private static final int SNAP_DISTANCE = 4;

    private QuestTreeSnap() {
    }

    static Position snap(int rawX, int rawY, List<Position> anchors) {
        Position best = new Position(rawX, rawY);
        int bestDistance = SNAP_DISTANCE * SNAP_DISTANCE + 1;
        if (anchors == null) return best;

        for (Position anchor : anchors) {
            if (anchor == null) continue;
            Position candidate = closer(best, rawX, rawY, new Position(anchor.x, rawY), bestDistance);
            if (candidate != best) {
                best = candidate;
                bestDistance = distanceSquared(rawX, rawY, best);
            }
            candidate = closer(best, rawX, rawY, new Position(rawX, anchor.y), bestDistance);
            if (candidate != best) {
                best = candidate;
                bestDistance = distanceSquared(rawX, rawY, best);
            }

            int positiveX = Math.round((rawX + rawY + anchor.x - anchor.y) / 2f);
            Position positiveDiagonal = new Position(positiveX, positiveX - anchor.x + anchor.y);
            candidate = closer(best, rawX, rawY, positiveDiagonal, bestDistance);
            if (candidate != best) {
                best = candidate;
                bestDistance = distanceSquared(rawX, rawY, best);
            }

            int negativeX = Math.round((rawX - rawY + anchor.x + anchor.y) / 2f);
            Position negativeDiagonal = new Position(negativeX, anchor.x + anchor.y - negativeX);
            candidate = closer(best, rawX, rawY, negativeDiagonal, bestDistance);
            if (candidate != best) {
                best = candidate;
                bestDistance = distanceSquared(rawX, rawY, best);
            }
        }
        return best;
    }

    static Position snapDrag(int rawX, int rawY, int startX, int startY, List<Position> anchors) {
        Position aligned = snap(rawX, rawY, anchors);
        if (aligned.x != rawX || aligned.y != rawY) return aligned;

        int xDistance = rawX - startX;
        int yDistance = rawY - startY;
        int absoluteX = Math.abs(xDistance);
        int absoluteY = Math.abs(yDistance);
        if (absoluteX == 0 && absoluteY == 0) return new Position(rawX, rawY);

        if (absoluteX > absoluteY * (1f + (float) Math.sqrt(2))) {
            return new Position(rawX, startY);
        }
        if (absoluteY > absoluteX * (1f + (float) Math.sqrt(2))) {
            return new Position(startX, rawY);
        }

        int diagonalDistance = Math.round((absoluteX + absoluteY) / 2f);
        return new Position(startX + Integer.signum(xDistance) * diagonalDistance,
                startY + Integer.signum(yDistance) * diagonalDistance);
    }

    private static Position closer(Position current, int rawX, int rawY, Position candidate, int bestDistance) {
        int xDistance = Math.abs(candidate.x - rawX);
        int yDistance = Math.abs(candidate.y - rawY);
        if (xDistance > SNAP_DISTANCE || yDistance > SNAP_DISTANCE) return current;
        int distance = distanceSquared(rawX, rawY, candidate);
        return distance < bestDistance ? candidate : current;
    }

    private static int distanceSquared(int rawX, int rawY, Position position) {
        int xDistance = position.x - rawX;
        int yDistance = position.y - rawY;
        return xDistance * xDistance + yDistance * yDistance;
    }

    record Position(int x, int y) {
    }
}
