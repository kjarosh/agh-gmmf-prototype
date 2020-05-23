package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventStats {
    private int processing;
    private int queued;
    private long total;
    private double load1;
    private double load5;
    private double load10;

    public static EventStats empty() {
        return new EventStats(0, 0, 0, 0, 0, 0);
    }

    @JsonIgnore
    public EventStats combine(EventStats other) {
        return EventStats.builder()
                .processing(processing + other.processing)
                .total(total + other.total)
                .queued(queued + other.queued)
                .load1(load1 + other.load1)
                .load5(load5 + other.load5)
                .load10(load10 + other.load10)
                .build();
    }

    @Override
    public String toString() {
        return String.format("events: %d/%d", processing, processing + queued) +
                String.format("  load: %.0f/%.0f/%.0f", load1, load5, load10) +
                String.format("  total: %d", total);
    }
}
