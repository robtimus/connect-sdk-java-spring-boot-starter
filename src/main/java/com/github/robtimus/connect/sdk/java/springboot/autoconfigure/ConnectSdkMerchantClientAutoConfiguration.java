/*
 * ConnectSdkMerchantClientAutoConfiguration.java
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.merchant.MerchantClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>'s
 * {@link MerchantClient}.
 *
 * @author Rob Spoor
 */
@Configuration
@AutoConfigureAfter(ConnectSdkClientAutoConfiguration.class)
@ConditionalOnMissingBean(MerchantClient.class)
@ConditionalOnBean(Client.class)
@ConditionalOnProperty(name = "connect.api.merchant-id")
@EnableConfigurationProperties(ConnectSdkProperties.class)
@SuppressWarnings("javadoc")
public class ConnectSdkMerchantClientAutoConfiguration {

    @Bean
    public MerchantClient connectSdkMerchantClient(Client client, @Value("${connect.api.merchant-id}") String merchantId) {
        return client.merchant(merchantId);
    }
}
