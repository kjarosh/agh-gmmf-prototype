package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.query.QueriesWriter;
import com.github.kjarosh.agh.pp.util.RandomUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
public class QuerySequenceGeneratorMain {
    private static final Random random = new Random();
    private static String operationType;
    private static QueriesWriter writer;
    private static Graph graph;
    private static List<Vertex> graphVertices;
    private static double existingRatio;

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addRequiredOption("t", "op-type", true, "operation type");
        options.addRequiredOption("n", "count", true, "number of requests to be generated");
        options.addOption("g", "graph", true, "path to graph json file");
        options.addOption("o", "output", true, "output file path");
        options.addOption("e", "existing", true, "existing ratio");
        CommandLine cmd = new DefaultParser().parse(options, args);

        graph = GraphLoader.loadGraph(cmd.getOptionValue("g", "graph.json"));
        graphVertices = new ArrayList<>(graph.allVertices());
        String outputPath = cmd.getOptionValue("o", "output.jsonl");
        operationType = cmd.getOptionValue("t");
        int count = Integer.parseInt(cmd.getOptionValue("n"));
        existingRatio = Double.parseDouble(cmd.getOptionValue("e", "0"));

        try (OutputStream os = Files.newOutputStream(Paths.get(outputPath), StandardOpenOption.CREATE_NEW)) {
            writer = new QueriesWriter(os);
            AtomicInteger generated = new AtomicInteger(0);
            IntStream.range(0, count)
                    .parallel()
                    .forEach(i -> {
                        generateQuery();
                        int g = generated.incrementAndGet();
                        if (g % 10_000 == 0) {
                            log.info("Generated: {} of {}", g, count);
                        }
                    });
        }

        log.info("Sequence of {} operations has been generated. Output file: {}", count, outputPath);
    }

    private static VertexId randomVertex(Vertex.Type... types) {
        Set<Vertex.Type> s = new HashSet<>(Arrays.asList(types));
        Vertex v;
        do {
            v = RandomUtils.randomElement(random, graphVertices);
        } while (!s.contains(v.type()));
        return v.id();
    }

    private static VertexId findRandomPath(VertexId from) {
        VertexId to = from;
        while (!graph.getEdgesBySource(to).isEmpty()) {
            to = RandomUtils.randomElement(random, graph.getEdgesBySource(to)).dst();
        }
        return to;
    }

    private static void generateQuery() {
        VertexId from;
        if (operationType.equals("members")) {
            from = randomVertex(Vertex.Type.GROUP, Vertex.Type.SPACE, Vertex.Type.PROVIDER);
            writer.member(from);
            return;
        }

        VertexId to;
        boolean existing = false;
        if (random.nextDouble() >= existingRatio) {
            from = RandomUtils.randomElement(random, graphVertices).id();
            to = RandomUtils.randomElement(random, graphVertices).id();
        } else {
            Set<Edge> edgesBySource;
            do {
                from = RandomUtils.randomElement(random, graphVertices).id();
                edgesBySource = graph.getEdgesBySource(from);
            } while (edgesBySource.isEmpty());
            to = findRandomPath(from);
            existing = true;
        }

        if (operationType.equals("reaches")) {
            writer.reaches(from, to, existing);
        } else if (operationType.equals("ep")) {
            writer.effectivePermissions(from, to, existing);
        }
    }
}
