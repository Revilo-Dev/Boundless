package net.revilodev.boundless.client.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// shares tree connector geometry between the editor and quest book
public final class QuestTreeConnectorRouting {
    private static final int MAX_RASTER_STEPS = 4096;
    private static final int DETAILED_MAX_NODES = 128;
    private static final int DETAILED_MAX_EDGES = 256;
    private static final int NODE_SIZE = 32;
    private static final int GRID_SIZE = 16;

    private QuestTreeConnectorRouting() {
    }

    public static List<RoutedEdge> route(List<Node> nodes, List<Edge> edges) {
        List<Node> safeNodes = nodes == null ? List.of() : nodes;
        List<Edge> safeEdges = edges == null ? List.of() : edges;
        Map<String, Node> nodesById = new HashMap<>();
        for (Node node : safeNodes) {
            if (node != null && node.id != null && !node.id.isBlank()) nodesById.put(node.id, node);
        }

        List<RoutedEdge> result = new ArrayList<>();
        List<Route> routed = new ArrayList<>();
        for (Edge edge : safeEdges) {
            if (edge == null) continue;
            Node from = nodesById.get(edge.fromId);
            Node to = nodesById.get(edge.toId);
            if (from == null || to == null) continue;
            boolean detailed = safeNodes.size() <= DETAILED_MAX_NODES && result.size() < DETAILED_MAX_EDGES;
            Route route = detailed ? detailedRoute(from, to, safeNodes, routed) : simpleRoute(from, to);
            result.add(new RoutedEdge(edge, route.points));
            if (detailed) routed.add(route);
        }
        return List.copyOf(result);
    }

    private static Route detailedRoute(Node from, Node to, List<Node> nodes, List<Route> routed) {
        int x1 = from.x + NODE_SIZE / 2;
        int y1 = from.y + NODE_SIZE / 2;
        int x2 = to.x + NODE_SIZE / 2;
        int y2 = to.y + NODE_SIZE / 2;
        List<Route> candidates = new ArrayList<>();
        candidates.add(route(new Point(x1, y1), new Point(x2, y2)));
        candidates.add(route(new Point(x1, y1), new Point(x2, y1), new Point(x2, y2)));
        candidates.add(route(new Point(x1, y1), new Point(x1, y2), new Point(x2, y2)));

        int middleX = Math.round((x1 + x2) / 2f / GRID_SIZE) * GRID_SIZE;
        int middleY = Math.round((y1 + y2) / 2f / GRID_SIZE) * GRID_SIZE;
        int[] offsets = {0, GRID_SIZE, -GRID_SIZE, GRID_SIZE * 2, -GRID_SIZE * 2,
                GRID_SIZE * 3, -GRID_SIZE * 3, GRID_SIZE * 4, -GRID_SIZE * 4};
        for (int offset : offsets) {
            int bendX = middleX + offset;
            candidates.add(route(new Point(x1, y1), new Point(bendX, y1),
                    new Point(bendX, y2), new Point(x2, y2)));
            int bendY = middleY + offset;
            candidates.add(route(new Point(x1, y1), new Point(x1, bendY),
                    new Point(x2, bendY), new Point(x2, y2)));
        }

        Route best = candidates.get(0);
        long bestScore = Long.MAX_VALUE;
        for (Route candidate : candidates) {
            long score = score(candidate, from, to, nodes, routed);
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static Route simpleRoute(Node from, Node to) {
        int x1 = from.x + NODE_SIZE / 2;
        int y1 = from.y + NODE_SIZE / 2;
        int x2 = to.x + NODE_SIZE / 2;
        int y2 = to.y + NODE_SIZE / 2;
        int middleX = Math.round((x1 + x2) / 2f / GRID_SIZE) * GRID_SIZE;
        return route(new Point(x1, y1), new Point(middleX, y1), new Point(middleX, y2), new Point(x2, y2));
    }

    private static Route route(Point... rawPoints) {
        List<Point> points = new ArrayList<>();
        for (Point point : rawPoints) {
            if (point == null || !points.isEmpty() && points.get(points.size() - 1).equals(point)) continue;
            points.add(point);
        }
        return new Route(List.copyOf(points), pixels(points));
    }

    private static long score(Route candidate, Node from, Node to, List<Node> nodes, List<Route> routed) {
        long score = Math.max(0, candidate.points.size() - 2) * 12L;
        for (int index = 1; index < candidate.points.size(); index++) {
            Point start = candidate.points.get(index - 1);
            Point end = candidate.points.get(index);
            score += Math.abs(end.x - start.x) + Math.abs(end.y - start.y);
        }
        for (Node node : nodes) {
            if (node == from || node == to) continue;
            int left = node.x - 4;
            int top = node.y - 4;
            int right = left + NODE_SIZE + 8;
            int bottom = top + NODE_SIZE + 8;
            for (long packed : candidate.pixels) {
                int x = (int) (packed >> 32);
                int y = (int) packed;
                if (x >= left && x <= right && y >= top && y <= bottom) score += 8_000L;
            }
        }
        for (Route existing : routed) {
            for (long packed : candidate.pixels) {
                int x = (int) (packed >> 32);
                int y = (int) packed;
                if (nearEndpoint(x, y, candidate) || nearEndpoint(x, y, existing)) continue;
                if (existing.pixels.contains(packed)) {
                    score += 2_000L;
                    continue;
                }
                for (int offsetX = -2; offsetX <= 2; offsetX++) {
                    for (int offsetY = -2; offsetY <= 2; offsetY++) {
                        if (existing.pixels.contains(pack(x + offsetX, y + offsetY))) score += 20L;
                    }
                }
            }
        }
        return score;
    }

    private static boolean nearEndpoint(int x, int y, Route route) {
        if (route.points.isEmpty()) return false;
        Point first = route.points.get(0);
        Point last = route.points.get(route.points.size() - 1);
        int radius = NODE_SIZE / 2 + 4;
        return Math.abs(x - first.x) <= radius && Math.abs(y - first.y) <= radius
                || Math.abs(x - last.x) <= radius && Math.abs(y - last.y) <= radius;
    }

    private static Set<Long> pixels(List<Point> points) {
        Set<Long> pixels = new HashSet<>();
        for (int index = 1; index < points.size(); index++) raster(points.get(index - 1), points.get(index), pixels);
        return pixels;
    }

    private static void raster(Point start, Point end, Set<Long> pixels) {
        int x1 = start.x;
        int y1 = start.y;
        int x2 = end.x;
        int y2 = end.y;
        int distance = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (distance > MAX_RASTER_STEPS) {
            for (int step = 0; step <= MAX_RASTER_STEPS; step++) {
                double progress = step / (double) MAX_RASTER_STEPS;
                pixels.add(pack((int) Math.round(x1 + (x2 - x1) * progress),
                        (int) Math.round(y1 + (y2 - y1) * progress)));
            }
            return;
        }
        int dx = Math.abs(x2 - x1);
        int stepX = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1);
        int stepY = y1 < y2 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            pixels.add(pack(x1, y1));
            if (x1 == x2 && y1 == y2) return;
            int twice = error * 2;
            if (twice >= dy) {
                error += dy;
                x1 += stepX;
            }
            if (twice <= dx) {
                error += dx;
                y1 += stepY;
            }
        }
    }

    private static long pack(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public record Node(String id, int x, int y) {
    }

    public record Edge(String fromId, String toId) {
    }

    public record Point(int x, int y) {
    }

    public record RoutedEdge(Edge edge, List<Point> points) {
    }

    private record Route(List<Point> points, Set<Long> pixels) {
    }
}
