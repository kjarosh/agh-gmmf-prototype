package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Kamil Jarosz
 */
@Getter
public class VertexIndex {
    private final Map<VertexId, EffectiveVertex> effectiveChildren = new ConcurrentHashMap<>();
    private final Set<VertexId> effectiveParents = new ConcurrentSkipListSet<>();

    public void addEffectiveParent(VertexId id, Runnable createListener) {
        if (!effectiveParents.contains(id)) {
            effectiveParents.add(id);
            createListener.run();
        }
    }

    public EffectiveVertex getEffectiveChild(VertexId id, Runnable createListener) {
        EffectiveVertex effectiveVertex;
        if (!effectiveChildren.containsKey(id)) {
            effectiveVertex = new EffectiveVertex();
            effectiveChildren.put(id, effectiveVertex);
            createListener.run();
        } else {
            effectiveVertex = effectiveChildren.get(id);
        }
        return effectiveVertex;
    }
}
