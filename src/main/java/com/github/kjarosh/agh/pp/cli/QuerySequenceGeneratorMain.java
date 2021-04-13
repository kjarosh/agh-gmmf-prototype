package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.query.QuerriesWriter;
import com.github.kjarosh.agh.pp.graph.util.QueryType;
import com.github.kjarosh.agh.pp.util.RandomUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.*;

public class QuerySequenceGeneratorMain {
    private static String filename;
    private static String operationType;
    private static QuerriesWriter writer = new QuerriesWriter();
    private static Random random = new Random();
    private static Graph graph;
    private static double existingRatio;

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("t", "op-type", true, "operation type");
        options.addOption("g", "graph", true, "Path to graph json file");
        options.addOption("n", "amount", true, "Amount of requests to be generated");
        options.addOption("o", "output", true, "Output file path");
        options.addOption("e", "existing", true, "existing ratio");
        CommandLine cmd = new DefaultParser().parse(options, args);

        if(!cmd.hasOption("n")) {
            throw new RuntimeException("Argument 'n' is required");
        }

        graph = GraphLoader.loadGraph(cmd.getOptionValue("g", "graph.json"));
        filename = cmd.getOptionValue("o", "output.json");
        operationType = cmd.getOptionValue("t");
        int amount = Integer.parseInt(cmd.getOptionValue("n"));
        existingRatio = Double.parseDouble(cmd.getOptionValue("e", "0"));

        for(int n = 0; n < amount; n++) {
            generateQuery();
        }

        commitFile();
        System.out.printf("Sequence of %d operations has been generated. Output file: '%s'", amount, filename);
    }

    private static void commitFile() {
        writer.save(filename);
    }

    private static VertexId randomVertex(Vertex.Type... types) {
        Set<Vertex.Type> s = new HashSet<>(Arrays.asList(types));
        Vertex v;
        do {
            v = RandomUtils.randomElement(random, graph.allVertices());
        } while (!s.contains(v.type()));
        return v.id();
    }

    private static VertexId findRandomPath(VertexId from) {
        VertexId to = from;
        while(!graph.getEdgesBySource(to).isEmpty()){
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
            from = RandomUtils.randomElement(random, graph.allVertices()).id();
            to = RandomUtils.randomElement(random, graph.allVertices()).id();
        } else {
            Set<Edge> edgesBySource;
            do {
                from = RandomUtils.randomElement(random, graph.allVertices()).id();
                edgesBySource = graph.getEdgesBySource(from);
            } while (edgesBySource.isEmpty());
            to = findRandomPath(from);
            existing = true;
        }

        if (operationType.equals("reaches")) {
            writer.reaches(from, to, existing);
        } else if (operationType.equals("ep")) {
            writer.effectivePermisions(from, to, existing);
        }
    }
}
