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
        boolean isChildChange = event.getType() == EventType.CHILD_CHANGE;
        boolean isParentChange = event.getType() == EventType.PARENT_CHANGE;
        if (isChildChange) {
            processChildChange(id, event);
        } else if (isParentChange) {
            processParentChange(id, event);
        } else {
            throw new AssertionError();
        }
    }

    public void processParentChange(VertexId id, Event event) {
        Graph graph = graphLoader.getGraph();

        Vertex current = graph.getVertex(id);
        VertexIndex index = current.index();

        Map<VertexId, EffectiveVertex> effectiveParents = index.getEffectiveParents();
        Map<VertexId, Edge> edgesToPropagate = graph.getEdgesByDestination(id)
                .stream()
                .collect(Collectors.toMap(Edge::src, Function.identity()));
        Map<VertexId, Edge> edgesToCalculate = graph.getEdgesBySource(id)
                .stream()
                .collect(Collectors.toMap(Edge::dst, Function.identity()));

        boolean propagate = false;

        for (Map.Entry<VertexId, EffectiveVertex> e : event.getEffectiveVertices().entrySet()) {
            VertexId subjectId = e.getKey();
            EffectiveVertex subject = e.getValue();

            Permissions permissions = subject.getEffectivePermissions();
            Set<VertexId> eventData = new HashSet<>(subject.getIntermediateVertices());

            if (edgesToCalculate.containsKey(subjectId)) {
                eventData.add(id);
                permissions = Permissions.combine(permissions, edgesToCalculate.get(subjectId).permissions());
            }

            EffectiveVertex effectiveVertex;
            if (!effectiveParents.containsKey(subjectId)) {
                effectiveVertex = new EffectiveVertex();
                effectiveParents.put(subjectId, effectiveVertex);
                propagate = true;
            } else {
                effectiveVertex = effectiveParents.get(subjectId);
            }

            Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
            if (!intermediateVertices.containsAll(eventData)) {
                propagate = true;
                intermediateVertices.addAll(eventData);
            }
            effectiveVertex.combine(permissions);
        }

        EffectiveVertex effectiveVertex;
        VertexId sender = event.getSender();
        if (!effectiveParents.containsKey(sender)) {
            effectiveVertex = new EffectiveVertex();
            effectiveParents.put(sender, effectiveVertex);
            propagate = true;
        } else {
            effectiveVertex = effectiveParents.get(sender);
        }

        Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
        if (!intermediateVertices.contains(id)) {
            propagate = true;
            intermediateVertices.add(id);
        }

        Permissions perms = edgesToCalculate.values()
                .stream()
                .filter(e -> intermediateVertices.contains(e.src()) && e.dst().equals(sender))
                .map(Edge::permissions)
                .filter(Objects::nonNull)
                .reduce(Permissions::combine)
                .orElse(Permissions.NONE);
        effectiveVertex.combine(perms);

        if (propagate) {
            edgesToPropagate.keySet().forEach(recipient ->
                    propagateEvent(id, recipient, event, effectiveParents));
        }
    }

    public void processChildChange(VertexId id, Event event) {
        Graph graph = graphLoader.getGraph();

        Vertex current = graph.getVertex(id);
        VertexIndex index = current.index();

        Map<VertexId, EffectiveVertex> effectiveChildren = index.getEffectiveChildren();
        Map<VertexId, Edge> edgesToPropagate = graph.getEdgesBySource(id)
                .stream()
                .collect(Collectors.toMap(Edge::dst, Function.identity()));
        Map<VertexId, Edge> edgesToCalculate = graph.getEdgesByDestination(id)
                .stream()
                .collect(Collectors.toMap(Edge::src, Function.identity()));

        boolean propagate = false;

        for (VertexId subjectId : event.getEffectiveVertices().keySet()) {
            EffectiveVertex effectiveVertex;
            if (!effectiveChildren.containsKey(subjectId)) {
                effectiveVertex = new EffectiveVertex();
                effectiveChildren.put(subjectId, effectiveVertex);
                propagate = true;
            } else {
                effectiveVertex = effectiveChildren.get(subjectId);
            }

            Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
            if (!intermediateVertices.contains(event.getSender())) {
                propagate = true;
                intermediateVertices.add(event.getSender());
            }
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


        EffectiveVertex effectiveVertex;
        VertexId sender = event.getSender();
        if (!effectiveChildren.containsKey(sender)) {
            effectiveVertex = new EffectiveVertex();
            effectiveChildren.put(sender, effectiveVertex);
            propagate = true;
        } else {
            effectiveVertex = effectiveChildren.get(sender);
        }

        Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
        if (!intermediateVertices.contains(event.getSender())) {
            propagate = true;
            intermediateVertices.add(event.getSender());
        }
        List<Permissions> perms = edgesToCalculate.values()
                .stream()
                .filter(x -> intermediateVertices.contains(x.src()))
                .map(Edge::permissions)
                .collect(Collectors.toList());
        if (perms.size() != intermediateVertices.size()) {
            throw new RuntimeException();
        }
        Permissions permissions = perms.stream()
                .reduce(Permissions.NONE, Permissions::combine);
        effectiveVertex.setEffectivePermissions(permissions);

        if (propagate) {
            edgesToPropagate.keySet().forEach(recipient ->
                    propagateEvent(id, recipient, event, effectiveChildren));
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
