package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kamil Jarosz
 */
@Getter
@Builder
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Event {
    @JsonProperty("type")
    private EventType type;

    @JsonProperty("trace")
    private String trace;

    @JsonProperty("sender")
    private VertexId sender;

    @JsonProperty("effectiveVertices")
    private Set<VertexId> effectiveVertices;

    public Event(
            EventType type,
            String trace,
            VertexId sender,
            Set<VertexId> effectiveVertices) {
        this.type = type;
        this.trace = trace;
        this.sender = sender;
        this.effectiveVertices = new HashSet<>(effectiveVertices);
    }

    @JsonIgnore
    public Set<VertexId> getAllSubjects() {
        Set<VertexId> subjects = new HashSet<>(effectiveVertices);
        subjects.add(sender);
        return subjects;
    }
}
