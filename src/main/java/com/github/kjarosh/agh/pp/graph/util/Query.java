package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.VertexId;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode
public class Query {
    private final QueryType type;
    private final VertexId from;
    private final VertexId to;
    private final boolean existing;

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

    @JsonCreator
    public Query(
            @JsonProperty("type") QueryType type,
            @JsonProperty("from") VertexId from,
            @JsonProperty("to") VertexId to,
            @JsonProperty("existing") boolean existing
    ) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.existing = existing;
    }
}
