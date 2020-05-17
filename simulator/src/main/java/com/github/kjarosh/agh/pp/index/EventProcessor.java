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
        Graph graph = graphLoader.getGraph();

        Vertex current = graph.getVertex(id);
        VertexIndex index = current.index();

        Map<VertexId, EffectiveVertex> effectiveVertices;
        Map<VertexId, Edge> edgesToPropagate;
        Map<VertexId, Edge> edgesToCalculate;
        if (event.getType() == EventType.CHILD_CHANGE) {
            effectiveVertices = index.getEffectiveChildren();
            edgesToPropagate = graph.getEdgesBySource(id)
                    .stream()
                    .collect(Collectors.toMap(Edge::dst, Function.identity()));
            edgesToCalculate = graph.getEdgesByDestination(id)
                    .stream()
                    .collect(Collectors.toMap(Edge::src, Function.identity()));
        } else if (event.getType() == EventType.PARENT_CHANGE) {
            effectiveVertices = index.getEffectiveParents();
            edgesToPropagate = graph.getEdgesByDestination(id)
                    .stream()
                    .collect(Collectors.toMap(Edge::src, Function.identity()));
            edgesToCalculate = graph.getEdgesBySource(id)
                    .stream()
                    .collect(Collectors.toMap(Edge::dst, Function.identity()));
        } else {
            throw new AssertionError();
        }

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
            if (!effectiveVertices.containsKey(subjectId)) {
                effectiveVertex = new EffectiveVertex();
                effectiveVertices.put(subjectId, effectiveVertex);
                propagate = true;
            } else {
                effectiveVertex = effectiveVertices.get(subjectId);
            }

            Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
            if (!intermediateVertices.containsAll(eventData)) {
                propagate = true;
                intermediateVertices.addAll(eventData);
            }

            effectiveVertex.setEffectivePermissions(
                    Permissions.combine(effectiveVertex.getEffectivePermissions(), permissions));
        }


        EffectiveVertex effectiveVertex;
        VertexId sender = event.getSender();
        if (!effectiveVertices.containsKey(sender)) {
            effectiveVertex = new EffectiveVertex();
            effectiveVertices.put(sender, effectiveVertex);
            propagate = true;
        } else {
            effectiveVertex = effectiveVertices.get(sender);
        }

        Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
        if (!intermediateVertices.contains(id)) {
            propagate = true;
            intermediateVertices.add(id);
        }

        Permissions perms = edgesToCalculate.values()
                .stream()
                .filter(e -> {
                    if (event.getType() == EventType.CHILD_CHANGE) {
                        return intermediateVertices.contains(e.dst()) && e.src().equals(sender);
                    } else if (event.getType() == EventType.PARENT_CHANGE) {
                        return intermediateVertices.contains(e.src()) && e.dst().equals(sender);
                    } else {
                        throw new AssertionError();
                    }
                })
                .map(Edge::permissions)
                .filter(Objects::nonNull)
                .reduce(Permissions::combine)
                .orElse(Permissions.NONE);
        effectiveVertex.setEffectivePermissions(perms);


        if (propagate) {
            edgesToPropagate.forEach((k, e) -> propagateEvent(id, event, e, effectiveVertices));
        }
    }

    private void propagateEvent(
            VertexId id, Event event, Edge e,
            Map<VertexId, EffectiveVertex> effectiveVertices) {
        // select the other end of the relation
        VertexId recipient;
        if (id.equals(e.dst())) {
            recipient = e.src();
        } else {
            recipient = e.dst();
        }

        Event newEvent = Event.builder()
                .type(event.getType())
                .effectiveVertices(effectiveVertices)
                .sender(id)
                .build();

        inbox.post(recipient, newEvent);
    }
}
