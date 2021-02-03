package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.VertexId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The index for a vertex which contains pre-computed graph
 * information used for near-constant time queries.
 *
 * @author Kamil Jarosz
 */
public interface VertexIndex {
    Map<VertexId, EffectiveVertex> getEffectiveChildren();

    Map<VertexId, EffectiveVertex> getEffectiveParents();

    default Set<VertexId> getEffectiveChildrenSet() {
        return getEffectiveChildren().keySet();
    }

    default Set<VertexId> getEffectiveParentsSet() {
        return getEffectiveParents().keySet();
    }

    default EffectiveVertex getOrAddEffectiveParent(VertexId id, Runnable createListener) {
        Map<VertexId, EffectiveVertex> effectiveParents = getEffectiveParents();
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

    default Optional<EffectiveVertex> getEffectiveParent(VertexId id) {
        return Optional.ofNullable(getEffectiveParents().get(id));
    }

    default void removeEffectiveParent(VertexId subjectId) {
        getEffectiveParents().remove(subjectId);
    }

    default EffectiveVertex getOrAddEffectiveChild(VertexId id, Runnable createListener) {
        Map<VertexId, EffectiveVertex> effectiveChildren = getEffectiveChildren();
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

    default Optional<EffectiveVertex> getEffectiveChild(VertexId id) {
        return Optional.ofNullable(getEffectiveChildren().get(id));
    }

    default void removeEffectiveChild(VertexId subjectId) {
        getEffectiveChildren().remove(subjectId);
    }
}
