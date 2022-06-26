package com.github.kjarosh.agh.pp.persistence.redis.redisson;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import org.redisson.client.codec.Codec;
import org.redisson.codec.TypedJsonJacksonCodec;

/**
 * @author Kamil Jarosz
 */
class Codecs {
    public static final Codec PERMISSIONS = new TypedJsonJacksonCodec(Permissions.class);
    public static final Codec VERTEX_ID = new TypedJsonJacksonCodec(VertexId.class);
    public static final Codec VERTEX_TYPE = new TypedJsonJacksonCodec(Vertex.Type.class);
    public static final Codec ZONE_ID = new TypedJsonJacksonCodec(ZoneId.class);
}
