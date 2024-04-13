/*
 * LoggingEndpoint.java
 * Copyright 2019 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.connect.sdk.java.springboot.actuator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import com.worldline.connect.sdk.java.Client;
import com.worldline.connect.sdk.java.Communicator;
import com.worldline.connect.sdk.java.communication.Connection;
import com.worldline.connect.sdk.java.logging.CommunicatorLogger;
import com.worldline.connect.sdk.java.logging.LoggingCapable;

/**
 * An {@link Endpoint} for enabling and disabling logging in
 * <a href="https://github.com/Worldline-Global-Collectconnect-sdk-java/">connect-sdk-java</a>.
 *
 * @author Rob Spoor
 */
@Endpoint(id = "connectSdkLogging", enableByDefault = false)
@SuppressWarnings("javadoc")
public class LoggingEndpoint {

    private final ApplicationContext context;

    public LoggingEndpoint(ApplicationContext context) {
        this.context = context;
    }

    /**
     * @return All {@link LoggingCapable} and {@link CommunicatorLogger} beans.
     */
    @ReadOperation
    public LoggingCapableAndLoggerBeans listLoggingCapableAndLoggerBeans() {
        Map<String, LoggingCapable> loggingCapables = context.getBeansOfType(LoggingCapable.class);
        Map<String, CommunicatorLogger> loggers = context.getBeansOfType(CommunicatorLogger.class);

        LoggingCapableAndLoggerBeans result = new LoggingCapableAndLoggerBeans();
        result.addLoggingCapables(loggingCapables);
        result.addCommunicatorLoggers(loggers);
        return result;
    }

    /**
     * Enables logging for all {@link LoggingCapable} beans.
     *
     * @param logger If given, the bean name of the {@link CommunicatorLogger} to use.
     *                   Otherwise all available {@link CommunicatorLogger}s will be used.
     */
    @WriteOperation
    public void enableLogging(@Nullable String logger) {
        findCommunicatorLogger(logger).ifPresent(loggerBean -> context.getBeansOfType(LoggingCapable.class).values()
                .forEach(loggingCapable -> loggingCapable.enableLogging(loggerBean)));
    }

    /**
     * Enables logging for a specific {@link LoggingCapable} bean.
     *
     * @param beanName The name of the {@link LoggingCapable} bean.
     * @param logger If given, the bean name of the {@link CommunicatorLogger} to use.
     *                   Otherwise all available {@link CommunicatorLogger}s will be used.
     */
    @WriteOperation
    public void enableLoggingOnBean(@Selector String beanName, @Nullable String logger) {
        LoggingCapable loggingCapable = context.getBean(beanName, LoggingCapable.class);
        findCommunicatorLogger(logger).ifPresent(loggingCapable::enableLogging);
    }

    /**
     * Disables logging for all {@link LoggingCapable} beans.
     */
    @DeleteOperation
    public void disableLogging() {
        context.getBeansOfType(LoggingCapable.class).values()
                .forEach(LoggingCapable::disableLogging);
    }

    /**
     * Disables logging for a specific {@link LoggingCapable} bean.
     *
     * @param beanName The name of the {@link LoggingCapable} bean.
     */
    @DeleteOperation
    public void disableLoggingOnBean(@Selector String beanName) {
        LoggingCapable loggingCapable = context.getBean(beanName, LoggingCapable.class);
        loggingCapable.disableLogging();
    }

    private Optional<CommunicatorLogger> findCommunicatorLogger(String logger) {
        if (logger != null) {
            return Optional.of(context.getBean(logger, CommunicatorLogger.class));
        }
        Collection<CommunicatorLogger> loggers = context.getBeansOfType(CommunicatorLogger.class).values();
        if (loggers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(loggers.size() == 1 ? loggers.iterator().next() : new CompoundCommunicatorLogger(loggers));
    }

    public static class LoggingCapableAndLoggerBeans {

        private List<String> connections = new ArrayList<>();
        private List<String> communicators = new ArrayList<>();
        private List<String> clients = new ArrayList<>();
        private List<String> loggers = new ArrayList<>();

        public List<String> getConnections() {
            return connections;
        }

        public List<String> getCommunicators() {
            return communicators;
        }

        public List<String> getClients() {
            return clients;
        }

        public List<String> getLoggers() {
            return loggers;
        }

        private void addLoggingCapables(Map<String, LoggingCapable> loggingCapables) {
            loggingCapables.forEach(this::addLoggingCapable);
        }

        private void addLoggingCapable(String name, LoggingCapable loggingCapable) {
            if (loggingCapable instanceof Connection) {
                connections.add(name);
            }
            if (loggingCapable instanceof Communicator) {
                communicators.add(name);
            }
            if (loggingCapable instanceof Client) {
                clients.add(name);
            }
        }

        private void addCommunicatorLoggers(Map<String, CommunicatorLogger> loggers) {
            this.loggers.addAll(loggers.keySet());
        }
    }

    static final class CompoundCommunicatorLogger implements CommunicatorLogger {

        final List<CommunicatorLogger> loggers = new ArrayList<>();

        CompoundCommunicatorLogger(Collection<CommunicatorLogger> loggers) {
            this.loggers.addAll(loggers);
        }

        @Override
        public void log(String message) {
            loggers.forEach(logger -> logger.log(message));
        }

        @Override
        public void log(String message, Throwable thrown) {
            loggers.forEach(logger -> logger.log(message, thrown));
        }
    }
}
