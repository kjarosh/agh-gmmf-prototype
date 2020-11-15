package com.github.kjarosh.agh.pp.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class ZoneConfig {
    private String address;
}
