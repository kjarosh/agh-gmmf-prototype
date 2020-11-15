package com.github.kjarosh.agh.pp.test;

import ch.qos.logback.classic.Level;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.test.strategy.DynamicTestsStrategy;
import com.github.kjarosh.agh.pp.test.strategy.LoadMeasurementStrategy;
import com.github.kjarosh.agh.pp.test.strategy.TestContext;
import com.github.kjarosh.agh.pp.test.strategy.TestExampleGraphStrategy;
import com.github.kjarosh.agh.pp.util.LoggerUtils;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * @author Kamil Jarosz
 */
public class Tester {
    private static final Logger logger = LoggerFactory.getLogger(Tester.class);

    private final boolean dynamicTests;
    private final TestExampleGraphStrategy exampleStrategy;
    private final DynamicTestsStrategy dynamicStrategy;
    private final TestContext context;

    public Tester(
            ZoneClient client,
            String zone,
            String graphPath,
            boolean dynamicTests) {
        this.dynamicTests = dynamicTests;
        this.exampleStrategy = new TestExampleGraphStrategy(graphPath);
        this.dynamicStrategy = new DynamicTestsStrategy(graphPath);
        this.context = TestContext.builder()
                .zone(new ZoneId(zone))
                .client(client)
                .build();
    }

    public static void main(String zone) {
        LoggerUtils.setLoggingLevel("org.springframework.web", Level.INFO);

        boolean dynamicTests = "true".equals(System.getenv("DYNAMIC_TESTS"));
        String graphPath = System.getenv("GRAPH_PATH");
        new Tester(new ZoneClient(), zone, graphPath, dynamicTests).test();
    }

    @SneakyThrows
    private void test() {
        logger.info("Starting tests");

        if (dynamicTests) {
            new LoadMeasurementStrategy("generated_graph.json", Duration.ofSeconds(100))
                    .execute(context);
            if (true) return;
            dynamicStrategy.execute(context);
        } else {
            exampleStrategy.execute(context);
        }
    }
}
