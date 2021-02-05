package com.github.kjarosh.agh.pp.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;

/**
 * @author Kamil Jarosz
 */
class Codecs {
    public static final RedisCodec<String, ByteBuffer> BB = new ByteBufferCodec();
    public static final RedisCodec<String, String> STRING = new StringCodec();
    public static final RedisCodec<String, Permissions> PERMISSIONS = new TypedJsonJacksonCodec<>(Permissions.class);
    public static final RedisCodec<String, VertexId> VERTEX_ID = new TypedJsonJacksonCodec<>(VertexId.class);
}
