package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * @author Kamil Jarosz
 */
public class Permissions {
    private final String value;

    @JsonCreator
    public Permissions(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static Permissions combine(Permissions a, Permissions b) {
        if (a == null) return b;
        return a.combine(b);
    }

    private Permissions combine(Permissions other) {
        if (other == null) {
            return this;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 5; ++i) {
            if (other.value.charAt(i) == '1' ||
                    value.charAt(i) == '1') {
                result.append(1);
            } else {
                result.append(0);
            }
        }
        return new Permissions(result.toString());
    }

    @JsonValue
    public String toString() {
        return value;
    }
}
