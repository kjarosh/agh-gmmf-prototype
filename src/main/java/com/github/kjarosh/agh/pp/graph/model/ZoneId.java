package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Kamil Jarosz
 */
@EqualsAndHashCode
public class ZoneId implements Comparable<ZoneId>, Serializable {
    private final String id;

    @JsonCreator
    public ZoneId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int compareTo(ZoneId o) {
        return id.compareTo(o.id);
    }
}
