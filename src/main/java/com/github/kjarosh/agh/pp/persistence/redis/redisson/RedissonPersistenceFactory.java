package com.github.kjarosh.agh.pp.persistence.redis.redisson;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.index.VertexIndex;
import com.github.kjarosh.agh.pp.persistence.PersistenceFactory;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;

/**
 * @author Kamil Jarosz
 */
public class RedissonPersistenceFactory implements PersistenceFactory {
    private static final RedissonPersistenceFactory instance = new RedissonPersistenceFactory();

    private RedissonPersistenceFactory() {

    }

    public static RedissonPersistenceFactory getInstance() {
        return instance;
    }

    @Override
    public VertexIndex createIndex(String id) {
        String prefix = "index/" + id;
        return new RedissonVertexIndex(LazyRedisson.redisson0, prefix);
    }

    @Override
    public Graph createGraph() {
        String prefix = "graph";
        return new RedissonGraph(LazyRedisson.redisson0, prefix);
    }

    private static final class LazyRedisson {
        private static final RedissonClient redisson0;

        static {
            Config config = new Config();
            config.setNettyThreads(AppConfig.threads);
            config.setTransportMode(TransportMode.EPOLL);
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379")
                    .setConnectionMinimumIdleSize(AppConfig.threads / 3)
                    .setConnectionPoolSize(AppConfig.threads)
                    .setTcpNoDelay(true);
            redisson0 = Redisson.create(config);
        }
    }
}
