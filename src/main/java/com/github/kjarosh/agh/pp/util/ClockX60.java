package com.github.kjarosh.agh.pp.util;

import com.codahale.metrics.Clock;

/**
 * @author Kamil Jarosz
 */
public class ClockX60 extends Clock {
    @Override
    public long getTick() {
        return System.nanoTime() * 60;
    }
}
