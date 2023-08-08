/*
 * ConnectSdkAuthenticatorAutoConfigurationTest.java
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
import static org.mockito.Mockito.mock;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.ConfigurableAuthenticator;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ApiKeyEndpoint;
import com.ingenico.connect.gateway.sdk.java.Authenticator;

@SuppressWarnings("nls")
class ConnectSdkAuthenticatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkAuthenticatorAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class)
                .withPropertyValues("connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret",
                        // enable the API key endpoint as well - it won't be available though
                        "management.endpoint.connectSdkApiKey.enabled=true", "spring.jmx.enabled=true",
                        "management.endpoints.jmx.exposure.include=connectSdkApiKey")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkAuthenticator");
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).getBean(Authenticator.class).isSameAs(context.getBean(ExistingBeanProvider.class).authenticator());
                    assertThat(context).doesNotHaveBean(ConfigurableAuthenticator.class);
                    assertThat(context).doesNotHaveBean(ApiKeyEndpoint.class);
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingProperties() {
        contextRunner
                .withPropertyValues("connect.api.api-key-id=keyId",
                        // enable the API key endpoint as well - it won't be available though
                        "management.endpoint.connectSdkApiKey.enabled=true", "spring.jmx.enabled=true",
                        "management.endpoints.jmx.exposure.include=connectSdkApiKey")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Authenticator.class);
                    assertThat(context).doesNotHaveBean(ConfigurableAuthenticator.class);
                    assertThat(context).doesNotHaveBean(ApiKeyEndpoint.class);
                });
        contextRunner
                .withPropertyValues("connect.api.secret-api-key=secret",
                        // enable the API key endpoint as well - it won't be available though
                        "management.endpoint.connectSdkApiKey.enabled=true", "spring.jmx.enabled=true",
                        "management.endpoints.jmx.exposure.include=connectSdkApiKey")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Authenticator.class);
                    assertThat(context).doesNotHaveBean(ConfigurableAuthenticator.class);
                    assertThat(context).doesNotHaveBean(ApiKeyEndpoint.class);
                });
    }

    @Test
    void testAutoConfigurationWithoutEndpoint() {
        contextRunner
                .withPropertyValues("connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkAuthenticator");
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).getBean(Authenticator.class).isExactlyInstanceOf(ConfigurableAuthenticator.class);
                    assertThat(context).hasSingleBean(ConfigurableAuthenticator.class);
                    assertThat(context).doesNotHaveBean(ApiKeyEndpoint.class);
                });
    }

    @Test
    void testAutoConfigurationWithEnabledEndpoint() {
        contextRunner
                .withPropertyValues("connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret",
                        "management.endpoint.connectSdkApiKey.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkAuthenticator");
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).getBean(Authenticator.class).isExactlyInstanceOf(ConfigurableAuthenticator.class);
                    assertThat(context).hasSingleBean(ConfigurableAuthenticator.class);
                    assertThat(context).doesNotHaveBean(ApiKeyEndpoint.class);
                });
    }

    @Test
    void testAutoConfigurationWithAvailableEndpoint() {
        contextRunner
                .withPropertyValues("connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret",
                        "management.endpoint.connectSdkApiKey.enabled=true", "spring.jmx.enabled=true",
                        "management.endpoints.jmx.exposure.include=connectSdkApiKey")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkAuthenticator");
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).getBean(Authenticator.class).isExactlyInstanceOf(ConfigurableAuthenticator.class);
                    assertThat(context).hasSingleBean(ConfigurableAuthenticator.class);
                    assertThat(context).hasSingleBean(ApiKeyEndpoint.class);
                });
    }

    @Test
    void testNoEndpointWithMissingClass() throws IOException {
        try (FilteredClassLoader classLoader = new FilteredClassLoader(Endpoint.class)) {
            contextRunner
                    .withClassLoader(classLoader)
                    .withPropertyValues("connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret",
                            "management.endpoint.connectSdkApiKey.enabled=true", "spring.jmx.enabled=true",
                            "management.endpoints.jmx.exposure.include=connectSdkApiKey")
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkAuthenticator");
                        assertThat(context).hasSingleBean(Authenticator.class);
                        assertThat(context).getBean(Authenticator.class).isExactlyInstanceOf(ConfigurableAuthenticator.class);
                        assertThat(context).hasSingleBean(ConfigurableAuthenticator.class);
                        assertThat(context).doesNotHaveBean(ApiKeyEndpoint.class);
                    });
        }
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        Authenticator authenticator() {
            return mock(Authenticator.class);
        }
    }
}
