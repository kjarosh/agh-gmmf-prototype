package com.github.kjarosh.agh.pp.rest.client;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.EdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.VertexCreationRequestDto;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class RemoteGraphBuilder {
    private static final int BULK_SIZE = 5_000;
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
        Instant start = Instant.now();

        Collection<ZoneId> allZones = graph.allZones();

        log.info("Checking all zones if they are healthy: {}", allZones);
        while (!healthy(allZones)) {
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
            client.waitForIndex(allZones, Duration.ofHours(2));
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
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

        log.info("Graph built in {}", Duration.between(start, Instant.now()));
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while building graph");
        }
    }

    private boolean healthy(Collection<ZoneId> allZones) {
        List<ZoneId> notHealthy = allZones.stream()
                .filter(zone -> !client.healthcheck(zone))
                .collect(Collectors.toList());
        if (notHealthy.isEmpty()) {
            return true;
        } else {
            log.info("Zones not healthy: {}", notHealthy);
            return false;
        }
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
                    int n = edgesBuilt.getAndIncrement();
                    String trace = "builder-edge-" + String.format("%05d", n) + "-" + UUID.randomUUID().toString();
                    client.addEdge(e.src().owner(), e.id(), e.permissions(), trace);
                });
    }

    public enum BulkOption {
        NO_BULK_EDGES,
        NO_BULK_VERTICES,
    }
}
