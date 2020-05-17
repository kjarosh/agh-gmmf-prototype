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

}
