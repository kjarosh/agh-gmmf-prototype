package com.github.kjarosh.agh.pp.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.index.events.Event;
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
public class MessageDto {
    @JsonProperty("vn")
    private String vertexName;
    @JsonProperty("e")
    private Event event;
}
