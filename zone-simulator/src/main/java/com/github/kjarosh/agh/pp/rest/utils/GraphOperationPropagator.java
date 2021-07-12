package com.github.kjarosh.agh.pp.rest.utils;

import com.github.kjarosh.agh.pp.graph.model.ZoneId;

/**
 * @author Kamil Jarosz
 */
@FunctionalInterface
public interface GraphOperationPropagator {
    void propagate(ZoneId zone, boolean successive);
}
