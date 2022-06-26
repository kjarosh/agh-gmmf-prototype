package com.github.kjarosh.agh.pp.persistence.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.VertexIndex;
import com.github.kjarosh.agh.pp.persistence.PersistenceFactory;

/**
 * @author Kamil Jarosz
 */
public class LettucePersistenceFactory implements PersistenceFactory {
    private static final LettucePersistenceFactory instance = new LettucePersistenceFactory();

    private LettucePersistenceFactory() {

    }

    public static LettucePersistenceFactory getInstance() {
        return instance;
    }

    @Override
    public VertexIndex createIndex(String id) {
        return new LettuceVertexIndex(LazyLettuce.lettuce0, "index/" + id);
    }

    @Override
    public Graph createGraph(ZoneId currentZoneId) {
        return new LettuceGraph(currentZoneId, LazyLettuce.lettuce1, "graph");
    }

    private static final class LazyLettuce {
        private static final LettuceConnections lettuce0 = new LettuceConnections(0);
        private static final LettuceConnections lettuce1 = new LettuceConnections(1);
    }
}
