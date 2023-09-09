/*
 * ConnectSdkSessionAutoConfigurationTest.java
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
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.MetaDataProvider;
import com.ingenico.connect.gateway.sdk.java.Session;

@SuppressWarnings("nls")
class ConnectSdkSessionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkSessionAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, ConnectSdkConnectionAutoConfiguration.class,
                        ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetaDataProviderAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkSession");
                    assertThat(context).hasSingleBean(Session.class);
                    assertThat(context).getBean(Session.class).isSameAs(context.getBean(ExistingBeanProvider.class).session());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingProperties() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class)
                .withPropertyValues("connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Session.class);
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingBeans() {
        contextRunner
                .withUserConfiguration(ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkMetaDataProviderAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).doesNotHaveBean(Connection.class);
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).doesNotHaveBean(Session.class);
                });
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkMetaDataProviderAutoConfiguration.class)
                    .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                            "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(Authenticator.class);
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(MetaDataProvider.class);
                        assertThat(context).doesNotHaveBean(Session.class);
                    });
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).doesNotHaveBean(MetaDataProvider.class);
                    assertThat(context).doesNotHaveBean(Session.class);
                });
    }

    @Test
    @SuppressWarnings({ "resource", "deprecation" })
    void testAutoConfiguration() {
        contextRunner
                .withUserConfiguration(ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
                        ConnectSdkMetaDataProviderAutoConfiguration.class)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.api-key-id=keyId", "connect.api.secret-api-key=secret", "connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkSession");
                    assertThat(context).hasSingleBean(Session.class);
                    Session session = context.getBean(Session.class);
                    assertThat(session).isExactlyInstanceOf(Session.class);
                    assertThat(session.getApiEndpoint()).isEqualTo(URI.create("https://eu.sandbox.api-ingenico.com"));
                    assertThat(session.getAuthenticator())
                            .isSameAs(context.getBean(ConnectSdkAuthenticatorAutoConfiguration.class).connectSdkAuthenticator());
                    assertThat(session.getConnection())
                            .isSameAs(context.getBean(ConnectSdkConnectionAutoConfiguration.class).connectSdkConnection());
                    List<MetaDataProviderBuilderCustomizer> customizers = Collections.emptyList();
                    assertThat(session.getMetaDataProvider())
                            .isSameAs(context.getBean(ConnectSdkMetaDataProviderAutoConfiguration.class).connectSdkMetaDataProvider(customizers));
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        Session session() {
            return mock(Session.class);
        }
    }
}
