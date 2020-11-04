package com.github.kjarosh.agh.pp.index.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Kamil Jarosz
 */
public enum EventType {
    /**
     * When some child changed. E.g. a user changes permissions in a group.
     * The event is then propagated to the group and all parents.
     */
    @JsonProperty("child")
    CHILD_CHANGE,

    /**
     * When some parent changed. E.g. a group changes permissions in a space.
     * The event is then propagated to all childrem.
     */
    @JsonProperty("parent")
    PARENT_CHANGE,

    /**
     * Analogous to {@link #CHILD_CHANGE} but represents a removal.
     */
    @JsonProperty("child_remove")
    CHILD_REMOVE,

    /**
     * Analogous to {@link #PARENT_CHANGE} but represents a removal.
     */
    @JsonProperty("parent_remove")
    PARENT_REMOVE,
}
