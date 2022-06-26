package com.github.kjarosh.agh.pp.persistence.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

import java.nio.ByteBuffer;

/**
 * @author Kamil Jarosz
 */
public class LettuceConnections {
    private final RedisClient client;
    private final StatefulRedisConnection<String, VertexId> vertexId;
    private final StatefulRedisConnection<String, String> string;
    private final StatefulRedisConnection<String, Permissions> permissions;
    private final StatefulRedisConnection<String, ByteBuffer> byteBuffer;

    public LettuceConnections(int database) {
        RedisURI redisUri = RedisURI.Builder.socket("/redis-server.sock")
                .withDatabase(database)
                .build();
        this.client = RedisClient.create(redisUri);
        this.vertexId = client.connect(Codecs.VERTEX_ID);
        this.string = client.connect(Codecs.STRING);
        this.permissions = client.connect(Codecs.PERMISSIONS);
        this.byteBuffer = client.connect(Codecs.BYTE_BUFFER);
    }

    public RedisClient client() {
        return client;
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
