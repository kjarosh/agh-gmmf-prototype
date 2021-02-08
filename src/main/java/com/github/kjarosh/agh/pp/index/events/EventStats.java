package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Statistics about event processing.
 *
 * @author Kamil Jarosz
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventStats {
    /**
     * Number of events that are scheduled,
     * i.e. are sent to the executor.
     */
    private int processing;
    private double processingNanos;

    private Map<Vertex.Type, Integer> processingByType;

    /**
     * Number of events that are waiting to be scheduled.
     */
    private int queued;

    /**
     * Number of events that are waiting to be sent in the outbox.
     */
    private int outbox;

    /**
     * Total number of events.
     */
    private long total;

    /**
     * Number of events per second computed in the last second.
     */
    private double load1;

    /**
     * Number of events per second computed in the last 5 seconds.
     */
    private double load5;

    /**
     * Number of events per second computed in the last 15 seconds.
     */
    private double load15;

    public static EventStats empty() {
        return EventStats.builder()
                .processing(0)
                .processingNanos(0)
                .processingByType(new HashMap<>())
                .total(0)
                .queued(0)
                .outbox(0)
                .load1(0)
                .load5(0)
                .load15(0)
                .build();
    }

    @JsonIgnore
    public EventStats combine(EventStats other) {
        Map<Vertex.Type, Integer> processingByType2 = new HashMap<>(processingByType);
        other.processingByType.forEach((key, value) ->
                processingByType2.merge(key, value, Integer::sum));

        double newProcessingNanos;
        if (Double.isNaN(this.processingNanos)) {
            newProcessingNanos = other.processingNanos;
        } else if (Double.isNaN(other.processingNanos)) {
            newProcessingNanos = this.processingNanos;
        } else {
            newProcessingNanos = (this.processingNanos + other.processingNanos) / 2;
        }
        return EventStats.builder()
                .processing(processing + other.processing)
                .processingNanos(newProcessingNanos)
                .processingByType(processingByType2)
                .total(total + other.total)
                .queued(queued + other.queued)
                .outbox(outbox + other.outbox)
                .load1(load1 + other.load1)
                .load5(load5 + other.load5)
                .load15(load15 + other.load15)
                .build();
    }

    @Override
    public String toString() {
        return String.format("eq: %d/%d", processing, queued) +
                String.format("  out: %d", outbox) +
                String.format("  ld: %.0f/%.0f/%.0f", load1, load5, load15) +
                String.format("  tot: %d", total) +
                String.format("  pt: %.2f ms", processingNanos / TimeUnit.MILLISECONDS.toNanos(1)) +
                String.format("  %dp %ds %dg %du",
                        processingByType.getOrDefault(Vertex.Type.PROVIDER, 0),
                        processingByType.getOrDefault(Vertex.Type.SPACE, 0),
                        processingByType.getOrDefault(Vertex.Type.GROUP, 0),
                        processingByType.getOrDefault(Vertex.Type.USER, 0));
    }
}
