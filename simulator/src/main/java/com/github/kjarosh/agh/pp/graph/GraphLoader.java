package com.github.kjarosh.agh.pp.graph;

import com.github.kjarosh.agh.pp.graph.model.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * A singleton that contains the service-wide graph representation.
 *
 * @author Kamil Jarosz
 */
@Service
@Scope("singleton")
public class GraphLoader {
    private static final Logger logger = LoggerFactory.getLogger(GraphLoader.class);

    public Graph graph;

    public static Graph loadGraph(String path) {
        logger.debug("Loading graph");
        try (InputStream is = GraphLoader.class.getClassLoader()
                .getResourceAsStream(path)) {
            return Graph.deserialize(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PostConstruct
    public void init() {
        graph = new Graph();
    }

    public Graph getGraph() {
        return graph;
    }
}
