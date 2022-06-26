package com.github.kjarosh.agh.pp.graph.generator;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author Kamil Jarosz
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class VertexIdPair {
    private final VertexId from;
    private final VertexId to;
}
