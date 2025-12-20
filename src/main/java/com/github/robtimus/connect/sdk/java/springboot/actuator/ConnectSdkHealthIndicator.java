/*
 * ConnectSdkHealthIndicator.java
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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health.Builder;
import org.springframework.boot.health.contributor.HealthIndicator;
import com.worldline.connect.sdk.java.v1.ApiException;
import com.worldline.connect.sdk.java.v1.domain.TestConnection;
import com.worldline.connect.sdk.java.v1.merchant.MerchantClient;
import com.worldline.connect.sdk.java.v1.merchant.services.ServicesClient;

/**
 * {@link HealthIndicator} for <a href="https://github.com/Worldline-Global-Collect/connect-sdk-java/">connect-sdk-java</a>.
 * This {@code HealthIndicator} uses
 * {@link MerchantClient#services() merchantClient.services()}{@code .}{@link ServicesClient#testconnection() testconnection()}
 * to check the connectivity to the Worldline Connect Server API.
 *
 * @author Rob Spoor
 */
@SuppressWarnings("nls")
public class ConnectSdkHealthIndicator extends AbstractHealthIndicator {

    /** Add a small margin to account for small delays between calling the health indicator. */
    private static final int MARGIN = 10;

    private final MerchantClient merchantClient;

    private final long minInterval;
    private long lastCheck;

    /**
     * Creates a new health indicator.
     *
     * @param merchantClient The merchant client to use.
     * @param minInterval The minimum interval in seconds between calls. Must be &gt; 0.
     */
    public ConnectSdkHealthIndicator(MerchantClient merchantClient, int minInterval) {
        if (minInterval <= 0) {
            throw new IllegalArgumentException("minInterval must be > 0, is " + minInterval);
        }
        this.merchantClient = Objects.requireNonNull(merchantClient);
        this.minInterval = TimeUnit.SECONDS.toMillis(minInterval);
        lastCheck = Long.MIN_VALUE;
    }

    @Override
    protected void doHealthCheck(Builder builder) {
        final long now = System.currentTimeMillis();
        if (lastCheck + minInterval - MARGIN > now) {
            return;
        }

        lastCheck = now;

        try {
            TestConnection result = merchantClient.services().testconnection();
            builder.withDetail("result", result.getResult());
            builder.up();
        } catch (ApiException e) {
            builder.withDetail("statusCode", e.getStatusCode());
            builder.withDetail("errorId", e.getErrorId());
            builder.withDetail("errors", e.getErrors());
            builder.down();
        }
    }

    long lastCheck() {
        return lastCheck;
    }
}
