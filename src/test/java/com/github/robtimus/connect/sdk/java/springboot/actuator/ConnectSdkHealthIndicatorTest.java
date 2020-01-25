/*
 * ConnectSdkHealthIndicatorTest.java
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

package com.github.robtimus.connect.sdk.java.springboot.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import com.ingenico.connect.gateway.sdk.java.ApiException;
import com.ingenico.connect.gateway.sdk.java.domain.errors.definitions.APIError;
import com.ingenico.connect.gateway.sdk.java.domain.services.TestConnection;
import com.ingenico.connect.gateway.sdk.java.merchant.MerchantClient;
import com.ingenico.connect.gateway.sdk.java.merchant.services.ServicesClient;

public class ConnectSdkHealthIndicatorTest {

    @Test
    public void testNonPositiveMinInterval() {
        MerchantClient merchantClient = mock(MerchantClient.class);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new ConnectSdkHealthIndicator(merchantClient, 0));
        assertThat(exception.getMessage()).isEqualTo("minInterval must be > 0, is 0");
    }

    @Test
    public void testHealthResultOK() {
        MerchantClient merchantClient = mock(MerchantClient.class);
        ServicesClient servicesClient = mock(ServicesClient.class);

        TestConnection testConnection = new TestConnection();
        testConnection.setResult("OK");

        when(servicesClient.testconnection()).thenReturn(testConnection);
        when(merchantClient.services()).thenReturn(servicesClient);

        ConnectSdkHealthIndicator healthIndicator = new ConnectSdkHealthIndicator(merchantClient, 1);

        Health health = healthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isEqualTo(Collections.singletonMap("result", "OK"));
    }

    @Test
    public void testHealthThrottled() throws InterruptedException {
        MerchantClient merchantClient = mock(MerchantClient.class);
        ServicesClient servicesClient = mock(ServicesClient.class);

        TestConnection testConnection = new TestConnection();
        testConnection.setResult("OK");

        when(servicesClient.testconnection()).thenReturn(testConnection);
        when(merchantClient.services()).thenReturn(servicesClient);

        ConnectSdkHealthIndicator healthIndicator = new ConnectSdkHealthIndicator(merchantClient, 1);

        Health health = healthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isEqualTo(Collections.singletonMap("result", "OK"));

        health = healthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).isEqualTo(Collections.emptyMap());

        Thread.sleep(1000);

        health = healthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isEqualTo(Collections.singletonMap("result", "OK"));
    }

    @Test
    public void testHealthApiException() {
        MerchantClient merchantClient = mock(MerchantClient.class);
        ServicesClient servicesClient = mock(ServicesClient.class);

        APIError apiError = new APIError();
        apiError.setCode("9007");
        apiError.setId("ACCESS_TO_MERCHANT_NOT_ALLOWED");
        apiError.setCategory("CONNECT_PLATFORM_ERROR");
        apiError.setMessage("ACCESS_TO_MERCHANT_NOT_ALLOWED");
        apiError.setHttpStatusCode(403);

        ApiException apiException = new ApiException(403, "", UUID.randomUUID().toString(), Collections.singletonList(apiError));

        when(servicesClient.testconnection()).thenThrow(apiException);
        when(merchantClient.services()).thenReturn(servicesClient);

        ConnectSdkHealthIndicator healthIndicator = new ConnectSdkHealthIndicator(merchantClient, 1);

        Map<String, Object> expectedDetails = new HashMap<>();
        expectedDetails.put("statusCode", apiException.getStatusCode());
        expectedDetails.put("errorId", apiException.getErrorId());
        expectedDetails.put("errors", apiException.getErrors());

        Health health = healthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isEqualTo(expectedDetails);
    }
}
