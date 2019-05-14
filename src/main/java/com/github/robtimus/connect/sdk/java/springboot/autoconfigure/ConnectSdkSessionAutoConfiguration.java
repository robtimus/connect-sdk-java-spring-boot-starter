/*
 * ConnectSdkSessionAutoConfiguration.java
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.Connection;
import com.ingenico.connect.gateway.sdk.java.MetaDataProvider;
import com.ingenico.connect.gateway.sdk.java.Session;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>'s
 * {@link Session}.
 *
 * @author Rob Spoor
 */
@Configuration
@AutoConfigureAfter({ ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
        ConnectSdkMetaDataProviderAutoConfiguration.class })
@ConditionalOnMissingBean(Session.class)
@ConditionalOnBean({ Connection.class, Authenticator.class, MetaDataProvider.class })
@ConditionalOnProperty(name = "connect.api.endpoint.host")
@EnableConfigurationProperties(ConnectSdkProperties.class)
public class ConnectSdkSessionAutoConfiguration {

    private final ConnectSdkProperties properties;

    @Autowired
    public ConnectSdkSessionAutoConfiguration(ConnectSdkProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Bean
    public Session connectSdkSession(Connection connection, Authenticator authenticator, MetaDataProvider metaDataProvider) {
        URI apiEndpoint = getApiEndpoint();
        return new Session(apiEndpoint, connection, authenticator, metaDataProvider);
    }

    private URI getApiEndpoint() {
        ConnectSdkProperties.Endpoint endpoint = properties.getEndpoint();
        try {
            String scheme = endpoint.getScheme();
            String host = endpoint.getHost();
            int port = endpoint.getPort();
            return new URI(scheme, null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to construct API endpoint URI", e);
        }
    }
}
