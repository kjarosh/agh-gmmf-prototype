package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.EdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.VertexCreationRequestDto;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
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

    public void build(ZoneClient client, BulkOption... options) {
        EnumSet<BulkOption> optionsSet = options.length == 0 ?
                EnumSet.noneOf(BulkOption.class) :
                EnumSet.copyOf(Arrays.asList(options));
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
            log.info("Disabling instrumentation");
            allZones.forEach(z -> client.setInstrumentationEnabled(z, false));

            if (!optionsSet.contains(BulkOption.NO_BULK_VERTICES)) {
                buildVerticesBulk(client);
            } else {
                buildVertices(client);
            }

            if (!optionsSet.contains(BulkOption.NO_BULK_EDGES)) {
                buildEdgesBulk(client);
            } else {
                buildEdges(client);
            }

            log.debug("Waiting for index to be built: {}", allZones);
            while (indexNotReady(allZones)) {
                log.trace("Index not built");
                sleep();
            }
        } finally {
            supervisor.interrupt();
            log.info("Enabling instrumentation");
            allZones.forEach(z -> client.setInstrumentationEnabled(z, true));
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

    private void buildVerticesBulk(ZoneClient client) {
        Map<ZoneId, List<Vertex>> groupedByOwner = graph.allVertices()
                .stream()
                .collect(Collectors.groupingBy(v -> v.id().owner()));

        for (ZoneId owner : groupedByOwner.keySet()) {
            log.debug("Sending batches of vertices to {}", owner);
            List<Vertex> vertices = groupedByOwner.get(owner);
            for (List<Vertex> bulk : Lists.partition(vertices, BULK_SIZE)) {
                List<VertexCreationRequestDto> requests = bulk.stream()
                        .map(v -> new VertexCreationRequestDto(v.id().name(), v.type()))
                        .collect(Collectors.toList());
                log.debug("Sending a batch of {} vertices to {}", requests.size(), owner);
                client.addVertices(owner, new BulkVertexCreationRequestDto(requests));
                verticesBuilt.addAndGet(requests.size());
            }
            log.debug("Finished sending batches of vertices to {}", owner);
        }
    }

    private void buildVertices(ZoneClient client) {
        graph.allVertices()
                .stream()
                .parallel()
                .forEach(v -> {
                    client.addVertex(v.id(), v.type());
                    verticesBuilt.incrementAndGet();
                });
    }

    private void buildEdgesBulk(ZoneClient client) {
        Map<Pair<ZoneId, ZoneId>, List<Edge>> grouped = graph.allEdges()
                .stream()
                .collect(Collectors.groupingBy(e -> Pair.of(e.src().owner(), e.dst().owner())));

        for (Pair<ZoneId, ZoneId> pair : grouped.keySet()) {
            log.debug("Sending batches of edges between {} and {}",
                    pair.getLeft(), pair.getRight());
            List<Edge> edges = grouped.get(pair);
            for (List<Edge> bulk : Lists.partition(edges, BULK_SIZE)) {
                List<EdgeCreationRequestDto> requests = bulk.stream()
                        .map(e -> EdgeCreationRequestDto.fromEdge(e, null))
                        .collect(Collectors.toList());
                log.debug("Sending a batch of {} edges between {} and {}", requests.size(), pair.getLeft(), pair.getRight());
                client.addEdges(pair.getLeft(), BulkEdgeCreationRequestDto.builder()
                        .sourceZone(pair.getLeft())
                        .destinationZone(pair.getRight())
                        .successive(false)
                        .edges(requests)
                        .build());
                edgesBuilt.addAndGet(requests.size());
            }
            log.debug("Finished sending batches of edges between {} and {}",
                    pair.getLeft(), pair.getRight());
        }
    }

    private void buildEdges(ZoneClient client) {
        graph.allEdges()
                .stream()
                .sorted(Comparator.comparing(Edge::src)
                        .thenComparing(Edge::dst))
                .parallel()
                .forEach(e -> {
                    client.addEdge(e.src().owner(), e.id(), e.permissions());
                    edgesBuilt.incrementAndGet();
                });
    }

    public enum BulkOption {
        NO_BULK_EDGES,
        NO_BULK_VERTICES,
    }
}
