package com.github.kjarosh.agh.pp.util;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * @author Kamil Jarosz
 */
public class NonCloseableInputStream extends FilterInputStream {
    public NonCloseableInputStream(InputStream delegate) {
        super(delegate);
    }

    @Override
    public void close() {
        // non-closeable
    }
}
