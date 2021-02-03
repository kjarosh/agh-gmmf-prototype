package com.github.kjarosh.agh.pp.index.impl;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.util.Set;

/**
 * @author Kamil Jarosz
 */
public class RedisEffectiveVertex extends EffectiveVertex {
    private final RedissonClient redisson;
    private final String prefix;

    public RedisEffectiveVertex(RedissonClient redisson, String prefix) {
        this.redisson = redisson;
        this.prefix = prefix;
    }

    private String keyEffectivePermissions() {
        return prefix + "/effective-permissions";
    }

    private String keyDirty() {
        return prefix + "/dirty";
    }

    private String keyIntermediateVertices() {
        return prefix + "/intermediate-vertices";
    }

    @Override
    public RSet<VertexId> getIntermediateVertices() {
        return redisson.getSet(keyIntermediateVertices());
    }

    @Override
    public void setIntermediateVertices(Set<VertexId> intermediateVertices) {
        RLock lock = redisson.getLock(keyIntermediateVertices());
        lock.lock();
        try {
            RSet<VertexId> set = getIntermediateVertices();
            set.clear();
            set.addAll(intermediateVertices);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isDirty() {
        return redisson.getAtomicLong(keyDirty()).get() != 0;
    }

    @Override
    public void setDirty(boolean dirty) {
        redisson.getAtomicLong(keyDirty()).set(1);
    }

    @Override
    public Permissions getEffectivePermissions() {
        return redisson.<Permissions>getBucket(keyEffectivePermissions()).get();
    }

    @Override
    public void setEffectivePermissions(Permissions effectivePermissions) {
        redisson.getBucket(keyEffectivePermissions()).set(effectivePermissions);
    }
}
