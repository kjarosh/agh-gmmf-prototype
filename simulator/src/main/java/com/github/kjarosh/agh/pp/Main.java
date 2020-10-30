package com.github.kjarosh.agh.pp;

import com.github.kjarosh.agh.pp.cli.Cmd;
import com.github.kjarosh.agh.pp.test.Tester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Kamil Jarosz
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Too few arguments");
            printHelp();
            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

//        String mode = Optional.ofNullable(System.getProperty("app.mode"))
//                .filter(Predicate.not(String::isEmpty))
//                .orElse("zone");
//        logger.debug("Starting application in mode '{}'", mode);
//
//        switch (mode) {
//            case "tester": {
//                logger.info("Running tester");
//
//                String testedZone = System.getenv("TESTED_ZONE");
//                logger.debug("Testing zone '{}'", testedZone);
//
//                List<String> allZones = Arrays.asList(System.getenv("ALL_ZONES").split(","));
//                logger.debug("All zones: {}", allZones);
//                Tester.main(testedZone, allZones);
//                break;
//            }
//
//            case "client": {
//                logger.info("Running client");
//                Cmd.main(args);
//                break;
//            }
//
//            case "loader": {
//                logger.info("Running loader");
//                Cmd.main(args);
//                break;
//            }
//
//            case "zone": {
//                logger.info("Running Spring");
//                SpringApp.main(args);
//                break;
//            }
//
//            default: {
//                logger.error("Unknown mode: {}", mode);
//                System.exit(1);
//                break;
//            }
//        }
    }

    private static void printHelp() {
        logger.info("Usage:");
        logger.info("  app test <zone id>   -- run tests on the given zone");
        logger.info("  app client           -- run CLI client");
        logger.info("  app server           -- run server");
    }
}
