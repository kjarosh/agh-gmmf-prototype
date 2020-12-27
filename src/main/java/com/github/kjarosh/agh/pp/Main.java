package com.github.kjarosh.agh.pp;

import com.github.kjarosh.agh.pp.cli.Cmd;
import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.test.RemoteGraphBuilder;
import com.github.kjarosh.agh.pp.test.Tester;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class Main {
    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            log.error("Too few arguments");
            printHelp();
            System.exit(1);
            return;
        }

        switch (args[0]) {
            case "test": {
                if (args.length != 2) {
                    log.error("Invalid number of arguments for test");
                    printHelp();
                    System.exit(1);
                    return;
                }

                log.info("Running tester");

                String testedZone = args[1];
                log.debug("Testing zone '{}'", testedZone);
                Tester.main(testedZone);
                break;
            }

            case "client": {
                if (args.length != 1) {
                    log.error("Invalid number of arguments for client");
                    printHelp();
                    System.exit(1);
                    return;
                }

                log.info("Running client");
                Cmd.main(args);
                break;
            }

            case "load": {
                if (args.length != 3) {
                    log.error("Invalid number of arguments for load");
                    printHelp();
                    System.exit(1);
                    return;
                }

                String zone = args[1];
                String graphPath = args[2];
                log.info("Loading graph {} into zone {}", graphPath, zone);

                Graph graph = GraphLoader.loadGraph(graphPath);
                ZoneClient client = new ZoneClient();
                new RemoteGraphBuilder(graph, client).build(client, new ZoneId(zone));

                log.info("Graph loaded");
                break;
            }

            case "server": {
                if (args.length != 1) {
                    log.error("Invalid number of arguments for server");
                    printHelp();
                    System.exit(1);
                    return;
                }

                log.info("Running server");
                SpringApp.main(args);
                break;
            }

            default: {
                log.error("Unknown command: {}", args[0]);
                System.exit(1);
                break;
            }
        }
    }

    private static void printHelp() {
        log.info("Usage:");
        log.info("  app test <zone id>               -- run tests on the given zone");
        log.info("  app client                       -- run CLI client");
        log.info("  app server                       -- run server");
        log.info("  app load <zone id> <graph path>  -- run server");
    }
}
