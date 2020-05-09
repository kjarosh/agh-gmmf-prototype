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

    public void process(VertexId id, Event event) {
        Graph graph = graphLoader.getGraph();
        VertexId sourceId = event.getSource();
        Set<VertexId> subjectIds = event.getSubjects();

        Vertex current = graph.getVertex(id);
        VertexIndex index = current.index();

        Map<VertexId, VertexIndex.EffectiveVertex> effectiveVertices;
        if (event.getType() == EventType.CHILD_CHANGE) {
            effectiveVertices = index.getEffectiveChildren();
        } else if (event.getType() == EventType.PARENT_CHANGE) {
            effectiveVertices = index.getEffectiveParents();
        } else {
            throw new AssertionError();
        }

        boolean propagate = false;

        for (VertexId subjectId : subjectIds) {
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
                intermediateVertices.add(sourceId);
            }
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
