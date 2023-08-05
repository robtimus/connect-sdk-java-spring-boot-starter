/*
 * ApiKeyEndpointTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.connect.sdk.java.springboot.ReconfigurableAuthenticator;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.MetaDataProvider;
import com.ingenico.connect.gateway.sdk.java.RequestHeader;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultAuthenticator;

@SuppressWarnings("nls")
class ApiKeyEndpointTest {

    @Nested
    class SetApiKey {

        @Test
        void testWithoutAuthorizationType() {
            String apiKeyId = UUID.randomUUID().toString();
            String secretApiKey = UUID.randomUUID().toString();

            String method = "GET";
            URI uri = URI.create("http://localhost/v1/test/services/testconnection");
            List<RequestHeader> headers = getRequestHeaders();

            ReconfigurableAuthenticator reconfigurableAuthenticator = new ReconfigurableAuthenticator(AuthorizationType.V1HMAC, "x", "x");

            ApiKeyEndpoint endpoint = new ApiKeyEndpoint(reconfigurableAuthenticator);
            endpoint.setApiKey(apiKeyId, secretApiKey, null);
            reconfigurableAuthenticator.setApiKey(AuthorizationType.V1HMAC, apiKeyId, secretApiKey);

            Authenticator defaultAuthenticator = new DefaultAuthenticator(AuthorizationType.V1HMAC, apiKeyId, secretApiKey);

            assertEquals(defaultAuthenticator.createSimpleAuthenticationSignature(method, uri, headers),
                    reconfigurableAuthenticator.createSimpleAuthenticationSignature(method, uri, headers));
        }

        @Test
        void testWithAuthorizationType() {
            String apiKeyId = UUID.randomUUID().toString();
            String secretApiKey = UUID.randomUUID().toString();

            String method = "GET";
            URI uri = URI.create("http://localhost/v1/test/services/testconnection");
            List<RequestHeader> headers = getRequestHeaders();

            ReconfigurableAuthenticator reconfigurableAuthenticator = new ReconfigurableAuthenticator(AuthorizationType.V1HMAC, "x", "x");

            ApiKeyEndpoint endpoint = new ApiKeyEndpoint(reconfigurableAuthenticator);
            endpoint.setApiKey(apiKeyId, secretApiKey, AuthorizationType.V1HMAC);

            Authenticator defaultAuthenticator = new DefaultAuthenticator(AuthorizationType.V1HMAC, apiKeyId, secretApiKey);

            assertEquals(defaultAuthenticator.createSimpleAuthenticationSignature(method, uri, headers),
                    reconfigurableAuthenticator.createSimpleAuthenticationSignature(method, uri, headers));
        }
    }

    private static List<RequestHeader> getRequestHeaders() {
        List<RequestHeader> headers = new ArrayList<>();
        headers.addAll(new MetaDataProvider("robtimus").getServerMetaDataHeaders());
        headers.add(new RequestHeader("Date", getDate()));
        headers.add(new RequestHeader("Content-Type", "application/json"));
        return headers;
    }

    private static String getDate() {
        return ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
