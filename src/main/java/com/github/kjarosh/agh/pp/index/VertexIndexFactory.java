package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.memory.InMemoryVertexIndex;
import com.github.kjarosh.agh.pp.redis.lettuce.LettuceConnections;
import com.github.kjarosh.agh.pp.redis.lettuce.LettuceVertexIndex;
import com.github.kjarosh.agh.pp.redis.redisson.RedissonVertexIndex;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;

/**
 * @author Kamil Jarosz
 */
public class VertexIndexFactory {
    private volatile RedissonClient redisson = null;
    private volatile RedisClient redis = null;
    private volatile LettuceConnections lettuce = null;

    public VertexIndex createIndex(String id) {
        if (AppConfig.redis) {
            switch (AppConfig.redisClient) {
                case "lettuce":
                default:
                    setupLettuce();
                    return new LettuceVertexIndex(lettuce, id);
                case "redisson":
                    setupRedisson();
                    return new RedissonVertexIndex(redisson, id);
            }
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

    private void setupLettuce() {
        if (redis == null) {
            synchronized (this) {
                if (redis == null) {
                    RedisURI redisUri = RedisURI.create("redis-socket:///redis-server.sock");
                    ClientResources clientResources = ClientResources.builder()
                            .ioThreadPoolSize(AppConfig.threads)
                            .computationThreadPoolSize(AppConfig.threads)
                            .build();
                    redis = RedisClient.create(clientResources, redisUri);
                    lettuce = new LettuceConnections(redis);
                }
            }
        }
    }
}
