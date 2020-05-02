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
    @JsonProperty("zone")
    private final ZoneId zone;

    @JsonIgnore
    private final VertexIndex index = new VertexIndex();

    public Vertex(
            @JsonProperty("id") VertexId id,
            @JsonProperty("type") Type type,
            @JsonProperty("zone") ZoneId zone) {
        this.id = id;
        this.type = type;
        this.zone = zone;
    }

    public VertexId id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public ZoneId zone() {
        return zone;
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
