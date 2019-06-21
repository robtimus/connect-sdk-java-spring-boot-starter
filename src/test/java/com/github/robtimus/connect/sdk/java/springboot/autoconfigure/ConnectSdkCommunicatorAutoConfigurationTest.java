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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.net.URI;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.Marshaller;
import com.ingenico.connect.gateway.sdk.java.MetaDataProvider;
import com.ingenico.connect.gateway.sdk.java.Session;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultAuthenticator;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultMarshaller;

public class ConnectSdkCommunicatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkCommunicatorAutoConfiguration.class));

    @Test
    public void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, ConnectSdkConnectionAutoConfiguration.class,
                        ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetaDataProviderAutoConfiguration.class,
                        ConnectSdkSessionAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkCommunicator");
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).getBean(Communicator.class).isSameAs(context.getBean(ExistingBeanProvider.class).communicator());
                });
    }

    @Test
    public void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .withUserConfiguration(ConnectSdkMarshallerAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).doesNotHaveBean(Session.class);
                    assertThat(context).doesNotHaveBean(Communicator.class);
                });
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class, ConnectSdkSessionAutoConfiguration.class)
                    .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                            "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(Marshaller.class);
                        assertThat(context).hasSingleBean(Session.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                    });
    }

    @Test
    public void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class, ConnectSdkSessionAutoConfiguration.class,
                        ConnectSdkMarshallerAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkCommunicator");
                    assertThat(context).hasSingleBean(Communicator.class);
                    @SuppressWarnings("resource")
                    Communicator communicator = context.getBean(Communicator.class);
                    assertThat(communicator).isExactlyInstanceOf(Communicator.class);
                    assertThat(communicator.getMarshaller()).isSameAs(DefaultMarshaller.INSTANCE);
                });
        contextRunner
                .withUserConfiguration(SessionProvider.class, ConnectSdkMarshallerAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("connectSdkCommunicator");
                    assertThat(context).hasSingleBean(Communicator.class);
                    @SuppressWarnings("resource")
                    Communicator communicator = context.getBean(Communicator.class);
                    assertThat(communicator).isExactlyInstanceOf(Communicator.class);
                    assertThat(communicator.getMarshaller()).isSameAs(DefaultMarshaller.INSTANCE);

                    // verify that the session's connection is used
                    @SuppressWarnings("resource")
                    Connection connection = context.getBean(SessionProvider.class).connection();
                    when(connection.post(any(URI.class), anyList(), anyString(), any())).thenReturn(null);
                    context.getBean(Communicator.class).post("/path", null, null, null, Void.class, null);
                    verify(connection).post(any(URI.class), anyList(), eq((String) null), any());
                    verifyNoMoreInteractions(connection);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        public Communicator communicator() {
            return mock(Communicator.class);
        }
    }

    @Configuration
    static class SessionProvider {

        @Bean
        public Session session() {
            Session session = mock(Session.class);
            when(session.getApiEndpoint()).thenReturn(URI.create("https://eu.sandbox.api-ingenico.com"));
            when(session.getConnection()).thenReturn(connection());
            when(session.getAuthenticator()).thenReturn(authenticator());
            when(session.getMetaDataProvider()).thenReturn(metaDataProvider());
            return session;
        }

        @Bean
        public Connection connection() {
            return mock(Connection.class);
        }

        @Bean
        public Authenticator authenticator() {
            return new DefaultAuthenticator(AuthorizationType.V1HMAC, "apiKeyId", "secretApiKey");
        }

        @Bean
        public MetaDataProvider metaDataProvider() {
            return new MetaDataProvider("Integrator");
        }
    }
}
