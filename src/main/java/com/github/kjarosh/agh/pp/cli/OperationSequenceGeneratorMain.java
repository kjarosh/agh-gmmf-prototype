package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.generator.OperationWriter;
import com.github.kjarosh.agh.pp.graph.modification.RandomOperationIssuer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OperationSequenceGeneratorMain {
    private static String graphPath = "graph.json";
    private static String filename;
    private static OperationWriter writer;

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("g", "graph", true, "Path to graph json file");
        options.addOption("n", "amount", true, "Amount of requests to be generated");
        options.addOption("o", "output", true, "Output file path");
        CommandLine cmd = new DefaultParser().parse(options, args);

        if(!cmd.hasOption("n")) {
            throw new RuntimeException("Argument 'n' is required");
        }

        graphPath = cmd.getOptionValue("g", "graph.json");
        filename = cmd.getOptionValue("o", "output.json");
        int amount = Integer.parseInt(cmd.getOptionValue("n"));

        var generator = getGenerator();

        for(int n = 0; n < amount; n++) {
            generator.perform();
        }

        commitFile();
        System.out.printf("Sequence of %d operations has been generated. Output file: '%s'", amount, filename);
    }

    public static RandomOperationIssuer getGenerator() {
        var graph = GraphLoader.loadGraph(graphPath);
        RandomOperationIssuer generator = new RandomOperationIssuer(graph);
        writer = createWriterForFile();
        generator.withOperationIssuer(writer);
        return generator;
    }

    private static OperationWriter createWriterForFile() {
        return new OperationWriter();
    }

    private static void commitFile() {
       writer.save(filename);
    }

}