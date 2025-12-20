/*
 * ConnectSdkHealthIndicatorAutoConfiguration.java
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectSdkHealthIndicator;
import com.worldline.connect.sdk.java.v1.merchant.MerchantClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ConnectSdkHealthIndicator}.
 *
 * @author Rob Spoor
 */
@Configuration
@AutoConfigureBefore(HealthContributorAutoConfiguration.class)
@AutoConfigureAfter(ConnectSdkMerchantClientAutoConfiguration.class)
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnMissingBean(ConnectSdkHealthIndicator.class)
@ConditionalOnBean(MerchantClient.class)
@ConditionalOnEnabledHealthIndicator("connect-sdk")
@SuppressWarnings("javadoc")
public class ConnectSdkHealthIndicatorAutoConfiguration {

    @Bean
    public HealthIndicator connectSdkHealthIndicator(MerchantClient merchantClient, @Value("${connect.api.health.min-interval:60}") int minInterval) {
        return new ConnectSdkHealthIndicator(merchantClient, minInterval);
    }
}
