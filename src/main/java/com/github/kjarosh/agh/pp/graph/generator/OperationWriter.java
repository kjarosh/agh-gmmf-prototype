package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.kjarosh.agh.pp.graph.model.*;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.util.Operation;
import com.github.kjarosh.agh.pp.graph.util.OperationType;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import lombok.SneakyThrows;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;

public class OperationWriter implements OperationIssuer {
    private final PrintStream file;
    private static final ObjectWriter writer = new ObjectMapper().writerFor(Operation.class);

    public OperationWriter(String filepath) throws IOException {
        file = _setupOutputStream(filepath);
    }

    private static PrintStream _setupOutputStream(String filepath) throws IOException {
        var path = Path.of(filepath);

        if(Files.isDirectory(path)) {
            path = Path.of(filepath, "sequence_"+ DateTime.now()+".json");
        }
        Files.deleteIfExists(path);

        return new PrintStream(Files.newOutputStream(path, CREATE_NEW, APPEND));
    }

    public void save() {
        file.flush();
        file.close();
    }

    @SneakyThrows
    private void put(OperationType type, EdgeId eid, ZoneId zid, String trace, Permissions permissions) {
        put(new Operation(type, eid, zid, trace, permissions));
    }

    @SneakyThrows
    private void put(Operation o) {
        file.println(writer.writeValueAsString(o));
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        put(Operation.builder()
                .type(OperationType.ADD_EDGE)
                .permissions(permissions)
                .zoneId(zone)
                .edgeId(id)
                .trace(trace)
                .build()
        );
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id, String trace) {
        put(Operation.builder()
                .type(OperationType.REMOVE_EDGE)
                .edgeId(id)
                .zoneId(zone)
                .trace(trace)
                .build()
        );
    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        put(Operation.builder()
                .type(OperationType.SET_PERMISSIONS)
                .edgeId(id)
                .zoneId(zone)
                .trace(trace)
                .permissions(permissions)
                .build()
        );
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        throw new RuntimeException("Not implemented");
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