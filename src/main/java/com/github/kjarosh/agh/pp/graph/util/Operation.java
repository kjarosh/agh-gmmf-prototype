package com.github.kjarosh.agh.pp.graph.util;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
public class Operation {
    private OperationType type;
    private EdgeId edgeId;
    private ZoneId zoneId;
    private String trace;
    private Permissions permissions;

    public Operation(OperationType type,
                     EdgeId edgeId,
                     ZoneId zoneId,
                     String trace,
                     Permissions permissions) {
        this.type = type;
        this.edgeId = edgeId;
        this.zoneId = zoneId;
        this.trace = trace;
        this.permissions = permissions;
    }
}