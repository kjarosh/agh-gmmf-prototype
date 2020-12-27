package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class RemoteGraphBuilder {
    private final Graph graph;
    private final ZoneClient client;
    private final AtomicInteger verticesBuilt = new AtomicInteger(0);
    private final AtomicInteger edgesBuilt = new AtomicInteger(0);

    public RemoteGraphBuilder(Graph graph, ZoneClient client) {
        this.graph = graph;
        this.client = client;
    }

    public void build(ZoneClient client, ZoneId zone) {
        log.info("Building graph");

        Collection<ZoneId> allZones = graph.allZones();

        log.info("Checking all zones if they are healthy: {}", allZones);
        while (notHealthy(allZones)) {
            log.trace("Not healthy");
            sleep();
        }

        Supervisor supervisor = new Supervisor(
                () -> (double) verticesBuilt.get() / graph.allVertices().size(),
                () -> (double) edgesBuilt.get() / graph.allEdges().size(),
                new EventStatsGatherer(allZones));
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

            log.debug("Waiting for index to be built: {}", allZones);
            while (indexNotReady(allZones)) {
                log.trace("Index not built");
                sleep();
            }
        } finally {
            supervisor.interrupt();
        }

        try {
            supervisor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Graph built");
    }

    private void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while building graph");
        }
    }

    private boolean indexNotReady(Collection<ZoneId> allZones) {
        return allZones.stream()
                .anyMatch(zone -> !client.indexReady(zone));
    }

    private boolean notHealthy(Collection<ZoneId> allZones) {
        return allZones.stream()
                .anyMatch(zone -> !client.healthcheck(zone));
    }
}
