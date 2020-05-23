package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author Kamil Jarosz
 */
@EqualsAndHashCode(exclude = "permissions")
@ToString
public class Edge implements Comparable<Edge> {
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
        this.permissions = permissions != null ?
                permissions : Permissions.NONE;
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

    @Override
    public int compareTo(Edge other) {
        int cmp;
        cmp = src.compareTo(other.src);
        if (cmp != 0) return cmp;
        cmp = dst.compareTo(other.dst);
        if (cmp != 0) return cmp;

        return permissions.compareTo(other.permissions);
    }
}
