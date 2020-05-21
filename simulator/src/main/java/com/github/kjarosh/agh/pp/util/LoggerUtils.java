package com.github.kjarosh.agh.pp.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

/**
 * @author Kamil Jarosz
 */
public class LoggerUtils {
    public static void setLoggingLevel(String packageName, Level level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(packageName);
        logger.setLevel(level);
    }
}
