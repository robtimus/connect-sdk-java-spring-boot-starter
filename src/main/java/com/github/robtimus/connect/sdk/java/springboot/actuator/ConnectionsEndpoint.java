/*
 * ConnectionsEndpoint.java
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;

/**
 * An {@link Endpoint} for managing connections in <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>.
 *
 * @author Rob Spoor
 */
@Endpoint(id = "connectSdkConnections", enableByDefault = false)
@SuppressWarnings({ "nls", "javadoc" })
public class ConnectionsEndpoint {

    private final ApplicationContext context;

    private final long defaultIdleTimeInMillis;

    public ConnectionsEndpoint(ApplicationContext context, long defaultIdleTimeInMillis) {
        this.context = context;

        this.defaultIdleTimeInMillis = defaultIdleTimeInMillis;
    }

    /**
     * @return All {@link PooledConnection}, {@link Communicator} and {@link Client} beans.
     */
    @ReadOperation
    public CloseableBeans listCloseableBeans() {
        CloseableBeans result = new CloseableBeans();
        result.connections.addAll(Arrays.asList(context.getBeanNamesForType(PooledConnection.class)));
        result.communicators.addAll(Arrays.asList(context.getBeanNamesForType(Communicator.class)));
        result.clients.addAll(Arrays.asList(context.getBeanNamesForType(Client.class)));
        return result;
    }

    /**
     * Closes all connections that are idle, expired or both for all {@link PooledConnection}, {@link Communicator} and {@link Client} beans.
     *
     * @param idleTime The idle time; ignored if {@code close} is {@link CloseableConnectionState#EXPIRED}.
     * @param state {@link CloseableConnectionState#IDLE} to close all idle connections,
     *              {@link CloseableConnectionState#EXPIRED} to close all expired connections,
     *              or {@code null} to close all idle and expired connections.
     * @since 3.8
     */
    @DeleteOperation
    public void closeConnections(@Nullable Duration idleTime, @Nullable CloseableConnectionState state) {
        if (state == CloseableConnectionState.IDLE) {
            long idleTimeInMillis = toMillis(idleTime);
            TimeUnit timeUnitToUse = TimeUnit.MILLISECONDS;
            closeConnections(
                    connection -> connection.closeIdleConnections(idleTimeInMillis, timeUnitToUse),
                    communicator -> communicator.closeIdleConnections(idleTimeInMillis, timeUnitToUse),
                    client -> client.closeIdleConnections(idleTimeInMillis, timeUnitToUse));
        } else if (state == CloseableConnectionState.EXPIRED) {
            closeConnections(
                    PooledConnection::closeExpiredConnections,
                    Communicator::closeExpiredConnections,
                    Client::closeExpiredConnections);
        } else {
            // null - close both idle and expired
            long idleTimeInMillis = toMillis(idleTime);
            TimeUnit timeUnitToUse = TimeUnit.MILLISECONDS;
            closeConnections(
                    connection -> closeIdleAndExpiredConnections(connection, idleTimeInMillis, timeUnitToUse),
                    communicator -> closeIdleAndExpiredConnections(communicator, idleTimeInMillis, timeUnitToUse),
                    client -> closeIdleAndExpiredConnections(client, idleTimeInMillis, timeUnitToUse));
        }
    }

    /**
     * Closes all connections that are idle, expired or both for a specific {@link PooledConnection}, {@link Communicator} and {@link Client} bean.
     *
     * @param beanName The name of the {@link PooledConnection}, {@link Communicator} or {@link Client} bean.
     * @param idleTime The idle time; ignored if {@code close} is {@link CloseableConnectionState#EXPIRED}.
     * @param state {@link CloseableConnectionState#IDLE} to close all idle connections,
     *              {@link CloseableConnectionState#EXPIRED} to close all expired connections,
     *              or {@code null} to close all idle and expired connections.
     * @since 3.8
     */
    @DeleteOperation
    public void closeConnectionsForBean(@Selector String beanName, @Nullable Duration idleTime, @Nullable CloseableConnectionState state) {
        if (state == CloseableConnectionState.IDLE) {
            long idleTimeInMillis = toMillis(idleTime);
            TimeUnit timeUnitToUse = TimeUnit.MILLISECONDS;
            closeConnectionsForBean(beanName,
                    connection -> connection.closeIdleConnections(idleTimeInMillis, timeUnitToUse),
                    communicator -> communicator.closeIdleConnections(idleTimeInMillis, timeUnitToUse),
                    client -> client.closeIdleConnections(idleTimeInMillis, timeUnitToUse));
        } else if (state == CloseableConnectionState.EXPIRED) {
            closeConnectionsForBean(beanName,
                    PooledConnection::closeExpiredConnections,
                    Communicator::closeExpiredConnections,
                    Client::closeExpiredConnections);
        } else {
            // null - close both idle and expired
            long idleTimeInMillis = toMillis(idleTime);
            TimeUnit timeUnitToUse = TimeUnit.MILLISECONDS;
            closeConnectionsForBean(beanName,
                    connection -> closeIdleAndExpiredConnections(connection, idleTimeInMillis, timeUnitToUse),
                    communicator -> closeIdleAndExpiredConnections(communicator, idleTimeInMillis, timeUnitToUse),
                    client -> closeIdleAndExpiredConnections(client, idleTimeInMillis, timeUnitToUse));
        }
    }

