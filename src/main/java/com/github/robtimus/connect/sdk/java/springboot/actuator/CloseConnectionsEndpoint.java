/*
 * CloseConnectionsEndpoint.java
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

import java.util.function.Consumer;
import org.springframework.context.ApplicationContext;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.BeanNotCloseableException;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.Communicator;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;

abstract class CloseConnectionsEndpoint {

    private final ApplicationContext context;

    protected CloseConnectionsEndpoint(ApplicationContext context) {
        this.context = context;
    }

    void closeConnections(Consumer<PooledConnection> connectionAction,
            Consumer<Communicator> communicatorAction,
            Consumer<Client> clientAction) {

        context.getBeansOfType(PooledConnection.class).values().forEach(connectionAction);
        context.getBeansOfType(Communicator.class).values().forEach(communicatorAction);
        context.getBeansOfType(Client.class).values().forEach(clientAction);
    }

    void closeConnectionsForBean(String beanName,
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
}
