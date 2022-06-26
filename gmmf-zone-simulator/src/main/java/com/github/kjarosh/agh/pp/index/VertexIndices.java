package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.persistence.Persistence;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kamil Jarosz
 */
@Component
public class VertexIndices {
    private final Map<Vertex, VertexIndex> indices = new HashMap<>();

    public VertexIndex getIndexOf(Vertex vertex) {
        return indices.computeIfAbsent(vertex, i -> Persistence.getPersistenceFactory()
                .createIndex(vertex.id().toString()));
    }
}
