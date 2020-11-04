package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The index for a vertex which contains pre-computed graph
 * information used for near-constant time queries.
 *
 * @author Kamil Jarosz
 */
@Getter
public class VertexIndex {
    private final Map<VertexId, EffectiveVertex> effectiveChildren = new ConcurrentHashMap<>();
    private final Map<VertexId, EffectiveVertex> effectiveParents = new ConcurrentHashMap<>();

    public EffectiveVertex getOrAddEffectiveParent(VertexId id, Runnable createListener) {
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

    public Optional<EffectiveVertex> getEffectiveParent(VertexId id) {
        return Optional.ofNullable(effectiveParents.get(id));
    }

    public void removeEffectiveParent(VertexId subjectId) {
        effectiveParents.remove(subjectId);
    }

    public EffectiveVertex getOrAddEffectiveChild(VertexId id, Runnable createListener) {
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

    public Optional<EffectiveVertex> getEffectiveChild(VertexId id) {
        return Optional.ofNullable(effectiveChildren.get(id));
    }

    public void removeEffectiveChild(VertexId subjectId) {
        effectiveChildren.remove(subjectId);
    }
}
