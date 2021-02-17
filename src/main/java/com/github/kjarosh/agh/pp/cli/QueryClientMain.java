package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.rest.client.GraphQueryClient;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.EffectivePermissionsResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.MembersResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.ReachesResponseDto;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
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
    private static int existing = 0;

    private static List<Duration> times = new ArrayList<>();
    private static List<Integer> results = new ArrayList<>();
    private static double existingRatio;

    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) throws ParseException, TimeoutException {
        Options options = new Options();
        options.addRequiredOption("g", "graph", true, "path to graph");
        options.addRequiredOption("t", "op-type", true, "operation type");
        options.addRequiredOption("d", "duration-seconds", true, "stop queries after the given number of seconds");
        options.addOption(null, "naive", false, "naive");
        options.addOption(null, "existing", true, "existing ratio");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        graph = GraphLoader.loadGraph(cmd.getOptionValue("g"));
        operationType = cmd.getOptionValue("t");
        naive = cmd.hasOption("naive");
        existingRatio = Double.parseDouble(cmd.getOptionValue("existing", "0"));
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
        Instant deadline = durationSeconds > 0 ?
                Instant.now().plus(durationSeconds, ChronoUnit.SECONDS) :
                Instant.MAX;
        while (!Thread.interrupted() && Instant.now().isBefore(deadline)) {
            try {
                performRequest(client);
            } catch (Exception e) {
                log.error("Error performing request", e);
            }
        }
        log.info("Performed {} requests ({} existing)", times.size(), existing);
        logStats(times, Duration::plus, Duration::dividedBy);
        log.info("Result stats:");
        logStats(results, Integer::sum, (a, b) -> (double) a / b);
    }

    private static <T extends Comparable<T>> void logStats(List<T> data, BinaryOperator<T> accumulator, BiFunction<T, Integer, Object> divide) {
        log.info("  min {}", data.stream().min(Comparator.comparing(Function.identity())).orElseThrow());
        T sum = data.stream().reduce(accumulator).orElseThrow();
        log.info("  avg {}", divide.apply(sum, data.size()));
        log.info("  max {}", data.stream().max(Comparator.comparing(Function.identity())).orElseThrow());
    }

    private static void performRequest(GraphQueryClient client) {
        VertexId from;
        if (operationType.equals("members")) {
            ++existing;
            from = randomVertex(Vertex.Type.GROUP, Vertex.Type.SPACE, Vertex.Type.PROVIDER);
            performRequest0(client, from, null);
            return;
        }

        VertexId to;
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
            ++existing;
        }

        performRequest0(client, from, to);
    }

    private static VertexId findRandomPath(VertexId from) {
        VertexId to = from;
        while(!graph.getEdgesBySource(to).isEmpty()){
            to = RandomUtils.randomElement(random, graph.getEdgesBySource(to)).dst();
        }
        return to;
    }

    private static VertexId randomVertex(Vertex.Type... types) {
        Set<Vertex.Type> s = new HashSet<>(Arrays.asList(types));
        Vertex v;
        do {
            v = RandomUtils.randomElement(random, graph.allVertices());
        } while (!s.contains(v.type()));
        return v.id();
    }

    private static void performRequest0(GraphQueryClient client, VertexId from, VertexId to) {
        switch (operationType) {
            case "reaches": {
                ReachesResponseDto response = client.reaches(from.owner(), new EdgeId(from, to));
                boolean reaches = response.isReaches();
                times.add(response.getDuration());
                results.add(reaches ? 1 : 0);
                break;
            }

            case "members": {
                MembersResponseDto response = client.members(from.owner(), from);
                List<String> members = response.getMembers();
                times.add(response.getDuration());
                results.add(members.size());
                break;
            }

            case "ep": {
                EffectivePermissionsResponseDto response = client.effectivePermissions(from.owner(), new EdgeId(from, to));
                String ep = response.getEffectivePermissions();
                times.add(response.getDuration());
                results.add(ep != null ? 1 : 0);
                break;
            }

            default:
                throw new RuntimeException("Unknown op: " + operationType);
        }
    }
}
