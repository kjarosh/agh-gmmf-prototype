package com.github.kjarosh.agh.pp.graph.util;

import com.github.kjarosh.agh.pp.graph.model.VertexId;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
public class Query {
    private QueryType type;
    private VertexId from;
    private VertexId to;
    private boolean existing;

    public QueryType getType() {
        return type;
    }

    public VertexId getFrom() {
        return from;
    }

    public VertexId getTo() {
        return to;
    }

    public boolean getExisting() {
        return existing;
    }

    public Query(
            QueryType type,
            VertexId from,
            VertexId to,
            boolean existing
    ) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.existing = existing;
    }
}
