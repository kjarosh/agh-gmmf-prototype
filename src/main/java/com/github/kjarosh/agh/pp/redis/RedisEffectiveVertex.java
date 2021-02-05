package com.github.kjarosh.agh.pp.redis;

import com.github.kjarosh.agh.pp.index.EffectiveVertex;

/**
 * @author Kamil Jarosz
 */
public abstract class RedisEffectiveVertex extends EffectiveVertex {
    private final String prefix;

    public RedisEffectiveVertex(String prefix) {
        this.prefix = prefix;
    }

    protected String keyEffectivePermissions() {
        return prefix + "/effective-permissions";
    }

    protected String keyDirty() {
        return prefix + "/dirty";
    }

    protected String keyIntermediateVertices() {
        return prefix + "/intermediate-vertices";
    }
}
