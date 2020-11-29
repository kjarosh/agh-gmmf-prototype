package com.github.kjarosh.agh.pp.cli.utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * @author Kamil Jarosz
 */
public class LogbackUtils {
    @SneakyThrows
    public static void loadLogbackCli() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        try (InputStream configStream = LogbackUtils.class.getClassLoader()
                .getResourceAsStream("logback-cli.xml")) {
            configurator.setContext(loggerContext);
            configurator.doConfigure(configStream);
        }
    }
}
