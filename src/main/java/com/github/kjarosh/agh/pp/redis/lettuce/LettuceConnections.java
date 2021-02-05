package com.github.kjarosh.agh.pp.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Kamil Jarosz
 */
public class LettuceConnections {
    private final RedisClient client;
    private final List<StatefulRedisConnection<String, VertexId>> vertexId = new ArrayList<>();
    private final List<StatefulRedisConnection<String, String>> string = new ArrayList<>();
    private final List<StatefulRedisConnection<String, Permissions>> permissions = new ArrayList<>();
    private final List<StatefulRedisConnection<String, ByteBuffer>> byteBuffer = new ArrayList<>();
    private final Random random = new Random();
    private final int poolSize = 5;

    public LettuceConnections(RedisClient client) {
        this.client = client;
        for (int i = 0; i < poolSize; ++i) {
            this.vertexId.add(client.connect(Codecs.VERTEX_ID));
            this.string.add(client.connect(Codecs.STRING));
            this.permissions.add(client.connect(Codecs.PERMISSIONS));
            this.byteBuffer.add(client.connect(Codecs.BB));
        }
    }

    public StatefulRedisConnection<String, VertexId> vertexId() {
        return vertexId.get(random.nextInt(poolSize));
    }

    public StatefulRedisConnection<String, String> string() {
        return string.get(random.nextInt(poolSize));
    }

    public StatefulRedisConnection<String, Permissions> permissions() {
        return permissions.get(random.nextInt(poolSize));
    }

    public StatefulRedisConnection<String, ByteBuffer> byteBuffer() {
        return byteBuffer.get(random.nextInt(poolSize));
    }
}
