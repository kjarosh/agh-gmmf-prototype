package com.github.kjarosh.agh.pp.instrumentation;

/**
 * @author Kamil Jarosz
 */
public interface InstrumentationListener extends AutoCloseable {
    void open();

    @Override
    void close();

    void handle(Notification[] bulk, int size);
}
