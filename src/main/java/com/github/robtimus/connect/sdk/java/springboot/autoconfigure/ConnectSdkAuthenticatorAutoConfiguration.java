/*
 * ConnectSdkAuthenticatorAutoConfiguration.java
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

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.ReconfigurableAuthenticator;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ApiKeyEndpoint;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>'s
 * {@link Authenticator}.
 *
 * @author Rob Spoor
 */
@Configuration
@ConditionalOnMissingBean(Authenticator.class)
@ConditionalOnProperty(prefix = "connect.api", name = { "api-key-id", "secret-api-key" })
@EnableConfigurationProperties(ConnectSdkProperties.class)
@SuppressWarnings("javadoc")
public class ConnectSdkAuthenticatorAutoConfiguration {

    private final ConnectSdkProperties properties;

    @Autowired
    public ConnectSdkAuthenticatorAutoConfiguration(ConnectSdkProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Bean
    public Authenticator connectSdkAuthenticator() {
        AuthorizationType authorizationType = properties.getAuthorizationType();
        String apiKeyId = properties.getApiKeyId();
        String secretApiKey = properties.getSecretApiKey();
        return new ReconfigurableAuthenticator(authorizationType, apiKeyId, secretApiKey);
    }

    @Bean
    @ConditionalOnClass(Endpoint.class)
    @ConditionalOnAvailableEndpoint(endpoint = ApiKeyEndpoint.class)
    public ApiKeyEndpoint connectSdkApiKeyEndpoint(ReconfigurableAuthenticator authenticator) {
        return new ApiKeyEndpoint(authenticator);
    }
}
