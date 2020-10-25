package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.index.events.EventStats;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * @author Kamil Jarosz
 */
public class Supervisor extends Thread {
    private final DoubleSupplier verticesBuilt;
    private final DoubleSupplier edgesBuilt;
    private final Supplier<EventStats> statsSupplier;
    private EventStats lastStats = null;

    public Supervisor(Supplier<EventStats> statsSupplier) {
        this(null, null, statsSupplier);
    }

    public Supervisor(
            DoubleSupplier verticesBuilt,
            DoubleSupplier edgesBuilt,
            Supplier<EventStats> statsSupplier) {
        this.verticesBuilt = verticesBuilt;
        this.edgesBuilt = edgesBuilt;
        this.statsSupplier = statsSupplier;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }

            StringBuilder message = new StringBuilder();
            if (verticesBuilt != null) {
                double v = verticesBuilt.getAsDouble();
                message.append(String.format("V: %.2f%%    ", v * 100));
            }

            if (edgesBuilt != null) {
                double e = edgesBuilt.getAsDouble();
                message.append(String.format("E: %.2f%%    ", e * 100));
            }

            if (statsSupplier != null) {
                lastStats = statsSupplier.get();
                message.append(lastStats);
            }

            System.out.println(message.toString());
        }
    }

    public EventStats getLastStats() {
        return lastStats != null ?
                lastStats :
                (lastStats = statsSupplier.get());
    }
}
