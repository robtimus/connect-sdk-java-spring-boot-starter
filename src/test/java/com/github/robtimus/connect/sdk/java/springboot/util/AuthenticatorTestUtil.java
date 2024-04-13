/*
 * AuthenticatorTestUtil.java
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

package com.github.robtimus.connect.sdk.java.springboot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.worldline.connect.sdk.java.authentication.Authenticator;
import com.worldline.connect.sdk.java.authentication.V1HMACAuthenticator;
import com.worldline.connect.sdk.java.communication.MetadataProvider;
import com.worldline.connect.sdk.java.communication.RequestHeader;

@SuppressWarnings({ "nls", "javadoc" })
public final class AuthenticatorTestUtil {

    private AuthenticatorTestUtil() {
    }

    public static void assertSignatureCalculation(Authenticator authenticator, String apiKeyId, String secretApiKey) {
        String method = "GET";
        URI uri = URI.create("http://localhost/v1/test/services/testconnection");
        List<RequestHeader> headers = getRequestHeaders();

        Authenticator defaultAuthenticator = new V1HMACAuthenticator(apiKeyId, secretApiKey);

        assertEquals(defaultAuthenticator.getAuthorization(method, uri, headers), authenticator.getAuthorization(method, uri, headers));
    }

    public static void assertDifferentSignatureCalculation(Authenticator authenticator, String apiKeyId, String secretApiKey) {
        String method = "GET";
        URI uri = URI.create("http://localhost/v1/test/services/testconnection");
        List<RequestHeader> headers = getRequestHeaders();

        Authenticator defaultAuthenticator = new V1HMACAuthenticator(apiKeyId, secretApiKey);

        assertNotEquals(defaultAuthenticator.getAuthorization(method, uri, headers), authenticator.getAuthorization(method, uri, headers));
    }

    private static List<RequestHeader> getRequestHeaders() {
        List<RequestHeader> headers = new ArrayList<>();
        headers.addAll(new MetadataProvider("robtimus").getServerMetadataHeaders());
        headers.add(new RequestHeader("Date", getDate()));
        headers.add(new RequestHeader("Content-Type", "application/json"));
        return headers;
    }

    private static String getDate() {
        return ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
