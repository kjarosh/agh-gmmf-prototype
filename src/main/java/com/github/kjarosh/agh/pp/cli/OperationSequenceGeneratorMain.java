package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.generator.OperationWriter;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.modification.RandomOperationIssuer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class OperationSequenceGeneratorMain {
    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addRequiredOption("n", "count", true, "number of requests to be generated");
        options.addOption("g", "graph", true, "path to graph json file");
        options.addOption("o", "output", true, "output file path");
        CommandLine cmd = new DefaultParser().parse(options, args);

        String graphPath = cmd.getOptionValue("g", "graph.json");
        String outputPath = cmd.getOptionValue("o", "output.jsonl");
        int count = Integer.parseInt(cmd.getOptionValue("n"));

        try (OutputStream os = Files.newOutputStream(Paths.get(outputPath), StandardOpenOption.CREATE_NEW)) {
            var graph = GraphLoader.loadGraph(graphPath);
            var generator = createIssuer(os, graph);
            for (int n = 0; n < count; n++) {
                generator.perform();
            }
        }

        log.info("A sequence of {} operations has been generated. Output file: {}", count, outputPath);
    }

    private static RandomOperationIssuer createIssuer(OutputStream os, Graph graph) {
        RandomOperationIssuer generator = new RandomOperationIssuer(graph);
        generator.withOperationIssuer(new OperationWriter(os));
        return generator;
    }
}
