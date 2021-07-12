package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode
public class Operation {
    private final OperationType type;

    private ZoneId zoneId;
    private EdgeId edgeId;
    private Permissions permissions;
    private String trace;
    private VertexId vertexId;
    private Vertex.Type vertexType;

    @JsonCreator
    public Operation(@JsonProperty("type") OperationType type) {
        this.type = type;
    }
}
