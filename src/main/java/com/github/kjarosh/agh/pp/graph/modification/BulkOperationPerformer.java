package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.OperationDto;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class BulkOperationPerformer implements OperationPerformer {
    private final int bulkSize;
    private final OperationPerformer delegate;
    private final Map<ZoneId, Deque<OperationDto>> operationQueue = new ConcurrentHashMap<>();

    public BulkOperationPerformer(OperationPerformer delegate, int bulkSize) {
        this.delegate = delegate;
        this.bulkSize = bulkSize;
    }

    private Deque<OperationDto> queue(ZoneId zone) {
        return operationQueue.computeIfAbsent(zone, k -> new ConcurrentLinkedDeque<>());
    }

    private synchronized void delegateIfPossible() {
        operationQueue.forEach((zone, queue) -> {
            while (queue.size() >= bulkSize) {
                List<OperationDto> ops = new ArrayList<>();
                for (int i = 0; i < bulkSize; ++i) {
                    ops.add(queue.removeFirst());
                }
                delegate(zone, ops);
            }
        });
    }

    private void delegate(ZoneId zone, List<OperationDto> ops) {
        delegate.simulateLoad(zone, LoadSimulationRequestDto.builder()
                .operations(ops)
                .build());
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        queue(zone).add(OperationDto.builder()
                .type(OperationDto.OperationType.ADD_EDGE)
                .fromId(id.getFrom())
                .toId(id.getTo())
                .permissions(permissions)
                .trace(trace)
                .build());
        delegateIfPossible();
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        bulkRequest.getEdges().forEach(e -> {
            VertexId from = new VertexId(bulkRequest.getSourceZone(), e.getFromName());
            VertexId to = new VertexId(bulkRequest.getDestinationZone(), e.getToName());
            addEdge(zone,
                    new EdgeId(from, to),
                    new Permissions(e.getPermissions()),
                    e.getTrace());
        });
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id, String trace) {
        queue(zone).add(OperationDto.builder()
                .type(OperationDto.OperationType.REMOVE_EDGE)
                .fromId(id.getFrom())
                .toId(id.getTo())
                .trace(trace)
                .build());
        delegateIfPossible();

    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        queue(zone).add(OperationDto.builder()
                .type(OperationDto.OperationType.SET_PERMS)
                .fromId(id.getFrom())
                .toId(id.getTo())
                .permissions(permissions)
                .trace(trace)
                .build());
        delegateIfPossible();
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        delegate.addVertex(id, type);
    }

    @Override
    public void addVertices(ZoneId zone, BulkVertexCreationRequestDto bulkRequest) {
        delegate.addVertices(zone, bulkRequest);
    }

    @Override
    public void simulateLoad(ZoneId zone, LoadSimulationRequestDto request) {
        queue(zone).addAll(request.getOperations());
        delegateIfPossible();
    }
}
