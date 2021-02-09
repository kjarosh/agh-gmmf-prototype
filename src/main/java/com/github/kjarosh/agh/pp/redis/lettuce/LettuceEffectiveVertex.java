package com.github.kjarosh.agh.pp.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.redis.RedisEffectiveVertex;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;

/**
 * @author Kamil Jarosz
 */
public class LettuceEffectiveVertex extends RedisEffectiveVertex {
    private final LettuceConnections lettuce;

    public LettuceEffectiveVertex(LettuceConnections lettuce, String prefix) {
        super(prefix);
        this.lettuce = lettuce;
    }

    @Override
    public Set<VertexId> getIntermediateVertices() {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        return Collections.unmodifiableSet(commands.smembers(keyIntermediateVertices()));
    }

    @Override
    public void setIntermediateVertices(Set<VertexId> intermediateVertices) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addIntermediateVertices(Set<VertexId> ids, Runnable modifyListener) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        boolean modified = commands.sadd(keyIntermediateVertices(), ids.toArray(new VertexId[0])) > 0;
        if (modified) {
            modifyListener.run();
        }
    }

    @Override
    public void removeIntermediateVertices(Set<VertexId> ids, Runnable modifyListener) {
        RedisCommands<String, VertexId> commands = lettuce.vertexId().sync();
        boolean modified = commands.srem(keyIntermediateVertices(), ids.toArray(new VertexId[0])) > 0;
        if (modified) {
            modifyListener.run();
        }
    }

    @Override
    public Set<VertexId> getIntermediateVerticesEager() {
        return getIntermediateVertices();
    }

    @Override
    public boolean isDirty() {
        RedisCommands<String, String> commands = lettuce.string().sync();
        return Boolean.parseBoolean(commands.get(keyDirty()));
    }

    @Override
    public void setDirty(boolean dirty) {
        RedisCommands<String, String> commands = lettuce.string().sync();
        commands.set(keyDirty(), String.valueOf(dirty));
    }

    @Override
    public Permissions getEffectivePermissions() {
        RedisCommands<String, Permissions> commands = lettuce.permissions().sync();
        return commands.get(keyEffectivePermissions());
    }

    @Override
    public void setEffectivePermissions(Permissions effectivePermissions) {
        RedisCommands<String, Permissions> commands = lettuce.permissions().sync();
        commands.set(keyEffectivePermissions(), effectivePermissions);
    }

    @SneakyThrows
    @Override
    public boolean getDirtyAndSetResult(CalculationResult result) {
        RedisCommands<String, ByteBuffer> commands = lettuce.byteBuffer().sync();
        ByteBuffer bytes = commands.getset(keyDirty(),
                Codecs.STRING.encodeValue(String.valueOf(result.isDirty())));
        commands.set(keyEffectivePermissions(), Codecs.PERMISSIONS.encodeValue(result.getCalculated()));
        return bytes != null && Boolean.parseBoolean(Codecs.STRING.decodeValue(bytes));
    }
}
