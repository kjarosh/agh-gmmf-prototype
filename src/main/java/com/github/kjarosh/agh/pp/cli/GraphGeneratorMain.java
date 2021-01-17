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

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        GraphGeneratorConfig config = new ObjectMapper()
                .readValue(new File(cmd.getOptionValue("c")), GraphGeneratorConfig.class);
        GraphGenerator g = new GraphGenerator(config);

        System.out.println("Estimated number of vertices: " + g.estimateVertices());
        System.out.println("Estimated number of edges: " + g.estimateEdges());
        Graph graph = g.generateGraph();
        System.out.println("Number of vertices: " + graph.allVertices().size());
        System.out.println("Number of edges: " + graph.allEdges().size());
        graph.serialize(Files.newOutputStream(Paths.get(cmd.getOptionValue("o"))));
    }
}
