package com.github.kjarosh.agh.pp.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import java.nio.ByteBuffer;

/**
 * @author Kamil Jarosz
 */
public class LettuceConnection {
    private final RedisClient client;
    private final StatefulRedisConnection<String, VertexId> vertexId;
    private final StatefulRedisConnection<String, String> string;
    private final StatefulRedisConnection<String, Permissions> permissions;
    private final StatefulRedisConnection<String, ByteBuffer> byteBuffer;

    public LettuceConnection(RedisClient client) {
        this.client = client;
        this.vertexId = client.connect(Codecs.VERTEX_ID);
        this.string = client.connect(Codecs.STRING);
        this.permissions = client.connect(Codecs.PERMISSIONS);
        this.byteBuffer = client.connect(Codecs.BB);
    }

    public StatefulRedisConnection<String, VertexId> vertexId() {
        return vertexId;
    }

    public StatefulRedisConnection<String, String> string() {
        return string;
    }

    public StatefulRedisConnection<String, Permissions> permissions() {
        return permissions;
    }

    public StatefulRedisConnection<String, ByteBuffer> byteBuffer() {
        return byteBuffer;
    }
}
