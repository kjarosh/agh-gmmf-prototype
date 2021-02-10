package com.github.kjarosh.agh.pp.persistence;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.index.VertexIndex;
import com.github.kjarosh.agh.pp.persistence.memory.InMemoryPersistenceFactory;
import com.github.kjarosh.agh.pp.persistence.redis.lettuce.LettucePersistenceFactory;
import com.github.kjarosh.agh.pp.persistence.redis.redisson.RedissonPersistenceFactory;

/**
 * @author Kamil Jarosz
 */
public interface PersistenceFactory {
    static PersistenceFactory getInstance() {
        if (!AppConfig.redis) {
            return InMemoryPersistenceFactory.getInstance();
        }

        switch (AppConfig.redisClient) {
            case "lettuce":
                return LettucePersistenceFactory.getInstance();
            case "redisson":
                return RedissonPersistenceFactory.getInstance();
            default:
                throw new RuntimeException("Unknown redis implementation: " + AppConfig.redisClient);
        }
    }

    VertexIndex createIndex(String id);

    Graph createGraph();
}
