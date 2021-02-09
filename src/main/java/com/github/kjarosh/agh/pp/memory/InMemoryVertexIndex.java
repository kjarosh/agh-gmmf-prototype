package com.github.kjarosh.agh.pp.memory;

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

    @Override
    public EffectiveVertex getOrAddEffectiveParent(VertexId id, Runnable createListener) {
        Map<VertexId, EffectiveVertex> effectiveParents = getEffectiveParents();
        EffectiveVertex effectiveVertex;
        if (!effectiveParents.containsKey(id)) {
            effectiveVertex = new InMemoryEffectiveVertex();
            effectiveParents.put(id, effectiveVertex);
            createListener.run();
        } else {
            effectiveVertex = effectiveParents.get(id);
        }
        return effectiveVertex;
    }

    @Override
    public EffectiveVertex getOrAddEffectiveChild(VertexId id, Runnable createListener) {
        Map<VertexId, EffectiveVertex> effectiveChildren = getEffectiveChildren();
        EffectiveVertex effectiveVertex;
        if (!effectiveChildren.containsKey(id)) {
            effectiveVertex = new InMemoryEffectiveVertex();
            effectiveChildren.put(id, effectiveVertex);
            createListener.run();
        } else {
            effectiveVertex = effectiveChildren.get(id);
        }
        return effectiveVertex;
    }
}
