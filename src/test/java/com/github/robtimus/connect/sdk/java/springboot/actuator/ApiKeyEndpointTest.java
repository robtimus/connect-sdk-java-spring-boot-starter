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

import static com.github.robtimus.connect.sdk.java.springboot.util.AuthenticatorTestUtil.assertSignatureCalculation;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.connect.sdk.java.springboot.ReconfigurableAuthenticator;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;

@SuppressWarnings("nls")
class ApiKeyEndpointTest {

    @Nested
    class SetApiKey {

        @Test
        void testWithoutAuthorizationType() {
            String apiKeyId = UUID.randomUUID().toString();
            String secretApiKey = UUID.randomUUID().toString();

            ReconfigurableAuthenticator reconfigurableAuthenticator = new ReconfigurableAuthenticator(AuthorizationType.V1HMAC, "x", "x");

            ApiKeyEndpoint endpoint = new ApiKeyEndpoint(reconfigurableAuthenticator);
            endpoint.setApiKey(apiKeyId, secretApiKey, null);
            reconfigurableAuthenticator.setApiKey(AuthorizationType.V1HMAC, apiKeyId, secretApiKey);

            assertSignatureCalculation(reconfigurableAuthenticator, apiKeyId, secretApiKey);
        }

        @Test
        void testWithAuthorizationType() {
            String apiKeyId = UUID.randomUUID().toString();
            String secretApiKey = UUID.randomUUID().toString();

            ReconfigurableAuthenticator reconfigurableAuthenticator = new ReconfigurableAuthenticator(AuthorizationType.V1HMAC, "x", "x");

            ApiKeyEndpoint endpoint = new ApiKeyEndpoint(reconfigurableAuthenticator);
            endpoint.setApiKey(apiKeyId, secretApiKey, AuthorizationType.V1HMAC);

            assertSignatureCalculation(reconfigurableAuthenticator, apiKeyId, secretApiKey);
        }
    }
}
