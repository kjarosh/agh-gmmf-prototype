package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.memory.InMemoryGraph;
import com.github.kjarosh.agh.pp.memory.InMemoryVertexIndex;
import com.github.kjarosh.agh.pp.redis.lettuce.LettuceConnections;
import com.github.kjarosh.agh.pp.redis.lettuce.LettuceGraph;
import com.github.kjarosh.agh.pp.redis.lettuce.LettuceVertexIndex;
import com.github.kjarosh.agh.pp.redis.redisson.RedissonGraph;
import com.github.kjarosh.agh.pp.redis.redisson.RedissonVertexIndex;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;

/**
 * @author Kamil Jarosz
 */
public class PersistenceFactory {
    private static final PersistenceFactory instance = new PersistenceFactory();

    public static PersistenceFactory getInstance() {
        return instance;
    }

    private PersistenceFactory() {

    }

    public VertexIndex createIndex(String id) {
        if (AppConfig.redis) {
            String prefix = "index/" + id;
            switch (AppConfig.redisClient) {
                case "lettuce":
                default:
                    return new LettuceVertexIndex(LazyLettuce.lettuce0, prefix);
                case "redisson":
                    return new RedissonVertexIndex(LazyRedisson.redisson0, prefix);
            }
        } else {
            return new InMemoryVertexIndex();
        }
    }

    public Graph createGraph() {
        if (AppConfig.redis) {
            String prefix = "graph";
            switch (AppConfig.redisClient) {
                case "lettuce":
                default:
                    return new LettuceGraph(LazyLettuce.lettuce1, prefix);
                case "redisson":
                    return new RedissonGraph(LazyRedisson.redisson0, prefix);
            }
        } else {
            return new InMemoryGraph();
        }
    }

    private static final class LazyLettuce {
        private static final LettuceConnections lettuce0 = new LettuceConnections(0);
        private static final LettuceConnections lettuce1 = new LettuceConnections(1);
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
