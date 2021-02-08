package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.config.Config;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Kamil Jarosz
 */
public class GlobalExecutor {
    private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat(Config.ZONE_ID + "-worker-%d")
            .build();
    private static final ExecutorService executor = Executors.newFixedThreadPool(AppConfig.threads, threadFactory);

    public static ExecutorService getExecutor() {
        return executor;
    }
}
