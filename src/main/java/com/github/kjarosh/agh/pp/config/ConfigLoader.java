package com.github.kjarosh.agh.pp.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Kamil Jarosz
 */
public class ConfigLoader {
    private static Config config;

    public static void reloadConfig() {
        reloadConfig(null);
    }

    public static void reloadConfig(Path configPath) {
        config = loadConfig(configPath);
    }

    private static Config loadConfig(Path configPath) {
        if (configPath == null) {
            configPath = Optional.ofNullable(System.getProperty("app.config_path"))
                    .map(Paths::get)
                    .orElseGet(() -> Paths.get("config.json"));
        }
        return Config.loadConfig(configPath);
    }

    public static Config getConfig() {
        if (config == null) {
            config = loadConfig(null);
        }
        return config;
    }
}
