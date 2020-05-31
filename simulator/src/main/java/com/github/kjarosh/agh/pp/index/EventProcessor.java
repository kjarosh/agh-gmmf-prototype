package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
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

        Vertex current = graph.getVertex(id);
        VertexIndex index = current.index();

        Map<VertexId, Edge> edgesToPropagate = graph.getEdgesByDestination(id)
                .stream()
                .collect(Collectors.toMap(Edge::src, Function.identity()));
        Map<VertexId, Edge> edgesToCalculate = graph.getEdgesBySource(id)
                .stream()
                .collect(Collectors.toMap(Edge::dst, Function.identity()));

        AtomicBoolean propagate = new AtomicBoolean(false);

        for (Map.Entry<VertexId, EffectiveVertex> e : event.getEffectiveVertices().entrySet()) {
            VertexId subjectId = e.getKey();
            EffectiveVertex subject = e.getValue();

            Permissions permissions = subject.getEffectivePermissions();
            Set<VertexId> eventData = new HashSet<>(subject.getIntermediateVertices());

            if (edgesToCalculate.containsKey(subjectId)) {
                eventData.add(id);
                permissions = Permissions.combine(permissions, edgesToCalculate.get(subjectId).permissions());
            }

            EffectiveVertex effectiveVertex = index.getEffectiveParent(subjectId, () -> propagate.set(true));
            effectiveVertex.addIntermediateVertices(eventData, () -> propagate.set(true));
            effectiveVertex.combine(permissions);
        }

        EffectiveVertex effectiveVertex = index.getEffectiveParent(event.getSender(), () -> propagate.set(true));
        effectiveVertex.addIntermediateVertex(id, () -> propagate.set(true));
        Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
        Permissions perms = edgesToCalculate.values()
                .stream()
                .filter(e -> intermediateVertices.contains(e.src()) && e.dst().equals(event.getSender()))
                .map(Edge::permissions)
                .filter(Objects::nonNull)
                .reduce(Permissions::combine)
                .orElse(Permissions.NONE);
        effectiveVertex.combine(perms);

        if (propagate.get()) {
            edgesToPropagate.keySet().forEach(recipient ->
                    propagateEvent(id, recipient, event, index.getEffectiveParents()));
        }
    }

    public void processChildChange(VertexId id, Event event) {
        Graph graph = graphLoader.getGraph();

        Vertex current = graph.getVertex(id);
        VertexIndex index = current.index();

        Map<VertexId, Edge> edgesToPropagate = graph.getEdgesBySource(id)
                .stream()
                .collect(Collectors.toMap(Edge::dst, Function.identity()));
        Map<VertexId, Edge> edgesToCalculate = graph.getEdgesByDestination(id)
                .stream()
                .collect(Collectors.toMap(Edge::src, Function.identity()));

        AtomicBoolean propagate = new AtomicBoolean(false);


        Set<VertexId> subjects = new HashSet<>(event.getEffectiveVertices().keySet());
        subjects.add(event.getSender());
        for (VertexId subjectId : subjects) {
            EffectiveVertex effectiveVertex = index.getEffectiveChild(subjectId, () -> propagate.set(true));
            effectiveVertex.addIntermediateVertex(event.getSender(), () -> propagate.set(true));
            Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
            List<Permissions> perms = edgesToCalculate.values()
                    .stream()
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
            edgesToPropagate.keySet().forEach(recipient ->
                    propagateEvent(id, recipient, event, index.getEffectiveChildren()));
        }
    }

    private void propagateEvent(
            VertexId id,
            VertexId recipient,
            Event event,
            Map<VertexId, EffectiveVertex> effectiveVertices) {
        Event newEvent = Event.builder()
                .type(event.getType())
                .effectiveVertices(effectiveVertices)
                .sender(id)
                .build();

        inbox.post(recipient, newEvent);
    }
}
