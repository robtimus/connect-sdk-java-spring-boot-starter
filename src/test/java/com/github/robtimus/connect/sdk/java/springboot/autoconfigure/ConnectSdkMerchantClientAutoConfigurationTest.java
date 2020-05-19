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
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.merchant.MerchantClient;

public class ConnectSdkMerchantClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkMerchantClientAutoConfiguration.class));

    @Test
    public void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, ConnectSdkConnectionAutoConfiguration.class,
                        ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetaDataProviderAutoConfiguration.class,
                        ConnectSdkSessionAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkMerchantClient");
                    assertThat(context).hasSingleBean(MerchantClient.class);
                    assertThat(context).getBean(MerchantClient.class).isSameAs(context.getBean(ExistingBeanProvider.class).merchantClient());
                });
    }

    @Test
    public void testNoAutoConfigurationWithMissingProperties() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class, ConnectSdkSessionAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class, ConnectSdkCommunicatorAutoConfiguration.class,
                        ConnectSdkClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MerchantClient.class);
                });
    }

    @Test
    public void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class, ConnectSdkSessionAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class, ConnectSdkCommunicatorAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MerchantClient.class);
                });
    }

    @Test
    @SuppressWarnings("resource")
    public void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class, ConnectSdkSessionAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class, ConnectSdkCommunicatorAutoConfiguration.class,
                        ConnectSdkClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator",
                        "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkMerchantClient");
                    assertThat(context).hasSingleBean(MerchantClient.class);
                });
        contextRunner
                .withUserConfiguration(ClientProvider.class)
                .withPropertyValues("connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkMerchantClient");
                    assertThat(context).hasSingleBean(MerchantClient.class);

                    // verify that the client is used
                    Client client = context.getBean(ClientProvider.class).client();
                    verify(client).merchant("merchantId");
                    verifyNoMoreInteractions(client);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        public MerchantClient merchantClient() {
            return mock(MerchantClient.class);
        }
    }

    @Configuration
    static class ClientProvider {

        @Bean
        public Client client() {
            return mock(Client.class);
        }
    }
}
