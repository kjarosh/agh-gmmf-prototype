package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.index.VertexIndex;
import lombok.ToString;

/**
 * @author Kamil Jarosz
 */
@ToString
public class Vertex {
    @JsonProperty("id")
    private final VertexId id;
    @JsonProperty("type")
    private final Type type;

    @JsonIgnore
    private final VertexIndex index = new VertexIndex();

    public Vertex(
            @JsonProperty("id") VertexId id,
            @JsonProperty("type") Type type) {
        this.id = id;
        this.type = type;
    }

    public VertexId id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public VertexIndex index() {
        return index;
    }

    public enum Type {
        @JsonProperty("provider")
        PROVIDER,
        @JsonProperty("space")
        SPACE,
        @JsonProperty("group")
        GROUP,
        @JsonProperty("user")
        USER,
    }
}
