package com.github.kjarosh.agh.pp.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EffectiveVertex implements Serializable { // TODO shouldnt be serializable
    @JsonProperty("dirty")
    private boolean dirty = false;
    @JsonProperty("effectivePermissions")
    private Permissions effectivePermissions = Permissions.NONE;
    @JsonProperty("intermediateVertices")
    private Set<VertexId> intermediateVertices = new HashSet<>();

    @JsonIgnore
    public void addIntermediateVertex(VertexId id, Runnable modifyListener) {
        addIntermediateVertices(Collections.singleton(id), modifyListener);
    }

    @JsonIgnore
    public void addIntermediateVertices(Set<VertexId> ids, Runnable modifyListener) {
        if (!intermediateVertices.containsAll(ids)) {
            intermediateVertices.addAll(ids);
            modifyListener.run();
        }
    }

    @JsonIgnore
    public void removeIntermediateVertex(VertexId id, Runnable modifyListener) {
        removeIntermediateVertices(Collections.singleton(id), modifyListener);
    }

    @JsonIgnore
    public void removeIntermediateVertices(Set<VertexId> ids, Runnable modifyListener) {
        if (intermediateVertices.removeAll(ids)) {
            modifyListener.run();
        }
    }

    @JsonIgnore
    public RecalculationResult recalculatePermissions(Set<Edge> edgesToCalculate) {
        Set<VertexId> intermediateVertices = getIntermediateVertices();
        List<Permissions> perms = edgesToCalculate.stream()
                .filter(x -> intermediateVertices.contains(x.src()))
                .map(Edge::permissions)
                .collect(Collectors.toList());

        setEffectivePermissions(perms.stream()
                .reduce(Permissions.NONE, Permissions::combine));

        if (perms.size() != intermediateVertices.size()) {
            dirty = true;
            return RecalculationResult.DIRTY;
        } else if (dirty) {
            dirty = false;
            return RecalculationResult.CLEANED;
        } else {
            return RecalculationResult.CLEAN;
        }
    }

    @Override
    public String toString() {
        return "EffectiveVertex(" + effectivePermissions +
                " by " + intermediateVertices + ')';
    }

    public enum RecalculationResult {
        CLEAN,
        CLEANED,
        DIRTY,
    }
}
