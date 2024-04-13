/*
 * ConnectSdkMerchantClientAutoConfigurationTest.java
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.worldline.connect.sdk.java.v1.V1Client;
import com.worldline.connect.sdk.java.v1.merchant.MerchantClient;

@SuppressWarnings("nls")
class ConnectSdkMerchantClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkMerchantClientAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, ConnectSdkConnectionAutoConfiguration.class,
                        ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetadataProviderAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class, ConnectSdkCommunicatorAutoConfiguration.class,
                        ConnectSdkClientAutoConfiguration.class, ConnectSdkVersionClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkV1MerchantClient");
                    assertThat(context).hasSingleBean(MerchantClient.class);
                    assertThat(context).getBean(MerchantClient.class).isSameAs(context.getBean(ExistingBeanProvider.class).merchantClient());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingProperties() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class,
                        ConnectSdkVersionClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MerchantClient.class);
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MerchantClient.class);
                });
    }

    @Test
    void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class,
                        ConnectSdkVersionClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkV1MerchantClient");
                    assertThat(context).hasSingleBean(MerchantClient.class);
                });
        contextRunner
                .withUserConfiguration(ClientProvider.class)
                .withPropertyValues("connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkV1MerchantClient");
                    assertThat(context).hasSingleBean(MerchantClient.class);

                    // verify that the client is used
                    V1Client v1Client = context.getBean(ClientProvider.class).v1Client();
                    verify(v1Client).merchant("merchantId");
                    verifyNoMoreInteractions(v1Client);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        MerchantClient merchantClient() {
            return mock(MerchantClient.class);
        }
    }

    @Configuration
    static class ClientProvider {

        @Bean
        V1Client v1Client() {
            return mock(V1Client.class);
        }
    }
}
