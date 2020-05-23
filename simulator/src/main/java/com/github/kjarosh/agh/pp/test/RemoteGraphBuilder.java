package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.events.EventStats;
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
        Thread supervisor = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }

                double v = (double) verticesBuilt.get() / graph.allVertices().size();
                double e = (double) edgesBuilt.get() / graph.allEdges().size();

                EventStats stats = allZones.stream()
                        .map(client::getEventStats)
                        .reduce(EventStats.empty(), EventStats::combine);
                System.out.println(String.format(
                        "built: V: %.2f%% E: %.2f%%    %s", v * 100, e * 100, stats));
            }
        });
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
                        client.addEdge(zone, e.src(), e.dst(), e.permissions());
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
