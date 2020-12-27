package com.github.kjarosh.agh.pp.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class EdgeCreationRequestDto {
    private String fromName;
    private String toName;
    private String permissions;
    private String trace;

    public static EdgeCreationRequestDto fromEdge(Edge edge, String trace) {
        return EdgeCreationRequestDto.builder()
                .fromName(edge.id().getFrom().name())
                .toName(edge.id().getTo().name())
                .permissions(edge.permissions().toString())
                .trace(trace)
                .build();
    }

    @JsonCreator
    public static EdgeCreationRequestDto fromString(String value) {
        String[] split = value.split("/");
        if (split.length < 3 || split.length > 4) {
            throw new IllegalArgumentException(value);
        }
        return EdgeCreationRequestDto.builder()
                .fromName(split[0])
                .toName(split[1])
                .permissions(split[2])
                .trace(split.length > 3 ? Strings.emptyToNull(split[3]) : null)
                .build();
    }

    @JsonValue
    @Override
    public String toString() {
        return fromName + "/" +
                toName + "/" +
                permissions + "/" +
                Strings.nullToEmpty(trace);
    }
}
