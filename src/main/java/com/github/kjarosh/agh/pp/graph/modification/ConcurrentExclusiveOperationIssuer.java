package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kamil Jarosz
 */
public class ConcurrentExclusiveOperationIssuer implements OperationIssuer {
    private final OperationIssuer delegate;
    private final Thread[] threads;

    private final Object lock = new Object();
    private final Deque<Runnable> unsyncd = new ArrayDeque<>();
    private final Map<EdgeId, Deque<Runnable>> syncd = new HashMap<>();
    private final Map<EdgeId, Boolean> syncdRunning = new HashMap<>();

    public ConcurrentExclusiveOperationIssuer(int poolSize, OperationIssuer delegate) {
        this.threads = new Thread[poolSize];
        this.delegate = delegate;

        for (int i = 0; i < poolSize; ++i) {
            this.threads[i] = new Thread(this::run);
            this.threads[i].start();
        }
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId id, Permissions permissions) {
        synchronized (lock) {
            syncd.computeIfAbsent(id, e -> new ArrayDeque<>())
                    .push(() -> delegate.addEdge(zone, id, permissions));
            syncdRunning.putIfAbsent(id, false);
            lock.notifyAll();
        }
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId id) {
        synchronized (lock) {
            syncd.computeIfAbsent(id, e -> new ArrayDeque<>())
                    .push(() -> delegate.removeEdge(zone, id));
            syncdRunning.putIfAbsent(id, false);
            lock.notifyAll();
        }
    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId id, Permissions permissions) {
        synchronized (lock) {
            syncd.computeIfAbsent(id, e -> new ArrayDeque<>())
                    .push(() -> delegate.setPermissions(zone, id, permissions));
            syncdRunning.putIfAbsent(id, false);
            lock.notifyAll();
        }
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        synchronized (lock) {
            unsyncd.push(() -> delegate.addVertex(id, type));
            lock.notifyAll();
        }
    }

    private void run() {
        Runnable todo = null;
        try {
            todo = popNextJob();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (todo != null) {
            todo.run();
        }
    }

    private Runnable popNextJob() throws InterruptedException {
        synchronized (lock) {
            while (true) {
                if (!unsyncd.isEmpty()) {
                    lock.notifyAll();
                    return unsyncd.pop();
                }

                for (EdgeId e : syncd.keySet()) {
                    boolean running = syncdRunning.get(e);
                    if (!running && !syncd.get(e).isEmpty()) {
                        syncdRunning.put(e, true);
                        Runnable job = syncd.get(e).pop();
                        return () -> {
                            try {
                                job.run();
                            } finally {
                                synchronized (lock) {
                                    syncdRunning.put(e, false);
                                    lock.notifyAll();
                                }
                            }
                        };
                    }
                }

                lock.wait();
            }
        }
    }
}
