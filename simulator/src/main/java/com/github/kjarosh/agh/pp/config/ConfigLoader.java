package com.github.kjarosh.agh.pp.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Kamil Jarosz
 */
public class ConfigLoader {
    private static Config config = loadConfig();

    public static void reloadConfig() {
        config = loadConfig();
    }

    private static Config loadConfig() {
        Path configPath = Optional.ofNullable(System.getProperty("app.config_path"))
                .map(Paths::get)
                .orElseGet(() -> Paths.get("config.json"));
        return Config.loadConfig(configPath);
    }

    public static Config getConfig() {
        return config;
    }
}
