package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.OperationPerformer;
import com.github.kjarosh.agh.pp.graph.util.Operation;
import com.github.kjarosh.agh.pp.graph.util.OperationType;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import com.github.kjarosh.agh.pp.util.NonCloseableOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public class OperationWriter implements OperationPerformer {
    private final OutputStream os;
    private final ObjectMapper mapper;

    public OperationWriter(OutputStream os) {
        this.os = os;
        this.mapper = new ObjectMapper();
    }

    private void write(Operation op) {
        try {
            mapper.writeValue(new NonCloseableOutputStream(os), op);
            os.write('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        Operation op = new Operation(OperationType.ADD_EDGE);
        op.setEdgeId(id);
        op.setPermissions(permissions);
        op.setTrace(trace);
        write(op);
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id, String trace) {
        Operation op = new Operation(OperationType.REMOVE_EDGE);
        op.setEdgeId(id);
        op.setTrace(trace);
        write(op);
    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        Operation op = new Operation(OperationType.SET_PERMISSIONS);
        op.setEdgeId(id);
        op.setTrace(trace);
        write(op);
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        Operation op = new Operation(OperationType.ADD_VERTEX);
        op.setVertexId(id);
        op.setVertexType(type);
        write(op);
    }

    @Override
    public void addVertices(ZoneId zone, BulkVertexCreationRequestDto bulkRequest) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void simulateLoad(ZoneId zone, LoadSimulationRequestDto request) {
        throw new RuntimeException("Not implemented");
    }
}
