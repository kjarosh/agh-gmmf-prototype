package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
        return new EventStats(0, 0, 0, 0, 0, 0, 0);
    }

    @JsonIgnore
    public EventStats combine(EventStats other) {
        return EventStats.builder()
                .processing(processing + other.processing)
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
        return String.format("events: %d/%d", processing, queued) +
                String.format("  out: %d", outbox) +
                String.format("  load: %.0f/%.0f/%.0f", load1, load5, load15) +
                String.format("  total: %d", total);
    }
}
