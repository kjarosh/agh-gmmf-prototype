package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.generator.SequenceOperationIssuer;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.*;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.rest.client.EventStatsGatherer;
import com.github.kjarosh.agh.pp.rest.client.RemoteGraphBuilder;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.*;
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

    //private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(treadFactory);
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
    private static ArrayList<IOperationPerformer> operationPerformers = new ArrayList<>();
    private static ConcurrentOperationIssuer baseOperationIssuer;
    private static OperationIssuer operationIssuer;

    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) throws ParseException, TimeoutException, IOException, InterruptedException {
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

            if(skipLoad) {
                log.info("Skipping constant load part");
                return;
            }
        }

        baseOperationIssuer = new ConcurrentOperationIssuer(maxPoolSize, zoneClient);

        //if (cmd.hasOption("s")) {
        // tu trzeba dodac tyle operationissuerow ile jest zone'ow
        //    operationPerformers.add(new SequenceOperationIssuer(cmd.getOptionValue("s")));

        //} else {
        //tu trzeba dodac tyle operationissuerow ile jest zone'ow
        for(ZoneId zone: graph.allZones()) {
            if (bulkSize >= 1) {
                operationIssuer = new BulkOperationIssuer(baseOperationIssuer, bulkSize);
            } else {
                operationIssuer = new ConcurrentOperationIssuer(maxPoolSize, zoneClient);
            }
            ConcurrentOperationPerformer concurrentOperationPerformer = new ConcurrentOperationPerformer(graph);
            concurrentOperationPerformer = concurrentOperationPerformer.withOperationIssuer(operationIssuer);
            concurrentOperationPerformer.setZone(zone);
            operationPerformers.add(concurrentOperationPerformer);
        }
        //}

        // tu trzeba kazdemu pododawac zone i operationIssuera
        //operationPerformers = operationPerformer.withOperationIssuer(operationIssuer);

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

    private static void runRandomOperations() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger errored = new AtomicInteger(0);

        LinkedBlockingQueue<AtomicInteger> countQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<AtomicInteger> errorQueue = new LinkedBlockingQueue<>();
        Thread counter = countCollector(count, errored, countQueue, errorQueue);
        counter.start();
        ArrayList<ScheduledExecutorService> executorServiceArrayList = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        threads.add(counter);
        for(IOperationPerformer performer: operationPerformers) {
            ScheduledExecutorService scheduledZoneExecutor = Executors.newSingleThreadScheduledExecutor(treadFactory);
            executorServiceArrayList.add(scheduledZoneExecutor);
            Thread zoneExecutor = zoneExecutor(performer, scheduledZoneExecutor, countQueue, errorQueue);
            threads.add(zoneExecutor);
            zoneExecutor.start();
        }
        // scheduleRequestExecutor(count, errored);

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

        counter.join();
        for(Thread thread: threads){
            thread.join();
        }

        log.info("Shutting down gracefully...");
        for(ScheduledExecutorService executorService: executorServiceArrayList){
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.info("Interrupted. Exiting...");
            }
        }

        log.info("Finished");
    }

    private static Thread zoneExecutor(IOperationPerformer performer, ScheduledExecutorService scheduledExecutor, LinkedBlockingQueue<AtomicInteger> countQueue, LinkedBlockingQueue<AtomicInteger> errorQueue ){
        return new Thread(() ->{
            if (operationsPerSecond == 0) return;

            long period = (long) (1e9 / operationsPerSecond);
            scheduledExecutor.scheduleAtFixedRate(() -> {
                try {
                    performer.perform();
                } catch (Throwable t) {
                    log.error("Error while performing operation", t);
                    errorQueue.add(new AtomicInteger(1));
                    if (exitOnFail) System.exit(1);
                }
                countQueue.add(new AtomicInteger(1));
            }, 0, period, TimeUnit.NANOSECONDS);
        });
    }
    private static Thread countCollector(AtomicInteger count, AtomicInteger errored, LinkedBlockingQueue<AtomicInteger> countQueue, LinkedBlockingQueue<AtomicInteger> errorQueue ){
        return new Thread(() ->{
            if (operationsPerSecond == 0) return;

            while(true){
                while(!countQueue.isEmpty()){
                    count.incrementAndGet();
                    countQueue.peek();
                }
                while(!errorQueue.isEmpty()){
                    errored.incrementAndGet();
                    errorQueue.peek();
                }
            }
        });
    }
    /*
    private static void scheduleRequestExecutor(AtomicInteger count, AtomicInteger errored) {
        if (operationsPerSecond == 0) return;

        long period = (long) (1e9 / operationsPerSecond);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                operationPerformer.perform();
            } catch (Throwable t) {
                log.error("Error while performing operation", t);
                errored.incrementAndGet();
                if (exitOnFail) System.exit(1);
            }
            count.incrementAndGet();
        }, 0, period, TimeUnit.NANOSECONDS);
    }
    */
    private static String fd(double d) {
        return String.format("%.2f", d);
    }
}
