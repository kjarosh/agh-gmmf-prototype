package com.github.kjarosh.agh.pp.config;

/**
 * @author Kamil Jarosz
 */
public class AppConfig {
    public static final boolean redis = Boolean.parseBoolean(System.getProperty("app.redis", "false"));
    public static final String redisClient = System.getProperty("app.redis_client", "lettuce");
    public static final int threads = Integer.parseInt(System.getProperty("app.threads", "64"));
}
