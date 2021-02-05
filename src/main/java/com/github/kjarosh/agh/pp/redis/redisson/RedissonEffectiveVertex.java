package com.github.kjarosh.agh.pp.redis.redisson;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.redis.RedisEffectiveVertex;
import org.redisson.api.RBatch;
import org.redisson.api.RFuture;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.util.Set;

/**
 * @author Kamil Jarosz
 */
public class RedissonEffectiveVertex extends RedisEffectiveVertex {
    private final RedissonClient redisson;

    public RedissonEffectiveVertex(RedissonClient redisson, String prefix) {
        super(prefix);
        this.redisson = redisson;
    }

    @Override
    public RSet<VertexId> getIntermediateVertices() {
        return redisson.getSet(keyIntermediateVertices(), Codecs.VERTEX_ID);
    }

    @Override
    public void setIntermediateVertices(Set<VertexId> intermediateVertices) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<VertexId> getIntermediateVerticesEager() {
        return getIntermediateVertices().readAll();
    }

    @Override
    public boolean isDirty() {
        return redisson.getAtomicLong(keyDirty()).get() != 0;
    }

    @Override
    public void setDirty(boolean dirty) {
        getAndSetDirty(dirty);
    }

    @Override
    public boolean getAndSetDirty(boolean dirty) {
        return redisson.getAtomicLong(keyDirty()).getAndSet(dirty ? 1 : 0) != 0;
    }

    @Override
    public Permissions getEffectivePermissions() {
        return redisson.<Permissions>getBucket(keyEffectivePermissions(), Codecs.PERMISSIONS).get();
    }

    @Override
    public void setEffectivePermissions(Permissions effectivePermissions) {
        redisson.getBucket(keyEffectivePermissions(), Codecs.PERMISSIONS).set(effectivePermissions);
    }

    @Override
    protected boolean getDirtyAndSetResult(CalculationResult result) {
        RBatch batch = redisson.createBatch();
        RFuture<Long> wasDirty = batch.getAtomicLong(keyDirty())
                .getAndSetAsync(result.isDirty() ? 1 : 0);
        batch.getBucket(keyEffectivePermissions(), Codecs.PERMISSIONS)
                .setAsync(result.getCalculated());
        batch.executeAsync();
        return wasDirty.syncUninterruptibly().getNow() != 0;
    }
}
