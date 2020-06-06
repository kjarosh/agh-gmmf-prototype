package com.github.kjarosh.agh.pp.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EffectiveVertex {
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

    @Override
    public String toString() {
        return "EffectiveVertex(" + effectivePermissions +
                " by " + intermediateVertices + ')';
    }
}
