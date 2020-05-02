package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Kamil Jarosz
 */
public enum EventType {
    @JsonProperty("child")
    CHILD_CHANGE,
    @JsonProperty("parent")
    PARENT_CHANGE,
}
