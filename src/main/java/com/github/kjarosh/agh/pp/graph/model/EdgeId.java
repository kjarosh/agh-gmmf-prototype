package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Kamil Jarosz
 */
@EqualsAndHashCode
@Getter
@ToString
public class EdgeId implements Comparable<EdgeId> {
    private final VertexId from;
    private final VertexId to;

    public EdgeId(@JsonProperty("from") VertexId from, @JsonProperty("to") VertexId to) {
        this.from = from;
        this.to = to;
    }

    public static EdgeId of(VertexId from, VertexId to) {
        return new EdgeId(from, to);
    }

    @Override
    public int compareTo(EdgeId o) {
        int cmp = from.compareTo(o.from);
        if (cmp != 0) {
            return cmp;
        }

        return to.compareTo(o.to);
    }
}
