/*
 * ConnectSdkVersionClientAutoConfiguration.java
 * Copyright 2024 Rob Spoor
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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.worldline.connect.sdk.java.Client;
import com.worldline.connect.sdk.java.v1.V1Client;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for version-specific clients for
 * <a href="https://github.com/Worldline-Global-Collectconnect-sdk-java/">connect-sdk-java</a>.
 *
 * @author Rob Spoor
 * @since 4.0
 */
@Configuration
@AutoConfigureAfter(ConnectSdkClientAutoConfiguration.class)
@ConditionalOnBean(Client.class)
@SuppressWarnings("javadoc")
public class ConnectSdkVersionClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(V1Client.class)
    public V1Client connectSdkV1Client(Client client) {
        return client.v1();
    }
}
