package com.github.kjarosh.agh.pp.persistence.redis.lettuce;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * @author Kamil Jarosz
 */
public class ByteBufferCodec implements RedisCodec<String, ByteBuffer> {
    @Override
    public String decodeKey(ByteBuffer byteBuffer) {
        return Codecs.STRING.decodeKey(byteBuffer);
    }

    @Override
    public ByteBuffer decodeValue(ByteBuffer bytes) {
        return copy(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String s) {
        return Codecs.STRING.encodeKey(s);
    }

    @Override
    public ByteBuffer encodeValue(ByteBuffer value) {
        return copy(value);
    }

    private static ByteBuffer copy(ByteBuffer source) {
        ByteBuffer copy = ByteBuffer.allocate(source.remaining());
        copy.put(source);
        copy.flip();
        return copy;
    }
}
