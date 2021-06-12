package com.github.kjarosh.agh.pp.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.generator.GraphGenerator;
import com.github.kjarosh.agh.pp.graph.generator.GraphGeneratorConfig;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class GraphGeneratorMain {
    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) throws IOException, ParseException {
        Options options = new Options();
        options.addRequiredOption("c", "config", true, "path to config file");
        options.addRequiredOption("o", "output", true, "path to output file");
        options.addOption("n", "nodes-per-zone", true, "demanded nodes per zone");
        options.addOption("s", "scale", true, "scale the graph");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        GraphGeneratorConfig config = new ObjectMapper()
                .readValue(new File(cmd.getOptionValue("c")), GraphGeneratorConfig.class);

        if (cmd.hasOption("s")) {
            int scale = Integer.parseInt(cmd.getOptionValue("s"));
            log.info("Scaling graph x" + scale);
            config.setZones(scale * config.getZones());
            config.setSpaces(scale * config.getSpaces());
            config.setProviders(scale * config.getProviders());
            log.info("Using zones: " + config.getZones());
            log.info("Using spaces: " + config.getSpaces());
            log.info("Using providers: " + config.getProviders());
        } else if (cmd.hasOption("n")) {
            long requiredNodesPerZone = Integer.parseInt(cmd.getOptionValue("n"));

            double error;
            double scale;
            do {
                long estimateVertices = new GraphGenerator(config).estimateVertices();
                error = Math.abs(1.0 - (double) estimateVertices / (requiredNodesPerZone * config.getZones()));
                scale = (double)(requiredNodesPerZone * config.getZones()) / estimateVertices;

                config.setSpaces((int) (scale * config.getSpaces()));
                config.setProviders((int) (scale * config.getProviders()));
            } while (error > 0.03);
        }

        GraphGenerator g = new GraphGenerator(config);

        log.info("Estimated number of vertices: " + g.estimateVertices());
        log.info("Estimated number of edges: " + g.estimateEdges());
        Graph graph = g.generateGraph();
        log.info("Number of vertices: " + graph.allVertices().size());
        log.info("Number of edges: " + graph.allEdges().size());
        graph.serialize(Files.newOutputStream(Paths.get(cmd.getOptionValue("o"))));
    }
}
