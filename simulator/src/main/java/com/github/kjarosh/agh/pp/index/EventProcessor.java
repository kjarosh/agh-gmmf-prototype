package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void process(VertexId id, Event event, Runnable callback) {
        Graph graph = graphLoader.getGraph();
        VertexId sourceId = event.getSource();
        VertexId subjectId = event.getSubject();
        Vertex subject = graph.getVertex(subjectId);
        VertexIndex index = subject.index();

        Map<VertexId, VertexIndex.EffectiveVertex> effectiveVertices;
        if (event.getType() == EventType.CHILD_CHANGE) {
            effectiveVertices = index.getEffectiveChildren();
        } else if (event.getType() == EventType.PARENT_CHANGE) {
            effectiveVertices = index.getEffectiveParents();
        } else {
            throw new AssertionError();
        }

        boolean propagate = false;
        VertexIndex.EffectiveVertex effectiveVertex;
        if (!effectiveVertices.containsKey(subjectId)) {
            effectiveVertex = new VertexIndex.EffectiveVertex();
            effectiveVertices.put(subjectId, effectiveVertex);
            propagate = true;
        } else {
            effectiveVertex = effectiveVertices.get(subjectId);
        }

        Set<VertexId> intermediateVertices = effectiveVertex.getIntermediateVertices();
        if (!intermediateVertices.contains(sourceId)) {
            propagate = true;
            intermediateVertices.add(subjectId);
        }

        if (propagate) {
            if (event.getType() == EventType.CHILD_CHANGE) {
                graph.getEdgesBySource(id).forEach(e -> {
                    propagateEvent(id, event, e);
                });
            } else if (event.getType() == EventType.PARENT_CHANGE) {
                graph.getEdgesByDestination(id).forEach(e -> {
                    propagateEvent(id, event, e);
                });
            } else {
                throw new AssertionError();
            }
        }

        callback.run();
    }

    private void propagateEvent(VertexId id, Event event, Edge e) {
        Event newEvent = Event.builder()
                .type(event.getType())
                .source(id)
                .subject(event.getSubject())
                .build();
        inbox.post(e.dst(), newEvent);
    }
}
