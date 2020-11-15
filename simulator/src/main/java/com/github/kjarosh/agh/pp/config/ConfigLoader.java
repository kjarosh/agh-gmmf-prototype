package com.github.kjarosh.agh.pp.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Kamil Jarosz
 */
public class ConfigLoader {
    public static Path CONFIG_PATH = Optional.ofNullable(System.getProperty("app.config_path"))
            .map(Paths::get)
            .orElseGet(() -> Paths.get("config.json"));

    private static Config CONFIG = Config.loadConfig(CONFIG_PATH);

    public static Config getConfig() {
        return CONFIG;
    }
}
