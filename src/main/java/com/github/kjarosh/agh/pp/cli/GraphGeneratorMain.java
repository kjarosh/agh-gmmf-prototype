package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.generator.GraphGenerator;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.moandjiezana.toml.Toml;

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

    public static void main(String[] args) throws IOException {
        Toml config = new Toml().read(new File("config.toml"));
        GraphGenerator generator = new GraphGenerator(config);
        Graph graph = generator.generateGraph();
        graph.serialize(Files.newOutputStream(Paths.get(config.getString("output"))));
    }
}
