package com.github.kjarosh.agh.pp.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    public static Path CONFIG_PATH = Optional.ofNullable(System.getProperty("app.config_path"))
            .map(Paths::get)
            .orElseGet(() -> Paths.get("config.json"));

    public static ZoneId ZONE_ID = Optional.ofNullable(System.getProperty("app.zone_id", null))
            .filter(Predicate.not(Strings::isNullOrEmpty))
            .or(() -> Optional.ofNullable(System.getenv("ZONE_ID")))
            .map(ZoneId::new)
            .orElse(null);

    public static ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static Config CONFIG = loadConfig();

    static {
        logger.info("My zone ID: {}", ZONE_ID);
    }

    private Map<String, ZoneConfig> zones;

    public static Config getConfig() {
        return CONFIG;
    }

    private static Config loadConfig() {
        logger.debug("Loading configuration from {}", CONFIG_PATH);
        try {
            return MAPPER.readValue(CONFIG_PATH.toFile(), Config.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @JsonIgnore
    public String translateZoneToAddress(ZoneId zone) {
        String zoneId = zone.getId();
        ZoneConfig zoneConfig = zones.get(zoneId);
        return zoneConfig != null ? zoneConfig.getAddress() : zoneId;
    }
}
