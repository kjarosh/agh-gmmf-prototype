package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.viz.Neo4jImporter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author Kamil Jarosz
 */
public class Neo4jImportMain {
    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("a", "address", true, "neo4j address");
        options.addRequiredOption("u", "user", true, "user");
        options.addRequiredOption("p", "pass", true, "password");
        options.addRequiredOption("g", "graph", true, "path to the graph");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String uri = cmd.getOptionValue("a");
        String username = cmd.getOptionValue("u");
        String password = cmd.getOptionValue("p");
        String graph = cmd.getOptionValue("g");

        try (Neo4jImporter importer = new Neo4jImporter(uri, username, password)) {
            importer.importGraph(GraphLoader.loadGraph(graph));
        }
    }
}
