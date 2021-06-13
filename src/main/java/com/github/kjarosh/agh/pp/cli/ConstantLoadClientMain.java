package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.generator.SequenceOperationIssuer;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.BulkOperationPerformer;
import com.github.kjarosh.agh.pp.graph.modification.ConcurrentOperationPerformer;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.modification.OperationPerformer;
import com.github.kjarosh.agh.pp.graph.modification.RandomOperationIssuer;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.rest.client.EventStatsGatherer;
import com.github.kjarosh.agh.pp.rest.client.RemoteGraphBuilder;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class ConstantLoadClientMain {
    private static final ThreadFactory treadFactory = new ThreadFactoryBuilder()
            .setNameFormat("generator-%d")
            .setDaemon(true)
            .build();

    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(treadFactory);
    private static boolean loadGraph;
    private static boolean exitOnFail;
    private static int bulkSize;
    private static int operationsPerSecond;
    private static int requestsPerSecond;
    private static Graph graph;
    private static double permsProbability;
    private static int maxPoolSize;
    private static int durationSeconds;
    private static boolean disableIndexation;

    private static OperationIssuer operationIssuer;
    private static ConcurrentOperationPerformer baseOperationIssuer;
    private static OperationPerformer operationPerformer;

    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) throws ParseException, TimeoutException, IOException {
        Options options = new Options();
        options.addRequiredOption("n", "operations", true, "number of operations per second");
        options.addRequiredOption("g", "graph", true, "path to graph");
        options.addOption("l", "load", false, "decide whether to load graph before running tests");
        options.addOption("x", "exit-on-fail", false, "exit on first fail");
        options.addOption("b", "bulk", true, "enable bulk requests and set bulk size");
        options.addOption("r", "requests", true, "enable bulk requests and set requests per second");
        options.addOption("t", "concurrent-pool", true, "enable concurrency and set pool size");
        options.addOption("d", "duration-seconds", true, "stop load after the given number of seconds");
        options.addOption("s", "sequence", true, "execute requests from this file");
        options.addOption(null, "prob.perms", true,
                "probability that a random operation changes permissions");
        options.addOption(null, "disable-indexation", false, "");
        options.addOption(null, "no-load", false, "");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        loadGraph = cmd.hasOption("l");
        exitOnFail = cmd.hasOption("x");
        boolean skipLoad = cmd.hasOption("no-load");
        operationsPerSecond = Integer.parseInt(cmd.getOptionValue("n"));
        bulkSize = Integer.parseInt(cmd.getOptionValue("b", "-1"));
        requestsPerSecond = Integer.parseInt(cmd.getOptionValue("r", "-1"));
        if (requestsPerSecond != -1 && bulkSize != -1) {
            throw new RuntimeException("-b and -r defined simultaneously");
        } else if (requestsPerSecond != -1) {
            bulkSize = operationsPerSecond / requestsPerSecond;
        } else if (bulkSize == -1) {
            bulkSize = 1;
        }
        maxPoolSize = Integer.parseInt(cmd.getOptionValue("t", "0"));
        graph = GraphLoader.loadGraph(cmd.getOptionValue("g"));
        permsProbability = Double.parseDouble(cmd.getOptionValue("prob.perms", "0.95"));
        durationSeconds = Integer.parseInt(cmd.getOptionValue("d", "-1"));
        disableIndexation = cmd.hasOption("disable-indexation");
        ZoneClient zoneClient = new ZoneClient();

        if (disableIndexation) {
            log.info("Disabling indexation");
            for (ZoneId z : graph.allZones()) {
                zoneClient.setIndexationEnabled(z, false);
            }
        }

        if (loadGraph) {
            loadGraph();

            if (skipLoad) {
                log.info("Skipping constant load part");
                return;
            }
        }

        baseOperationIssuer = new ConcurrentOperationPerformer(maxPoolSize, zoneClient);
        if (bulkSize >= 1) {
            operationPerformer = new BulkOperationPerformer(baseOperationIssuer, bulkSize);
        } else {
            operationPerformer = baseOperationIssuer;
        }

        if (cmd.hasOption("s")) {
            String sequenceFile = cmd.getOptionValue("s");
            InputStream is = Files.newInputStream(Paths.get(sequenceFile));
            operationIssuer = new SequenceOperationIssuer(is);
        } else {
            operationIssuer = new RandomOperationIssuer(graph)
                    .withPermissionsProbability(permsProbability);
        }

        operationIssuer.withOperationPerformer(operationPerformer);

        String durationSuffix = "";
        if (durationSeconds > 0) {
            durationSuffix = " for " + durationSeconds + " seconds";
        }
        log.info("Running constant load: {} requests per second{}", operationsPerSecond, durationSuffix);
        runRandomOperations();
    }

    private static void loadGraph() {
        ZoneClient client = new ZoneClient();
        new RemoteGraphBuilder(graph, client).build(client);
    }

    private static void runRandomOperations() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger errored = new AtomicInteger(0);
        scheduleRequestExecutor(count, errored);

        EventStatsGatherer eventStatsGatherer = new EventStatsGatherer(graph.allZones());

        Instant last = Instant.now();
        long total = 0;
        Instant deadline = durationSeconds > 0 ?
                Instant.now().plus(durationSeconds, ChronoUnit.SECONDS) :
                Instant.MAX;
        while (!Thread.interrupted() && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

            Instant now = Instant.now();

            EventStats stats = eventStatsGatherer.get();
            int currentCount = count.getAndSet(0);
            total += currentCount;
            double gps = (double) currentCount / Duration.between(last, now).toMillis() * Duration.ofSeconds(1).toMillis();
            last = now;
            log.info("{}  (gps={}, err={}, tot={}, sat={}, rt={})",
                    stats.toString(),
                    fd(gps),
                    errored.get() + baseOperationIssuer.getFailed(),
                    total,
                    fd(baseOperationIssuer.getSaturation()),
                    fd(baseOperationIssuer.getRequestTime()));
        }

        log.info("Shutting down gracefully...");
        scheduledExecutor.shutdown();
        try {
            scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("Interrupted. Exiting...");
        }
        log.info("Finished");
    }

    private static void scheduleRequestExecutor(AtomicInteger count, AtomicInteger errored) {
        if (operationsPerSecond == 0) return;

        long period = (long) (1e9 / operationsPerSecond);
        long atOnce = 1;
        while (period < TimeUnit.MILLISECONDS.toNanos(50)) {
            period *= 2;
            atOnce *= 2;
        }
        long atOnceFinal = atOnce;
        scheduledExecutor.scheduleAtFixedRate(() -> {
            for (int i = 0; i < atOnceFinal; ++i) {
                try {
                    operationIssuer.issue();
                } catch (Throwable t) {
                    log.error("Error while performing operation", t);
                    errored.incrementAndGet();
                    if (exitOnFail) System.exit(1);
                }
                count.incrementAndGet();
            }
        }, 0, period, TimeUnit.NANOSECONDS);
    }

    private static String fd(double d) {
        return String.format("%.2f", d);
    }
}
