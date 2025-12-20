/*
 * ConnectSdkConnectionAutoConfiguration.java
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.worldline.connect.sdk.java.CommunicatorConfiguration;
import com.worldline.connect.sdk.java.ProxyConfiguration;
import com.worldline.connect.sdk.java.communication.Connection;
import com.worldline.connect.sdk.java.communication.DefaultConnection;
import com.worldline.connect.sdk.java.communication.DefaultConnectionBuilder;
import com.worldline.connect.sdk.java.communication.PooledConnection;
import com.worldline.connect.sdk.java.logging.BodyObfuscator;
import com.worldline.connect.sdk.java.logging.HeaderObfuscator;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * <a href="https://github.com/Worldline-Global-Collect/connect-sdk-java/">connect-sdk-java</a>'s {@link PooledConnection}.
 *
 * @author Rob Spoor
 */
@Configuration
@ConditionalOnMissingBean(Connection.class)
@EnableConfigurationProperties(ConnectSdkProperties.class)
@SuppressWarnings({ "nls", "javadoc" })
public class ConnectSdkConnectionAutoConfiguration {

    private final ConnectSdkProperties properties;

    @Autowired
    public ConnectSdkConnectionAutoConfiguration(ConnectSdkProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Bean(destroyMethod = "close")
    public PooledConnection connectSdkConnection(@Nullable BodyObfuscator bodyObfuscator, @Nullable HeaderObfuscator headerObfuscator) {
        int connectTimeout = properties.getConnectTimeout();
        int socketTimeout = properties.getSocketTimeout();
        int maxConnections = properties.getMaxConnections();
        boolean connectionReuse = properties.isConnectionReuse();

        ProxyConfiguration proxyConfiguration = getProxyConfiguration();
        Set<String> httpsProtocols = getHttpsProtocols();

        DefaultConnection connection = new DefaultConnectionBuilder(connectTimeout, socketTimeout)
                .withMaxConnections(maxConnections)
                .withConnectionReuse(connectionReuse)
                .withProxyConfiguration(proxyConfiguration)
                .withHttpsProtocols(httpsProtocols)
                .build();
        if (bodyObfuscator != null) {
            connection.setBodyObfuscator(bodyObfuscator);
        }
        if (headerObfuscator != null) {
            connection.setHeaderObfuscator(headerObfuscator);
        }
        return connection;
    }

    private ProxyConfiguration getProxyConfiguration() {
        ConnectSdkProperties.Proxy proxy = properties.getProxy();
        if (proxy != null) {
            String uri = proxy.getUri();
            if (uri != null) {
                String username = proxy.getUsername();
                String password = proxy.getPassword();
                return new ProxyConfiguration(URI.create(uri), username, password);
            }
        }
        return null;
    }

    private Set<String> getHttpsProtocols() {
        ConnectSdkProperties.HTTPS https = properties.getHttps();
        if (https != null) {
            List<String> protocols = https.getProtocols();
            if (protocols != null) {
                return new LinkedHashSet<>(protocols);
            }
        }
        return CommunicatorConfiguration.DEFAULT_HTTPS_PROTOCOLS;
    }

    @Service
    @ConditionalOnProperty(name = "connect.api.close-idle-connections.enabled", havingValue = "true", matchIfMissing = true)
    @EnableScheduling
    static class ConnectionManager {

        static final String IDLE_TIME = "${connect.api.close-idle-connections.idle-time:20000}";
        private static final String INTERVAL = "${connect.api.close-idle-connections.interval:10000}";

        private final PooledConnection connection;
        private final long idleTime;

        ConnectionManager(PooledConnection connection, @Value(IDLE_TIME) long idleTime) {
            this.connection = Objects.requireNonNull(connection);
            this.idleTime = idleTime;
        }

        @Scheduled(fixedDelayString = INTERVAL, initialDelayString = INTERVAL)
        public void closeIdleAndExpiredConnections() {
            connection.closeIdleConnections(idleTime, TimeUnit.MILLISECONDS);
            connection.closeExpiredConnections();
        }
    }
}
