package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Kamil Jarosz
 */
public enum EventType {
    /**
     * top-down
     */
    @JsonProperty("child")
    CHILD_CHANGE,

    /**
     * bottom-up
     */
    @JsonProperty("parent")
    PARENT_CHANGE,
}
