package com.github.kjarosh.agh.pp.rest.dto;

import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DependentZonesDto {
    private Set<ZoneId> zones;
}
