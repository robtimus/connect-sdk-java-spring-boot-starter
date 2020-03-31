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
import static org.mockito.Mockito.mock;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultConnection;

public class ConnectSdkConnectionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkConnectionAutoConfiguration.class));

    @Test
    @SuppressWarnings("resource")
    public void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkConnection");
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).doesNotHaveBean(PooledConnection.class);
                    assertThat(context).getBean(Connection.class).isSameAs(context.getBean(ExistingBeanProvider.class).connection());
                });
    }

    @Test
    @SuppressWarnings("resource")
    public void testAutoConfiguration() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasBean("connectSdkConnection");
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).hasSingleBean(PooledConnection.class);
                    assertThat(context).getBean(Connection.class).isExactlyInstanceOf(DefaultConnection.class);
                    assertThat(context).getBean(Connection.class).isSameAs(context.getBean(PooledConnection.class));
                    assertThat(context).hasSingleBean(ConnectSdkConnectionAutoConfiguration.ConnectionManager.class);
                });
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
                    Thread.sleep(50);
                });
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

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        public Connection connection() {
            return mock(Connection.class);
        }
    }
}
