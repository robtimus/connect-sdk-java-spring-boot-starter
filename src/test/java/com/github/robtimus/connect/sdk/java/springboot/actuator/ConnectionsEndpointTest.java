/*
 * ConnectionsEndpointTest.java
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

package com.github.robtimus.connect.sdk.java.springboot.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.BeanNotCloseableException;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.CloseableBeans;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;

@SuppressWarnings("nls")
class ConnectionsEndpointTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Nested
    class ListCloseableBeans {

        @Test
        void testWithNonPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        CloseableBeans beans = endpoint.listCloseableBeans();
                        assertThat(beans.getConnections()).isEmpty();
                        assertThat(beans.getCommunicators()).isEmpty();
                        assertThat(beans.getClients()).isEmpty();
                    });
        }

        @Test
        void testWithPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(PooledConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        CloseableBeans beans = endpoint.listCloseableBeans();
                        assertThat(beans.getConnections()).isEqualTo(Arrays.asList("pooledConnection"));
                        assertThat(beans.getCommunicators()).isEmpty();
                        assertThat(beans.getClients()).isEmpty();
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        CloseableBeans beans = endpoint.listCloseableBeans();
                        assertThat(beans.getConnections()).isEmpty();
                        assertThat(beans.getCommunicators()).isEqualTo(Arrays.asList("communicator"));
                        assertThat(beans.getClients()).isEmpty();
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        CloseableBeans beans = endpoint.listCloseableBeans();
                        assertThat(beans.getConnections()).isEmpty();
                        assertThat(beans.getCommunicators()).isEmpty();
                        assertThat(beans.getClients()).isEqualTo(Arrays.asList("client"));
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        CloseableBeans beans = endpoint.listCloseableBeans();
                        assertThat(beans.getConnections()).isEqualTo(Arrays.asList("pooledConnection"));
                        assertThat(beans.getCommunicators()).isEqualTo(Arrays.asList("communicator"));
                        assertThat(beans.getClients()).isEqualTo(Arrays.asList("client"));
                    });
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class CloseIdleConnections {

        @Test
        void testWithNoSupportedBeans() {
            contextRunner
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(20L, TimeUnit.SECONDS);
                    });
        }

        @Test
        void testWithNonPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(20L, TimeUnit.SECONDS);

                        Connection connection = context.getBean(ConnectionProvider.class).connection();

                        verifyNoMoreInteractions(connection);
                    });
        }

        @Test
        void testWithPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(PooledConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // the PooledConnection bean is also a Connection bean
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(20L, TimeUnit.SECONDS);

                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                        verify(pooledConnection).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verifyNoMoreInteractions(pooledConnection);
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnections(20L, TimeUnit.SECONDS);

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnections(20L, TimeUnit.SECONDS);

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // two Connection beans - connection and pooledConnection
                        assertThat(context).getBeans(Connection.class).hasSize(2);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnections(20L, TimeUnit.SECONDS);

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(pooledConnection).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verify(communicator).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verify(client).closeIdleConnections(20L, TimeUnit.SECONDS);
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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // the PooledConnection bean is also a Connection bean
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("pooledConnection", 20L, TimeUnit.SECONDS);

                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                        verify(pooledConnection).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verifyNoMoreInteractions(pooledConnection);
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("communicator", 20L, TimeUnit.SECONDS);

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("client", 20L, TimeUnit.SECONDS);

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // two Connection beans - connection and pooledConnection
                        assertThat(context).getBeans(Connection.class).hasSize(2);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeIdleConnectionsForBean("client", 20L, TimeUnit.SECONDS);

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeIdleConnections(20L, TimeUnit.SECONDS);
                        verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                    });
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class CloseExpiredConnections {

        @Test
        void testWithNoSupportedBeans() {
            contextRunner
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeExpiredConnections();
                    });
        }

        @Test
        void testWithNonPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeExpiredConnections();

                        Connection connection = context.getBean(ConnectionProvider.class).connection();

                        verifyNoMoreInteractions(connection);
                    });
        }

        @Test
        void testWithPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(PooledConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // the PooledConnection bean is also a Connection bean
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeExpiredConnections();

                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                        verify(pooledConnection).closeExpiredConnections();
                        verifyNoMoreInteractions(pooledConnection);
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeExpiredConnections();

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).closeExpiredConnections();
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeExpiredConnections();

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeExpiredConnections();
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // two Connection beans - connection and pooledConnection
                        assertThat(context).getBeans(Connection.class).hasSize(2);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeExpiredConnections();

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(pooledConnection).closeExpiredConnections();
                        verify(communicator).closeExpiredConnections();
                        verify(client).closeExpiredConnections();
                        verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                    });
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class CloseExpiredConnectionsForBean {

        @Test
        void testWithNoSupportedBeans() {
            contextRunner
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        assertThatThrownBy(() -> endpoint.closeExpiredConnectionsForBean("pooledConnection"))
                                .isInstanceOf(NoSuchBeanDefinitionException.class)
                                .extracting("beanName", "beanType").isEqualTo(Arrays.asList("pooledConnection", null));
                    });
        }

        @Test
        void testWithNonPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        Connection connection = context.getBean(ConnectionProvider.class).connection();

                        assertThatThrownBy(() -> endpoint.closeExpiredConnectionsForBean("connection"))
                                .isInstanceOf(BeanNotCloseableException.class)
                                .extracting("beanName", "actualType").isEqualTo(Arrays.asList("connection", connection.getClass()));
                    });
        }

        @Test
        void testWithPooledConnectionBean() {
            contextRunner
                    .withUserConfiguration(PooledConnectionProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // the PooledConnection bean is also a Connection bean
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeExpiredConnectionsForBean("pooledConnection");

                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                        verify(pooledConnection).closeExpiredConnections();
                        verifyNoMoreInteractions(pooledConnection);
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).doesNotHaveBean(Client.class);

                        endpoint.closeExpiredConnectionsForBean("communicator");

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).closeExpiredConnections();
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        assertThat(context).doesNotHaveBean(Connection.class);
                        assertThat(context).doesNotHaveBean(PooledConnection.class);
                        assertThat(context).doesNotHaveBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeExpiredConnectionsForBean("client");

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeExpiredConnections();
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context);

                        // two Connection beans - connection and pooledConnection
                        assertThat(context).getBeans(Connection.class).hasSize(2);
                        assertThat(context).hasSingleBean(PooledConnection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.closeExpiredConnectionsForBean("client");

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).closeExpiredConnections();
                        verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                    });
        }
    }

    @Configuration
    static class ConnectionProvider {

        @Bean
        @Primary
        Connection connection() {
            return mock(Connection.class);
        }
    }

    @Configuration
    static class PooledConnectionProvider {

        @Bean
        PooledConnection pooledConnection() {
            return mock(PooledConnection.class);
        }
    }

    @Configuration
    static class CommunicatorProvider {

        @Bean
        Communicator communicator() {
            return mock(Communicator.class);
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
