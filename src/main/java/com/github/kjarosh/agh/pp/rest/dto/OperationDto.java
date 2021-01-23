package com.github.kjarosh.agh.pp.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
public class OperationDto {
    @JsonProperty("t")
    private OperationType type;
    @JsonProperty("f")
    private VertexId fromId;
    @JsonProperty("to")
    private VertexId toId;
    @JsonProperty("p")
    private Permissions permissions;
    @JsonProperty("tr")
    private String trace;

    public enum OperationType {
        @JsonProperty("a")
        ADD_EDGE,
        @JsonProperty("r")
        REMOVE_EDGE,
        @JsonProperty("p")
        SET_PERMS,
    }
}
