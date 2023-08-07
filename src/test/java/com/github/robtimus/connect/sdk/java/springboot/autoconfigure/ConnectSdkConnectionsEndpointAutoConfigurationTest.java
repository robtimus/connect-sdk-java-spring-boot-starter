/*
 * ConnectSdkConnectionsEndpointAutoConfigurationTest.java
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint;

@SuppressWarnings("nls")
class ConnectSdkConnectionsEndpointAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkConnectionsEndpointAutoConfiguration.class));

    @Nested
    class ConnectionsEndpointTest {

        @Test
        void testNoAutoConfigurationWithMissingClass() throws IOException {
            try (FilteredClassLoader classLoader = new FilteredClassLoader(Endpoint.class)) {
                contextRunner
                        .withClassLoader(classLoader)
                        .withPropertyValues("management.endpoint.connectSdkConnections.enabled=true", "spring.jmx.enabled=true",
                                "management.endpoints.jmx.exposure.include=connectSdkConnections")
                        .run(context -> {
                            assertThat(context).doesNotHaveBean(ConnectionsEndpoint.class);
                        });
            }
        }

        @Test
        void testNoAutoConfigurationWithExistingBean() {
            contextRunner
                    .withUserConfiguration(ExistingBeanProvider.class)
                    .withPropertyValues("management.endpoint.connectSdkConnections.enabled=true", "spring.jmx.enabled=true",
                            "management.endpoints.jmx.exposure.include=connectSdkConnections")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean("connectSdkConnectionsEndpoint");
                        assertThat(context).hasSingleBean(ConnectionsEndpoint.class);
                        assertThat(context).getBean(ConnectionsEndpoint.class)
                                .isSameAs(context.getBean(ExistingBeanProvider.class).connectionsEndpoint());
                    });
        }

        @Test
        void testNoAutoConfigurationWithoutEnabledEndpoint() {
            contextRunner
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ConnectionsEndpoint.class);
                    });
        }

        @Test
        void testAutoConfigurationWithEnabledEndpoint() {
            contextRunner
                    .withPropertyValues("management.endpoint.connectSdkConnections.enabled=true")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ConnectionsEndpoint.class);
                    });
        }

        @Test
        void testAutoConfigurationWithAvailableEndpoint() {
            contextRunner
                    .withPropertyValues("management.endpoint.connectSdkConnections.enabled=true", "spring.jmx.enabled=true",
                            "management.endpoints.jmx.exposure.include=connectSdkConnections")
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkConnectionsEndpoint");
                        assertThat(context).hasSingleBean(ConnectionsEndpoint.class);
                    });
            contextRunner
                    .withPropertyValues("management.endpoint.connectSdkConnections.enabled=true",
                            "management.endpoints.web.exposure.include=connectSdkConnections")
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkConnectionsEndpoint");
                        assertThat(context).hasSingleBean(ConnectionsEndpoint.class);
                    });
        }
    }

    @Configuration
    static class ExistingBeanProvider {

        @Autowired
        private ApplicationContext context;

        @Bean
        ConnectionsEndpoint connectionsEndpoint() {
            return new ConnectionsEndpoint(context, 20_000);
        }
    }
}
