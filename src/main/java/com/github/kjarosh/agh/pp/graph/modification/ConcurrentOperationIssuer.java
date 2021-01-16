package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.util.BlockingRejectedExecutionHandler;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class ConcurrentOperationIssuer implements OperationIssuer {
    private static final int QUEUE_CAPACITY = 50;

    private final ThreadFactory treadFactory = new ThreadFactoryBuilder()
            .setNameFormat("operation-issuer-%d")
            .build();
    private final OperationIssuer delegate;
    private final ThreadPoolExecutor executor;
    private final AtomicDouble saturation = new AtomicDouble();
    private final AtomicDouble requestTime = new AtomicDouble();

    public ConcurrentOperationIssuer(int maxPoolSize, OperationIssuer delegate) {
        this.executor = new ThreadPoolExecutor(1, maxPoolSize,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY), treadFactory,
                new BlockingRejectedExecutionHandler());
        this.delegate = delegate;
    }

    public double getSaturation() {
        return saturation.get();
    }

    public double getRequestTime() {
        return requestTime.get();
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions) {
        submit(() -> delegate.addEdge(zone, id, permissions));
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        submit(() -> delegate.addEdges(zone, bulkRequest));
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id) {
        submit(() -> delegate.removeEdge(zone, id));

    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions) {
        submit(() -> delegate.setPermissions(zone, id, permissions));
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        submit(() -> delegate.addVertex(id, type));
    }

    @Override
    public void addVertices(ZoneId zone, BulkVertexCreationRequestDto bulkRequest) {
        submit(() -> delegate.addVertices(zone, bulkRequest));
    }

    private void submit(Runnable op) {
        executor.submit(() -> {
            long time = System.nanoTime();
            op.run();
            time = System.nanoTime() - time;
            requestTime.set(time / 1000000000d);
        });
        refreshSaturation();
    }

    private void refreshSaturation() {
        saturation.set((double) executor.getQueue().size() / QUEUE_CAPACITY);
    }
}