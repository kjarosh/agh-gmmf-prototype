package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public void process(VertexId id, Event event) {
        if (event.getType() == EventType.CHILD_CHANGE) {
            processChildChange(id, event);
        } else if (event.getType() == EventType.PARENT_CHANGE) {
            processParentChange(id, event);
        } else {
            throw new AssertionError();
        }
    }

    public void processParentChange(VertexId id, Event event) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = graph.getVertex(id).index();
        boolean delete = graph.getEdgesBySource(id)
                .stream()
                .map(Edge::dst)
                .noneMatch(event.getSender()::equals);

        AtomicBoolean propagate = new AtomicBoolean(false);
        for (VertexId subjectId : event.getAllSubjects()) {
            if (delete) {
                index.removeEffectiveParentIfExists(subjectId, () -> propagate.set(true));
            } else {
                index.addEffectiveParentIfNotExists(subjectId, () -> propagate.set(true));
            }
        }

        if (propagate.get()) {
            Set<VertexId> effectiveParents = index.getEffectiveParents();
            graph.getEdgesByDestination(id)
                    .stream()
                    .map(Edge::src)
                    .forEach(recipient -> propagateEvent(id, recipient, event, effectiveParents));
        }
    }

    public void processChildChange(VertexId id, Event event) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = graph.getVertex(id).index();
        Set<Edge> edgesToCalculate = graph.getEdgesByDestination(id);
        boolean delete = edgesToCalculate.stream()
                .map(Edge::src)
                .noneMatch(event.getSender()::equals);

        AtomicBoolean propagate = new AtomicBoolean(false);
        if (delete) {
            for (VertexId subjectId : event.getAllSubjects()) {
                index.getEffectiveChild(subjectId).ifPresent(effectiveVertex -> {
                    effectiveVertex.removeIntermediateVertex(event.getSender(), () -> propagate.set(true));
                    if (effectiveVertex.getIntermediateVertices().isEmpty()) {
                        index.removeEffectiveChild(subjectId);
                    } else {
                        effectiveVertex.recalculatePermissions(edgesToCalculate);
                    }
                });
            }
        } else {
            for (VertexId subjectId : event.getAllSubjects()) {
                EffectiveVertex effectiveVertex = index.getOrAddEffectiveChild(subjectId, () -> propagate.set(true));
                effectiveVertex.addIntermediateVertex(event.getSender(), () -> propagate.set(true));
                effectiveVertex.recalculatePermissions(edgesToCalculate);
            }
        }

        if (propagate.get()) {
            Set<VertexId> effectiveChildren = index.getEffectiveChildren().keySet();
            graph.getEdgesBySource(id)
                    .stream()
                    .map(Edge::dst)
                    .forEach(recipient -> propagateEvent(id, recipient, event, effectiveChildren));
        }
    }

    private void propagateEvent(
            VertexId sender,
            VertexId recipient,
            Event event,
            Set<VertexId> effectiveVertices) {
        Event newEvent = Event.builder()
                .trace(event.getTrace())
                .type(event.getType())
                .effectiveVertices(effectiveVertices)
                .sender(sender)
                .build();

        inbox.post(recipient, newEvent);
    }
}
