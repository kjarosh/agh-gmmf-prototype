package com.github.kjarosh.agh.pp.graph;

import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.index.PersistenceFactory;
import com.github.kjarosh.agh.pp.memory.InMemoryGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A singleton that contains the service-wide graph representation.
 *
 * @author Kamil Jarosz
 */
@Slf4j
@Service
@Scope("singleton")
public class GraphLoader {
    private Graph graph;

    public static Graph loadGraph(String path) {
        log.debug("Loading graph");
        try (InputStream is = open(path)) {
            return InMemoryGraph.deserialize(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream open(String path) throws IOException {
        InputStream is = GraphLoader.class.getClassLoader()
                .getResourceAsStream(path);
        if (is != null) return is;

        return Files.newInputStream(Paths.get(path));
    }

    @PostConstruct
    public void init() {
        graph = PersistenceFactory.getInstance().createGraph();
    }

    public Graph getGraph() {
        return graph;
    }
}
