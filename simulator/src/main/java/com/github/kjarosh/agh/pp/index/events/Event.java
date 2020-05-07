package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Kamil Jarosz
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Event {
    @JsonProperty("type")
    private EventType type;

    /**
     * What is being added/modified.
     */
    @JsonProperty("subject")
    private VertexId subject;

    /**
     * Which vertex the event comes from.
     */
    @JsonProperty("source")
    private VertexId source;
}
