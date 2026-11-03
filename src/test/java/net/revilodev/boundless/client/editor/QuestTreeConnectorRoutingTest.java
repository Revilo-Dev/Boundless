package net.revilodev.boundless.client.editor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTreeConnectorRoutingTest {
    @Test
    void routesAroundNodesDeterministically() {
        List<QuestTreeConnectorRouting.Node> nodes = List.of(
                new QuestTreeConnectorRouting.Node("start", 0, 0),
                new QuestTreeConnectorRouting.Node("middle", 48, 48),
                new QuestTreeConnectorRouting.Node("end", 96, 96));
        List<QuestTreeConnectorRouting.Edge> edges = List.of(
                new QuestTreeConnectorRouting.Edge("start", "end"));

        List<QuestTreeConnectorRouting.RoutedEdge> first = QuestTreeConnectorRouting.route(nodes, edges);
        List<QuestTreeConnectorRouting.RoutedEdge> second = QuestTreeConnectorRouting.route(nodes, edges);

        assertEquals(first, second);
        assertEquals(1, first.size());
        assertTrue(first.get(0).points().size() > 2);
    }

    @Test
    void preservesDependencyOrder() {
        List<QuestTreeConnectorRouting.Node> nodes = List.of(
                new QuestTreeConnectorRouting.Node("one", 0, 0),
                new QuestTreeConnectorRouting.Node("two", 64, 0),
                new QuestTreeConnectorRouting.Node("three", 128, 0));
        List<QuestTreeConnectorRouting.Edge> edges = List.of(
                new QuestTreeConnectorRouting.Edge("one", "two"),
                new QuestTreeConnectorRouting.Edge("two", "three"));

        List<QuestTreeConnectorRouting.RoutedEdge> routed = QuestTreeConnectorRouting.route(nodes, edges);

        assertEquals(edges, routed.stream().map(QuestTreeConnectorRouting.RoutedEdge::edge).toList());
    }
}
