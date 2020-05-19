/*
 * ConnectSdkCommunicatorLoggerAutoConfigurationTest.java
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.logging.LogbackCommunicatorLogger;
import com.ingenico.connect.gateway.sdk.java.logging.CommunicatorLogger;

// PowerMock doesn't work well with JUnit5 yet, so use JUnit4 for just this test class
@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerFactory.class)
public class ConnectSdkCommunicatorLoggerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkCommunicatorLoggerAutoConfiguration.class));

    @Test
    public void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkCommunicatorLogger");
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).getBean(CommunicatorLogger.class).isSameAs(context.getBean(ExistingBeanProvider.class).communicatorLogger());
                });
    }

    @Test
    public void testNoAutoConfigurationWithNoLogbackLoggers() {
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(anyString())).thenAnswer(context -> {
            String name = context.getArgument(0);
            Logger logger = mock(Logger.class);
            when(logger.getName()).thenReturn(name);
            return logger;
        });
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkCommunicatorLogger");
                    assertThat(context).doesNotHaveBean(CommunicatorLogger.class);
                });
    }

    @Test
    public void testAutoConfiguration() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasBean("connectSdkCommunicatorLogger");
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).getBean(CommunicatorLogger.class).isInstanceOf(LogbackCommunicatorLogger.class);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        public CommunicatorLogger communicatorLogger() {
            return mock(CommunicatorLogger.class);
        }
    }
}
