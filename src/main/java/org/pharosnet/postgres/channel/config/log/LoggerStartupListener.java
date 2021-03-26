package org.pharosnet.postgres.channel.config.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

import java.util.Optional;

public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private boolean isStarted = false;

    @Override
    public void start() {
        if (isStarted) {
            return;
        }

        context.putProperty("LOGGER_FORMAT", Optional.ofNullable(System.getenv("POSTGRES_CHANNEL_LOGGER_FORMAT")).orElse("text").trim());


        isStarted = true;
    }

    @Override
    public void onStart(LoggerContext context) {
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onReset(LoggerContext context) {

    }

    @Override
    public void onStop(LoggerContext context) {

    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

}
