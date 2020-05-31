package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kamil Jarosz
 */
@Getter
public class VertexIndex {
    private final Map<VertexId, EffectiveVertex> effectiveChildren = new ConcurrentHashMap<>();
    private final Map<VertexId, EffectiveVertex> effectiveParents = new ConcurrentHashMap<>();

    public EffectiveVertex getEffectiveParent(VertexId id, Runnable createListener) {
        EffectiveVertex effectiveVertex;
        if (!effectiveParents.containsKey(id)) {
            effectiveVertex = new EffectiveVertex();
            effectiveParents.put(id, effectiveVertex);
            createListener.run();
        } else {
            effectiveVertex = effectiveParents.get(id);
        }
        return effectiveVertex;
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
