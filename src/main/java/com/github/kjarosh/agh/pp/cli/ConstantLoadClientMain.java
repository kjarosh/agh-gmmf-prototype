package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.test.EventStatsGatherer;
import com.github.kjarosh.agh.pp.test.RemoteGraphBuilder;
import com.github.kjarosh.agh.pp.test.util.RandomOperationIssuer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Kamil Jarosz
 */
public class ConstantLoadClientMain {
    private static final Logger logger = LoggerFactory.getLogger(ConstantLoadClientMain.class);

    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("n", "requests", true, "number of requests per second");
        options.addRequiredOption("z", "zone-id", true, "zone ID");
        options.addRequiredOption("g", "graph", true, "path to graph");
        options.addRequiredOption("l", "load", false, "decide whether to load graph before running tests");
        options.addOption(null, "prob.perms", true,
                "probability that a random operation changes permissions");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        boolean load = cmd.hasOption("l");
        int requestsPerSecond = Integer.parseInt(cmd.getOptionValue("n"));
        ZoneId zone = new ZoneId(cmd.getOptionValue("z"));
        Graph graph = GraphLoader.loadGraph(cmd.getOptionValue("g"));
        double permsProbability = Double.parseDouble(cmd.getOptionValue("prob.perms", "0.8"));

        if (load) {
            load(graph, zone);
        }

        runRandomOperations(requestsPerSecond, zone, graph, permsProbability);
    }

    private static void load(Graph graph, ZoneId zone) {
        ZoneClient client = new ZoneClient();
        new RemoteGraphBuilder(graph, client).build(client, zone);
    }

    private static void runRandomOperations(int requestsPerSecond, ZoneId zone, Graph graph, double permsProbability) {
        RandomOperationIssuer randomOperationIssuer =
                new RandomOperationIssuer(graph, zone);
        randomOperationIssuer.setPermissionsProbability(permsProbability);
        randomOperationIssuer.setExecutor(Executors.newFixedThreadPool(24));

        long period = (long) (1e9 / requestsPerSecond);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                randomOperationIssuer.perform();
            } catch (Throwable t) {
                logger.error("Error while performing operation", t);
            }
        }, 0, period, TimeUnit.NANOSECONDS);

        EventStatsGatherer eventStatsGatherer = new EventStatsGatherer(zone);

        while (!Thread.interrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

            EventStats stats = eventStatsGatherer.get();
            logger.info(stats.toString());
        }

        logger.info("Interrupted. Shutting down gracefully...");
        scheduledExecutor.shutdown();
        try {
            scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.info("Interrupted. Exiting...");
        }
    }
}
