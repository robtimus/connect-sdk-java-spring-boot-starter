/*
 * ConnectSdkHealthIndicatorAutoConfigurationTest.java
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectSdkHealthIndicator;
import com.worldline.connect.sdk.java.v1.domain.TestConnection;
import com.worldline.connect.sdk.java.v1.merchant.MerchantClient;
import com.worldline.connect.sdk.java.v1.merchant.services.ServicesClient;

@SuppressWarnings("nls")
class ConnectSdkHealthIndicatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkHealthIndicatorAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithMissingClass() throws IOException {
        try (FilteredClassLoader classLoader = new FilteredClassLoader(HealthIndicator.class)) {
            contextRunner
                    .withClassLoader(classLoader)
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ConnectSdkHealthIndicator.class);
                    });
        }
    }

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkHealthIndicator");
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).getBean(ConnectSdkHealthIndicator.class)
                            .isSameAs(context.getBean(ExistingBeanProvider.class).healthIndicator());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ConnectSdkHealthIndicator.class);
                });
    }

    @Test
    void testNoAutoConfigurationWithDisabledHealthIndicator() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class,
                        ConnectSdkVersionClientAutoConfiguration.class, ConnectSdkMerchantClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId", "management.health.connect-sdk.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).hasSingleBean(MerchantClient.class);
                });
    }

    @Test
    void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class,
                        ConnectSdkVersionClientAutoConfiguration.class, ConnectSdkMerchantClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkHealthIndicator");
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);
                });
        contextRunner
                .withUserConfiguration(MerchantClientProvider.class, ConnectSdkMerchantClientAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("connectSdkHealthIndicator");
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);

                    // verify that the merchant client factory is used
                    TestConnection testConnection = new TestConnection();
                    testConnection.setResult("OK");

                    ServicesClient servicesClient = mock(ServicesClient.class);
                    when(servicesClient.testconnection()).thenReturn(testConnection);

                    MerchantClient merchantClient = context.getBean(MerchantClientProvider.class).merchantClient();
                    when(merchantClient.services()).thenReturn(servicesClient);

                    Health health = context.getBean(ConnectSdkHealthIndicator.class).health();
                    assertThat(health.getStatus()).isEqualTo(Status.UP);
                    assertThat(health.getDetails()).isEqualTo(Collections.singletonMap("result", "OK"));

                    verify(servicesClient).testconnection();
                    verify(merchantClient).services();
                    verifyNoMoreInteractions(servicesClient, merchantClient);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        ConnectSdkHealthIndicator healthIndicator() {
            return mock(ConnectSdkHealthIndicator.class);
        }
    }

    @Configuration
    static class MerchantClientProvider {

        @Bean
        MerchantClient merchantClient() {
            return mock(MerchantClient.class);
        }
    }
}
