package com.github.kjarosh.agh.pp.config;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class AppConfig {
    public static final boolean indexationEnabled = "true".equals(System.getProperty("app.indexation.enabled", "true"));
    public static final String redisClient = System.getProperty("app.redis", "false");
    public static final boolean redis = !"false".equals(redisClient);
    public static final int workerThreads;
    public static final int calculationThreads;

    static {
        if (redis) {
            workerThreads = Integer.parseInt(System.getProperty("app.threads.worker", "4"));
            calculationThreads = Integer.parseInt(System.getProperty("app.threads.calc", "16"));
        } else {
            workerThreads = Integer.parseInt(System.getProperty("app.threads.worker", "8"));
            calculationThreads = Integer.parseInt(System.getProperty("app.threads.calc", "-1"));
        }
        log.info("Configuration:");
        log.info("  indexation=" + indexationEnabled);
        log.info("  redis=" + redisClient);
        log.info("  workerThreads=" + workerThreads);
        log.info("  calculationThreads=" + calculationThreads);
    }
}
