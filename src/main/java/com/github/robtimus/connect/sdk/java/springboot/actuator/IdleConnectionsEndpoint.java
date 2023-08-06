/*
 * IdleConnectionsEndpoint.java
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;

/**
 * An {@link Endpoint} for closing idle connections in <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>.
 *
 * @author Rob Spoor
 * @since 3.8
 */
@Endpoint(id = "idleConnectSdkConnections", enableByDefault = false)
@SuppressWarnings("javadoc")
public class IdleConnectionsEndpoint extends CloseConnectionsEndpoint {

    private final long defaultIdleTimeInMillis;

    public IdleConnectionsEndpoint(ApplicationContext context, long defaultIdleTimeInMillis) {
        super(context);
        this.defaultIdleTimeInMillis = defaultIdleTimeInMillis;
    }

    /**
     * Closes all idle connections for all {@link PooledConnection}, {@link Communicator} and {@link Client} beans.
     *
     * @param idleTime The idle time.
     */
    @DeleteOperation
    public void closeIdleConnections(@Nullable Duration idleTime) {
        closeIdleConnections(toMillis(idleTime), TimeUnit.MILLISECONDS);
    }

    void closeIdleConnections(long idleTime, TimeUnit timeUnit) {
        closeConnections(
                connection -> connection.closeIdleConnections(idleTime, timeUnit),
                communicator -> communicator.closeIdleConnections(idleTime, timeUnit),
                client -> client.closeIdleConnections(idleTime, timeUnit));
    }

    /**
     * Closes all idle connections for a specific {@link PooledConnection}, {@link Communicator} or {@link Client} bean.
     *
     * @param beanName The name of the {@link PooledConnection}, {@link Communicator} or {@link Client} bean.
     * @param idleTime The idle time.
     */
    @DeleteOperation
    public void closeIdleConnectionsForBean(@Selector String beanName, @Nullable Duration idleTime) {
        closeIdleConnectionsForBean(beanName, toMillis(idleTime), TimeUnit.MILLISECONDS);
    }

    void closeIdleConnectionsForBean(String beanName, long idleTime, TimeUnit timeUnit) {
        closeConnectionsForBean(beanName,
                connection -> connection.closeIdleConnections(idleTime, timeUnit),
                communicator -> communicator.closeIdleConnections(idleTime, timeUnit),
                client -> client.closeIdleConnections(idleTime, timeUnit));
    }

    private long toMillis(Duration idleTime) {
        return idleTime != null
                ? idleTime.toMillis()
                : defaultIdleTimeInMillis;
    }
}
