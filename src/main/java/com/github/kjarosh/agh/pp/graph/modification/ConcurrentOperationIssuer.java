package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kamil Jarosz
 */
public class ConcurrentOperationIssuer implements OperationIssuer {
    private final OperationIssuer delegate;
    private final ExecutorService executor;

    public ConcurrentOperationIssuer(int poolSize, OperationIssuer delegate) {
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.delegate = delegate;
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions) {
        executor.submit(() -> delegate.addEdge(zone, id, permissions));
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        executor.submit(() -> delegate.addEdges(zone, bulkRequest));
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id) {
        executor.submit(() -> delegate.removeEdge(zone, id));

    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions) {
        executor.submit(() -> delegate.setPermissions(zone, id, permissions));
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        executor.submit(() -> delegate.addVertex(id, type));
    }

    @Override
    public void addVertices(ZoneId zone, BulkVertexCreationRequestDto bulkRequest) {
        executor.submit(() -> delegate.addVertices(zone, bulkRequest));
    }
}
