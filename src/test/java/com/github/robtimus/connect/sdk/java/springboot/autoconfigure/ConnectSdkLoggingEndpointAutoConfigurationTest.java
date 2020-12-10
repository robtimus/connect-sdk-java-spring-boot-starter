/*
 * ConnectSdkLoggingEndpointAutoConfigurationTest.java
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
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.actuator.LoggingEndpoint;

@SuppressWarnings("nls")
class ConnectSdkLoggingEndpointAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkLoggingEndpointAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithMissingClass() throws IOException {
        try (FilteredClassLoader classLoader = new FilteredClassLoader(Endpoint.class)) {
            contextRunner
                    .withClassLoader(classLoader)
                    .withPropertyValues("management.endpoint.connectSdkLogging.enabled=true", "spring.jmx.enabled=true")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(LoggingEndpoint.class);
                    });
        }
    }

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class)
                .withPropertyValues("management.endpoint.connectSdkLogging.enabled=true", "spring.jmx.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkLoggingEndpoint");
                    assertThat(context).hasSingleBean(LoggingEndpoint.class);
                    assertThat(context).getBean(LoggingEndpoint.class)
                            .isSameAs(context.getBean(ExistingBeanProvider.class).loggingEndpoint());
                });
    }

    @Test
    void testNoAutoConfigurationWithoutEnabledEndpoint() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingEndpoint.class);
                });
    }

    @Test
    void testAutoConfigurationWithEnabledEndpoint() {
        contextRunner
                .withPropertyValues("management.endpoint.connectSdkLogging.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingEndpoint.class);
                });
    }

    @Test
    void testAutoConfigurationWithAvailableEndpoint() {
        contextRunner
                .withPropertyValues("management.endpoint.connectSdkLogging.enabled=true", "spring.jmx.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkLoggingEndpoint");
                    assertThat(context).hasSingleBean(LoggingEndpoint.class);
                });
        contextRunner
                .withPropertyValues("management.endpoint.connectSdkLogging.enabled=true",
                        "management.endpoints.web.exposure.include=connectSdkLogging")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkLoggingEndpoint");
                    assertThat(context).hasSingleBean(LoggingEndpoint.class);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Autowired
        private ApplicationContext context;

        @Bean
        LoggingEndpoint loggingEndpoint() {
            return new LoggingEndpoint(context);
        }
    }
}
