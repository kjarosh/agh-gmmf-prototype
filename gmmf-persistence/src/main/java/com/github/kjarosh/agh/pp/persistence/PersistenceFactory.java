package com.github.kjarosh.agh.pp.persistence;

import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.VertexIndex;

/**
 * @author Kamil Jarosz
 */
public interface PersistenceFactory {
    VertexIndex createIndex(String id);

    Graph createGraph(ZoneId currentZoneId);
}
