package com.github.kjarosh.agh.pp.persistence.redis.redisson;

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
        return new RedissonGraph(LazyRedisson.redisson1, prefix);
    }

    private static final class LazyRedisson {
        private static final RedissonClient redisson0;
        private static final RedissonClient redisson1;

        static {
            redisson0 = createClient(0);
            redisson1 = createClient(1);
        }

        private static RedissonClient createClient(int database) {
            Config config = new Config();
            config.setTransportMode(TransportMode.EPOLL);
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379")
                    .setDatabase(database)
                    .setTcpNoDelay(true);
            return Redisson.create(config);
        }
    }
}
