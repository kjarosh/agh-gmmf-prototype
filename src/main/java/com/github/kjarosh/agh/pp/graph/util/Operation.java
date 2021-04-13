package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class Operation {
    private final OperationType type;
    private final EdgeId edgeId;
    private final ZoneId zoneId;
    private final String trace;
    private final Permissions permissions;

    @JsonCreator
    public Operation(@JsonProperty("type") OperationType type,
                     @JsonProperty("edgeId") EdgeId edgeId,
                     @JsonProperty("zoneId") ZoneId zoneId,
                     @JsonProperty("trace") String trace,
                     @JsonProperty("permissions") Permissions permissions) {
        this.type = type;
        this.edgeId = edgeId;
        this.zoneId = zoneId;
        this.trace = trace;
        this.permissions = permissions;
    }
}