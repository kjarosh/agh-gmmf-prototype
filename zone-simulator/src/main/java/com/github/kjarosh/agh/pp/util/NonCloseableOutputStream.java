package com.github.kjarosh.agh.pp.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Kamil Jarosz
 */
public class NonCloseableOutputStream extends FilterOutputStream {
    public NonCloseableOutputStream(OutputStream delegate) {
        super(delegate);
    }

    @Override
    public void close() throws IOException {
        // non-closeable
    }
}
