package com.github.kjarosh.agh.pp.redis;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.VertexIndex;

/**
 * @author Kamil Jarosz
 */
public abstract class RedisVertexIndex implements VertexIndex {
    private final String prefix;

    public RedisVertexIndex(String prefix) {
        this.prefix = prefix;
    }

    protected String keyEffectiveParents() {
        return prefix + "/effective-parents";
    }

    protected String keyEffectiveParents(VertexId vertex) {
        return keyEffectiveParents() + "/" + vertex;
    }

    protected String keyEffectiveChildren() {
        return prefix + "/effective-children";
    }

    protected String keyEffectiveChildren(VertexId vertex) {
        return keyEffectiveChildren() + "/" + vertex;
    }
}
