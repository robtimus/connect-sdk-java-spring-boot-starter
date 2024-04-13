/*
 * ConnectSdkConnectionAutoConfigurationTest.java
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
import static org.assertj.core.api.InstanceOfAssertFactories.atomicReference;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.worldline.connect.sdk.java.communication.Connection;
import com.worldline.connect.sdk.java.communication.DefaultConnection;
import com.worldline.connect.sdk.java.communication.PooledConnection;
import com.worldline.connect.sdk.java.logging.BodyObfuscator;
import com.worldline.connect.sdk.java.logging.HeaderObfuscator;

@SuppressWarnings("nls")
class ConnectSdkConnectionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkConnectionAutoConfiguration.class));

    @Test
    @SuppressWarnings("resource")
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkConnection");
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).doesNotHaveBean(PooledConnection.class);
                    assertThat(context).getBean(Connection.class).isSameAs(context.getBean(ExistingBeanProvider.class).connection());
                });
    }

    @Nested
    @SuppressWarnings("resource")
    class AutoConfiguration {

        @Test
        void testMinimal() {
            contextRunner
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkConnection");
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).getBean(Connection.class).isExactlyInstanceOf(DefaultConnection.class);
                        assertThat(context).getBean(Connection.class).isSameAs(context.getBean(PooledConnection.class));
                        assertThat(context).getBean(Connection.class).extracting("bodyObfuscator")
                                .asInstanceOf(atomicReference(BodyObfuscator.class))
                                .doesNotHaveValue(null)
                                .doesNotHaveValue(BodyObfuscatorProvider.BODY_OBFUSCATOR);
                        assertThat(context).getBean(Connection.class).extracting("headerObfuscator")
                                .asInstanceOf(atomicReference(HeaderObfuscator.class))
                                .doesNotHaveValue(null)
                                .doesNotHaveValue(HeaderObfuscatorProvider.HEADER_OBFUSCATOR);
                        assertThat(context).hasSingleBean(ConnectSdkConnectionAutoConfiguration.ConnectionManager.class);
                    });
        }

        @Test
        void testWithBodyObfuscator() {
            contextRunner
                    .withUserConfiguration(BodyObfuscatorProvider.class)
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkConnection");
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).getBean(Connection.class).isExactlyInstanceOf(DefaultConnection.class);
                        assertThat(context).getBean(Connection.class).isSameAs(context.getBean(PooledConnection.class));
                        assertThat(context).getBean(Connection.class).extracting("bodyObfuscator")
                                .asInstanceOf(atomicReference(BodyObfuscator.class))
                                .hasValue(BodyObfuscatorProvider.BODY_OBFUSCATOR);
                        assertThat(context).getBean(Connection.class).extracting("headerObfuscator")
                                .asInstanceOf(atomicReference(HeaderObfuscator.class))
                                .doesNotHaveValue(null)
                                .doesNotHaveValue(HeaderObfuscatorProvider.HEADER_OBFUSCATOR);
                        assertThat(context).hasSingleBean(ConnectSdkConnectionAutoConfiguration.ConnectionManager.class);
                    });
        }

        @Test
        void testWithHeaderObfuscator() {
            contextRunner
                    .withUserConfiguration(HeaderObfuscatorProvider.class)
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkConnection");
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).getBean(Connection.class).isExactlyInstanceOf(DefaultConnection.class);
                        assertThat(context).getBean(Connection.class).isSameAs(context.getBean(PooledConnection.class));
                        assertThat(context).getBean(Connection.class).extracting("bodyObfuscator")
                                .asInstanceOf(atomicReference(BodyObfuscator.class))
                                .doesNotHaveValue(null)
                                .doesNotHaveValue(BodyObfuscatorProvider.BODY_OBFUSCATOR);
                        assertThat(context).getBean(Connection.class).extracting("headerObfuscator")
                                .asInstanceOf(atomicReference(HeaderObfuscator.class))
                                .hasValue(HeaderObfuscatorProvider.HEADER_OBFUSCATOR);
                        assertThat(context).hasSingleBean(ConnectSdkConnectionAutoConfiguration.ConnectionManager.class);
                    });
        }

        @Test
        void testCloseIdleConnectionsEnabled() {
            contextRunner
                    .withPropertyValues("connect.api.close-idle-connections.enabled=true", "connect.api.close-idle-connections.idle-time=10",
                            "connect.api.close-idle-connections.interval=10")
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkConnection");
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).getBean(Connection.class).isExactlyInstanceOf(DefaultConnection.class);
                        assertThat(context).getBean(Connection.class).isSameAs(context.getBean(PooledConnection.class));
                        assertThat(context).hasSingleBean(ConnectSdkConnectionAutoConfiguration.ConnectionManager.class);
                        // Note: it's currently not possible to test that idle connections have been closed after 10 seconds
                    });
        }

        @Test
        void testCloseIdleConnectionsDisabled() {
            contextRunner
                    .withPropertyValues("connect.api.close-idle-connections.enabled=false")
                    .run(context -> {
                        assertThat(context).hasBean("connectSdkConnection");
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).getBean(Connection.class).isExactlyInstanceOf(DefaultConnection.class);
                        assertThat(context).getBean(Connection.class).isSameAs(context.getBean(PooledConnection.class));
                        assertThat(context).doesNotHaveBean(ConnectSdkConnectionAutoConfiguration.ConnectionManager.class);
                    });
        }
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        Connection connection() {
            return mock(Connection.class);
        }
    }

    @Configuration
    static class BodyObfuscatorProvider {

        private static final BodyObfuscator BODY_OBFUSCATOR = BodyObfuscator.custom()
                    .obfuscateAll("dummy")
                    .build();

        @Bean
        BodyObfuscator bodyObfuscator() {
            return BODY_OBFUSCATOR;
        }
    }

    @Configuration
    static class HeaderObfuscatorProvider {

        private static final HeaderObfuscator HEADER_OBFUSCATOR = HeaderObfuscator.custom()
                    .obfuscateAll("dummy")
                    .build();

        @Bean
        HeaderObfuscator headerObfuscator() {
            return HEADER_OBFUSCATOR;
        }
    }
}
