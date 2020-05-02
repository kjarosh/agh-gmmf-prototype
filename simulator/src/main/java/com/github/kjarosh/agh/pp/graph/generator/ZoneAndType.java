package com.github.kjarosh.agh.pp.graph.generator;

import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * @author Kamil Jarosz
 */
@AllArgsConstructor
@EqualsAndHashCode
class ZoneAndType {
    private ZoneId zone;
    private Vertex.Type type;
}
