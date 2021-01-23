package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class RandomOperationIssuer {
    private final Random random;
    private final Graph graph;
    private final Set<Edge> removedEdges = new HashSet<>();
    private final AtomicLong traceCounter = new AtomicLong();

    // config
    private double permissionsProbability = 0.8;
    private OperationIssuer operationIssuer = new ZoneClient();

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

    public RandomOperationIssuer withOperationIssuer(OperationIssuer operationIssuer) {
        this.operationIssuer = operationIssuer;
        return this;
    }

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
            EdgeId id = randomElement(graph.allEdges()).id();
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
        Edge e = randomElement(removedEdges);
        removedEdges.remove(e);
        graph.addEdge(e);
        log.debug("Adding edge {}", e);
        operationIssuer.addEdge(e.src().owner(), e.id(), randomPermissions(), trace());
    }

    private void removeEdge() {
        Edge e = randomElement(graph.allEdges());
        graph.removeEdge(e);
        removedEdges.add(e);
        log.debug("Removing edge {}", e);
        operationIssuer.removeEdge(e.src().owner(), e.id(), trace());
    }

    private <X> X randomElement(Collection<? extends X> collection) {
        if (collection instanceof List) {
            return randomElementList((List<? extends X>) collection);
        } else {
            return randomElementCollection(collection);
        }
    }

    <X> X randomElementList(List<? extends X> list) {
        return list.get(random.nextInt(list.size()));
    }

    <X> X randomElementCollection(Collection<? extends X> collection) {
        int ix = random.nextInt(collection.size());
        Iterator<? extends X> it = collection.iterator();
        while (ix-- > 0) {
            it.next();
        }
        return it.next();
    }

    private Permissions randomPermissions() {
        return Permissions.random(random);
    }
}
