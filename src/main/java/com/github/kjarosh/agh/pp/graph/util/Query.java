package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Query {
    private QueryType type;
    private VertexId from;
    private VertexId to;
    private boolean existing;
}
