package com.github.kjarosh.agh.pp.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Kamil Jarosz
 */
@EqualsAndHashCode
public class Permissions implements Comparable<Permissions> {
    public static final Permissions NONE = new Permissions("00000");

    private final String value;

    @JsonCreator
    public Permissions(String value) {
        this.value = Objects.requireNonNull(value);

        if (!value.matches("[01]{5}")) {
            throw new IllegalArgumentException();
        }
    }

    public static Permissions combine(Permissions a, Permissions b) {
        if (a == null) return b;
        return a.combine(b);
    }

    public static Permissions random(Random random) {
        return new Permissions(IntStream.range(0, 5)
                .map(x -> random.nextBoolean() ? 1 : 0)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining()));
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

    @Override
    public int compareTo(Permissions o) {
        return value.compareTo(o.value);
    }
}
