package com.github.kjarosh.agh.pp.index.impl;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.index.VertexIndex;
import org.redisson.api.RedissonClient;

import java.util.Map;

/**
 * @author Kamil Jarosz
 */
public class RedisVertexIndex implements VertexIndex {
    private final RedissonClient redisson;

    private final String keyEffectiveParents;
    private final String keyEffectiveChildren;

    public RedisVertexIndex(RedissonClient redisson, String prefix) {
        this.redisson = redisson;
        this.keyEffectiveParents = prefix + "/effective-parents";
        this.keyEffectiveChildren = prefix + "/effective-children";
    }

    @Override
    public Map<VertexId, EffectiveVertex> getEffectiveChildren() {
        return redisson.getMap(keyEffectiveChildren);
    }

    @Override
    public Map<VertexId, EffectiveVertex> getEffectiveParents() {
        return redisson.getMap(keyEffectiveParents);
    }
}
