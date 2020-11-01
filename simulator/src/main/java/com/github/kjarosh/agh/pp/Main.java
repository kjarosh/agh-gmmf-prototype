package com.github.kjarosh.agh.pp;

import com.github.kjarosh.agh.pp.cli.Cmd;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import com.github.kjarosh.agh.pp.test.RemoteGraphBuilder;
import com.github.kjarosh.agh.pp.test.Tester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author Kamil Jarosz
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Too few arguments");
            printHelp();
            System.exit(1);
            return;
        }

        switch (args[0]) {
            case "test": {
                if (args.length != 2) {
                    logger.error("Invalid number of arguments for test");
                    printHelp();
                    System.exit(1);
                    return;
                }

                logger.info("Running tester");

                String testedZone = args[1];
                logger.debug("Testing zone '{}'", testedZone);

                List<String> allZones = Arrays.asList(System.getenv("ALL_ZONES").split(","));
                logger.debug("All zones: {}", allZones);
                Tester.main(testedZone, allZones);
                break;
            }

            case "client": {
                if (args.length != 1) {
                    logger.error("Invalid number of arguments for client");
                    printHelp();
                    System.exit(1);
                    return;
                }

                logger.info("Running client");
                Cmd.main(args);
                break;
            }

            case "load": {
                if (args.length != 3) {
                    logger.error("Invalid number of arguments for load");
                    printHelp();
                    System.exit(1);
                    return;
                }

                String zone = args[1];
                String graphPath = args[2];
                logger.info("Loading graph {} into zone {}", graphPath, zone);

                Graph graph = GraphLoader.loadGraph(graphPath);
                ZoneClient client = new ZoneClient();
                new RemoteGraphBuilder(graph, client, null).build(client, new ZoneId(zone));

                logger.info("Graph loaded");
                break;
            }

            case "server": {
                if (args.length != 1) {
                    logger.error("Invalid number of arguments for server");
                    printHelp();
                    System.exit(1);
                    return;
                }

                logger.info("Running server");
                SpringApp.main(args);
                break;
            }

            default: {
                logger.error("Unknown command: {}", args[0]);
                System.exit(1);
                break;
            }
        }
    }

    private static void printHelp() {
        logger.info("Usage:");
        logger.info("  app test <zone id>               -- run tests on the given zone");
        logger.info("  app client                       -- run CLI client");
        logger.info("  app server                       -- run server");
        logger.info("  app load <zone id> <graph path>  -- run server");
    }
}
