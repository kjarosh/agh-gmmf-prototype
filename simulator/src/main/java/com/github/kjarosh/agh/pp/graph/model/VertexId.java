package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

/**
 * @author Kamil Jarosz
 */
@EqualsAndHashCode
public class VertexId {
    private final ZoneId owner;
    private final String name;

    @JsonCreator
    public VertexId(String string) {
        String[] split = string.split(":", 2);
        if (split.length != 2) {
            throw new IllegalStateException("Invalid id string: " + string);
        }
        this.owner = new ZoneId(split[0]);
        this.name = split[1];
    }

    public VertexId(ZoneId owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public String name() {
        return name;
    }

    public ZoneId owner() {
        return owner;
    }

    @JsonValue
    @Override
    public String toString() {
        return owner + ":" + name();
    }
}
