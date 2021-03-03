package com.github.kjarosh.agh.pp.test.util;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.config.ConfigLoader;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.client.RemoteGraphBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * @author Kamil Jarosz
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public abstract class IntegrationTestBase {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestBase.class);
    public ZoneId zone = new ZoneId(System.getProperty("test.zoneId", ""));
    public ZoneClient client = new ZoneClient();

    @SneakyThrows
    @BeforeAll
    void setUp() {
        if (!zone.getId().isEmpty()) {
            logger.info("Testing an existing environment, zone {}", zone);
        } else {
            logger.info("Setting up environment...");
            startEnvironment();

            logger.info("Setting up configuration...");
            try {
                Path configPath = Files.createTempFile("kjarosz_sim_", ".json");
                Config testConfig = createConfiguration();
                testConfig.saveConfig(configPath);
                ConfigLoader.reloadConfig(configPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            zone = new ZoneId("zone0");
            logger.info("Configuration set up");
        }

        String graphPath = getGraphPath();
        if (graphPath != null) {
            Graph graph = GraphLoader.loadGraph(graphPath);
            new RemoteGraphBuilder(graph, client).build(client);
        }

        logger.info("Setup complete");

        afterSetup();

        client.waitForIndex(zone, Duration.ofMinutes(1));
    }

    @AfterAll
    void tearDown() {
        stopEnvironment();
    }

    protected void afterSetup() {

    }

    protected abstract void startEnvironment();

    protected abstract void stopEnvironment();

    protected abstract Config createConfiguration();

    protected abstract String getGraphPath();

    public VertexId vid(String bob) {
        return new VertexId(bob);
    }

    public EdgeId eid(String from, String to) {
        return new EdgeId(new VertexId(from), new VertexId(to));
    }
}
