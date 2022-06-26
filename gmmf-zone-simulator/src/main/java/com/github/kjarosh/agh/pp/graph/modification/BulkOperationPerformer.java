package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkOperationDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class BulkOperationPerformer implements OperationPerformer {
    private final int bulkSize;
    private final OperationPerformer delegate;
    private final Map<ZoneId, Deque<BulkOperationDto>> operationQueue = new ConcurrentHashMap<>();
    private final Map<ZoneId, AtomicInteger> operationQueueSizes = new ConcurrentHashMap<>();

    public BulkOperationPerformer(OperationPerformer delegate, int bulkSize) {
        this.delegate = delegate;
        this.bulkSize = bulkSize;
    }

    private void queueAdd(ZoneId zone, BulkOperationDto dto) {
        operationQueue.computeIfAbsent(zone, k -> new ConcurrentLinkedDeque<>()).add(dto);
        size(zone).incrementAndGet();
    }

    private void queueAddAll(ZoneId zone, Collection<BulkOperationDto> dtos) {
        operationQueue.computeIfAbsent(zone, k -> new ConcurrentLinkedDeque<>()).addAll(dtos);
        size(zone).addAndGet(dtos.size());
    }

    private AtomicInteger size(ZoneId zone) {
        return operationQueueSizes.computeIfAbsent(zone, k -> new AtomicInteger(0));
    }

    private synchronized void delegateIfPossible() {
        operationQueue.forEach((zone, queue) -> {
            AtomicInteger size = size(zone);
            while (size.get() >= bulkSize) {
                List<BulkOperationDto> ops = new ArrayList<>();
                for (int i = 0; i < bulkSize; ++i) {
                    BulkOperationDto dto = queue.pollFirst();
                    if (dto == null) break;
                    ops.add(dto);
                    size.decrementAndGet();
                }
                delegate(zone, ops);
            }
        });
    }

    private void delegate(ZoneId zone, List<BulkOperationDto> ops) {
        delegate.simulateLoad(zone, LoadSimulationRequestDto.builder()
                .operations(ops)
                .build());
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        Objects.requireNonNull(zone);
        Objects.requireNonNull(id);
        Objects.requireNonNull(permissions);
        Objects.requireNonNull(trace);
        queueAdd(zone, BulkOperationDto.builder()
                .type(BulkOperationDto.OperationType.ADD_EDGE)
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
        Objects.requireNonNull(zone);
        Objects.requireNonNull(id);
        Objects.requireNonNull(trace);
        queueAdd(zone, BulkOperationDto.builder()
                .type(BulkOperationDto.OperationType.REMOVE_EDGE)
                .fromId(id.getFrom())
                .toId(id.getTo())
                .trace(trace)
                .build());
        delegateIfPossible();

    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        Objects.requireNonNull(zone);
        Objects.requireNonNull(id);
        Objects.requireNonNull(permissions);
        Objects.requireNonNull(trace);
        queueAdd(zone, BulkOperationDto.builder()
                .type(BulkOperationDto.OperationType.SET_PERMS)
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
        queueAddAll(zone, request.getOperations());
        delegateIfPossible();
    }
}
