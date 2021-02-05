package com.github.kjarosh.agh.pp.redis.redisson;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import org.redisson.client.codec.Codec;
import org.redisson.codec.TypedJsonJacksonCodec;

/**
 * @author Kamil Jarosz
 */
class Codecs {
    public static final Codec PERMISSIONS = new TypedJsonJacksonCodec(Permissions.class);
    public static final Codec VERTEX_ID = new TypedJsonJacksonCodec(VertexId.class);
}
