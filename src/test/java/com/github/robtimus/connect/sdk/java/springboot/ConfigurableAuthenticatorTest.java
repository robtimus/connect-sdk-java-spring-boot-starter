/*
 * ConfigurableAuthenticatorTest.java
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

import static com.github.robtimus.connect.sdk.java.springboot.util.AuthenticatorTestUtil.assertSignatureCalculation;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;

@SuppressWarnings("nls")
class ConfigurableAuthenticatorTest {

    @Test
    void testSignatureWithInitialApiKey() {
        String apiKeyId = UUID.randomUUID().toString();
        String secretApiKey = UUID.randomUUID().toString();

        Authenticator authenticator = new ConfigurableAuthenticator(AuthorizationType.V1HMAC, apiKeyId, secretApiKey);

        assertSignatureCalculation(authenticator, apiKeyId, secretApiKey);
    }

    @Test
    void testSignatureWithNewApiKey() {
        String apiKeyId = UUID.randomUUID().toString();
        String secretApiKey = UUID.randomUUID().toString();

        ConfigurableAuthenticator authenticator = new ConfigurableAuthenticator(AuthorizationType.V1HMAC, "x", "x");
        authenticator.setApiKey(AuthorizationType.V1HMAC, apiKeyId, secretApiKey);

        assertSignatureCalculation(authenticator, apiKeyId, secretApiKey);
    }
}
