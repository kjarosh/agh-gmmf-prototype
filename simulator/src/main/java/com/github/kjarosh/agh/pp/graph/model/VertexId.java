package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

/**
 * @author Kamil Jarosz
 */
@EqualsAndHashCode
public class VertexId {
    private final String id;

    @JsonCreator
    public VertexId(String id) {
        this.id = id;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId();
    }
}
