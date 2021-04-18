package com.github.kjarosh.agh.pp.graph.util;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
public class QueryClientResults {
    private QueryType type;
    private boolean naive;
    private Object max;
    private Object avg;

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

    public QueryClientResults(
            QueryType type,
            boolean naive,
            Object max,
            Object avg
    ) {
        this.type = type;
        this.naive = naive;
        this.max = max;
        this.avg = avg;
    }
}
