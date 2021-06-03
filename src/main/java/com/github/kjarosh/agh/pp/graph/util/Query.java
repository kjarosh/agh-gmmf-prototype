package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode
public class Query {
    private final QueryType type;
    private final VertexId from;
    private final VertexId to;
    private final boolean existing;

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
