/*
 * ConnectSdkCommunicatorLoggerAutoConfiguration.java
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

package com.github.robtimus.connect.sdk.java.springboot.autoconfigure;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import com.github.robtimus.connect.sdk.java.springboot.autoconfigure.ConnectSdkCommunicatorLoggerAutoConfiguration.OnUsesLogbackCondition;
import com.github.robtimus.connect.sdk.java.springboot.logging.LogbackCommunicatorLogger;
import com.worldline.connect.sdk.java.Communicator;
import com.worldline.connect.sdk.java.logging.CommunicatorLogger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * <a href="https://github.com/Worldline-Global-Collectconnect-sdk-java/">connect-sdk-java</a>'s {@link CommunicatorLogger}.
 *
 * @author Rob Spoor
 */
@Configuration
@ConditionalOnMissingBean(CommunicatorLogger.class)
@ConditionalOnClass({ Logger.class, Level.class })
@Conditional(OnUsesLogbackCondition.class)
@SuppressWarnings({ "nls", "javadoc" })
public class ConnectSdkCommunicatorLoggerAutoConfiguration {

    static final String DEFAULT_LOGGER_NAME = Communicator.class.getName();

    @Bean
    public CommunicatorLogger connectSdkCommunicatorLogger(
            @Value("${connect.api.logger.name:com.worldline.connect.sdk.java.Communicator}") String loggerName,
            @Value("${connect.api.logger.level:INFO}") String logLevel,
            @Value("${connect.api.logger.errorLevel:ERROR}") String errorLogLevel) {

        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        return new LogbackCommunicatorLogger(logger, Level.toLevel(logLevel, Level.INFO), Level.toLevel(errorLogLevel, Level.ERROR));
    }

    static class OnUsesLogbackCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            org.slf4j.Logger logger = LoggerFactory.getLogger(DEFAULT_LOGGER_NAME);
            if (logger instanceof Logger) {
                return ConditionOutcome.match();
            }
            return ConditionOutcome.noMatch("LoggerFactory does not return Logback Loggers");
        }
    }
}
