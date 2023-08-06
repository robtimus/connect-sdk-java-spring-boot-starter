/*
 * EndpointsTest.java
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

import static com.github.robtimus.connect.sdk.java.springboot.util.AuthenticatorTestUtil.assertDifferentSignatureCalculation;
import static com.github.robtimus.connect.sdk.java.springboot.util.AuthenticatorTestUtil.assertSignatureCalculation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import com.github.robtimus.connect.sdk.java.springboot.actuator.ConnectionsEndpoint.CloseableBeans;
import com.github.robtimus.connect.sdk.java.springboot.actuator.EndpointsTest.TestApplication;
import com.github.robtimus.connect.sdk.java.springboot.actuator.LoggingEndpoint.LoggingCapableAndLoggerBeans;
import com.ingenico.connect.gateway.sdk.java.Authenticator;
import com.ingenico.connect.gateway.sdk.java.PooledConnection;
import com.ingenico.connect.gateway.sdk.java.ResponseHandler;
import com.ingenico.connect.gateway.sdk.java.ResponseHeader;
import com.ingenico.connect.gateway.sdk.java.domain.services.TestConnection;
import com.ingenico.connect.gateway.sdk.java.logging.CommunicatorLogger;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = TestApplication.class, properties = {
        "connect.api.endpoint.host=localhost",
        "connect.api.integrator=robtimus",
        "connect.api.api-key-id=keyId",
        "connect.api.secret-api-key=secret",
        "connect.api.merchant-id=123",
        "management.endpoint.health.show-details=always",
        "management.endpoint.connectSdkApiKey.enabled=true",
        "management.endpoint.connectSdkConnections.enabled=true",
        "management.endpoint.connectSdkLogging.enabled=true",
        "management.endpoints.web.exposure.include=health,connectSdkApiKey,connectSdkConnections,connectSdkLogging"
})
@SuppressWarnings("nls")
class EndpointsTest {

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private PooledConnection connection;

    @Autowired
    private Authenticator authenticator;

    @Autowired
    private CommunicatorLogger logger;

    @BeforeEach
    void resetMocks() {
        reset(connection);
    }

    private URI getActuatorBaseURI() {
        return URI.create("http://localhost:" + managementPort + "/actuator/");
    }

    @Nested
    class ApiKey {

        @Nested
        class SetApiKey {

            private URI getActuatorURI() {
                return getActuatorBaseURI().resolve("connectSdkApiKey");
            }

            @Test
            void testWithoutAuthorizationType() {
                String apiKeyId = UUID.randomUUID().toString();
                String secretApiKey = UUID.randomUUID().toString();

                assertDifferentSignatureCalculation(authenticator, apiKeyId, secretApiKey);

                String requestBody = String.format("{\"apiKeyId\": \"%s\", \"secretApiKey\": \"%s\"}", apiKeyId, secretApiKey);

                RequestEntity<String> request = RequestEntity
                        .post(getActuatorURI())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody);

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
                assertNull(response.getBody());

                assertSignatureCalculation(authenticator, apiKeyId, secretApiKey);
            }

            @Test
            void testWithAuthorizationType() {
                String apiKeyId = UUID.randomUUID().toString();
                String secretApiKey = UUID.randomUUID().toString();

                assertDifferentSignatureCalculation(authenticator, apiKeyId, secretApiKey);

                String requestBody = String.format("{\"apiKeyId\": \"%s\", \"secretApiKey\": \"%s\", \"authorizationType\": \"V1HMAC\"}",
                        apiKeyId, secretApiKey);

                RequestEntity<String> request = RequestEntity
                        .post(getActuatorURI())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody);

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
                assertNull(response.getBody());

                assertSignatureCalculation(authenticator, apiKeyId, secretApiKey);
            }
        }
    }

    @Nested
    class Connections {

        @Test
        void testListCloseableBeans() {
            RequestEntity<Void> request = RequestEntity
                    .get(getActuatorBaseURI().resolve("connectSdkConnections"))
                    .build();

            ResponseEntity<CloseableBeans> response = restTemplateBuilder
                    .build()
                    .exchange(request, CloseableBeans.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());

            CloseableBeans beans = response.getBody();

            assertEquals(Arrays.asList("mockConnection"), beans.getConnections());
            assertEquals(Arrays.asList("connectSdkCommunicator"), beans.getCommunicators());
            assertEquals(Arrays.asList("connectSdkClient"), beans.getClients());
        }

        @Nested
        class CloseIdleConnections {

            @Test
            @SuppressWarnings("resource")
            void testForAllBeans() {
                String requestBody = "{\"idleTime\": 20, \"timeUnit\": \"SECONDS\"}";

                RequestEntity<String> request = RequestEntity
                        .post(getActuatorBaseURI().resolve("connectSdkConnections"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody);

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                // 3 times: one through the client, once through the communicator, once through the connection itself
                verify(connection, times(3)).closeIdleConnections(20, TimeUnit.SECONDS);
            }

            @Test
            @SuppressWarnings("resource")
            void testForSpecificBean() {
                String requestBody = "{\"idleTime\": 20, \"timeUnit\": \"SECONDS\"}";

                RequestEntity<String> request = RequestEntity
                        .post(getActuatorBaseURI().resolve("connectSdkConnections/mockConnection"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody);

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                verify(connection).closeIdleConnections(20, TimeUnit.SECONDS);
            }
        }

        @Nested
        class CloseExpiredConnections {

            @Test
            @SuppressWarnings("resource")
            void testForAllBeans() {
                RequestEntity<Void> request = RequestEntity
                        .post(getActuatorBaseURI().resolve("connectSdkConnections"))
                        .build();

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                // 3 times: one through the client, once through the communicator, once through the connection itself
                verify(connection, times(3)).closeExpiredConnections();
            }

            @Test
            @SuppressWarnings("resource")
            void testForSpecificBean() {
                RequestEntity<Void> request = RequestEntity
                        .post(getActuatorBaseURI().resolve("connectSdkConnections/mockConnection"))
                        .build();

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                verify(connection).closeExpiredConnections();
            }
        }
    }

    @Nested
    class Logging {

        @Test
        void testListLoggingCapableAndLoggerBeans() {
            RequestEntity<Void> request = RequestEntity
                    .get(getActuatorBaseURI().resolve("connectSdkLogging"))
                    .build();

            ResponseEntity<LoggingCapableAndLoggerBeans> response = restTemplateBuilder
                    .build()
                    .exchange(request, LoggingCapableAndLoggerBeans.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());

            LoggingCapableAndLoggerBeans beans = response.getBody();

            assertEquals(Arrays.asList("mockConnection"), beans.getConnections());
            assertEquals(Arrays.asList("connectSdkCommunicator"), beans.getCommunicators());
            assertEquals(Arrays.asList("connectSdkClient"), beans.getClients());
            assertEquals(Arrays.asList("mockLogger"), beans.getLoggers());
        }

        @Nested
        class EnableLogging {

            @Test
            @SuppressWarnings("resource")
            void testAllLoggersForAllBeans() {
                RequestEntity<Void> request = RequestEntity
                        .post(getActuatorBaseURI().resolve("connectSdkLogging"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .build();

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                // 3 times: one through the client, once through the communicator, once through the connection itself
                verify(connection, times(3)).enableLogging(logger);
            }

            @Nested
            class SpecificLoggerForAllBeans {

                @Test
                @SuppressWarnings("resource")
                void testWithQueryParams() {
                    String requestBody = "{\"logger\": \"mockLogger\"}";

                    RequestEntity<String> request = RequestEntity
                            .post(getActuatorBaseURI().resolve("connectSdkLogging?logger=mockLogger"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody);

                    ResponseEntity<Void> response = restTemplateBuilder
                            .build()
                            .exchange(request, Void.class);

                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                    // 3 times: one through the client, once through the communicator, once through the connection itself
                    verify(connection, times(3)).enableLogging(logger);
                }

                @Test
                @SuppressWarnings("resource")
                void testWithRequestBody() {
                    String requestBody = "{\"logger\": \"mockLogger\"}";

                    RequestEntity<String> request = RequestEntity
                            .post(getActuatorBaseURI().resolve("connectSdkLogging"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody);

                    ResponseEntity<Void> response = restTemplateBuilder
                            .build()
                            .exchange(request, Void.class);

                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                    // 3 times: one through the client, once through the communicator, once through the connection itself
                    verify(connection, times(3)).enableLogging(logger);
                }
            }

            @Test
            @SuppressWarnings("resource")
            void testAllLoggersForSpecificBean() {
                RequestEntity<Void> request = RequestEntity
                        .post(getActuatorBaseURI().resolve("connectSdkLogging/mockConnection"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .build();

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                verify(connection).enableLogging(logger);
            }

            @Nested
            class SpecificLoggerForSpecificBean {

                @Test
                @SuppressWarnings("resource")
                void testWithQueryParams() {
                    String requestBody = "{\"logger\": \"mockLogger\"}";

                    RequestEntity<String> request = RequestEntity
                            .post(getActuatorBaseURI().resolve("connectSdkLogging/mockConnection?logger=mockLogger"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody);

                    ResponseEntity<Void> response = restTemplateBuilder
                            .build()
                            .exchange(request, Void.class);

                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                    verify(connection).enableLogging(logger);
                }

                @Test
                @SuppressWarnings("resource")
                void testWithRequestBody() {
                    String requestBody = "{\"logger\": \"mockLogger\"}";

                    RequestEntity<String> request = RequestEntity
                            .post(getActuatorBaseURI().resolve("connectSdkLogging/mockConnection"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody);

                    ResponseEntity<Void> response = restTemplateBuilder
                            .build()
                            .exchange(request, Void.class);

                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                    verify(connection).enableLogging(logger);
                }
            }
        }

        @Nested
        class DisableLogging {

            @Test
            @SuppressWarnings("resource")
            void testForAllBeans() {
                RequestEntity<Void> request = RequestEntity
                        .delete(getActuatorBaseURI().resolve("connectSdkLogging"))
                        .build();

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                // 3 times: one through the client, once through the communicator, once through the connection itself
                verify(connection, times(3)).disableLogging();
            }

            @Test
            @SuppressWarnings("resource")
            void testForSpecificBean() {
                RequestEntity<Void> request = RequestEntity
                        .delete(getActuatorBaseURI().resolve("connectSdkLogging/mockConnection"))
                        .build();

                ResponseEntity<Void> response = restTemplateBuilder
                        .build()
                        .exchange(request, Void.class);

                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

                verify(connection).disableLogging();
            }
        }
    }

    @Test
    void testHealt() {
        URI uri = URI.create("https://localhost/v1/123/services/testconnection");
        when(connection.get(eq(uri), any(), any())).thenAnswer(i -> {
            ResponseHandler<TestConnection> handler = i.getArgument(2);
            String response = "{\"result\" : \"OK\"}";
            List<ResponseHeader> headers = Arrays.asList(new ResponseHeader("Content-Type", "application/json"));
            return handler.handleResponse(200, new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), headers);
        });

        RequestEntity<Void> request = RequestEntity
                .get(getActuatorBaseURI().resolve("health"))
                .build();

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplateBuilder
                .build()
                .exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> responseMap = response.getBody();

        assertThat(responseMap, hasEntry("status", "UP"));

        // Support both older format (connectSdk directly in the root of the response) and the newer one (with nested components entry)
        Map<?, ?> componentsMap = responseMap.containsKey("components")
                ? (Map<?, ?>) responseMap.get("components")
                : responseMap;

        Map<String, Object> expectedSdkComponent = new HashMap<>();
        expectedSdkComponent.put("status", "UP");
        expectedSdkComponent.put("details", Collections.singletonMap("result", "OK"));

        assertThat(componentsMap, hasEntry("connectSdk", expectedSdkComponent));
    }

    // No @SpringBootApplication, so @ComponentScan won't be applied
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        PooledConnection mockConnection() {
            return mock(PooledConnection.class);
        }

        @Bean
        CommunicatorLogger mockLogger() {
            return mock(CommunicatorLogger.class);
        }
    }
}
