package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.viz.Neo4jImporter;

/**
 * @author Kamil Jarosz
 */
public class Neo4jImportMain {
    public static void main(String[] args) {
        if (args.length != 4) System.exit(1);

        String uri = args[0];
        String username = args[1];
        String password = args[2];
        String graph = args[3];

        try (Neo4jImporter importer = new Neo4jImporter(uri, username, password)) {
            importer.importGraph(GraphLoader.loadGraph(graph));
        }
    }
}
