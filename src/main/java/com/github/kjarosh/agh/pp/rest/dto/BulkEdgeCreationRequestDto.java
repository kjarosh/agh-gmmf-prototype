package com.github.kjarosh.agh.pp.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BulkEdgeCreationRequestDto {
    @JsonProperty("sourceZone")
    private ZoneId sourceZone;
    @JsonProperty("destinationZone")
    private ZoneId destinationZone;
    @JsonProperty("successive")
    private boolean successive;
    @JsonProperty("edges")
    private List<EdgeCreationRequestDto> edges;
}
