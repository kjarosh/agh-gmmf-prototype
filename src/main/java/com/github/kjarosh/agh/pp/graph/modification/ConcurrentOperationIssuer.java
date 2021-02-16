package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import com.github.kjarosh.agh.pp.util.BlockingRejectedExecutionHandler;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class ConcurrentOperationIssuer implements OperationIssuer {
    private static final int QUEUE_CAPACITY = 100;

    private final OperationIssuer delegate;
    private final Map<ZoneId, ThreadPoolExecutor> executors;
    private final AtomicDouble saturation = new AtomicDouble();
    private final AtomicDouble requestTime = new AtomicDouble();
    private final AtomicInteger failed = new AtomicInteger();
    private final int maxPoolSize;

    public ConcurrentOperationIssuer(int maxPoolSize, OperationIssuer delegate) {
        this.maxPoolSize = maxPoolSize;
        if (maxPoolSize >= 1) {
            this.executors = new HashMap<>();
        } else {
            this.executors = null;
        }
        this.delegate = delegate;
    }

    public double getSaturation() {
        return saturation.get();
    }

    public double getRequestTime() {
        return requestTime.get();
    }

    public int getFailed() {
        return failed.get();
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        submit(zone, () -> delegate.addEdge(zone, id, permissions, trace));
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        submit(zone, () -> delegate.addEdges(zone, bulkRequest));
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id, String trace) {
        submit(zone, () -> delegate.removeEdge(zone, id, trace));

    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions, String trace) {
        submit(zone, () -> delegate.setPermissions(zone, id, permissions, trace));
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        submit(id.owner(), () -> delegate.addVertex(id, type));
    }

    @Override
    public void addVertices(ZoneId zone, BulkVertexCreationRequestDto bulkRequest) {
        submit(zone, () -> delegate.addVertices(zone, bulkRequest));
    }

    @Override
    public void simulateLoad(ZoneId zone, LoadSimulationRequestDto request) {
        submit(zone, () -> delegate.simulateLoad(zone, request));
    }

    private void submit(ZoneId zone, Runnable op) {
        ThreadPoolExecutor executor = getExecutor(zone);
        if (executor == null) {
            execute(op);
            return;
        }
        executor.submit(() -> execute(op));
        refreshSaturation();
    }

    private ThreadPoolExecutor getExecutor(ZoneId zone) {
        if (this.executors == null) {
            return null;
        }
        return this.executors.computeIfAbsent(zone, z -> {
            ThreadFactory factory = new ThreadFactoryBuilder()
                    .setNameFormat("operation-issuer-" + zone + "-%d")
                    .setDaemon(true)
                    .build();
            return new ThreadPoolExecutor(
                    1, maxPoolSize,
                    0, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(QUEUE_CAPACITY), factory,
                    new BlockingRejectedExecutionHandler());
        });
    }

    private void execute(Runnable op) {
        long time = System.nanoTime();
        try {
            op.run();
        } catch (Exception e) {
            log.debug("Error while issuing an operation", e);
            failed.incrementAndGet();
            return;
        }
        time = System.nanoTime() - time;
        requestTime.set(time / 1000000000d);
    }

    private void refreshSaturation() {
        double queue = executors.values()
                .stream()
                .map(ThreadPoolExecutor::getQueue)
                .mapToInt(Collection::size)
                .average()
                .getAsDouble();
        saturation.set(queue / QUEUE_CAPACITY);
    }
}
