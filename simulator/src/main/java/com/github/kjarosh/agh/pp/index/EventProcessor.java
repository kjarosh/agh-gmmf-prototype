package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.VertexIndex.EffectiveVertex;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        VertexId sourceId = event.getSource();
        Set<VertexId> subjectIds = event.getSubjects();

        Vertex current = graph.getVertex(id);
        VertexIndex index = current.index();

        Map<VertexId, EffectiveVertex> effectiveVertices;
        Set<Edge> edgesToPropagate;
        if (event.getType() == EventType.CHILD_CHANGE) {
            effectiveVertices = index.getEffectiveChildren();
            edgesToPropagate = graph.getEdgesBySource(id);
        } else if (event.getType() == EventType.PARENT_CHANGE) {
            effectiveVertices = index.getEffectiveParents();
            edgesToPropagate = graph.getEdgesByDestination(id);
        } else {
            throw new AssertionError();
        }

        boolean propagate = false;

        for (VertexId subjectId : subjectIds) {
            EffectiveVertex effectiveVertex;
            if (!effectiveVertices.containsKey(subjectId)) {
                effectiveVertex = new EffectiveVertex();
                effectiveVertices.put(subjectId, effectiveVertex);
                propagate = true;
            } else {
                effectiveVertex = effectiveVertices.get(subjectId);
            }

            Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
            if (!intermediateVertices.contains(sourceId)) {
                propagate = true;
                intermediateVertices.add(sourceId);
            }

            refreshPermissions(id, effectiveVertex, edgesToPropagate);
        }

        if (propagate) {
            edgesToPropagate.forEach(e -> propagateEvent(id, event, e));
        }
    }

    private void refreshPermissions(
            VertexId id,
            EffectiveVertex effectiveVertex,
            Set<Edge> edgesToPropagate) {
        Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
        Set<Permissions> allPermissions = new HashSet<>();
        edgesToPropagate.forEach(edge -> {
            VertexId src = edge.src();
            VertexId dst = edge.dst();
            if (dst.equals(id) && intermediateVertices.contains(src)) {
                allPermissions.add(edge.permissions());
            }
            if (src.equals(id) && intermediateVertices.contains(dst)) {
                allPermissions.add(edge.permissions());
            }
        });
        allPermissions.stream()
                .reduce(Permissions::combine)
                .ifPresent(effectiveVertex::setEffectivePermissions);
    }

    private void propagateEvent(VertexId id, Event event, Edge e) {
        Event newEvent = Event.builder()
                .type(event.getType())
                .source(id)
                .subjects(event.getSubjects())
                .build();

        // select the other end of the relation
        VertexId recipient;
        if (id.equals(e.dst())) {
            recipient = e.src();
        } else {
            recipient = e.dst();
        }
        inbox.post(recipient, newEvent);
    }
}
