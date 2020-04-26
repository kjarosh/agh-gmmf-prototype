package com.github.kjarosh.agh.pp.graph;

import com.github.kjarosh.agh.pp.SpringApp;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @author Kamil Jarosz
 */
@Component
@Scope("singleton")
public class GraphLoader {
    private static final Logger logger = LoggerFactory.getLogger(GraphLoader.class);

    public Graph graph;

    @PostConstruct
    public void init() {
        logger.debug("Loading graph");
        try (InputStream is = SpringApp.class.getClassLoader()
                .getResourceAsStream("graph.json")) {
            graph = Graph.deserialize(is);
            logger.info("Graph loaded: " + graph);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Graph getGraph() {
        return graph;
    }
}
