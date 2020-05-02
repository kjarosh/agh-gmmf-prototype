package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kamil Jarosz
 */
@Getter
public class VertexIndex {
    private final Map<VertexId, EffectiveVertex> effectiveChildren = new ConcurrentHashMap<>();
    private final Map<VertexId, EffectiveVertex> effectiveParents = new ConcurrentHashMap<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EffectiveVertex {
        private Permissions effectivePermissions = Permissions.NONE;
        private Set<VertexId> intermediateVertices = new HashSet<>();
    }
}
