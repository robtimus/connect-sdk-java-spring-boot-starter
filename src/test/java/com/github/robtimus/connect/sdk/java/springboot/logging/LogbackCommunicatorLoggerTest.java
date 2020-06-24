/*
 * LogbackCommunicatorLoggerTest.java
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import com.ingenico.connect.gateway.sdk.java.logging.CommunicatorLogger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

class LogbackCommunicatorLoggerTest {

    @Test
    void testLogWithOneLevel() {
        @SuppressWarnings("unchecked")
        Appender<ILoggingEvent> appender = mock(Appender.class);

        Logger logger = (Logger) LoggerFactory.getLogger(getClass());
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);

        Throwable error = new AssertionError("assertion failed");

        CommunicatorLogger communicatorLogger = new LogbackCommunicatorLogger(logger, Level.DEBUG);
        communicatorLogger.log("message without exception");
        communicatorLogger.log("message with exception", error);

        ArgumentCaptor<ILoggingEvent> eventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(appender, times(2)).doAppend(eventCaptor.capture());
        verifyNoMoreInteractions(appender);

        List<ILoggingEvent> events = eventCaptor.getAllValues();
        assertThat(events.size()).isEqualTo(2);

        ILoggingEvent event1 = events.get(0);
        assertThat(event1.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(event1.getMessage()).isEqualTo("message without exception");
        assertThat(event1.getArgumentArray()).isNull();
        assertThat(event1.getFormattedMessage()).isEqualTo("message without exception");
        assertThat(event1.getLoggerName()).isEqualTo(getClass().getName());
        assertThat(event1.getThrowableProxy()).isNull();

        ILoggingEvent event2 = events.get(1);
        assertThat(event2.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(event2.getMessage()).isEqualTo("message with exception");
        assertThat(event2.getArgumentArray()).isNull();
        assertThat(event2.getFormattedMessage()).isEqualTo("message with exception");
        assertThat(event2.getLoggerName()).isEqualTo(getClass().getName());
        assertThat(event2.getThrowableProxy()).isNotNull();
        assertThat(event2.getThrowableProxy().getMessage()).isEqualTo(error.getMessage());
        assertThat(event2.getThrowableProxy().getClassName()).isEqualTo(error.getClass().getName());
    }

    @Test
    void testLogWithDifferentLevels() {
        @SuppressWarnings("unchecked")
        Appender<ILoggingEvent> appender = mock(Appender.class);

        Logger logger = (Logger) LoggerFactory.getLogger(getClass());
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);

        Throwable error = new AssertionError("assertion failed");

        CommunicatorLogger communicatorLogger = new LogbackCommunicatorLogger(logger, Level.DEBUG, Level.WARN);
        communicatorLogger.log("message without exception");
        communicatorLogger.log("message with exception", error);

        ArgumentCaptor<ILoggingEvent> eventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(appender, times(2)).doAppend(eventCaptor.capture());
        verifyNoMoreInteractions(appender);

        List<ILoggingEvent> events = eventCaptor.getAllValues();
        assertThat(events.size()).isEqualTo(2);

        ILoggingEvent event1 = events.get(0);
        assertThat(event1.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(event1.getMessage()).isEqualTo("message without exception");
        assertThat(event1.getArgumentArray()).isNull();
        assertThat(event1.getFormattedMessage()).isEqualTo("message without exception");
        assertThat(event1.getLoggerName()).isEqualTo(getClass().getName());
        assertThat(event1.getThrowableProxy()).isNull();

        ILoggingEvent event2 = events.get(1);
        assertThat(event2.getLevel()).isEqualTo(Level.WARN);
        assertThat(event2.getMessage()).isEqualTo("message with exception");
        assertThat(event2.getArgumentArray()).isNull();
        assertThat(event2.getFormattedMessage()).isEqualTo("message with exception");
        assertThat(event2.getLoggerName()).isEqualTo(getClass().getName());
        assertThat(event2.getThrowableProxy()).isNotNull();
        assertThat(event2.getThrowableProxy().getMessage()).isEqualTo(error.getMessage());
        assertThat(event2.getThrowableProxy().getClassName()).isEqualTo(error.getClass().getName());
    }
}
