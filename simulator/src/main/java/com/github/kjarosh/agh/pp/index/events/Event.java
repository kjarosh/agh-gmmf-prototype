package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

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

    @JsonProperty("sender")
    private VertexId sender;

    @JsonProperty("effectiveVertices")
    private Map<VertexId, EffectiveVertex> effectiveVertices
            = new HashMap<>();

    public Event(
            EventType type,
            VertexId sender,
            Map<VertexId, EffectiveVertex> effectiveVertices) {
        this.type = type;
        this.sender = sender;
        effectiveVertices.forEach((key, ev) -> {
            this.effectiveVertices.put(key, ev.copy());
        });
    }
}
