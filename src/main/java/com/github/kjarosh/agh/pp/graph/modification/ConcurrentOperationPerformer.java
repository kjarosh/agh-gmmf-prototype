package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.*;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.util.RandomUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class ConcurrentOperationPerformer implements IOperationPerformer {
    private final Random random;
    private final Graph graph;
    private final Set<Edge> removedEdges = new HashSet<>();
    private final AtomicLong traceCounter = new AtomicLong();
    private ZoneId zone;
    // config
    private double permissionsProbability = 0.8;
    private OperationIssuer operationIssuer = new ZoneClient();

    public ConcurrentOperationPerformer(Graph graph) {
        this(new Random(), graph);
    }

    public ConcurrentOperationPerformer(Random random, Graph graph) {
        this.random = random;
        this.graph = graph;
    }

    public ConcurrentOperationPerformer withPermissionsProbability(double permissionsProbability) {
        this.permissionsProbability = permissionsProbability;
        return this;
    }
    @Override
    public void setZone(ZoneId zone) { this.zone = zone; }
    @Override
    public ConcurrentOperationPerformer withOperationIssuer(OperationIssuer operationIssuer) {
        this.operationIssuer = operationIssuer;
        return this;
    }

    @Override
    public synchronized void perform() {
        perform0();
    }

    private void perform0() {
        boolean mustAddEdge = graph.allEdges().isEmpty();
        boolean mustRemoveEdge = removedEdges.isEmpty();

        if (mustAddEdge && mustRemoveEdge) {
            log.error("Cannot perform random operation: empty graph");
            return;
        }

        if (!mustAddEdge && random.nextDouble() < permissionsProbability) {
            EdgeId id = RandomUtils.randomElement(random, graph.allEdges().stream().filter(edge -> edge.src().owner().equals(zone)).collect(Collectors.toList())).id();
            log.debug("Changing permissions of {}", id);
            operationIssuer.setPermissions(id.getFrom().owner(), id, randomPermissions(), trace());
            return;
        }

        if (mustRemoveEdge) {
            removeEdge();
            return;
        } else if (mustAddEdge) {
            addEdge();
            return;
        }

        if (random.nextBoolean()) {
            removeEdge();
        } else {
            addEdge();
        }
    }

    String trace() {
        return "generated-" + String.format("%05d", traceCounter.getAndIncrement()) + "-" + UUID.randomUUID().toString();
    }

    private void addEdge() {
        Edge e = RandomUtils.randomElement(random, removedEdges);
        removedEdges.remove(e);
        graph.addEdge(e);
        log.debug("Adding edge {}", e);
        operationIssuer.addEdge(zone, e.id(), randomPermissions(), trace());
    }

    private void removeEdge() {
        Edge e = RandomUtils.randomElement(random, graph.allEdges().stream().filter(edge -> edge.src().owner().equals(zone)).collect(Collectors.toList()));
        graph.removeEdge(e);
        removedEdges.add(e);
        log.debug("Removing edge {}", e);
        operationIssuer.removeEdge(zone, e.id(), trace());
    }

    private Permissions randomPermissions() {
        return Permissions.random(random);
    }
}
