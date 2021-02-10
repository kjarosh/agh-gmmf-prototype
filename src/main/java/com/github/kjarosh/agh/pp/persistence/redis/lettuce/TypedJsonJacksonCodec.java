package com.github.kjarosh.agh.pp.persistence.redis.lettuce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import io.lettuce.core.codec.RedisCodec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

/**
 * @author Kamil Jarosz
 */
public class TypedJsonJacksonCodec<T> implements RedisCodec<String, T> {
    private final Class<T> clazz;

    public TypedJsonJacksonCodec(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String decodeKey(ByteBuffer byteBuffer) {
        return Codecs.STRING.decodeKey(byteBuffer);
    }

    @Override
    public T decodeValue(ByteBuffer byteBuffer) {
        try {
            return new ObjectMapper().readValue(new ByteBufferBackedInputStream(byteBuffer), clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ByteBuffer encodeKey(String s) {
        return Codecs.STRING.encodeKey(s);
    }

    @Override
    public ByteBuffer encodeValue(T permissions) {
        try {
            return ByteBuffer.wrap(new ObjectMapper().writeValueAsBytes(permissions));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
