package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.util.RandomUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class RandomOperationIssuer implements OperationIssuer {
    private final Random random;
    private final Graph graph;
    private final Set<Edge> removedEdges = new HashSet<>();
    private final AtomicLong traceCounter = new AtomicLong();

    // config
    private double permissionsProbability = 0.8;
    private OperationPerformer operationPerformer = new ZoneClient();

    public RandomOperationIssuer(Graph graph) {
        this(new Random(), graph);
    }

    public RandomOperationIssuer(Random random, Graph graph) {
        this.random = random;
        this.graph = graph;
    }

    public RandomOperationIssuer withPermissionsProbability(double permissionsProbability) {
        this.permissionsProbability = permissionsProbability;
        return this;
    }

    public RandomOperationIssuer withOperationPerformer(OperationPerformer operationPerformer) {
        this.operationPerformer = operationPerformer;
        return this;
    }

    public synchronized void issue() {
        issue0();
    }

    private void issue0() {
        boolean mustAddEdge = graph.allEdges().isEmpty();
        boolean mustRemoveEdge = removedEdges.isEmpty();

        if (mustAddEdge && mustRemoveEdge) {
            log.error("Cannot perform random operation: empty graph");
            return;
        }

        if (!mustAddEdge && random.nextDouble() < permissionsProbability) {
            EdgeId id = RandomUtils.randomElement(random, graph.allEdges()).id();
//            log.debug("Changing permissions of {}", id);
            operationPerformer.setPermissions(id.getFrom().owner(), id, randomPermissions(), trace());
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
//        log.debug("Adding edge {}", e);
        operationPerformer.addEdge(e.src().owner(), e.id(), randomPermissions(), trace());
    }

    private void removeEdge() {
        Edge e = RandomUtils.randomElement(random, graph.allEdges());
        graph.removeEdge(e);
        removedEdges.add(e);
//        log.debug("Removing edge {}", e);
        operationPerformer.removeEdge(e.src().owner(), e.id(), trace());
    }

    private Permissions randomPermissions() {
        return Permissions.random(random);
    }
}
