package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex.RecalculationResult;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.instrumentation.Instrumentation;
import com.github.kjarosh.agh.pp.instrumentation.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
            totalTime.addAndGet(time);
            count.incrementAndGet();
            instrumentation.notify(notification);
        }
    }

    public void processParent(VertexId id, Event event, boolean delete) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = graph.getVertex(id).index();

        AtomicBoolean propagate = new AtomicBoolean(false);
        if (delete) {
            VertexId toRemove = event.getOriginalSender();
            index.getEffectiveParent(toRemove).ifPresent(effectiveVertex -> {
                effectiveVertex.removeIntermediateVertex(event.getSender(), () -> propagate.set(true));
                if (effectiveVertex.getIntermediateVertices().isEmpty()) {
                    index.removeEffectiveParent(toRemove);
                }
            });
        } else {
            for (VertexId subjectId : event.getAllSubjects()) {
                EffectiveVertex effectiveVertex = index.getOrAddEffectiveParent(subjectId, () -> propagate.set(true));
                effectiveVertex.addIntermediateVertex(event.getSender(), () -> propagate.set(true));
            }
        }

        if (propagate.get()) {
            Set<VertexId> effectiveParents = index.getEffectiveParentsSet();
            Set<VertexId> recipients = graph.getEdgesByDestination(id)
                    .stream()
                    .map(Edge::src)
                    .collect(Collectors.toSet());
            propagateEvent(id, recipients, event, effectiveParents);
        }
    }

    public void processChild(VertexId id, Event event, boolean delete) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = graph.getVertex(id).index();
        Set<Edge> edgesToCalculate = graph.getEdgesByDestination(id);

        AtomicBoolean propagate = new AtomicBoolean(false);
        if (delete) {
            VertexId toRemove = event.getOriginalSender();
            index.getEffectiveChild(toRemove).ifPresent(effectiveVertex -> {
                effectiveVertex.removeIntermediateVertex(event.getSender(), () -> propagate.set(true));
                if (effectiveVertex.getIntermediateVertices().isEmpty()) {
                    index.removeEffectiveChild(toRemove);
                }
            });
        } else {
            for (VertexId subjectId : event.getAllSubjects()) {
                EffectiveVertex effectiveVertex = index.getOrAddEffectiveChild(subjectId, () -> propagate.set(true));
                effectiveVertex.addIntermediateVertex(event.getSender(), () -> propagate.set(true));
                effectiveVertex.recalculatePermissions(edgesToCalculate).thenAccept(result -> {
                    if (result == RecalculationResult.DIRTY) {
                        instrumentation.notify(Notification.markedDirty(id, event));
                        log.warn("Marking vertex {} as dirty", subjectId);
                    } else if (result == RecalculationResult.CLEANED) {
                        instrumentation.notify(Notification.markedClean(id, event));
                        log.info("Marking vertex {} as not dirty", subjectId);
                    }
                });
            }
        }

        if (propagate.get()) {
            Set<VertexId> effectiveChildren = index.getEffectiveChildrenSet();
            Set<VertexId> recipients = graph.getEdgesBySource(id)
                    .stream()
                    .map(Edge::dst)
                    .collect(Collectors.toSet());
            propagateEvent(id, recipients, event, effectiveChildren);
        }
    }

    private void propagateEvent(
            VertexId sender,
            Collection<VertexId> recipients,
            Event event,
            Set<VertexId> effectiveVertices) {
        int size = recipients.size();
        if (size > 0) {
            instrumentation.notify(Notification.forkEvent(sender, event, size));
        }
        recipients.forEach(r -> {
            Event newEvent = Event.builder()
                    .trace(event.getTrace())
                    .type(event.getType())
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
