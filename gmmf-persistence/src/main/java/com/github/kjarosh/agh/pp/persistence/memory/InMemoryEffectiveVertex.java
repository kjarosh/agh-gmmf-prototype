package com.github.kjarosh.agh.pp.persistence.memory;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class InMemoryEffectiveVertex implements EffectiveVertex {
    boolean dirty = false;
    Permissions effectivePermissions = Permissions.NONE;
    Set<VertexId> intermediateVertices = new HashSet<>();

    @Override
    public Set<VertexId> getIntermediateVerticesEager() {
        return getIntermediateVertices();
    }

    @Override
    public String toString() {
        return "EffectiveVertex(" + effectivePermissions +
                " by " + intermediateVertices + ')';
    }
}
