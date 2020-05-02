package com.github.kjarosh.agh.pp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;

import java.util.Optional;

/**
 * @author Kamil Jarosz
 */
public class Config {
    public static ZoneId ZONE_ID = Optional.ofNullable(System.getenv("ZONE_ID"))
            .map(ZoneId::new)
            .orElse(null);
    public static ObjectMapper MAPPER = new ObjectMapper();
}
