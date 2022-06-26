package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author Kamil Jarosz
 */
@EqualsAndHashCode
@ToString
public class Vertex implements Comparable<Vertex> {
    @JsonProperty("id")
    private final VertexId id;
    @JsonProperty("type")
    private final Type type;

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

    @Override
    public int compareTo(Vertex o) {
        return id.compareTo(o.id);
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
