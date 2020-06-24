/*
 * FullAutoConfigurationTest.java
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
import java.net.URI;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectSdkHealthIndicator;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint;
import com.github.robtimus.connect.sdk.java.springboot.actuator.LoggingEndpoint;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.Marshaller;
import com.ingenico.connect.gateway.sdk.java.MetaDataProvider;
import com.ingenico.connect.gateway.sdk.java.RequestHeader;
import com.ingenico.connect.gateway.sdk.java.Session;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultAuthenticator;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultConnection;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultMarshaller;
import com.ingenico.connect.gateway.sdk.java.logging.CommunicatorLogger;
import com.ingenico.connect.gateway.sdk.java.logging.SysOutCommunicatorLogger;
import com.ingenico.connect.gateway.sdk.java.merchant.MerchantClient;

class FullAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConnectSdkAuthenticatorAutoConfiguration.class, ConnectSdkClientAutoConfiguration.class,
                    ConnectSdkConnectionsEndpointAutoConfiguration.class, ConnectSdkCommunicatorAutoConfiguration.class,
                    ConnectSdkCommunicatorLoggerAutoConfiguration.class, ConnectSdkConnectionAutoConfiguration.class,
                    ConnectSdkHealthIndicatorAutoConfiguration.class, ConnectSdkLoggingEndpointAutoConfiguration.class,
                    ConnectSdkMarshallerAutoConfiguration.class, ConnectSdkMerchantClientAutoConfiguration.class,
                    ConnectSdkMetaDataProviderAutoConfiguration.class, ConnectSdkSessionAutoConfiguration.class));

    @Test
    void testWithNoProperties() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(Authenticator.class);
            assertThat(context).doesNotHaveBean(Client.class);
            assertThat(context).doesNotHaveBean(ConnectionsEndpoint.class);
            assertThat(context).doesNotHaveBean(Communicator.class);
            assertThat(context).hasSingleBean(CommunicatorLogger.class);
            assertThat(context).hasSingleBean(Connection.class);
            assertThat(context).doesNotHaveBean(ConnectSdkHealthIndicator.class);
            assertThat(context).doesNotHaveBean(LoggingEndpoint.class);
            assertThat(context).hasSingleBean(Marshaller.class);
            assertThat(context).doesNotHaveBean(MerchantClient.class);
            assertThat(context).doesNotHaveBean(MetaDataProvider.class);
            assertThat(context).doesNotHaveBean(Session.class);
        });
    }

    @Test
    void testWithMinimalProperties() {
        contextRunner
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com", "connect.api.integrator=Integrator",
                        "connect.api.api-key-id=apiKeyId", "connect.api.secret-api-key=secretApiKey")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).doesNotHaveBean(ConnectionsEndpoint.class);
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).doesNotHaveBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).doesNotHaveBean(LoggingEndpoint.class);
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).doesNotHaveBean(MerchantClient.class);
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).hasSingleBean(Session.class);
                });
    }

    @Test
    void testWithMinimalPropertiesForHealth() {
        contextRunner
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com", "connect.api.integrator=Integrator",
                        "connect.api.api-key-id=apiKeyId", "connect.api.secret-api-key=secretApiKey", "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).doesNotHaveBean(ConnectionsEndpoint.class);
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).doesNotHaveBean(LoggingEndpoint.class);
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).hasSingleBean(MerchantClient.class);
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).hasSingleBean(Session.class);
                });
    }

    @Test
    void testWithMinimalPropertiesForEndpoints() {
        contextRunner
                .withPropertyValues("management.endpoint.connectSdkConnections.enabled=true",
                        "management.endpoint.connectSdkLogging.enabled=true", "spring.jmx.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Authenticator.class);
                    assertThat(context).doesNotHaveBean(Client.class);
                    assertThat(context).hasSingleBean(ConnectionsEndpoint.class);
                    assertThat(context).doesNotHaveBean(Communicator.class);
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).doesNotHaveBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).hasSingleBean(LoggingEndpoint.class);
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).doesNotHaveBean(MerchantClient.class);
                    assertThat(context).doesNotHaveBean(MetaDataProvider.class);
                    assertThat(context).doesNotHaveBean(Session.class);
                });
    }

    @Test
    void testWithAllPropertiesButShoppingCartExtensionExtensionId() {
        contextRunner
                .withPropertyValues("connect.api.merchant-id=merchantId", "connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.endpoint.scheme=https", "connect.api.endpoint.port=443", "connect.api.connect-timeout=1000",
                        "connect.api.socket-timeout=10000", "connect.api.max-connections=10", "connect.api.authorization-type=V1HMAC",
                        "connect.api.api-key-id=apiKeyId", "connect.api.secret-api-key=secretApiKey", "connect.api.proxy.uri=http://localhost",
                        "connect.api.proxy.username=user", "connect.api.proxy.password=pass", "connect.api.https.protocols=TLSv1.2",
                        "connect.api.integrator=Integrator", "connect.api.shopping-cart-extension.creator=Creator",
                        "connect.api.shopping-cart-extension.name=name", "connect.api.shopping-cart-extension.version=version",
                        "management.endpoint.connectSdkConnections.enabled=true", "management.endpoint.connectSdkLogging.enabled=true",
                        "spring.jmx.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).hasSingleBean(ConnectionsEndpoint.class);
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).hasSingleBean(LoggingEndpoint.class);
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).hasSingleBean(MerchantClient.class);
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).hasSingleBean(Session.class);
                });
    }

    @Test
    void testWithAllProperties() {
        contextRunner
                .withPropertyValues("connect.api.merchant-id=merchantId", "connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.endpoint.scheme=https", "connect.api.endpoint.port=443", "connect.api.connect-timeout=1000",
                        "connect.api.socket-timeout=10000", "connect.api.max-connections=10", "connect.api.authorization-type=V1HMAC",
                        "connect.api.api-key-id=apiKeyId", "connect.api.secret-api-key=secretApiKey", "connect.api.proxy.uri=http://localhost",
                        "connect.api.proxy.username=user", "connect.api.proxy.password=pass", "connect.api.https.protocols=TLSv1.2",
                        "connect.api.integrator=Integrator", "connect.api.shopping-cart-extension.creator=Creator",
                        "connect.api.shopping-cart-extension.name=name", "connect.api.shopping-cart-extension.version=version",
                        "connect.api.shopping-cart-extension.extension-id=extensionId",
                        "management.endpoint.connectSdkConnections.enabled=true", "management.endpoint.connectSdkLogging.enabled=true",
                        "spring.jmx.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).hasSingleBean(ConnectionsEndpoint.class);
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).hasSingleBean(LoggingEndpoint.class);
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).hasSingleBean(MerchantClient.class);
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).hasSingleBean(Session.class);
                });
    }

    @Test
    void testWithAllPropertiesAndAdditionalBeans() {
        contextRunner
                .withUserConfiguration(AdditionalBeanProvider.class)
                .withPropertyValues("connect.api.merchant-id=merchantId", "connect.api.endpoint.host=eu.sandbox.api-ingenico.com",
                        "connect.api.endpoint.scheme=https", "connect.api.endpoint.port=443", "connect.api.connect-timeout=1000",
                        "connect.api.socket-timeout=10000", "connect.api.max-connections=10", "connect.api.authorization-type=V1HMAC",
                        "connect.api.api-key-id=apiKeyId", "connect.api.secret-api-key=secretApiKey", "connect.api.proxy.uri=http://localhost",
                        "connect.api.proxy.username=user", "connect.api.proxy.password=pass", "connect.api.https.protocols=TLSv1.2",
                        "connect.api.integrator=Integrator", "connect.api.shopping-cart-extension.creator=Creator",
                        "connect.api.shopping-cart-extension.name=name", "connect.api.shopping-cart-extension.version=version",
                        "connect.api.shopping-cart-extension.extension-id=extensionId",
                        "management.endpoint.connectSdkConnections.enabled=true", "management.endpoint.connectSdkLogging.enabled=true",
                        "spring.jmx.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).hasSingleBean(ConnectionsEndpoint.class);
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).hasSingleBean(LoggingEndpoint.class);
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).hasSingleBean(MerchantClient.class);
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).hasSingleBean(Session.class);
                });
    }

    @Test
    void testWithInvalidEndpointHost() {
        contextRunner
                .withPropertyValues("connect.api.endpoint.host=https://eu.sandbox.api-ingenico.com", "connect.api.integrator=Integrator",
                        "connect.api.api-key-id=apiKeyId", "connect.api.secret-api-key=secretApiKey")
                .run(context -> {
                    assertThat(context).hasFailed();
                });
    }

    @Test
    void testWithProvidedBeans() {
        testWithProvidedBean(AuthenticatorProvider.class, Authenticator.class, c -> c.getBean(AuthenticatorProvider.class).authenticator());
        testWithProvidedBean(ClientProvider.class, Client.class, c -> c.getBean(ClientProvider.class).client());
        testWithProvidedBean(CommunicatorProvider.class, Communicator.class, c -> c.getBean(CommunicatorProvider.class).communicator());
        testWithProvidedBean(CommunicatorLoggerProvider.class, CommunicatorLogger.class,
                c -> c.getBean(CommunicatorLoggerProvider.class).communicatorLogger());
        testWithProvidedBean(ConnectionProvider.class, Connection.class, c -> c.getBean(ConnectionProvider.class).connection());
        testWithProvidedBean(HealthIndicatorProvider.class, ConnectSdkHealthIndicator.class,
                c -> c.getBean(HealthIndicatorProvider.class).healthIndicator());
        testWithProvidedBean(MarshallerProvider.class, Marshaller.class, c -> c.getBean(MarshallerProvider.class).marshaller());
        testWithProvidedBean(MerchantClientProvider.class, MerchantClient.class, c -> c.getBean(MerchantClientProvider.class).merchantClient());
        testWithProvidedBean(MetaDataProviderProvider.class, MetaDataProvider.class,
                c -> c.getBean(MetaDataProviderProvider.class).metaDataProvider());
        testWithProvidedBean(SessionProvider.class, Session.class, c -> c.getBean(SessionProvider.class).session());
    }

    private <P, B> void testWithProvidedBean(Class<P> providerClass, Class<B> beanClass, Function<ApplicationContext, B> beanGetter) {
        contextRunner
                .withUserConfiguration(providerClass)
                .withPropertyValues("connect.api.endpoint.host=eu.sandbox.api-ingenico.com", "connect.api.integrator=Integrator",
                        "connect.api.api-key-id=apiKeyId", "connect.api.secret-api-key=secretApiKey", "connect.api.merchant-id=merchantId")
                .run(context -> {
                    assertThat(context).hasSingleBean(Authenticator.class);
                    assertThat(context).hasSingleBean(Client.class);
                    assertThat(context).hasSingleBean(Communicator.class);
                    assertThat(context).hasSingleBean(CommunicatorLogger.class);
                    assertThat(context).hasSingleBean(Connection.class);
                    assertThat(context).hasSingleBean(ConnectSdkHealthIndicator.class);
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).hasSingleBean(MerchantClient.class);
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).hasSingleBean(Session.class);

                    assertThat(context).getBean(beanClass).isSameAs(beanGetter.apply(context));
                });
    }

    @Configuration
    static class AuthenticatorProvider {

        @Bean
        Authenticator authenticator() {
            return new DefaultAuthenticator(AuthorizationType.V1HMAC, "apiKeyId", "secretApiKey");
        }
    }

    @Configuration
    static class ClientProvider {

        @Autowired
        private Communicator communicator;

        @Bean
        Client client() {
            return new Client(communicator);
        }
    }

    @Configuration
    static class CommunicatorProvider {

        @Autowired
        private Session session;
        @Autowired
        private Marshaller marshaller;

        @Bean
        Communicator communicator() {
            return new Communicator(session, marshaller);
        }
    }

    @Configuration
    static class CommunicatorLoggerProvider {

        @Bean
        CommunicatorLogger communicatorLogger() {
            return SysOutCommunicatorLogger.INSTANCE;
        }
    }

    @Configuration
    static class ConnectionProvider {

        @Bean
        Connection connection() {
            return new DefaultConnection(1000, 30000);
        }
    }

    @Configuration
    static class HealthIndicatorProvider {

        @Autowired
        private MerchantClient merchantClient;

        @Bean
        ConnectSdkHealthIndicator healthIndicator() {
            return new ConnectSdkHealthIndicator(merchantClient, 1);
        }
    }

    @Configuration
    static class MarshallerProvider {

        @Bean
        Marshaller marshaller() {
            return DefaultMarshaller.INSTANCE;
        }
    }

    @Configuration
    static class MerchantClientProvider {

        @Autowired
        private Client client;

        @Bean
        MerchantClient merchantClient() {
            return client.merchant("merchantId");
        }
    }

    @Configuration
    static class MetaDataProviderProvider {

        @Bean
        MetaDataProvider metaDataProvider() {
            return new MetaDataProvider("Integrator");
        }
    }

    @Configuration
    static class SessionProvider {

        @Autowired
        private Connection connection;
        @Autowired
        private Authenticator authenticator;
        @Autowired
        private MetaDataProvider metaDataProvider;

        @Bean
        Session session() {
            return new Session(URI.create("https://eu.sandbox.api-ingenico.com"), connection, authenticator, metaDataProvider);
        }
    }

    @Configuration
    static class AdditionalBeanProvider {

        @Bean
        MetaDataProviderBuilderCustomizer metaDataProviderBuilderCustomizer() {
            return builder -> builder.withAdditionalRequestHeader(new RequestHeader("custom-name", "custom-value"));
        }
    }
}
