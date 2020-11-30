package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Kamil Jarosz
 */
public class RandomOperationIssuer {
    private static final Logger logger = LoggerFactory.getLogger(RandomOperationIssuer.class);

    private final Lock performLock = new ReentrantLock();
    private final Random random = new Random();
    private final Graph graph;
    private final ZoneId zone;
    private final Set<Edge> removedEdges = new HashSet<>();

    // config
    private double permissionsProbability = 0.8;
    private OperationIssuer operationIssuer = new ZoneClient();

    public RandomOperationIssuer(Graph graph, ZoneId zone) {
        this.graph = graph;
        this.zone = zone;
    }

    public RandomOperationIssuer withPermissionsProbability(double permissionsProbability) {
        this.permissionsProbability = permissionsProbability;
        return this;
    }

    public RandomOperationIssuer withOperationIssuer(OperationIssuer operationIssuer) {
        this.operationIssuer = operationIssuer;
        return this;
    }

    public void perform() {
        Lock lock = performLock;
        if (!lock.tryLock()) {
            logger.warn("Can't keep up");
            lock.lock();
        }
        try {
            perform0();
        } finally {
            lock.unlock();
        }
    }

    private void perform0() {
        boolean mustAddEdge = graph.allEdges().isEmpty();
        boolean mustRemoveEdge = removedEdges.isEmpty();

        if (mustAddEdge && mustRemoveEdge) {
            logger.error("Cannot perform random operation: empty graph");
            return;
        }

        if (!mustAddEdge && random.nextDouble() < permissionsProbability) {
            EdgeId id = randomElement(graph.allEdges()).id();
            logger.debug("Changing permissions of {}", id);
            operationIssuer.setPermissions(zone, id, randomPermissions());
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

    private void addEdge() {
        Edge e = randomElement(removedEdges);
        removedEdges.remove(e);
        graph.addEdge(e);
        logger.debug("Adding edge {}", e);
        operationIssuer.addEdge(zone, e.id(), randomPermissions());
    }

    private void removeEdge() {
        Edge e = randomElement(graph.allEdges());
        graph.removeEdge(e);
        removedEdges.add(e);
        logger.debug("Removing edge {}", e);
        operationIssuer.removeEdge(zone, e.id());
    }

    private <X> X randomElement(Collection<? extends X> collection) {
        List<X> list = new ArrayList<>(collection);
        return list.get(random.nextInt(list.size()));
    }

    private Permissions randomPermissions() {
        return Permissions.random(random);
    }
}
