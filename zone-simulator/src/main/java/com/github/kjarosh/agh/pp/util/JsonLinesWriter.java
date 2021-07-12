package com.github.kjarosh.agh.pp.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * @author Kamil Jarosz
 */
public class JsonLinesWriter {
    private final ObjectMapper mapper;
    private final OutputStream os;

    public JsonLinesWriter(OutputStream os) {
        this(os, new ObjectMapper());
    }

    public JsonLinesWriter(OutputStream os, ObjectMapper mapper) {
        this.os = new BufferedOutputStream(os);
        this.mapper = mapper;
    }

    public void writeValue(Object value) {
        try {
            mapper.writeValue(new NonCloseableOutputStream(os), value);
            os.write('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
