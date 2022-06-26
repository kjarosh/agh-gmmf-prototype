package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex.RecalculationResult;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import com.github.kjarosh.agh.pp.instrumentation.Instrumentation;
import com.github.kjarosh.agh.pp.instrumentation.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class responsible for processing {@link Event}s and
 * potentially propagating them.
 *
 * @author Kamil Jarosz
 */
@Slf4j
@Service
public class EventProcessor {
    private final Instrumentation instrumentation = Instrumentation.getInstance();

    private final GraphLoader graphLoader;
    private final VertexIndices vertexIndices;

    private final Inbox inbox;

    private final AtomicLong totalTime = new AtomicLong();
    private final AtomicLong count = new AtomicLong();
    private Instant lastReset = Instant.now();

    public EventProcessor(Inbox inbox, GraphLoader graphLoader, VertexIndices vertexIndices) {
        this.inbox = inbox;
        this.graphLoader = graphLoader;
        this.vertexIndices = vertexIndices;
    }

    public void process(VertexId id, Event event) {
        long start = System.nanoTime();
        instrumentation.notify(Notification.startProcessing(id, event));
        boolean successful = false;

        try {
            switch (event.getType()) {
                case CHILD_CHANGE: {
                    processChild(id, event, false);
                    break;
                }

                case PARENT_CHANGE: {
                    processParent(id, event, false);
                    break;
                }

                case CHILD_REMOVE: {
                    processChild(id, event, true);
                    break;
                }

                case PARENT_REMOVE: {
                    processParent(id, event, true);
                    break;
                }

                default: {
                    throw new AssertionError();
                }
            }
            successful = true;
        } finally {
            Notification notification;
            if (successful) {
                notification = Notification.endProcessing(id, event);
            } else {
                notification = Notification.failProcessing(id, event);
            }
            long time = System.nanoTime() - start;
            long tt = totalTime.addAndGet(time);
            long c = count.incrementAndGet();
            if (c > 0 && Duration.between(lastReset, Instant.now()).toSeconds() > 10) {
                lastReset = Instant.now();
                count.set(0);
                totalTime.set(0);
                log.info("Average processing time from last {} events: {} ms",
                        c,
                        ((double) tt / c / TimeUnit.MILLISECONDS.toNanos(1)));
            }
            instrumentation.notify(notification);
        }
    }

    public void processParent(VertexId id, Event event, boolean delete) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = vertexIndices.getIndexOf(graph.getVertex(id));
        Set<VertexId> effectiveParents = new ConcurrentSkipListSet<>();
        for (VertexId subjectId : event.getEffectiveVertices()) {
            if (delete) {
                index.getEffectiveParent(subjectId).ifPresent(effectiveVertex -> {
                    effectiveVertex.removeIntermediateVertex(event.getSender());
                    if (effectiveVertex.getIntermediateVertices().isEmpty()) {
                        index.removeEffectiveParent(subjectId);
                        effectiveParents.add(subjectId);
                    }
                });
            } else {
                EffectiveVertex effectiveVertex = index.getOrAddEffectiveParent(subjectId);
                effectiveVertex.addIntermediateVertex(event.getSender(), () -> effectiveParents.add(subjectId));
            }
        }

        if (!effectiveParents.isEmpty()) {
            Set<VertexId> recipients = graph.getSourcesByDestination(id);
            propagateEvent(id, recipients, event, effectiveParents, event.getType());
        }
    }

    public void processChild(VertexId id, Event event, boolean delete) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = vertexIndices.getIndexOf(graph.getVertex(id));
        Set<Edge> edgesToCalculate = graph.getEdgesByDestination(id);

        Set<VertexId> effectiveChildren = new ConcurrentSkipListSet<>();
        ExecutorService executor = GlobalExecutors.getCalculationExecutor();
        List<Future<?>> futures = new ArrayList<>();
        for (VertexId subjectId : event.getEffectiveVertices()) {
            Runnable job;
            if (delete) {
                job = () -> {
                    index.getEffectiveChild(subjectId).ifPresent(effectiveVertex -> {
                        effectiveVertex.removeIntermediateVertex(event.getSender());
                        if (effectiveVertex.getIntermediateVertices().isEmpty()) {
                            index.removeEffectiveChild(subjectId);
                            effectiveChildren.add(subjectId);
                        } else {
                            recalculatePermissions(event, edgesToCalculate, subjectId, effectiveVertex);
                        }
                    });
                };
            } else {
                job = () -> {
                    EffectiveVertex effectiveVertex = index.getOrAddEffectiveChild(subjectId);
                    effectiveVertex.addIntermediateVertex(event.getSender(), () -> effectiveChildren.add(subjectId));
                    recalculatePermissions(event, edgesToCalculate, subjectId, effectiveVertex);
                };
            }
            if (executor != null) {
                futures.add(executor.submit(job));
            } else {
                job.run();
            }
        }
        waitForAll(futures);

        if (!effectiveChildren.isEmpty()) {
            Set<VertexId> recipients = graph.getDestinationsBySource(id);
            propagateEvent(id, recipients, event, effectiveChildren, event.getType());
        }
    }

    private void recalculatePermissions(Event event, Set<Edge> edgesToCalculate, VertexId subjectId, EffectiveVertex effectiveVertex) {
        effectiveVertex.recalculatePermissions(edgesToCalculate).thenAccept(result -> {
            if (result == RecalculationResult.DIRTY) {
                instrumentation.notify(Notification.markedDirty(subjectId, event));
                log.debug("Marking vertex {} as dirty", subjectId);
            } else if (result == RecalculationResult.CLEANED) {
                instrumentation.notify(Notification.markedClean(subjectId, event));
                log.debug("Marking vertex {} as not dirty", subjectId);
            }
        });
    }

    private void waitForAll(Collection<Future<?>> futures) {
        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void propagateEvent(
            VertexId sender,
            Collection<VertexId> recipients,
            Event event,
            Set<VertexId> effectiveVertices,
            EventType type) {
        int size = recipients.size();
        if (size > 0) {
            instrumentation.notify(Notification.forkEvent(sender, event, size));
        }
        recipients.forEach(r -> {
            Event newEvent = Event.builder()
                    .trace(event.getTrace())
                    .type(type)
                    .effectiveVertices(effectiveVertices)
                    .sender(sender)
                    .originalSender(event.getOriginalSender())
                    .build();

            inbox.post(r, newEvent);
        });
    }

    public double getAverageProcessingNanos() {
        return (double) totalTime.get() / count.get();
    }
}
