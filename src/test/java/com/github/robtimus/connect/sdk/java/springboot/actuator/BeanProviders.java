/*
 * BeanProviders.java
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

import static org.mockito.Mockito.mock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.worldline.connect.sdk.java.Client;
import com.worldline.connect.sdk.java.Communicator;
import com.worldline.connect.sdk.java.communication.Connection;
import com.worldline.connect.sdk.java.communication.PooledConnection;
import com.worldline.connect.sdk.java.json.DefaultMarshaller;
import com.worldline.connect.sdk.java.json.Marshaller;
import com.worldline.connect.sdk.java.logging.CommunicatorLogger;

final class BeanProviders {

    private BeanProviders() {
    }

    @Configuration
    static class ConnectionProvider {

        @Bean
        @Primary
        Connection connection() {
            return mock(Connection.class);
        }
    }

    @Configuration
    static class PooledConnectionProvider {

        @Bean
        PooledConnection pooledConnection() {
            return mock(PooledConnection.class);
        }
    }

    @Configuration
    static class CommunicatorProvider {

        @Bean
        Communicator communicator() {
            return mock(Communicator.class);
        }
    }

    @Configuration
    static class ClientProvider {

        @Bean
        Client client() {
            return mock(Client.class);
        }
    }

    @Configuration
    static class LoggerProvider {

        @Bean
        CommunicatorLogger logger() {
            return mock(CommunicatorLogger.class);
        }
    }

    @Configuration
    static class AdditionalLoggerProvider {

        @Bean
        CommunicatorLogger additionalLogger() {
            return mock(CommunicatorLogger.class);
        }
    }

    @Configuration
    static class AdditionalBeanProvider {

        @Bean
        Marshaller marshaller() {
            return DefaultMarshaller.INSTANCE;
        }
    }
}
