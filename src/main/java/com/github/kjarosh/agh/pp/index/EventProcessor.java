package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.instrumentation.Instrumentation;
import com.github.kjarosh.agh.pp.instrumentation.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Class responsible for processing {@link Event}s and
 * potentially propagating them.
 *
 * @author Kamil Jarosz
 */
@Service
public class EventProcessor {
    @Autowired
    private GraphLoader graphLoader;

    @Autowired
    private Inbox inbox;

    private Instrumentation instrumentation = Instrumentation.getInstance();

    public void process(VertexId id, Event event) {
        instrumentation.notify(Notification.startProcessing(event));
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
            if (successful) {
                instrumentation.notify(Notification.endProcessing(event));
            } else {
                instrumentation.notify(Notification.failProcessing(event));
            }
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
            Set<VertexId> effectiveParents = index.getEffectiveParents().keySet();
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
                effectiveVertex.recalculatePermissions(edgesToCalculate);
            }
        }

        if (propagate.get()) {
            Set<VertexId> effectiveChildren = index.getEffectiveChildren().keySet();
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
            instrumentation.notify(Notification.forkEvent(event, size));
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
}
