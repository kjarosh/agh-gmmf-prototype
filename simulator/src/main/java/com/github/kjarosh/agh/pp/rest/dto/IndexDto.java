package com.github.kjarosh.agh.pp.rest.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * @author Kamil Jarosz
 */
@Getter
@Builder
public class IndexDto {
    private final Map<String, EffectiveVertexDto> children;
    private final Map<String, EffectiveVertexDto> parents;

    @Getter
    @Builder
    public static class EffectiveVertexDto {
        private final String permissions;
        private final List<String> intermediateVertices;
    }
}
