/*
 * ConfigurableV1HMACAuthenticator.java
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

package com.github.robtimus.connect.sdk.java.springboot;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.worldline.connect.sdk.java.authentication.Authenticator;
import com.worldline.connect.sdk.java.authentication.V1HMACAuthenticator;
import com.worldline.connect.sdk.java.communication.RequestHeader;

/**
 * An {@link Authenticator} implementation that allows the API key id and secret to be replaced, something that {@link V1HMACAuthenticator} does not
 * support.
 * This class is thread-safe.
 *
 * @author Rob Spoor
 * @since 3.8
 */
public class ConfigurableV1HMACAuthenticator implements Authenticator {

    private final AtomicReference<Authenticator> delegate;

    /**
     * Creates a new configurable authenticator.
     *
     * @param apiKeyId The initial API key id.
     * @param secretApiKey The initial secret API key.
     */
    public ConfigurableV1HMACAuthenticator(String apiKeyId, String secretApiKey) {
        delegate = new AtomicReference<>(new V1HMACAuthenticator(apiKeyId, secretApiKey));
    }

    /**
     * Sets the new API key to use.
     *
     * @param apiKeyId The new API key id.
     * @param secretApiKey The new secret API key.
     */
    public void setApiKey(String apiKeyId, String secretApiKey) {
        delegate.set(new V1HMACAuthenticator(apiKeyId, secretApiKey));
    }

    @Override
    public String getAuthorization(String httpMethod, URI resourceUri, List<RequestHeader> requestHeaders) {
        return delegate.get().getAuthorization(httpMethod, resourceUri, requestHeaders);
    }
}
