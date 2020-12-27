package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.VertexCreationRequestDto;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class RemoteGraphBuilder {
    private static final int BULK_SIZE = 20_000;

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
            buildVertices(client, verticesBuilt);

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

    private void buildVertices(ZoneClient client, AtomicInteger verticesBuilt) {
        Map<ZoneId, List<Vertex>> groupedByOwner = graph.allVertices()
                .stream()
                .collect(Collectors.groupingBy(v -> v.id().owner()));

        for (ZoneId owner : groupedByOwner.keySet()) {
            List<Vertex> vertices = groupedByOwner.get(owner);
            for (List<Vertex> bulk : Lists.partition(vertices, BULK_SIZE)) {
                List<VertexCreationRequestDto> requests = bulk.stream()
                        .map(v -> new VertexCreationRequestDto(v.id().name(), v.type()))
                        .collect(Collectors.toList());
                client.addVertices(owner, new BulkVertexCreationRequestDto(requests));
                verticesBuilt.addAndGet(requests.size());
            }
        }
    }
}
