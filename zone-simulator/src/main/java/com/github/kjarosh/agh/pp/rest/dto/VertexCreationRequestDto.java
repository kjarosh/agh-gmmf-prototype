package com.github.kjarosh.agh.pp.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
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
public class VertexCreationRequestDto {
    private String name;
    private Vertex.Type type;

    @JsonCreator
    public static VertexCreationRequestDto fromString(String value) {
        String[] split = value.split("/", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException(value);
        }
        Vertex.Type type = Vertex.Type.valueOf(split[0]);
        return new VertexCreationRequestDto(split[1], type);
    }

    @JsonValue
    @Override
    public String toString() {
        return type + "/" + name;
    }
}
