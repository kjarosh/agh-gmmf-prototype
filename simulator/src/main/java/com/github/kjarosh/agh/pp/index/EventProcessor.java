package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

        AtomicBoolean propagate = new AtomicBoolean(false);
        for (VertexId subjectId : event.getAllSubjects()) {
            index.addEffectiveParent(subjectId, () -> propagate.set(true));
        }

        if (propagate.get()) {
            Set<VertexId> effectiveVertices = index.getEffectiveParents();
            graph.getEdgesByDestination(id)
                    .stream()
                    .map(Edge::src)
                    .forEach(recipient -> propagateEvent(id, recipient, event, effectiveVertices));
        }
    }

    public void processChildChange(VertexId id, Event event) {
        Graph graph = graphLoader.getGraph();

        VertexIndex index = graph.getVertex(id).index();
        Set<Edge> edgesToCalculate = graph.getEdgesByDestination(id);

        AtomicBoolean propagate = new AtomicBoolean(false);
        for (VertexId subjectId : event.getAllSubjects()) {
            EffectiveVertex effectiveVertex = index.getEffectiveChild(subjectId, () -> propagate.set(true));
            effectiveVertex.addIntermediateVertex(event.getSender(), () -> propagate.set(true));
            Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
            List<Permissions> perms = edgesToCalculate.stream()
                    .filter(x -> intermediateVertices.contains(x.src()))
                    .map(Edge::permissions)
                    .collect(Collectors.toList());
            if (perms.size() != intermediateVertices.size()) {
                throw new RuntimeException();
            }
            effectiveVertex.setEffectivePermissions(perms.stream()
                    .reduce(Permissions.NONE, Permissions::combine));
        }

        if (propagate.get()) {
            Set<VertexId> effectiveVertices = index.getEffectiveChildren().keySet();
            graph.getEdgesBySource(id)
                    .stream()
                    .map(Edge::dst)
                    .forEach(recipient -> propagateEvent(id, recipient, event, effectiveVertices));
        }
    }

    private void propagateEvent(
            VertexId id,
            VertexId recipient,
            Event event,
            Set<VertexId> effectiveVertices) {
        Event newEvent = Event.builder()
                .trace(event.getTrace())
                .type(event.getType())
                .effectiveVertices(effectiveVertices)
                .sender(id)
                .build();

        inbox.post(recipient, newEvent);
    }
}
