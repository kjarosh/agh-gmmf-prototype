package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.index.impl.InMemoryVertexIndex;
import com.github.kjarosh.agh.pp.index.impl.RedisVertexIndex;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

/**
 * @author Kamil Jarosz
 */
public class VertexIndexFactory {
    private final boolean redis = Boolean.parseBoolean(System.getProperty("app.redis", "false"));
    private volatile RedissonClient redisson = null;

    public VertexIndex createIndex(String id) {
        if (redis) {
            setupRedisson();
            return new RedisVertexIndex(redisson, id);
        } else {
            return new InMemoryVertexIndex();
        }
    }

    private void setupRedisson() {
        if (redisson == null) {
            synchronized (this) {
                if (redisson == null) {
                    redisson = Redisson.create();
                }
            }
        }
    }
}
