/*
 * ConnectSdkClientAutoConfigurationTest.java
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultMarshaller;

class ConnectSdkClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkClientAutoConfiguration.class));

    @Test
    @SuppressWarnings("resource")
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, ConnectSdkConnectionAutoConfiguration.class,
                        ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetaDataProviderAutoConfiguration.class,
                        ConnectSdkSessionAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkClient");
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).getBean(Client.class).isSameAs(context.getBean(ExistingBeanProvider.class).client());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Client.class);
                });
    }

    @Test
    @SuppressWarnings("resource")
    void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class, ConnectSdkSessionAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class, ConnectSdkCommunicatorAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkClient");
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).getBean(Client.class).isExactlyInstanceOf(Client.class);
                });
        contextRunner
                .withUserConfiguration(CommunicatorProvider.class)
                .run(context -> {
                    assertThat(context).hasBean("connectSdkClient");
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).getBean(Client.class).isExactlyInstanceOf(Client.class);

                    // verify that the communicator is used
                    Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                    // no need to setup a null return value
                    context.getBean(Client.class).merchant("merchantId").payouts().cancel("payoutId", null);
                    verify(communicator).post(anyString(), eq(null), eq(null), eq(null), eq(void.class), eq(null));
                    verifyNoMoreInteractions(communicator);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        Client client() {
            return mock(Client.class);
        }
    }

    @Configuration
    static class CommunicatorProvider {

        @Bean
        Communicator communicator() {
            Communicator communicator = mock(Communicator.class);
            when(communicator.getMarshaller()).thenReturn(DefaultMarshaller.INSTANCE);
            return communicator;
        }
    }
}
