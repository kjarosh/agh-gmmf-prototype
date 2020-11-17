package com.github.kjarosh.agh.pp.test.util;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.test.strategy.TestContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
public class RandomOperationIssuer {
    private static final Executor clientExecutor = Executors.newFixedThreadPool(16);
    private final Random random = new Random();
    private final Graph graph;
    private final TestContext context;
    private final List<VertexId> vertices;
    private final boolean allowCycles;

    public RandomOperationIssuer(Graph graph, TestContext context, boolean allowCycles) {
        this.graph = graph;
        this.context = context;
        this.allowCycles = allowCycles;
        this.vertices = graph.allVertices()
                .stream()
                .map(Vertex::id)
                .collect(Collectors.toList());
    }

    public void perform() {
        ZoneClient client = context.getClient();
        ZoneId zone = context.getZone();

        if (random.nextDouble() > 0.2) {
            EdgeId edgeId = randomEdge(graph, vertices, true);
            clientExecutor.execute(() ->
                    client.setPermissions(zone, edgeId, randomPermissions()));
        } else if (random.nextBoolean()) {
            EdgeId edgeId = randomEdge(graph, vertices, true);
            clientExecutor.execute(() ->
                    client.removeEdge(zone, edgeId));
        } else {
            EdgeId edgeId = randomEdge(graph, vertices, false);
            clientExecutor.execute(() ->
                    client.addEdge(zone, edgeId, randomPermissions()));
        }
    }

    private EdgeId randomEdge(Graph graph, List<VertexId> vertices, boolean existing) {
        if (!existing) {
            while (true) {
                List<VertexId> vx = new ArrayList<>();
                vx.add(randomVertex(vertices));
                vx.add(randomVertex(vertices));

                if (!allowCycles) {
                    vx.sort(Comparator.naturalOrder());
                }

                EdgeId e = EdgeId.of(vx.get(0), vx.get(1));
                if (graph.getEdge(e) == null) {
                    return e;
                }
            }
        } else {
            while (true) {
                VertexId from = randomVertex(vertices);
                Set<Edge> possibleDestinations = graph.getEdgesBySource(from);
                if (possibleDestinations.isEmpty()) {
                    continue;
                }
                VertexId to = randomVertex(possibleDestinations
                        .stream()
                        .map(Edge::dst)
                        .collect(Collectors.toList()));
                return EdgeId.of(from, to);
            }
        }
    }

    private VertexId randomVertex(List<VertexId> vertices) {
        return vertices.get(random.nextInt(vertices.size()));
    }

    private Permissions randomPermissions() {
        return Permissions.random(random);
    }
}
