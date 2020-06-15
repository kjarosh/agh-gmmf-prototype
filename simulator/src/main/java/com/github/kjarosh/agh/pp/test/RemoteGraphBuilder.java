package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kamil Jarosz
 */
public class RemoteGraphBuilder {
    private final Graph graph;
    private final ZoneClient client;
    private final List<ZoneId> allZones;
    private final AtomicInteger verticesBuilt = new AtomicInteger(0);
    private final AtomicInteger edgesBuilt = new AtomicInteger(0);

    public RemoteGraphBuilder(Graph graph, ZoneClient client, List<ZoneId> allZones) {
        this.graph = graph;
        this.client = client;
        this.allZones = allZones;
    }

    public void build(ZoneClient client, ZoneId zone) {
        Supervisor supervisor = new Supervisor(
                () -> (double) verticesBuilt.get() / graph.allVertices().size(),
                () -> (double) edgesBuilt.get() / graph.allEdges().size(),
                new EventStatsGatherer(client, allZones));
        supervisor.start();
        try {
            graph.allVertices()
                    .stream()
                    .parallel()
                    .forEach(v -> {
                        client.addVertex(v.id(), v.type());
                        verticesBuilt.incrementAndGet();
                    });
            graph.allEdges()
                    .stream()
                    .sorted(Comparator.comparing(Edge::src)
                            .thenComparing(Edge::dst))
                    .parallel()
                    .forEach(e -> {
                        client.addEdge(zone, e.id(), e.permissions());
                        edgesBuilt.incrementAndGet();
                    });

            while (indexNotReady()) {
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while building graph");
        } finally {
            supervisor.interrupt();
        }

        try {
            supervisor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean indexNotReady() {
        return allZones.stream()
                .anyMatch(zone -> !client.indexReady(zone));
    }
}
