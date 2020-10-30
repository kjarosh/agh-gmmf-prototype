package com.github.kjarosh.agh.pp.test.strategy;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import com.github.kjarosh.agh.pp.test.RemoteGraphBuilder;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * @author Kamil Jarosz
 */
@Builder
public class TestContext {
    private static final Logger logger = LoggerFactory.getLogger(TestContext.class);

    private final ZoneClient client;
    private final ZoneId zone;
    private final List<ZoneId> allZones;

    public Graph buildGraph(String graphPath) {
        logger.info("Building graph");
        Graph graph = GraphLoader.loadGraph(graphPath);
        new RemoteGraphBuilder(graph, client, allZones).build(client, zone);
        logger.info("Graph built");
        return graph;
    }

    public ZoneClient getClient() {
        return client;
    }

    public ZoneId getZone() {
        return zone;
    }

    public List<ZoneId> getAllZones() {
        return Collections.unmodifiableList(allZones);
    }
}