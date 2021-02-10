package com.github.kjarosh.agh.pp.persistence.redis.redisson;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.persistence.redis.RedisVertexIndex;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.Optional;

/**
 * @author Kamil Jarosz
 */
public class RedissonVertexIndex extends RedisVertexIndex {
    private final RedissonClient redisson;

    public RedissonVertexIndex(RedissonClient redisson, String prefix) {
        super(prefix);
        this.redisson = redisson;
    }

    @Override
    public RSet<VertexId> getEffectiveChildrenSet() {
        return redisson.getSet(keyEffectiveChildren(), Codecs.VERTEX_ID);
    }

    @Override
    public RSet<VertexId> getEffectiveParentsSet() {
        return redisson.getSet(keyEffectiveParents(), Codecs.VERTEX_ID);
    }

    @Override
    public EffectiveVertex getOrAddEffectiveChild(VertexId id, Runnable createListener) {
        boolean created = getEffectiveChildrenSet().add(id);
        if (created) {
            createListener.run();
        }
        return new RedissonEffectiveVertex(redisson, keyEffectiveChildren(id));
    }

    @Override
    public EffectiveVertex getOrAddEffectiveParent(VertexId id, Runnable createListener) {
        boolean created = getEffectiveParentsSet().add(id);
        if (created) {
            createListener.run();
        }
        return new RedissonEffectiveVertex(redisson, keyEffectiveParents(id));
    }

    @Override
    public Optional<EffectiveVertex> getEffectiveParent(VertexId id) {
        if (getEffectiveParentsSet().contains(id)) {
            return Optional.of(new RedissonEffectiveVertex(redisson, keyEffectiveParents(id)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<EffectiveVertex> getEffectiveChild(VertexId id) {
        if (getEffectiveChildrenSet().contains(id)) {
            return Optional.of(new RedissonEffectiveVertex(redisson, keyEffectiveChildren(id)));
        }
        return Optional.empty();
    }

    @Override
    public void removeEffectiveParent(VertexId subjectId) {
        getEffectiveParentsSet().remove(subjectId);
        redisson.getBucket(keyEffectiveParents(subjectId)).delete();
    }

    @Override
    public void removeEffectiveChild(VertexId subjectId) {
        getEffectiveChildrenSet().remove(subjectId);
        redisson.getBucket(keyEffectiveChildren(subjectId)).delete();
    }

    @Override
    public Map<VertexId, EffectiveVertex> getEffectiveChildren() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<VertexId, EffectiveVertex> getEffectiveParents() {
        throw new UnsupportedOperationException();
    }
}
