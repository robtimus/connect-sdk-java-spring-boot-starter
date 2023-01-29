/*
 * LoggingEndpointTest.java
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.github.robtimus.connect.sdk.java.springboot.actuator.LoggingEndpoint.CompoundCommunicatorLogger;
import com.github.robtimus.connect.sdk.java.springboot.actuator.LoggingEndpoint.LoggingCapableAndLoggerBeans;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.Marshaller;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultMarshaller;
import com.ingenico.connect.gateway.sdk.java.logging.CommunicatorLogger;
import com.ingenico.connect.gateway.sdk.java.logging.LoggingCapable;

@SuppressWarnings("nls")
class LoggingEndpointTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Nested
    class ListLoggingCapableAndLoggerBeans {

        @Test
        void testWithNoSupportedBeans() {
            contextRunner
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        LoggingCapableAndLoggerBeans beans = endpoint.listLoggingCapableAndLoggerBeans();
                        assertThat(beans.getConnections()).isEmpty();
                        assertThat(beans.getCommunicators()).isEmpty();
                        assertThat(beans.getClients()).isEmpty();
                    });
        }

        @Test
        void testWithConnectionBean() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        LoggingCapableAndLoggerBeans beans = endpoint.listLoggingCapableAndLoggerBeans();
                        assertThat(beans.getConnections()).isEqualTo(Arrays.asList("connection"));
                        assertThat(beans.getCommunicators()).isEmpty();
                        assertThat(beans.getClients()).isEmpty();
                        assertThat(beans.getLoggers()).isEmpty();
                    });
        }

        @Test
        void testWithCommunicatorBean() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        LoggingCapableAndLoggerBeans beans = endpoint.listLoggingCapableAndLoggerBeans();
                        assertThat(beans.getConnections()).isEmpty();
                        assertThat(beans.getCommunicators()).isEqualTo(Arrays.asList("communicator"));
                        assertThat(beans.getClients()).isEmpty();
                        assertThat(beans.getLoggers()).isEmpty();
                    });
        }

        @Test
        void testWithClientBean() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        LoggingCapableAndLoggerBeans beans = endpoint.listLoggingCapableAndLoggerBeans();
                        assertThat(beans.getConnections()).isEmpty();
                        assertThat(beans.getCommunicators()).isEmpty();
                        assertThat(beans.getClients()).isEqualTo(Arrays.asList("client"));
                        assertThat(beans.getLoggers()).isEmpty();
                    });
        }

        @Test
        void testWithLoggerBean() {
            contextRunner
                    .withUserConfiguration(LoggerProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        LoggingCapableAndLoggerBeans beans = endpoint.listLoggingCapableAndLoggerBeans();
                        assertThat(beans.getConnections()).isEmpty();
                        assertThat(beans.getCommunicators()).isEmpty();
                        assertThat(beans.getClients()).isEmpty();
                        assertThat(beans.getLoggers()).isEqualTo(Arrays.asList("logger"));
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class, LoggerProvider.class,
                            AdditionalLoggerProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        LoggingCapableAndLoggerBeans beans = endpoint.listLoggingCapableAndLoggerBeans();
                        assertThat(beans.getConnections()).isEqualTo(Arrays.asList("connection"));
                        assertThat(beans.getCommunicators()).isEqualTo(Arrays.asList("communicator"));
                        assertThat(beans.getClients()).isEqualTo(Arrays.asList("client"));
                        assertThat(beans.getLoggers()).containsExactlyInAnyOrder("logger", "additionalLogger");
                    });
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class EnableLogging {

        @Nested
        class WithNoLoggers {

            @Test
            void testWithNoSupportedBeans() {
                contextRunner
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();

                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Client client = context.getBean(ClientProvider.class).client();

                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();

                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }

        @Nested
        class WithOneLogger {

            @Test
            void testWithNoSupportedBeans() {
                contextRunner
                        .withUserConfiguration(LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(communicator).enableLogging(logger);
                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(client).enableLogging(logger);
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLogging(null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verify(communicator).enableLogging(logger);
                            verify(client).enableLogging(logger);
                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }

        @Nested
        class WithMultipleLoggers {

            @Test
            void testWithNoSupportedBeans() {
                contextRunner
                        .withUserConfiguration(LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging(null);
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging(null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(connection, logger, additionalLogger);
                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging(null);

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(communicator, logger, additionalLogger);
                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging(null);

                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(client, logger, additionalLogger);
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class, LoggerProvider.class,
                                AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging(null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(connection, logger, additionalLogger);
                            verifyEnableLogging(communicator, logger, additionalLogger);
                            verifyEnableLogging(client, logger, additionalLogger);
                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }

        @Nested
        class WithSpecificLogger {

            @Test
            void testWithMissingBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("missing-logger", null))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                                    .extracting("beanName", "beanType").isEqualTo(Arrays.asList("missing-logger", null));
                        });
            }

            @Test
            void testWithTypeMismatch() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();

                            assertThatThrownBy(() -> endpoint.enableLogging("connection"))
                                    .isInstanceOf(BeanNotOfRequiredTypeException.class)
                                    .extracting("beanName", "requiredType", "actualType")
                                            .isEqualTo(Arrays.asList("connection", CommunicatorLogger.class, connection.getClass()));

                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithNoSupportedBeans() {
                contextRunner
                        .withUserConfiguration(LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging("logger");
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging("logger");

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging("logger");

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(communicator).enableLogging(logger);
                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging("logger");

                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(client).enableLogging(logger);
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class, LoggerProvider.class,
                                AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLogging("logger");

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verify(communicator).enableLogging(logger);
                            verify(client).enableLogging(logger);
                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class EnableLoggingOnBean {

        @Nested
        class WithNoLoggers {

            @Test
            void testWithMissingBean() {
                contextRunner
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("connection", null))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                                    .extracting("beanName", "beanType").isEqualTo(Arrays.asList("connection", null));
                        });
            }

            @Test
            void testWithTypeMismatch() {
                contextRunner
                        .withUserConfiguration(AdditionalBeanProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            Marshaller marshaller = context.getBean(AdditionalBeanProvider.class).marshaller();

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("marshaller", null))
                                    .isInstanceOf(BeanNotOfRequiredTypeException.class)
                                    .extracting("beanName", "requiredType", "actualType")
                                            .isEqualTo(Arrays.asList("marshaller", LoggingCapable.class, marshaller.getClass()));
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("connection", null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();

                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("communicator", null);

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("client", null);

                            Client client = context.getBean(ClientProvider.class).client();

                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("connection", null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();

                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }

        @Nested
        class WithOneLogger {

            @Test
            void testWithMissingBean() {
                contextRunner
                        .withUserConfiguration(LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("connection", null))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                                    .extracting("beanName", "beanType").isEqualTo(Arrays.asList("connection", null));
                        });
            }

            @Test
            void testWithTypeMismatch() {
                contextRunner
                        .withUserConfiguration(AdditionalBeanProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(Marshaller.class);
                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            Marshaller marshaller = context.getBean(AdditionalBeanProvider.class).marshaller();

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("marshaller", null))
                                    .isInstanceOf(BeanNotOfRequiredTypeException.class)
                                    .extracting("beanName", "requiredType", "actualType")
                                            .isEqualTo(Arrays.asList("marshaller", LoggingCapable.class, marshaller.getClass()));
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("connection", null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("communicator", null);

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(communicator).enableLogging(logger);
                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("client", null);

                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(client).enableLogging(logger);
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class, LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            endpoint.enableLoggingOnBean("connection", null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }

        @Nested
        class WithMultipleLoggers {

            @Test
            void testWithMissingBean() {
                contextRunner
                        .withUserConfiguration(LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("connection", null))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                                    .extracting("beanName", "beanType").isEqualTo(Arrays.asList("connection", null));
                        });
            }

            @Test
            void withTypeMismatch() {
                contextRunner
                        .withUserConfiguration(AdditionalBeanProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(Marshaller.class);
                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            Marshaller marshaller = context.getBean(AdditionalBeanProvider.class).marshaller();

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("marshaller", null))
                                    .isInstanceOf(BeanNotOfRequiredTypeException.class)
                                    .extracting("beanName", "requiredType", "actualType")
                                            .isEqualTo(Arrays.asList("marshaller", LoggingCapable.class, marshaller.getClass()));
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("connection", null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(connection, logger, additionalLogger);
                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("communicator", null);

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(communicator, logger, additionalLogger);
                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("client", null);

                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(client, logger, additionalLogger);
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class, LoggerProvider.class,
                                AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("connection", null);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();
                            CommunicatorLogger additionalLogger = context.getBean(AdditionalLoggerProvider.class).additionalLogger();

                            verifyEnableLogging(connection, logger, additionalLogger);
                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }

        @Nested
        class WithSpecificLogger {

            @Test
            void testWithMissingBean() {
                contextRunner
                        .withUserConfiguration(LoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(CommunicatorLogger.class);

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("connection", "logger"))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                                    .extracting("beanName", "beanType").isEqualTo(Arrays.asList("connection", null));
                        });
            }

            @Test
            void testWithTypeMismatch() {
                contextRunner
                        .withUserConfiguration(AdditionalBeanProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(Marshaller.class);
                            assertThat(context).doesNotHaveBean(LoggingCapable.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            Marshaller marshaller = context.getBean(AdditionalBeanProvider.class).marshaller();

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("marshaller", "connection"))
                                    .isInstanceOf(BeanNotOfRequiredTypeException.class)
                                    .extracting("beanName", "requiredType", "actualType")
                                            .isEqualTo(Arrays.asList("marshaller", LoggingCapable.class, marshaller.getClass()));
                        });
            }

            @Test
            void testWithMissingLoggerBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).doesNotHaveBean(CommunicatorLogger.class);

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("connection", "logger"))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                                    .extracting("beanName", "beanType").isEqualTo(Arrays.asList("logger", null));
                        });
            }

            @Test
            void testWithLoggerTypeMismatch() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            Connection connection = context.getBean(ConnectionProvider.class).connection();

                            assertThatThrownBy(() -> endpoint.enableLoggingOnBean("connection", "connection"))
                                    .isInstanceOf(BeanNotOfRequiredTypeException.class)
                                    .extracting("beanName", "requiredType", "actualType")
                                            .isEqualTo(Arrays.asList("connection", CommunicatorLogger.class, connection.getClass()));

                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithConnectionBean() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("connection", "logger");

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verifyNoMoreInteractions(connection);
                        });
            }

            @Test
            void testWithCommunicatorBean() {
                contextRunner
                        .withUserConfiguration(CommunicatorProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("communicator", "logger");

                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(communicator).enableLogging(logger);
                            verifyNoMoreInteractions(communicator);
                        });
            }

            @Test
            void testWithClientBean() {
                contextRunner
                        .withUserConfiguration(ClientProvider.class, LoggerProvider.class, AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            assertThat(context).hasSingleBean(LoggingCapable.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("client", "logger");

                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(client).enableLogging(logger);
                            verifyNoMoreInteractions(client);
                        });
            }

            @Test
            void testWithBeansOfAllSupportedTypes() {
                contextRunner
                        .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class, LoggerProvider.class,
                                AdditionalLoggerProvider.class)
                        .run(context -> {
                            LoggingEndpoint endpoint = new LoggingEndpoint(context);

                            // three LoggingCapable beans - connection, communicator and client
                            assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                            assertThat(context).hasSingleBean(Connection.class);
                            assertThat(context).hasSingleBean(Communicator.class);
                            assertThat(context).hasSingleBean(Client.class);
                            assertThat(context).getBeans(CommunicatorLogger.class).hasSize(2);

                            endpoint.enableLoggingOnBean("connection", "logger");

                            Connection connection = context.getBean(ConnectionProvider.class).connection();
                            Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                            Client client = context.getBean(ClientProvider.class).client();
                            CommunicatorLogger logger = context.getBean(LoggerProvider.class).logger();

                            verify(connection).enableLogging(logger);
                            verifyNoMoreInteractions(connection, communicator, client);
                        });
            }
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class DisableLogging {

        @Test
        void testWithNoSupportedBeans() {
            contextRunner
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).doesNotHaveBean(LoggingCapable.class);

                        endpoint.disableLogging();
                    });
        }

        @Test
        void testWithConnection() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).hasSingleBean(LoggingCapable.class);
                        assertThat(context).hasSingleBean(Connection.class);

                        endpoint.disableLogging();

                        Connection connection = context.getBean(ConnectionProvider.class).connection();

                        verify(connection).disableLogging();
                        verifyNoMoreInteractions(connection);
                    });
        }

        @Test
        void testWithCommunicator() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).hasSingleBean(LoggingCapable.class);
                        assertThat(context).hasSingleBean(Communicator.class);

                        endpoint.disableLogging();

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).disableLogging();
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClient() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).hasSingleBean(LoggingCapable.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.disableLogging();

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).disableLogging();
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        // three LoggingCapable beans - connection, communicator and client
                        assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.disableLogging();

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(connection).disableLogging();
                        verify(communicator).disableLogging();
                        verify(client).disableLogging();
                        verifyNoMoreInteractions(connection, communicator, client);
                    });
        }
    }

    @Nested
    @SuppressWarnings("resource")
    class DisableLoggingOnBean {

        @Test
        void testWithMissingBean() {
            contextRunner
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).doesNotHaveBean(LoggingCapable.class);

                        assertThatThrownBy(() -> endpoint.disableLoggingOnBean("connection"))
                                .isInstanceOf(NoSuchBeanDefinitionException.class)
                                .extracting("beanName", "beanType").isEqualTo(Arrays.asList("connection", null));
                    });
        }

        @Test
        void testWithTypeMismatch() {
            contextRunner
                    .withUserConfiguration(AdditionalBeanProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).doesNotHaveBean(LoggingCapable.class);

                        Marshaller marshaller = context.getBean(AdditionalBeanProvider.class).marshaller();

                        assertThatThrownBy(() -> endpoint.disableLoggingOnBean("marshaller"))
                                .isInstanceOf(BeanNotOfRequiredTypeException.class)
                                .extracting("beanName", "requiredType", "actualType")
                                        .isEqualTo(Arrays.asList("marshaller", LoggingCapable.class, marshaller.getClass()));
                    });
        }

        @Test
        void testWithConnection() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).hasSingleBean(LoggingCapable.class);
                        assertThat(context).hasSingleBean(Connection.class);

                        endpoint.disableLoggingOnBean("connection");

                        Connection connection = context.getBean(ConnectionProvider.class).connection();

                        verify(connection).disableLogging();
                        verifyNoMoreInteractions(connection);
                    });
        }

        @Test
        void testWithCommunicator() {
            contextRunner
                    .withUserConfiguration(CommunicatorProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).hasSingleBean(LoggingCapable.class);
                        assertThat(context).hasSingleBean(Communicator.class);

                        endpoint.disableLoggingOnBean("communicator");

                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();

                        verify(communicator).disableLogging();
                        verifyNoMoreInteractions(communicator);
                    });
        }

        @Test
        void testWithClient() {
            contextRunner
                    .withUserConfiguration(ClientProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        assertThat(context).hasSingleBean(LoggingCapable.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.disableLoggingOnBean("client");

                        Client client = context.getBean(ClientProvider.class).client();

                        verify(client).disableLogging();
                        verifyNoMoreInteractions(client);
                    });
        }

        @Test
        void testWithBeansOfAllSupportedTypes() {
            contextRunner
                    .withUserConfiguration(ConnectionProvider.class, CommunicatorProvider.class, ClientProvider.class)
                    .run(context -> {
                        LoggingEndpoint endpoint = new LoggingEndpoint(context);

                        // three LoggingCapable beans - connection, communicator and client
                        assertThat(context).getBeans(LoggingCapable.class).hasSize(3);
                        assertThat(context).hasSingleBean(Connection.class);
                        assertThat(context).hasSingleBean(Communicator.class);
                        assertThat(context).hasSingleBean(Client.class);

                        endpoint.disableLoggingOnBean("connection");

                        Connection connection = context.getBean(ConnectionProvider.class).connection();
                        Communicator communicator = context.getBean(CommunicatorProvider.class).communicator();
                        Client client = context.getBean(ClientProvider.class).client();

                        verify(connection).disableLogging();
                        verifyNoMoreInteractions(connection, communicator, client);
                    });
        }
    }

    @Test
    void testCompoundCommunicatorLogger() {
        CommunicatorLogger logger1 = mock(CommunicatorLogger.class);
        CommunicatorLogger logger2 = mock(CommunicatorLogger.class);
        CommunicatorLogger logger3 = mock(CommunicatorLogger.class);
        CompoundCommunicatorLogger compoundLogger = new CompoundCommunicatorLogger(Arrays.asList(logger1, logger2, logger3));

        Throwable error = new AssertionError("assertion failed");

        compoundLogger.log("message without exception");
        compoundLogger.log("message with exception", error);

        verify(logger1).log("message without exception");
        verify(logger2).log("message without exception");
        verify(logger3).log("message without exception");
        verify(logger1).log("message with exception", error);
        verify(logger2).log("message with exception", error);
        verify(logger3).log("message with exception", error);
        verifyNoMoreInteractions(logger1, logger2, logger3);
    }

    private void verifyEnableLogging(LoggingCapable loggingCapable, CommunicatorLogger... loggers) {
        ArgumentCaptor<CommunicatorLogger> loggerCaptor = ArgumentCaptor.forClass(CommunicatorLogger.class);
        verify(loggingCapable).enableLogging(loggerCaptor.capture());

        CommunicatorLogger logger = loggerCaptor.getValue();
        assertThat(logger).isInstanceOf(CompoundCommunicatorLogger.class);
        CompoundCommunicatorLogger compoundLogger = (CompoundCommunicatorLogger) logger;
        assertThat(compoundLogger.loggers).containsExactlyInAnyOrder(loggers);
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

    @Configuration
    static class LoggerProvider {

        @Bean
        CommunicatorLogger logger() {
            return mock(CommunicatorLogger.class);
        }
    }

    @Configuration
    static class AdditionalLoggerProvider {

        @Bean
        CommunicatorLogger additionalLogger() {
            return mock(CommunicatorLogger.class);
        }
    }

    @Configuration
    static class AdditionalBeanProvider {

        @Bean
        Marshaller marshaller() {
            return DefaultMarshaller.INSTANCE;
        }
    }
}
