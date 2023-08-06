/*
 * IdleConnectionsEndpointTest.java
 * Copyright 2023 Rob Spoor
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

package com.github.robtimus.connect.sdk.java.springboot.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.github.robtimus.connect.sdk.java.springboot.actuator.BeanProviders.ClientProvider;
import com.github.robtimus.connect.sdk.java.springboot.actuator.BeanProviders.CommunicatorProvider;
import com.github.robtimus.connect.sdk.java.springboot.actuator.BeanProviders.ConnectionProvider;
import com.github.robtimus.connect.sdk.java.springboot.actuator.BeanProviders.PooledConnectionProvider;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.BeanNotCloseableException;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;

@SuppressWarnings("nls")
class IdleConnectionsEndpointTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Nested
    @SuppressWarnings("resource")
    class CloseIdleConnections {

        @Test
        void testWithNoSupportedBeans() {
            contextRunner
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(Duration.ofSeconds(20));
                    });
        }

        @Test
        void testWithNonPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(Duration.ofSeconds(20));

                        Connection connection = context.getBean(ConnectionProvider.class).connection();

                        verifyNoMoreInteractions(connection);
                    });
        }

        @Test
        void testWithPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(PooledConnectionProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        // the PooledConnection bean is also a Connection bean
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(Duration.ofSeconds(20));

                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                        verify(pooledConnection).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(pooledConnection);
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(Duration.ofSeconds(20));

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnections(Duration.ofSeconds(20));

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        // two Connection beans - connection and pooledConnection
                        assertThat(context).getBeans(Connection.class).hasSize(2);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnections(null);

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(pooledConnection).closeIdleConnections(10_000, TimeUnit.MILLISECONDS);
                        verify(communicator).closeIdleConnections(10_000, TimeUnit.MILLISECONDS);
                        verify(client).closeIdleConnections(10_000, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                    });
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class CloseIdleConnectionsForBean {

        @Test
        void testWithNoSupportedBeans() {
            contextRunner
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        assertThatThrownBy(() -> endpoint.closeIdleConnectionsForBean("pooledConnection", 20L, TimeUnit.SECONDS))
                                .isInstanceOf(NoSuchBeanDefinitionException.class)
                                .extracting("beanName", "beanType").isEqualTo(Arrays.asList("pooledConnection", null));
                    });
        }

        @Test
        void testWithNonPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        Connection connection = context.getBean(ConnectionProvider.class).connection();

                        assertThatThrownBy(() -> endpoint.closeIdleConnectionsForBean("connection", 20L, TimeUnit.SECONDS))
                                .isInstanceOf(BeanNotCloseableException.class)
                                .extracting("beanName", "actualType").isEqualTo(Arrays.asList("connection", connection.getClass()));
                    });
        }

        @Test
        void testWithPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(PooledConnectionProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        // the PooledConnection bean is also a Connection bean
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("pooledConnection", Duration.ofSeconds(20));

                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                        verify(pooledConnection).closeIdleConnections(20_000, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(pooledConnection);
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("communicator", Duration.ofSeconds(20));

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).closeIdleConnections(20_000, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("client", Duration.ofSeconds(20));

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeIdleConnections(20_000, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        IdleConnectionsEndpoint endpoint = new IdleConnectionsEndpoint(context, 10_000);

                        // two Connection beans - connection and pooledConnection
                        assertThat(context).getBeans(Connection.class).hasSize(2);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("client", null);

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeIdleConnections(10_000, TimeUnit.MILLISECONDS);
                        verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                    });
        }
    }
}
