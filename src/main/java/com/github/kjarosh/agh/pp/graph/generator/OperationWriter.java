package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.*;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.util.Operation;
import com.github.kjarosh.agh.pp.graph.util.OperationType;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import lombok.SneakyThrows;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OperationWriter implements OperationIssuer {
    private final List<Operation> operations = new LinkedList<>();

    @SneakyThrows
    public void save(String filename) {
        File file = new File(filename);
        new ObjectMapper().writeValue(file, operations);
        operations.clear();
    }

    private void put(OperationType type, Map<String, Object> args) {
        operations.add(new Operation(type, args));
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        put(
                OperationType.ADD_EDGE,
                Map.of("EdgeId", id, "Permissions", permissions, "Trace", trace)
        );
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id, String trace) {
        put(
                OperationType.REMOVE_EDGE,
                Map.of(
                        "Id", id,
                        "Trace", trace
                )
        );
    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        put(
                OperationType.SET_PERMISSIONS,
                Map.of(
                        "Id", id,
                        "Permissions", permissions,
                        "Trace", trace
                )
        );
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        put(
                OperationType.ADD_VERTEX,
                Map.of(
                        "Id", id,
                        "type", type
                )
        );
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