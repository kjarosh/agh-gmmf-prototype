package com.github.kjarosh.agh.pp.test.strategy;

import lombok.SneakyThrows;

import java.time.Duration;

/**
 * @author Kamil Jarosz
 */
public class DelayMeasurementStrategy implements TestStrategy {
    private final String graphPath;
    private final Duration duration;
    private final double ops;

    public DelayMeasurementStrategy(String graphPath, Duration duration, double ops) {
        this.graphPath = graphPath;
        this.duration = duration;
        this.ops = ops;
    }

    @SneakyThrows
    @Override
    public void execute(TestContext context) {

    }
}
