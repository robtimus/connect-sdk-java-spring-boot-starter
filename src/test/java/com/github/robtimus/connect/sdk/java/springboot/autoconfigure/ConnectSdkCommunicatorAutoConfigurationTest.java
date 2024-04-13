/*
 * ConnectSdkCommunicatorAutoConfigurationTest.java
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.worldline.connect.sdk.java.Communicator;
import com.worldline.connect.sdk.java.authentication.Authenticator;
import com.worldline.connect.sdk.java.authentication.V1HMACAuthenticator;
import com.worldline.connect.sdk.java.communication.Connection;
import com.worldline.connect.sdk.java.communication.MetadataProvider;
import com.worldline.connect.sdk.java.json.DefaultMarshaller;
import com.worldline.connect.sdk.java.json.Marshaller;

@SuppressWarnings("nls")
class ConnectSdkCommunicatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkCommunicatorAutoConfiguration.class));

    @Test
    @SuppressWarnings("resource")
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, ConnectSdkConnectionAutoConfiguration.class,
                        ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetadataProviderAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkCommunicator");
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).getBean(Communicator.class).isSameAs(context.getBean(ExistingBeanProvider.class).communicator());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingProperties() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class)
                .withPropertyValues("connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret",
                        "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Communicator.class);
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .withUserConfiguration(ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetadataProviderAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class)
                    .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                            "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).hasSingleBean(Authenticator.class);
                        assertThat(context).hasSingleBean(MetadataProvider.class);
                        assertThat(context).hasSingleBean(Marshaller.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                    });
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkMetadataProviderAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class)
                    .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                            "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                    .run(context -> {
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).doesNotHaveBean(Authenticator.class);
                        assertThat(context).hasSingleBean(MetadataProvider.class);
                        assertThat(context).hasSingleBean(Marshaller.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                    });
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class)
                    .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                            "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                    .run(context -> {
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(Authenticator.class);
                        assertThat(context).doesNotHaveBean(MetadataProvider.class);
                        assertThat(context).hasSingleBean(Marshaller.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                    });
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class)
                    .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                            "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                    .run(context -> {
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(Authenticator.class);
                        assertThat(context).hasSingleBean(MetadataProvider.class);
                        assertThat(context).doesNotHaveBean(Marshaller.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                    });
    }

    @Test
    @SuppressWarnings("resource")
    void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com",
                        "connect.api.authorization-id=keyId", "connect.api.authorization-secret=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkCommunicator");
                    assertThat(context).hasSingleBean(Communicator.class);
                    Communicator communicator = context.getBean(Communicator.class);
                    assertThat(communicator).isExactlyInstanceOf(Communicator.class);
                    assertThat(getFieldValue(communicator, "apiEndpoint"))
                            .isEqualTo(URI.create("https://api.preprod.connect.worldline-solutions.com"));
                    assertThat(getFieldValue(communicator, "authenticator"))
                            .isSameAs(context.getBean(ConnectSdkAuthenticatorAutoConfiguration.class).connectSdkV1HMACAuthenticator());
                    assertThat(getFieldValue(communicator, "connection"))
                            .isSameAs(context.getBean(ConnectSdkConnectionAutoConfiguration.class).connectSdkConnection(null, null));
                    List<MetadataProviderBuilderCustomizer> customizers = Collections.emptyList();
                    assertThat(getFieldValue(communicator, "metadataProvider"))
                            .isSameAs(context.getBean(ConnectSdkMetadataProviderAutoConfiguration.class).connectSdkMetadataProvider(customizers));
                    assertThat(communicator.getMarshaller()).isSameAs(DefaultMarshaller.INSTANCE);
                });
        contextRunner
                .withUserConfiguration(CommunicatorComponentProvider.class)
                .withPropertyValues("connect.api.endpoint.host=api.preprod.connect.worldline-solutions.com")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkCommunicator");
                    assertThat(context).hasSingleBean(Communicator.class);
                    Communicator communicator = context.getBean(Communicator.class);
                    assertThat(communicator).isExactlyInstanceOf(Communicator.class);
                    assertThat(communicator.getMarshaller()).isSameAs(context.getBean(CommunicatorComponentProvider.class).marshaller());

                    // verify that the provided connection is used
                    Connection connection = context.getBean(CommunicatorComponentProvider.class).connection();
                    when(connection.post(any(URI.class), anyList(), anyString(), any())).thenReturn(null);
                    context.getBean(Communicator.class).post("/path", null, null, null, Void.class, null);
                    verify(connection).post(any(URI.class), anyList(), eq((String) null), any());
                    verifyNoMoreInteractions(connection);
                });
    }

    private Object getFieldValue(Communicator communicator, String fieldName) {
        return assertDoesNotThrow(() -> {
            Field field = Communicator.class.getDeclaredField(fieldName);
            field.trySetAccessible();
            return field.get(communicator);
        });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        Communicator communicator() {
            return mock(Communicator.class);
        }
    }

    @Configuration
    static class CommunicatorComponentProvider {

        @Bean
        Connection connection() {
            return mock(Connection.class);
        }

        @Bean
        Authenticator authenticator() {
            return new V1HMACAuthenticator("apiKeyId", "secretApiKey");
        }

        @Bean
        MetadataProvider metadataProvider() {
            return new MetadataProvider("Integrator");
        }

        @Bean
        Marshaller marshaller() {
            return mock(Marshaller.class);
        }
    }
}
