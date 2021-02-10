package com.github.kjarosh.agh.pp.config;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class AppConfig {
    public static final String redisClient = System.getProperty("app.redis", "false");
    public static final boolean redis = !"false".equals(redisClient);
    public static final int threads = Integer.parseInt(System.getProperty("app.threads", "16"));

    static {
        log.info("Configuration:");
        log.info("  redis=" + redisClient);
        log.info("  threads=" + threads);
    }
}
