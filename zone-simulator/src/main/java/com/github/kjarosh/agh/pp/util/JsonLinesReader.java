package com.github.kjarosh.agh.pp.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

/**
 * @author Kamil Jarosz
 */
public class JsonLinesReader {
    private final ObjectMapper mapper;
    private final BufferedReader reader;

    public JsonLinesReader(InputStream is) {
        this(is, new ObjectMapper());
    }

    public JsonLinesReader(InputStream is, ObjectMapper mapper) {
        this.reader = new BufferedReader(new InputStreamReader(is));
        this.mapper = mapper;
    }

    public <T> T nextValue(Class<T> clazz) {
        try {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            return mapper.readValue(line, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
