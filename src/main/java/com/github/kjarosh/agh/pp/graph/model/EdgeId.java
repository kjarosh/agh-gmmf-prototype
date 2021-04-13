package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Kamil Jarosz
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@ToString
public class EdgeId implements Comparable<EdgeId> {
    private final VertexId from;
    private final VertexId to;

    @JsonCreator
    public static EdgeId of(@JsonProperty("from") VertexId from, @JsonProperty("to") VertexId to) {
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
