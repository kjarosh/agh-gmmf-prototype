package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.rest.client.GraphQueryClient;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.util.RandomUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class QueryClientMain {
    private static Random random = new Random();
    private static int durationSeconds;
    private static Graph graph;
    private static String operationType;
    private static boolean naive;

    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) throws ParseException, TimeoutException {
        Options options = new Options();
        options.addRequiredOption("g", "graph", true, "path to graph");
        options.addRequiredOption("t", "op-type", true, "operation type");
        options.addRequiredOption("d", "duration-seconds", true, "stop queries after the given number of seconds");
        options.addOption(null, "naive", false, "naive");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        graph = GraphLoader.loadGraph(cmd.getOptionValue("g"));
        operationType = cmd.getOptionValue("t");
        naive = cmd.hasOption("naive");
        durationSeconds = Integer.parseInt(cmd.getOptionValue("d", "-1"));
        ZoneClient zoneClient = new ZoneClient();

        log.info("Running queries for {}", durationSeconds);
        log.info(" naive={}", naive);
        log.info(" operationType={}", operationType);

        if (naive) {
            runQueries(zoneClient.naive());
        } else {
            runQueries(zoneClient.indexed());
        }
    }

    private static void runQueries(GraphQueryClient client) {
        List<Duration> times = new ArrayList<>();
        Instant deadline = durationSeconds > 0 ?
                Instant.now().plus(durationSeconds, ChronoUnit.SECONDS) :
                Instant.MAX;
        while (!Thread.interrupted() && Instant.now().isBefore(deadline)) {
            long start = System.nanoTime();
            performRequest(client);
            long end = System.nanoTime();
            Duration time = Duration.ofNanos(end - start);
            times.add(time);
        }
        log.info("Performed {} requests", times.size());
        log.info("  min {}", times.stream().min(Comparator.comparing(Function.identity())).orElseThrow());
        log.info("  avg {}", times.stream().reduce(Duration::plus).orElseThrow().dividedBy(times.size()));
        log.info("  max {}", times.stream().max(Comparator.comparing(Function.identity())).orElseThrow());
    }

    private static void performRequest(GraphQueryClient client) {
        VertexId from;
        VertexId to;
        if (true) {
            from = RandomUtils.randomElement(random, graph.allVertices()).id();
            to = RandomUtils.randomElement(random, graph.allVertices()).id();
        } else {
            from = RandomUtils.randomElement(random, graph.allVertices()).id();
            Set<Edge> edgesBySource = graph.getEdgesBySource(from);
            if (edgesBySource.isEmpty()) {
                to = RandomUtils.randomElement(random, graph.allVertices()).id();
            } else {
                to = RandomUtils.randomElement(random, edgesBySource).dst();
            }
        }
        performRequest0(client, from, to);
    }

    private static void performRequest0(GraphQueryClient client, VertexId from, VertexId to) {
        switch (operationType) {
            case "reaches":
                client.reaches(from.owner(), new EdgeId(from, to));
                break;
            case "members":
                client.members(from.owner(), from);
                break;
            case "ep":
                client.effectivePermissions(from.owner(), new EdgeId(from, to));
                break;
            default:
                throw new RuntimeException("Unknown op: " + operationType);
        }
    }
}
