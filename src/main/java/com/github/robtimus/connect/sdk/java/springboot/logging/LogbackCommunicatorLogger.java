/*
 * LogbackCommunicatorLogger.java
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

package com.github.robtimus.connect.sdk.java.springboot.logging;

import java.util.Objects;
import com.ingenico.connect.gateway.sdk.java.logging.CommunicatorLogger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * A communicator logger that is backed by a {@link Logger}.
 *
 * @author Rob Spoor
 */
public class LogbackCommunicatorLogger implements CommunicatorLogger {

    private final Logger logger;
    private final Level logLevel;
    private final Level errorLogLevel;

    /**
     * @param logger The backing logger.
     * @param level The level to use when logging through both {@link #log(String)} and {@link #log(String, Throwable)}.
     */
    public LogbackCommunicatorLogger(Logger logger, Level level) {
        this(logger, level, level);
    }

    /**
     * @param logger The backing logger.
     * @param logLevel The level to use when logging through {@link #log(String)}.
     * @param errorLogLevel The level to use when logging through {@link #log(String, Throwable)}.
     */
    public LogbackCommunicatorLogger(Logger logger, Level logLevel, Level errorLogLevel) {
        this.logger = Objects.requireNonNull(logger);
        this.logLevel = Objects.requireNonNull(logLevel);
        this.errorLogLevel = Objects.requireNonNull(errorLogLevel);
    }

    @Override
    public void log(String message) {
        logger.log(null, Logger.FQCN, Level.toLocationAwareLoggerInteger(logLevel), message, null, null);
    }

    @Override
    public void log(String message, Throwable thrown) {
        logger.log(null, Logger.FQCN, Level.toLocationAwareLoggerInteger(errorLogLevel), message, null, thrown);
    }
}
