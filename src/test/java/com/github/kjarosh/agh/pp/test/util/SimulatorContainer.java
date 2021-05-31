package com.github.kjarosh.agh.pp.test.util;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Paths;
import java.time.Duration;

/**
 * @author Kamil Jarosz
 */
public class SimulatorContainer extends GenericContainer<SimulatorContainer> {
    public SimulatorContainer(String zoneId) {
        super(new ImageFromDockerfile()
                .withDockerfile(Paths.get("./Dockerfile")));

        this.withEnv("ZONE_ID", zoneId)
                .withCommand("server")
                .withNetworkAliases(zoneId)
                .withLabel("gmm.zone", zoneId)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(zoneId)))
                .waitingFor(new HttpWaitStrategy().forPath("/healthcheck"))
                .withStartupTimeout(Duration.ofMinutes(5))
                .withExposedPorts(80);
    }
}
