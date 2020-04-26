package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

/**
 * @author Kamil Jarosz
 */
@ToString
public class Edge {
    @JsonProperty("src")
    private final VertexId src;
    @JsonProperty("dst")
    private final VertexId dst;
    @JsonProperty("perms")
    private final Permissions permissions;

    public Edge(
            @JsonProperty("src") VertexId src,
            @JsonProperty("dst") VertexId dst,
            @JsonProperty("perms") Permissions permissions) {
        this.src = src;
        this.dst = dst;
        this.permissions = permissions;
    }

    public VertexId src() {
        return src;
    }

    public VertexId dst() {
        return dst;
    }

    public Permissions permissions() {
        return permissions;
    }
}
