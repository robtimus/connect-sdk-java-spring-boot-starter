/*
 * ExpiredConnectionsEndpoint.java
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

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.context.ApplicationContext;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;

/**
 * An {@link Endpoint} for closing expired connections in <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>.
 *
 * @author Rob Spoor
 * @since 3.8
 */
@Endpoint(id = "expiredConnectSdkConnections", enableByDefault = false)
@SuppressWarnings("javadoc")
public class ExpiredConnectionsEndpoint extends CloseConnectionsEndpoint {

    public ExpiredConnectionsEndpoint(ApplicationContext context) {
        super(context);
    }

    /**
     * Closes all expired connections for all {@link PooledConnection}, {@link Communicator} and {@link Client} beans.
     */
    @DeleteOperation
    public void closeExpiredConnections() {
        closeConnections(
                PooledConnection::closeExpiredConnections,
                Communicator::closeExpiredConnections,
                Client::closeExpiredConnections);
    }

    /**
     * Closes all expired connections for a specific {@link PooledConnection}, {@link Communicator} or {@link Client} bean.
     *
     * @param beanName The name of the {@link PooledConnection}, {@link Communicator} or {@link Client} bean.
     */
    @DeleteOperation
    public void closeExpiredConnectionsForBean(@Selector String beanName) {
        closeConnectionsForBean(beanName,
                PooledConnection::closeExpiredConnections,
                Communicator::closeExpiredConnections,
                Client::closeExpiredConnections);
    }
}
