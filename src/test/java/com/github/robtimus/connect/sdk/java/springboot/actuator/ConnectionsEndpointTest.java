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

import static com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.CloseableConnectionState.EXPIRED;
import static com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.CloseableConnectionState.IDLE;
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
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.CloseableBeans;
import com.worldline.connect.sdk.java.Client;
import com.worldline.connect.sdk.java.Communicator;
import com.worldline.connect.sdk.java.communication.Connection;
import com.worldline.connect.sdk.java.communication.PooledConnection;

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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

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
                        ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                        CloseableBeans beans = endpoint.listCloseableBeans();
                        assertThat(beans.getConnections()).isEqualTo(Arrays.asList("pooledConnection"));
                        assertThat(beans.getCommunicators()).isEqualTo(Arrays.asList("communicator"));
                        assertThat(beans.getClients()).isEqualTo(Arrays.asList("client"));
                    });
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class CloseConnections {

        @Nested
        class Idle {

            @Nested
            class WithSpecificIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), IDLE);
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), IDLE);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                verifyNoMoreInteractions(connection);
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), IDLE);

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
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), IDLE);

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
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), IDLE);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), IDLE);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(pooledConnection).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }

            @Nested
            class WithDefaultIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, IDLE);
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, IDLE);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                verifyNoMoreInteractions(connection);
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, IDLE);

                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                                verify(pooledConnection).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(pooledConnection);
                            });
                }

                @Test
                void testWithCommunicatorBean() {
                    contextRunner
                            .withUserConfiguration(CommunicatorProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, IDLE);

                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                                verify(communicator).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(communicator);
                            });
                }

                @Test
                void testWithClientBean() {
                    contextRunner
                            .withUserConfiguration(ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(null, IDLE);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(null, IDLE);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(pooledConnection).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }
        }

        @Nested
        class Expired {

            @Test
            void testWithNoSupportedBeans() {
                contextRunner
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).doesNotHaveBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            endpoint.closeConnections(null, EXPIRED);
                        });
            }

            @Test
            void testWithNonPooledConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class)
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            endpoint.closeConnections(null, EXPIRED);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();

                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithPooledConnectionBean() {
                contextRunner
                        .withUserConfiguration(PooledConnectionProvider.class)
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            // the PooledConnection bean is also a Connection bean
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            endpoint.closeConnections(null, EXPIRED);

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
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).doesNotHaveBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            endpoint.closeConnections(null, EXPIRED);

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
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).doesNotHaveBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);

                            endpoint.closeConnections(null, EXPIRED);

                            Client client = context.getBean(ClientProvider.class).client();

                            verify(client).closeExpiredConnections();
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                ClientProvider.class)
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            // two Connection beans - connection and pooledConnection
                            assertThat(context).getBeans(Connection.class).hasSize(2);
                            assertThat(context).hasSingleBean(PooledConnection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);

                            endpoint.closeConnections(null, EXPIRED);

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
        class IdleAndExpired {

            @Nested
            class WithSpecificIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), null);
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), null);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                verifyNoMoreInteractions(connection);
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), null);

                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                                verify(pooledConnection).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(pooledConnection).closeExpiredConnections();
                                verifyNoMoreInteractions(pooledConnection);
                            });
                }

                @Test
                void testWithCommunicatorBean() {
                    contextRunner
                            .withUserConfiguration(CommunicatorProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), null);

                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                                verify(communicator).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeExpiredConnections();
                                verifyNoMoreInteractions(communicator);
                            });
                }

                @Test
                void testWithClientBean() {
                    contextRunner
                            .withUserConfiguration(ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), null);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(Duration.ofSeconds(20), null);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(pooledConnection).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(pooledConnection).closeExpiredConnections();
                                verify(communicator).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeExpiredConnections();
                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }

            @Nested
            class WithDefaultIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, null);
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, null);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                verifyNoMoreInteractions(connection);
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, null);

                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                                verify(pooledConnection).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(pooledConnection).closeExpiredConnections();
                                verifyNoMoreInteractions(pooledConnection);
                            });
                }

                @Test
                void testWithCommunicatorBean() {
                    contextRunner
                            .withUserConfiguration(CommunicatorProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnections(null, null);

                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                                verify(communicator).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeExpiredConnections();
                                verifyNoMoreInteractions(communicator);
                            });
                }

                @Test
                void testWithClientBean() {
                    contextRunner
                            .withUserConfiguration(ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(null, null);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnections(null, null);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(pooledConnection).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(pooledConnection).closeExpiredConnections();
                                verify(communicator).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeExpiredConnections();
                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class CloseConnectionsForBean {

        @Nested
        class Idle {

            @Nested
            class WithSpecificIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                Duration idleTime = Duration.ofSeconds(20);
                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("pooledConnection", idleTime, IDLE))
                                        .isInstanceOf(NoSuchBeanDefinitionException.class).extracting("beanName", "beanType")
                                        .isEqualTo(Arrays.asList("pooledConnection", null));
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                Duration idleTime = Duration.ofSeconds(20);
                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("connection", idleTime, IDLE))
                                        .isInstanceOf(BeanNotCloseableException.class).extracting("beanName", "actualType")
                                        .isEqualTo(Arrays.asList("connection", connection.getClass()));
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("pooledConnection", Duration.ofSeconds(20), IDLE);

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
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("communicator", Duration.ofSeconds(20), IDLE);

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
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", Duration.ofSeconds(20), IDLE);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", Duration.ofSeconds(20), IDLE);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }

            @Nested
            class WithDefaultIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("pooledConnection", null, IDLE))
                                        .isInstanceOf(NoSuchBeanDefinitionException.class).extracting("beanName", "beanType")
                                        .isEqualTo(Arrays.asList("pooledConnection", null));
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("connection", null, IDLE))
                                        .isInstanceOf(BeanNotCloseableException.class).extracting("beanName", "actualType")
                                        .isEqualTo(Arrays.asList("connection", connection.getClass()));
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("pooledConnection", null, IDLE);

                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                                verify(pooledConnection).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(pooledConnection);
                            });
                }

                @Test
                void testWithCommunicatorBean() {
                    contextRunner
                            .withUserConfiguration(CommunicatorProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("communicator", null, IDLE);

                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                                verify(communicator).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(communicator);
                            });
                }

                @Test
                void testWithClientBean() {
                    contextRunner
                            .withUserConfiguration(ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", null, IDLE);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", null, IDLE);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }
        }

        @Nested
        class Expired {

            @Test
            void testWithNoSupportedBeans() {
                contextRunner
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).doesNotHaveBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            assertThatThrownBy(() -> endpoint.closeConnectionsForBean("pooledConnection", null, EXPIRED))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class).extracting("beanName", "beanType")
                                    .isEqualTo(Arrays.asList("pooledConnection", null));
                        });
            }

            @Test
            void testWithNonPooledConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class)
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();

                            assertThatThrownBy(() -> endpoint.closeConnectionsForBean("connection", null, EXPIRED))
                                    .isInstanceOf(BeanNotCloseableException.class).extracting("beanName", "actualType")
                                    .isEqualTo(Arrays.asList("connection", connection.getClass()));
                        });
            }

            @Test
            void testWithPooledConnectionBean() {
                contextRunner
                        .withUserConfiguration(PooledConnectionProvider.class)
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            // the PooledConnection bean is also a Connection bean
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            endpoint.closeConnectionsForBean("pooledConnection", null, EXPIRED);

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
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).doesNotHaveBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(Client.class);

                            endpoint.closeConnectionsForBean("communicator", null, EXPIRED);

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
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            assertThat(context).doesNotHaveBean(Connection.class);
                            assertThat(context).doesNotHaveBean(PooledConnection.class);
                            assertThat(context).doesNotHaveBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);

                            endpoint.closeConnectionsForBean("client", null, EXPIRED);

                            Client client = context.getBean(ClientProvider.class).client();

                            verify(client).closeExpiredConnections();
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                ClientProvider.class)
                        .run(context -> {
                            ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                            // two Connection beans - connection and pooledConnection
                            assertThat(context).getBeans(Connection.class).hasSize(2);
                            assertThat(context).hasSingleBean(PooledConnection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);

                            endpoint.closeConnectionsForBean("client", null, EXPIRED);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();

                            verify(client).closeExpiredConnections();
                            verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                        });
            }
        }

        @Nested
        class IdleAndExpired {

            @Nested
            class WithSpecificIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                Duration idleTime = Duration.ofSeconds(20);
                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("pooledConnection", idleTime, null))
                                        .isInstanceOf(NoSuchBeanDefinitionException.class).extracting("beanName", "beanType")
                                        .isEqualTo(Arrays.asList("pooledConnection", null));
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                Duration idleTime = Duration.ofSeconds(20);
                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("connection", idleTime, null))
                                        .isInstanceOf(BeanNotCloseableException.class).extracting("beanName", "actualType")
                                        .isEqualTo(Arrays.asList("connection", connection.getClass()));
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("pooledConnection", Duration.ofSeconds(20), null);

                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                                verify(pooledConnection).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(pooledConnection).closeExpiredConnections();
                                verifyNoMoreInteractions(pooledConnection);
                            });
                }

                @Test
                void testWithCommunicatorBean() {
                    contextRunner
                            .withUserConfiguration(CommunicatorProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("communicator", Duration.ofSeconds(20), null);

                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                                verify(communicator).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeExpiredConnections();
                                verifyNoMoreInteractions(communicator);
                            });
                }

                @Test
                void testWithClientBean() {
                    contextRunner
                            .withUserConfiguration(ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", Duration.ofSeconds(20), null);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", Duration.ofSeconds(20), null);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(20_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }

            @Nested
            class WithDefaultIdleTime {

                @Test
                void testWithNoSupportedBeans() {
                    contextRunner
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("pooledConnection", null, null))
                                        .isInstanceOf(NoSuchBeanDefinitionException.class).extracting("beanName", "beanType")
                                        .isEqualTo(Arrays.asList("pooledConnection", null));
                            });
                }

                @Test
                void testWithNonPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();

                                assertThatThrownBy(() -> endpoint.closeConnectionsForBean("connection", null, null))
                                        .isInstanceOf(BeanNotCloseableException.class).extracting("beanName", "actualType")
                                        .isEqualTo(Arrays.asList("connection", connection.getClass()));
                            });
                }

                @Test
                void testWithPooledConnectionBean() {
                    contextRunner
                            .withUserConfiguration(PooledConnectionProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // the PooledConnection bean is also a Connection bean
                                assertThat(context).hasSingleBean(Connection.class);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("pooledConnection", null, null);

                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();

                                verify(pooledConnection).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(pooledConnection).closeExpiredConnections();
                                verifyNoMoreInteractions(pooledConnection);
                            });
                }

                @Test
                void testWithCommunicatorBean() {
                    contextRunner
                            .withUserConfiguration(CommunicatorProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).doesNotHaveBean(Client.class);

                                endpoint.closeConnectionsForBean("communicator", null, null);

                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                                verify(communicator).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(communicator).closeExpiredConnections();
                                verifyNoMoreInteractions(communicator);
                            });
                }

                @Test
                void testWithClientBean() {
                    contextRunner
                            .withUserConfiguration(ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                assertThat(context).doesNotHaveBean(Connection.class);
                                assertThat(context).doesNotHaveBean(PooledConnection.class);
                                assertThat(context).doesNotHaveBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", null, null);

                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(client);
                            });
                }

                @Test
                void testWithBeansOfAllSupportedTypes() {
                    contextRunner
                            .withUserConfiguration(ConnectionProvider.class, PooledConnectionProvider.class, CommunicatorProvider.class,
                                    ClientProvider.class)
                            .run(context -> {
                                ConnectionsEndpoint endpoint = new ConnectionsEndpoint(context, 10_000);

                                // two Connection beans - connection and pooledConnection
                                assertThat(context).getBeans(Connection.class).hasSize(2);
                                assertThat(context).hasSingleBean(PooledConnection.class);
                                assertThat(context).hasSingleBean(Communicator.class);
                                assertThat(context).hasSingleBean(Client.class);

                                endpoint.closeConnectionsForBean("client", null, null);

                                Connection connection = context.getBean(ConnectionProvider.class).connection();
                                PooledConnection pooledConnection = context.getBean(PooledConnectionProvider.class).pooledConnection();
                                Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                                Client client = context.getBean(ClientProvider.class).client();

                                verify(client).closeIdleConnections(10_000L, TimeUnit.MILLISECONDS);
                                verify(client).closeExpiredConnections();
                                verifyNoMoreInteractions(connection, pooledConnection, communicator, client);
                            });
                }
            }
        }
    }
}
