package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EventProcessor;
import com.github.kjarosh.agh.pp.index.Inbox;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * An entity which may be transmitted between vertices.
 * Normally is stored in the {@link Inbox} and processed
 * by the {@link EventProcessor}.
 * <p>
 * Is serializable to JSON for sending through HTTP.
 *
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

    @JsonProperty("originalSender")
    private VertexId originalSender;

    @JsonProperty("effectiveVertices")
    private Set<VertexId> effectiveVertices;

    public Event(
            EventType type,
            String trace,
            VertexId sender,
            VertexId originalSender,
            Set<VertexId> effectiveVertices) {
        this.type = type;
        this.trace = trace;
        this.sender = sender;
        this.originalSender = originalSender;
        this.effectiveVertices = new HashSet<>(effectiveVertices);
    }

    @JsonIgnore
    public Set<VertexId> getAllSubjects() {
        Set<VertexId> subjects = new HashSet<>(effectiveVertices);
        subjects.add(sender);
        return subjects;
    }
}
