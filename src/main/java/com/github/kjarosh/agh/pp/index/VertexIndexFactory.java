package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.index.impl.InMemoryVertexIndex;
import com.github.kjarosh.agh.pp.redis.RedisVertexIndex;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;

/**
 * @author Kamil Jarosz
 */
public class VertexIndexFactory {
    private volatile RedissonClient redisson = null;

    public VertexIndex createIndex(String id) {
        if (AppConfig.redis) {
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
                    Config config = new Config();
                    config.setNettyThreads(AppConfig.threads);
                    config.setTransportMode(TransportMode.EPOLL);
                    config.useSingleServer()
                            .setAddress("redis://127.0.0.1:6379")
                            .setConnectionMinimumIdleSize(AppConfig.threads / 3)
                            .setConnectionPoolSize(AppConfig.threads)
                            .setTcpNoDelay(true);
                    redisson = Redisson.create(config);
                }
            }
        }
    }
}
