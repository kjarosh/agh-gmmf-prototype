package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private final VertexId from;
    @JsonProperty("dst")
    private final VertexId to;
    @JsonProperty("perms")
    private final Permissions permissions;

    public Edge(
            @JsonProperty("src") VertexId from,
            @JsonProperty("dst") VertexId to,
            @JsonProperty("perms") Permissions permissions) {
        this.from = from;
        this.to = to;
        this.permissions = permissions != null ?
                permissions : Permissions.NONE;
    }

    @JsonIgnore
    public EdgeId id() {
        return new EdgeId(from, to);
    }

    public VertexId src() {
        return from;
    }

    public VertexId dst() {
        return to;
    }

    public Permissions permissions() {
        return permissions;
    }

    @Override
    public int compareTo(Edge other) {
        int cmp;
        cmp = from.compareTo(other.from);
        if (cmp != 0) return cmp;
        cmp = to.compareTo(other.to);
        if (cmp != 0) return cmp;

        return permissions.compareTo(other.permissions);
    }
}
