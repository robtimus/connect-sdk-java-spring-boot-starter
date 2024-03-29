/*
 * ConnectSdkConnectionsEndpointAutoConfiguration.java
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

import static com.github.robtimus.connect.sdk.java.springboot.autoconfigure.ConnectSdkConnectionAutoConfiguration.ConnectionManager.IDLE_TIME;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ConnectionsEndpoint}.
 *
 * @author Rob Spoor
 */
@Configuration
@AutoConfigureAfter(ConnectSdkClientAutoConfiguration.class)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnAvailableEndpoint(endpoint = ConnectionsEndpoint.class)
@SuppressWarnings("javadoc")
public class ConnectSdkConnectionsEndpointAutoConfiguration {

    // there will always be at least one closeable bean available - either a custom Connection, or the auto-configured Connection

    @Bean
    @ConditionalOnMissingBean
    public ConnectionsEndpoint connectSdkConnectionsEndpoint(ApplicationContext context, @Value(IDLE_TIME) long defaultIdleTimeInMillis) {
        return new ConnectionsEndpoint(context, defaultIdleTimeInMillis);
    }
}
