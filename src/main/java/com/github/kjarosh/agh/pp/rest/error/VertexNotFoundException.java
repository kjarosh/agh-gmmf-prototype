package com.github.kjarosh.agh.pp.rest.error;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.Getter;

/**
 * @author Kamil Jarosz
 */
public class VertexNotFoundException extends RuntimeException {
    @Getter
    private final VertexId vertex;
    @Getter
    private final String actionDescription;

    public VertexNotFoundException(VertexId vertex, String actionDescription) {
        super("Vertex " + vertex + " not found while " + actionDescription);
        this.vertex = vertex;
        this.actionDescription = actionDescription;
    }
}
