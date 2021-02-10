package com.github.kjarosh.agh.pp.test.util;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.config.ZoneConfig;
import lombok.SneakyThrows;
import org.testcontainers.containers.Network;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Kamil Jarosz
 */
public abstract class ExampleTestBase extends IntegrationTestBase {
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    private SimulatorContainer zone0;
    private SimulatorContainer zone1;

    @Override
    protected String getGraphPath() {
        return "example_graph.json";
    }

    @SneakyThrows
    @Override
    protected void startEnvironment() {
        String redis = System.getProperty("test.redis", "false");

        Network network = Network.newNetwork();

        zone0 = new SimulatorContainer("zone0")
                .withEnv("REDIS", redis)
                .withNetwork(network);

        zone1 = new SimulatorContainer("zone1")
                .withEnv("REDIS", redis)
                .withNetwork(network);

        Future<?> zone0Future = executor.submit(() -> zone0.start());
        Future<?> zone1Future = executor.submit(() -> zone1.start());
        zone0Future.get();
        zone1Future.get();
    }

    @SneakyThrows
    @Override
    protected void stopEnvironment() {
        if (zone0 != null) {
            zone0.stop();
        }

        if (zone1 != null) {
            zone1.stop();
        }
    }

    @Override
    protected Config createConfiguration() {
        Config testConfig = new Config();
        testConfig.setZones(new HashMap<>());
        testConfig.getZones().put("zone0", ZoneConfig.builder()
                .address(getHostPort(zone0))
                .build());
        testConfig.getZones().put("zone1", ZoneConfig.builder()
                .address(getHostPort(zone1))
                .build());
        return testConfig;
    }

    private String getHostPort(SimulatorContainer container) {
        return container.getHost() + ":" + container.getMappedPort(80);
    }
}
