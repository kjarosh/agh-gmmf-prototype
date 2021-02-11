package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.config.Config;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kamil Jarosz
 */
public class GlobalExecutors {
    private static final ExecutorService eventProcessingExecutor =
            Executors.newFixedThreadPool(AppConfig.workerThreads, new ThreadFactoryBuilder()
                    .setNameFormat(Config.ZONE_ID + "-event-processor-%d")
                    .build());
    private static final ExecutorService calculationExecutor = !AppConfig.redis ? null :
            Executors.newFixedThreadPool(AppConfig.calculationThreads * 2, new ThreadFactoryBuilder()
                    .setNameFormat(Config.ZONE_ID + "-permissions-calculator-%d")
                    .build());

    public static ExecutorService getEventProcessingExecutor() {
        return eventProcessingExecutor;
    }

    public static ExecutorService getCalculationExecutor() {
        return calculationExecutor;
    }
}
