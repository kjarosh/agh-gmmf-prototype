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
    private Object max;
    private Object avg;
    private String label;

    public QueryType getType() {
        return this.type;
    }

    public boolean getNaive() {
        return this.naive;
    }

    public Object getMax() {
        return this.max;
    }

    public Object getAvg() {
        return this.avg;
    }

    public String getLabel() {
        return this.label;
    }

    public QueryClientResults(
            QueryType type,
            boolean naive,
            Object max,
            Object avg,
            String label
    ) {
        this.type = type;
        this.naive = naive;
        this.max = max;
        this.avg = avg;
        this.label = label;
    }
}
