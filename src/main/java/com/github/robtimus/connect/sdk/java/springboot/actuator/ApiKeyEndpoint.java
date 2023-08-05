/*
 * ApiKeyEndpoint.java
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

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.lang.Nullable;
import com.github.robtimus.connect.sdk.java.springboot.ReconfigurableAuthenticator;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;

/**
 * An {@link Endpoint} for managing API keys in <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>.
 *
 * @author Rob Spoor
 * @since 3.8
 */
@Endpoint(id = "connectSdkApiKey", enableByDefault = false)
@SuppressWarnings("javadoc")
public class ApiKeyEndpoint {

    private final ReconfigurableAuthenticator authenticator;

    public ApiKeyEndpoint(ReconfigurableAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Sets the current API key to use.
     *
     * @param apiKeyId The new API key id.
     * @param secretApiKey The new secret API key.
     * @param authorizationType The new authorization type. If not given, the current authorization type is used.
     */
    @WriteOperation
    public void setApiKey(String apiKeyId, String secretApiKey, @Nullable AuthorizationType authorizationType) {
        authenticator.setApiKey(getAuthorizationType(authorizationType), apiKeyId, secretApiKey);
    }

    private AuthorizationType getAuthorizationType(AuthorizationType authorizationType) {
        return authorizationType != null
                ? authorizationType
                : AuthorizationType.V1HMAC;
    }
}
