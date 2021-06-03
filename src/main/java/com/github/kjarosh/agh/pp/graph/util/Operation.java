package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Operation {
    private final OperationType type;
    private final Map<String, Object> arguments;

    @JsonCreator
    public Operation(@JsonProperty("type") OperationType type) {
        this(type, new HashMap<>());
    }

    public Operation(OperationType type, Map<String, Object> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    public OperationType getType() {
        return type;
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return this.arguments;
    }

    @JsonAnySetter
    public void setProperties(String property, Object value) {
        arguments.put(property, value);
    }
}
