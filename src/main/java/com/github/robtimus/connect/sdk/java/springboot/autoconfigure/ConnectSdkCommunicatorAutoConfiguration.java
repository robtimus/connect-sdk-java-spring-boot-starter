/*
 * ConnectSdkCommunicatorAutoConfiguration.java
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
import com.worldline.connect.sdk.java.Communicator;
import com.worldline.connect.sdk.java.authentication.Authenticator;
import com.worldline.connect.sdk.java.communication.Connection;
import com.worldline.connect.sdk.java.communication.MetadataProvider;
import com.worldline.connect.sdk.java.json.Marshaller;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * <a href="https://github.com/Worldline-Global-Collectconnect-sdk-java/">connect-sdk-java</a>'s {@link Communicator}.
 *
 * @author Rob Spoor
 */
@Configuration
@AutoConfigureAfter({ ConnectSdkConnectionAutoConfiguration.class, ConnectSdkAuthenticatorAutoConfiguration.class,
        ConnectSdkMetadataProviderAutoConfiguration.class, ConnectSdkMarshallerAutoConfiguration.class })
@ConditionalOnMissingBean(Communicator.class)
@ConditionalOnBean({ Connection.class, Authenticator.class, MetadataProvider.class, Marshaller.class })
@ConditionalOnProperty(name = "connect.api.endpoint.host")
@EnableConfigurationProperties(ConnectSdkProperties.class)
@SuppressWarnings({ "nls", "javadoc" })
public class ConnectSdkCommunicatorAutoConfiguration {

    private final ConnectSdkProperties properties;

    @Autowired
    public ConnectSdkCommunicatorAutoConfiguration(ConnectSdkProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    // don't close the communicator when the bean is destroyed, let the connection be closed directly
    @Bean(destroyMethod = "")
    public Communicator connectSdkCommunicator(Connection connection, Authenticator authenticator, MetadataProvider metadataProvider,
            Marshaller marshaller) {

        URI apiEndpoint = getApiEndpoint();
        return new Communicator(apiEndpoint, connection, authenticator, metadataProvider, marshaller);
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
