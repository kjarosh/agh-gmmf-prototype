package com.github.kjarosh.agh.pp.persistence.memory;

import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.index.VertexIndex;
import com.github.kjarosh.agh.pp.persistence.PersistenceFactory;

/**
 * @author Kamil Jarosz
 */
public class InMemoryPersistenceFactory implements PersistenceFactory {
    private static final InMemoryPersistenceFactory instance = new InMemoryPersistenceFactory();

    private InMemoryPersistenceFactory() {

    }

    public static InMemoryPersistenceFactory getInstance() {
        return instance;
    }

    @Override
    public VertexIndex createIndex(String id) {
        return new InMemoryVertexIndex();
    }

    @Override
    public Graph createGraph() {
        return new InMemoryGraph();
    }
}
