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
    private long averageNanos;

    public static EventStats empty() {
        return new EventStats(0, 0, -1);
    }

    @JsonIgnore
    public EventStats combine(EventStats other) {
        long avgNanos;
        if (averageNanos < 0) {
            avgNanos = other.averageNanos;
        } else if (other.averageNanos < 0) {
            avgNanos = averageNanos;
        } else {
            avgNanos = (averageNanos + other.averageNanos) / 2;
        }
        return EventStats.builder()
                .processing(processing + other.processing)
                .queued(queued + other.queued)
                .averageNanos(avgNanos)
                .build();
    }

    @Override
    public String toString() {
        String averageString = averageNanos > 0 ?
                String.format(" avg: %.2f/s", 1_000_000_000D / averageNanos) : "";
        return processing + "/" + (processing + queued) + averageString;
    }
}
