package com.github.kjarosh.agh.pp.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.generator.GraphGenerator;
import com.github.kjarosh.agh.pp.graph.generator.GraphGeneratorConfig;
import com.github.kjarosh.agh.pp.graph.model.Graph;
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

        GraphGenerator g = new GraphGenerator(config);

        if (cmd.hasOption("s")) {
            int scale = Integer.parseInt(cmd.getOptionValue("s"));
            config.setZones(scale * config.getZones());
            config.setSpaces(scale * config.getSpaces());
            config.setProviders(scale * config.getProviders());
        } else if (cmd.hasOption("n")) {
            long requiredNodesPerZone = Integer.parseInt(cmd.getOptionValue("n"));

            double error;
            double scale;
            do {
                error = Math.abs(1.0 - (double)g.estimateVertices() / (requiredNodesPerZone * config.getZones()));
                scale = (double)(requiredNodesPerZone * config.getZones()) / g.estimateVertices();

                config.setSpaces((int) (scale * config.getSpaces()));
                config.setProviders((int) (scale * config.getProviders()));
            } while (error > 0.03);
        }

        System.out.println("Estimated number of vertices: " + g.estimateVertices());
        System.out.println("Estimated number of edges: " + g.estimateEdges());
        Graph graph = g.generateGraph();
        System.out.println("Number of vertices: " + graph.allVertices().size());
        System.out.println("Number of edges: " + graph.allEdges().size());
        graph.serialize(Files.newOutputStream(Paths.get(cmd.getOptionValue("o"))));
    }
}
