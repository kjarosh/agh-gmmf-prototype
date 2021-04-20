package com.github.kjarosh.agh.pp.graph.util;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.LinkedHashMap;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
public class QueryClientResults {
    private QueryType type;
    private boolean naive;
    private LinkedHashMap max;
    private LinkedHashMap avg;

    public QueryType getType() {
        return this.type;
    }

    public boolean getNaive() {
        return this.naive;
    }

    public LinkedHashMap getMax() {
        return this.max;
    }

    public LinkedHashMap getAvg() {
        return this.avg;
    }

    public Duration getMaxDuration() {
        return Duration.ofNanos(
                ((Integer) (this.max.get("nano"))).longValue() + ((Integer) (this.max.get("seconds"))).longValue() * 1000000000
        );
    }

    public Duration getAvgDuration() {
        return Duration.ofNanos(
                ((Integer) (this.avg.get("nano"))).longValue() + ((Integer) (this.avg.get("seconds"))).longValue() * 1000000000
        );
    }

    public QueryClientResults(
            QueryType type,
            boolean naive,
            LinkedHashMap max,
            LinkedHashMap avg
    ) {
        this.type = type;
        this.naive = naive;
        this.max = max;
        this.avg = avg;
    }
}
