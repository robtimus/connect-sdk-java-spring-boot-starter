/*
 * ConnectSdkVersionClientAutoConfigurationTest.java
 * Copyright 2024 Rob Spoor
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
import com.worldline.connect.sdk.java.Client;
import com.worldline.connect.sdk.java.v1.V1Client;

@SuppressWarnings("nls")
class ConnectSdkVersionClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkVersionClientAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, ConnectSdkConnectionAutoConfiguration.class,
                        ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetadataProviderAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class, ConnectSdkCommunicatorAutoConfiguration.class,
                        ConnectSdkClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkV1Client");
                    assertThat(context).hasSingleBean(V1Client.class);
                    assertThat(context).getBean(V1Client.class).isSameAs(context.getBean(ExistingBeanProvider.class).v1Client());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(V1Client.class);
                });
    }

    @Test
    @SuppressWarnings("resource")
    void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class,
                        ConnectSdkCommunicatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkV1Client");
                    assertThat(context).hasSingleBean(V1Client.class);
                });
        contextRunner
                .withUserConfiguration(ClientProvider.class)
                .run(context -> {
                    assertThat(context).hasBean("connectSdkV1Client");
                    assertThat(context).hasSingleBean(V1Client.class);

                    // verify that the client is used
                    Client client = context.getBean(ClientProvider.class).client();
                    verify(client).v1();
                    verifyNoMoreInteractions(client);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        V1Client v1Client() {
            return mock(V1Client.class);
        }
    }

    @Configuration
    static class ClientProvider {

        @Bean
        Client client() {
            return mock(Client.class);
        }
    }
}