    private long toMillis(Duration idleTime) {
        return idleTime != null
                ? idleTime.toMillis()
                : defaultIdleTimeInMillis;
    }

    private void closeIdleAndExpiredConnections(PooledConnection connection, long idleTime, TimeUnit timeUnit) {
        connection.closeIdleConnections(idleTime, timeUnit);
        connection.closeExpiredConnections();
    }

    private void closeIdleAndExpiredConnections(Communicator communicator, long idleTime, TimeUnit timeUnit) {
        communicator.closeIdleConnections(idleTime, timeUnit);
        communicator.closeExpiredConnections();
    }

    private void closeIdleAndExpiredConnections(Client client, long idleTime, TimeUnit timeUnit) {
        client.closeIdleConnections(idleTime, timeUnit);
        client.closeExpiredConnections();
    }

    /**
     * Closes all idle connections for all {@link PooledConnection}, {@link Communicator} and {@link Client} beans.
     *
     * @param idleTime The idle time.
     * @param timeUnit The time unit.
     * @deprecated Use {@link #closeConnections(Duration, CloseableConnectionState)} instead.
     */
    @WriteOperation
    @Deprecated
    public void closeIdleConnections(Long idleTime, TimeUnit timeUnit) {
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
     * @param timeUnit The time unit.
     * @deprecated Use {@link #closeConnectionsForBean(String, Duration, CloseableConnectionState)} instead.
     */
    @WriteOperation
    @Deprecated
    public void closeIdleConnectionsForBean(@Selector String beanName, Long idleTime, TimeUnit timeUnit) {
        closeConnectionsForBean(beanName,
                connection -> connection.closeIdleConnections(idleTime, timeUnit),
                communicator -> communicator.closeIdleConnections(idleTime, timeUnit),
                client -> client.closeIdleConnections(idleTime, timeUnit));
    }

    /**
     * Closes all expired connections for all {@link PooledConnection}, {@link Communicator} and {@link Client} beans.
     *
     * @deprecated Use {@link #closeConnections(Duration, CloseableConnectionState)} instead.
     */
    @WriteOperation
    @Deprecated
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
     * @deprecated Use {@link #closeConnectionsForBean(String, Duration, CloseableConnectionState)} instead.
     */
    @WriteOperation
    @Deprecated
    public void closeExpiredConnectionsForBean(@Selector String beanName) {
        closeConnectionsForBean(beanName,
                PooledConnection::closeExpiredConnections,
                Communicator::closeExpiredConnections,
                Client::closeExpiredConnections);
    }

    private void closeConnections(Consumer<PooledConnection> connectionAction,
            Consumer<Communicator> communicatorAction,
            Consumer<Client> clientAction) {

        context.getBeansOfType(PooledConnection.class).values().forEach(connectionAction);
        context.getBeansOfType(Communicator.class).values().forEach(communicatorAction);
        context.getBeansOfType(Client.class).values().forEach(clientAction);
    }

    private void closeConnectionsForBean(String beanName,
            Consumer<PooledConnection> connectionAction,
            Consumer<Communicator> communicatorAction,
            Consumer<Client> clientAction) {

        Object bean = context.getBean(beanName);
        if (bean instanceof PooledConnection) {
            connectionAction.accept((PooledConnection) bean);
        } else if (bean instanceof Communicator) {
            communicatorAction.accept((Communicator) bean);
        } else if (bean instanceof Client) {
            clientAction.accept((Client) bean);
        } else {
            throw new BeanNotCloseableException(beanName, bean.getClass());
        }
    }

    public static class CloseableBeans {

        private List<String> connections = new ArrayList<>();
        private List<String> communicators = new ArrayList<>();
        private List<String> clients = new ArrayList<>();

        public List<String> getConnections() {
            return connections;
        }

        public List<String> getCommunicators() {
            return communicators;
        }

        public List<String> getClients() {
            return clients;
        }
    }

    public enum CloseableConnectionState {
        /** Only close idle connections. */
        IDLE,

        /** Only close expired connections. */
        EXPIRED,
    }

    @SuppressWarnings("serial")
    public static final class BeanNotCloseableException extends BeansException {

        private final String beanName;
        private final Class<?> actualType;

        private BeanNotCloseableException(String beanName, Class<?> actualType) {
            super("Bean named '" + beanName + "' is expected to be of type '" + PooledConnection.class.getName() + "', '"
                    + Communicator.class + "' or '" + Client.class + "' but was actually of type '" + actualType.getTypeName() + "'");

            this.beanName = beanName;
            this.actualType = actualType;
        }

        public String getBeanName() {
            return beanName;
        }

        public Class<?> getActualType() {
            return actualType;
        }
    }
}
