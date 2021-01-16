package com.github.kjarosh.agh.pp.test.strategy;

import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.test.EventStatsGatherer;
import com.github.kjarosh.agh.pp.test.Supervisor;
import com.github.kjarosh.agh.pp.graph.modification.RandomOperationIssuer;
import lombok.SneakyThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Kamil Jarosz
 */
public class LoadMeasurementStrategy implements TestStrategy {
    private static final Executor executor = Executors.newFixedThreadPool(16);

    private final int maxQueuedEvents = 10000;
    private final String graphPath;
    private final Duration duration;

    public LoadMeasurementStrategy(String graphPath, Duration duration) {
        this.graphPath = graphPath;
        this.duration = duration;
    }

    @SneakyThrows
    @Override
    public void execute(TestContext context) {
        Graph graph = context.buildGraph(graphPath);

        Supervisor supervisor = new Supervisor(new EventStatsGatherer(context.getZone()));
        supervisor.start();

        RandomOperationIssuer randomOperationIssuer =
                new RandomOperationIssuer(graph);
        Thread delegate = new Thread(() -> {
            while (!Thread.interrupted()) {
                if (supervisor.getLastStats().getQueued() > maxQueuedEvents) {
                    try {
                        Thread.sleep(200);
                        continue;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                executor.execute(randomOperationIssuer::perform);
            }
        });
        delegate.start();
        interruptAfterDuration(delegate);
        supervisor.interrupt();
        System.out.println("Supervisor interrupted");
        supervisor.join();
    }

    private void interruptAfterDuration(Thread delegate) {
        try {
            Instant deadline = Instant.now()
                    .plus(duration);

            while (delegate.isAlive()) {
                Duration left = Duration.between(Instant.now(), deadline);
                System.out.println("Deadline: " + deadline + ", left: " + left);
                if (left.toMillis() <= 0) {
                    break;
                }
                delegate.join(left.toMillis());
            }

            delegate.interrupt();
            System.out.println("Delegate interrupted");
            delegate.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
