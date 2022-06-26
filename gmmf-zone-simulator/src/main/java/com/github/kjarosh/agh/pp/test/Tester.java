package com.github.kjarosh.agh.pp.test;

import ch.qos.logback.classic.Level;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.test.strategy.DynamicTestsStrategy;
import com.github.kjarosh.agh.pp.test.strategy.LoadMeasurementStrategy;
import com.github.kjarosh.agh.pp.test.strategy.TestContext;
import com.github.kjarosh.agh.pp.util.LoggerUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class Tester {
    private final DynamicTestsStrategy dynamicStrategy;
    private final TestContext context;

    public Tester(
            ZoneClient client,
            String zone,
            String graphPath) {
        this.dynamicStrategy = new DynamicTestsStrategy(graphPath);
        this.context = TestContext.builder()
                .zone(new ZoneId(zone))
                .client(client)
                .build();
    }

    public static void main(String zone) {
        LoggerUtils.setLoggingLevel("org.springframework.web", Level.INFO);

        String graphPath = System.getenv("GRAPH_PATH");
        new Tester(new ZoneClient(), zone, graphPath).test();
    }

    @SneakyThrows
    private void test() {
        log.info("Starting tests");

        new LoadMeasurementStrategy("generated_graph.json", Duration.ofSeconds(100))
                .execute(context);

        dynamicStrategy.execute(context);
    }
}
