package com.github.kjarosh.agh.pp.test;

import ch.qos.logback.classic.Level;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import com.github.kjarosh.agh.pp.test.strategy.DynamicTestsStrategy;
import com.github.kjarosh.agh.pp.test.strategy.LoadMeasurementStrategy;
import com.github.kjarosh.agh.pp.test.strategy.TestContext;
import com.github.kjarosh.agh.pp.test.strategy.TestExampleGraphStrategy;
import com.github.kjarosh.agh.pp.util.LoggerUtils;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
public class Tester {
    private final boolean dynamicTests;
    private final TestExampleGraphStrategy exampleStrategy;
    private final DynamicTestsStrategy dynamicStrategy;
    private final TestContext context;

    public Tester(
            ZoneClient client,
            String zone,
            List<String> allZones,
            String graphPath,
            boolean dynamicTests) {
        this.dynamicTests = dynamicTests;
        this.exampleStrategy = new TestExampleGraphStrategy(graphPath);
        this.dynamicStrategy = new DynamicTestsStrategy(graphPath);
        this.context = TestContext.builder()
                .allZones(allZones.stream()
                        .map(ZoneId::new)
                        .collect(Collectors.toList()))
                .zone(new ZoneId(zone))
                .client(client)
                .build();
    }

    public static void main(String zone, List<String> allZones) {
        LoggerUtils.setLoggingLevel("org.springframework.web", Level.INFO);

        boolean dynamicTests = "true".equals(System.getenv("DYNAMIC_TESTS"));
        String graphPath = System.getenv("GRAPH_PATH");
        new Tester(new ZoneClient(), zone, allZones, graphPath, dynamicTests).test();
    }

    @SneakyThrows
    private void test() {
        while (notHealthy(context)) {
            Thread.sleep(200);
        }

        if (dynamicTests) {
            new LoadMeasurementStrategy("generated_graph.json", Duration.ofSeconds(10))
                    .execute(context);
            if (true) return;
            dynamicStrategy.execute(context);
        } else {
            exampleStrategy.execute(context);
        }
    }

    private boolean notHealthy(TestContext context) {
        return context.getAllZones().stream()
                .anyMatch(zone -> !context.getClient().healthcheck(zone));
    }
}
