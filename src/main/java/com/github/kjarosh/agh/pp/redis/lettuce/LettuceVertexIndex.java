package com.github.kjarosh.agh.pp.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.redis.RedisVertexIndex;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Kamil Jarosz
 */
public class LettuceVertexIndex extends RedisVertexIndex {
    private final LettuceConnections lettuce;

    public LettuceVertexIndex(LettuceConnections lettuce, String prefix) {
        super(prefix);
        this.lettuce = lettuce;
    }

    @Override
    public Set<VertexId> getEffectiveChildrenSet() {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        return Collections.unmodifiableSet(commands.smembers(keyEffectiveChildren()));
    }

    @Override
    public Set<VertexId> getEffectiveParentsSet() {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        return Collections.unmodifiableSet(commands.smembers(keyEffectiveParents()));
    }

    @Override
    public EffectiveVertex getOrAddEffectiveChild(VertexId id, Runnable createListener) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        boolean created = commands.sadd(keyEffectiveChildren(), id) > 0;
        if (created) {
            createListener.run();
        }
        return new LettuceEffectiveVertex(lettuce, keyEffectiveChildren(id));
    }

    @Override
    public EffectiveVertex getOrAddEffectiveParent(VertexId id, Runnable createListener) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        boolean created = commands.sadd(keyEffectiveParents(), id) > 0;
        if (created) {
            createListener.run();
        }
        return new LettuceEffectiveVertex(lettuce, keyEffectiveParents(id));
    }

    @Override
    public Optional<EffectiveVertex> getEffectiveParent(VertexId id) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        if (commands.sismember(keyEffectiveParents(), id)) {
            return Optional.of(new LettuceEffectiveVertex(lettuce, keyEffectiveParents(id)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<EffectiveVertex> getEffectiveChild(VertexId id) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        if (commands.sismember(keyEffectiveChildren(), id)) {
            return Optional.of(new LettuceEffectiveVertex(lettuce, keyEffectiveChildren(id)));
        }
        return Optional.empty();
    }

    @Override
    public void removeEffectiveParent(VertexId subjectId) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        commands.srem(keyEffectiveParents(), subjectId);
        commands.del(keyEffectiveParents(subjectId));
    }

    @Override
    public void removeEffectiveChild(VertexId subjectId) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        commands.srem(keyEffectiveChildren(), subjectId);
        commands.del(keyEffectiveChildren(subjectId));
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
