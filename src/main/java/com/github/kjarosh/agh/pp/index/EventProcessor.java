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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    @Autowired
    private GraphLoader graphLoader;

    @Autowired
    private Inbox inbox;

    private AtomicLong totalTime = new AtomicLong();
    private AtomicLong count = new AtomicLong();
    private Instant lastReset = Instant.now();

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

        VertexIndex index = graph.getVertex(id).index();

        AtomicBoolean propagate = new AtomicBoolean(false);
        AtomicReference<EventType> eventType = new AtomicReference<>(event.getType());
        if (delete) {
            VertexId toRemove = event.getOriginalSender();
            index.getEffectiveParent(toRemove).ifPresent(effectiveVertex -> {
                effectiveVertex.removeIntermediateVertex(event.getSender(), () -> propagate.set(true));
                if (effectiveVertex.getIntermediateVertices().isEmpty()) {
                    index.removeEffectiveParent(toRemove);
                } else {
                    eventType.set(EventType.PARENT_CHANGE);
                }
            });
        } else {
            for (VertexId subjectId : event.getAllSubjects()) {
                EffectiveVertex effectiveVertex = index.getOrAddEffectiveParent(subjectId, () -> propagate.set(true));
                effectiveVertex.addIntermediateVertex(event.getSender(), () -> propagate.set(true));
            }
        }

        if (propagate.get()) {
            Set<VertexId> effectiveParents = eventType.get() == EventType.PARENT_REMOVE ?
                    Collections.emptySet() : index.getEffectiveParentsSet();
            Set<VertexId> recipients = graph.getSourcesByDestination(id);
            propagateEvent(id, recipients, event, effectiveParents, eventType.get());
        }
    }

    public void processChild(VertexId id, Event event, boolean delete) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = graph.getVertex(id).index();
        Set<Edge> edgesToCalculate = graph.getEdgesByDestination(id);

        AtomicBoolean propagate = new AtomicBoolean(false);
        AtomicReference<EventType> eventType = new AtomicReference<>(event.getType());
        if (delete) {
            VertexId toRemove = event.getOriginalSender();
            index.getEffectiveChild(toRemove).ifPresent(effectiveVertex -> {
                effectiveVertex.removeIntermediateVertex(event.getSender(), () -> propagate.set(true));
                if (effectiveVertex.getIntermediateVertices().isEmpty()) {
                    index.removeEffectiveChild(toRemove);
                } else {
                    eventType.set(EventType.CHILD_CHANGE);
                    recalculatePermissions(event, edgesToCalculate, toRemove, effectiveVertex);
                }
            });
        } else {
            ExecutorService executor = GlobalExecutors.getCalculationExecutor();
            List<Future<?>> futures = new ArrayList<>();
            Set<VertexId> allSubjects = event.getAllSubjects();
            for (VertexId subjectId : allSubjects) {
                Runnable job = () -> {
                    EffectiveVertex effectiveVertex = index.getOrAddEffectiveChild(subjectId, () -> propagate.set(true));
                    effectiveVertex.addIntermediateVertex(event.getSender(), () -> propagate.set(true));
                    recalculatePermissions(event, edgesToCalculate, subjectId, effectiveVertex);
                };
                if (executor != null) {
                    futures.add(executor.submit(job));
                } else {
                    job.run();
                }
            }
            waitForAll(futures);
        }

        if (propagate.get()) {
            Set<VertexId> effectiveChildren = eventType.get() == EventType.CHILD_REMOVE ?
                    Collections.emptySet() : index.getEffectiveChildrenSet();
            Set<VertexId> recipients = graph.getDestinationsBySource(id);
            propagateEvent(id, recipients, event, effectiveChildren, eventType.get());
        }
    }

    private void recalculatePermissions(Event event, Set<Edge> edgesToCalculate, VertexId subjectId, EffectiveVertex effectiveVertex) {
        effectiveVertex.recalculatePermissions(edgesToCalculate).thenAccept(result -> {
            if (result == RecalculationResult.DIRTY) {
                instrumentation.notify(Notification.markedDirty(subjectId, event));
                log.warn("Marking vertex {} as dirty", subjectId);
            } else if (result == RecalculationResult.CLEANED) {
                instrumentation.notify(Notification.markedClean(subjectId, event));
                log.info("Marking vertex {} as not dirty", subjectId);
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
