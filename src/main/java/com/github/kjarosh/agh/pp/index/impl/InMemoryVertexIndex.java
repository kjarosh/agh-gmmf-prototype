package com.github.kjarosh.agh.pp.index.impl;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.index.VertexIndex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kamil Jarosz
 */
public class InMemoryVertexIndex implements VertexIndex {
    private final Map<VertexId, EffectiveVertex> effectiveChildren = new ConcurrentHashMap<>();
    private final Map<VertexId, EffectiveVertex> effectiveParents = new ConcurrentHashMap<>();

    @Override
    public Map<VertexId, EffectiveVertex> getEffectiveChildren() {
        return effectiveChildren;
    }

    @Override
    public Map<VertexId, EffectiveVertex> getEffectiveParents() {
        return effectiveParents;
    }
}
