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

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import com.github.robtimus.connect.sdk.java.springboot.ConfigurableV1HMACAuthenticator;

/**
 * An {@link Endpoint} for managing API keys in <a href="https://github.com/Worldline-Global-Collect/connect-sdk-java/">connect-sdk-java</a>.
 *
 * @author Rob Spoor
 * @since 3.8
 */
@Endpoint(id = "connectSdkApiKey", defaultAccess = Access.NONE)
@SuppressWarnings("javadoc")
public class ApiKeyEndpoint {

    private final ConfigurableV1HMACAuthenticator authenticator;

    public ApiKeyEndpoint(ConfigurableV1HMACAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Sets the current API key to use.
     *
     * @param apiKeyId The new API key id.
     * @param secretApiKey The new secret API key.
     */
    @WriteOperation
    public void setApiKey(String apiKeyId, String secretApiKey) {
        authenticator.setApiKey(apiKeyId, secretApiKey);
    }
}
