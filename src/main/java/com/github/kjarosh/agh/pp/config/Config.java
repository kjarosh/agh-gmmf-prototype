package com.github.kjarosh.agh.pp.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Kamil Jarosz
 */
@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Config {
    public static ZoneId ZONE_ID = Optional.ofNullable(System.getProperty("app.zone_id", null))
            .filter(Predicate.not(Strings::isNullOrEmpty))
            .or(() -> Optional.ofNullable(System.getenv("ZONE_ID")))
            .map(ZoneId::new)
            .orElse(null);

    public static ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    static {
        log.info("My zone ID: {}", ZONE_ID);
    }

    private boolean instrumentationEnabled = false;
    private String instrumentationReportPath = "instrumentation.csv";
    private Map<String, ZoneConfig> zones;

    public static Config loadConfig(Path path) {
        log.debug("Loading configuration from {}", path);
        try {
            return MAPPER.readValue(path.toFile(), Config.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @JsonIgnore
    public void saveConfig(Path destination) {
        try {
            MAPPER.writeValue(destination.toFile(), this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @JsonIgnore
    public String translateZoneToAddress(ZoneId zone) {
        if (ZONE_ID != null && ZONE_ID.equals(zone)) {
            return "127.0.0.1";
        }

        String zoneId = zone.getId();
        ZoneConfig zoneConfig = zones.get(zoneId);
        return zoneConfig != null ? zoneConfig.getAddress() : zoneId;
    }
}
