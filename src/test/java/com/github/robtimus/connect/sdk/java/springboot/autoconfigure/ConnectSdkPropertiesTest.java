/*
 * ConnectSdkPropertiesTest.java
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

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;

class ConnectSdkPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PropertiesProvider.class));

    @Test
    void testDefaultProperties() {
        contextRunner
                .run(context -> {
                    ConnectSdkProperties properties = context.getBean(ConnectSdkProperties.class);
                    assertThat(properties.getEndpoint()).isNull();
                    assertThat(properties.getConnectTimeout()).isEqualTo(5000);
                    assertThat(properties.getSocketTimeout()).isEqualTo(300000);
                    assertThat(properties.getMaxConnections()).isEqualTo(10);
                    assertThat(properties.getAuthorizationType()).isEqualTo(AuthorizationType.V1HMAC);
                    assertThat(properties.getApiKeyId()).isNull();
                    assertThat(properties.getSecretApiKey()).isNull();
                    assertThat(properties.getProxy()).isNull();
                    assertThat(properties.getHttps()).isNull();
                    assertThat(properties.getIntegrator()).isNull();
                    assertThat(properties.getShoppingCartExtension()).isNull();
                });
    }

    @Test
    void testAllPropertiesKebabCase() {
        contextRunner
                .withPropertyValues("connect.api.merchant-id=myMerchantId", "connect.api.endpoint.host=localhost", "connect.api.endpoint.scheme=http",
                        "connect.api.endpoint.port=80", "connect.api.connect-timeout=1000", "connect.api.socket-timeout=10000",
                        "connect.api.max-connections=1", "connect.api.authorization-type=V1HMAC", "connect.api.api-key-id=myKeyId",
                        "connect.api.secret-api-key=mySecretApiKey", "connect.api.proxy.uri=http://proxy-host",
                        "connect.api.proxy.username=proxy-user", "connect.api.proxy.password=proxy-pass", "connect.api.https.protocols=TLSv1.2",
                        "connect.api.integrator=myIntegrator", "connect.api.shopping-cart-extension.creator=myExtensionCreator",
                        "connect.api.shopping-cart-extension.name=myExtensionName", "connect.api.shopping-cart-extension.version=myExtensionVersion",
                        "connect.api.shopping-cart-extension.extension-id=myExtensionId")
                .run(context -> {
                    ConnectSdkProperties properties = context.getBean(ConnectSdkProperties.class);
                    assertThat(properties.getEndpoint()).isNotNull();
                    assertThat(properties.getEndpoint().getHost()).isEqualTo("localhost");
                    assertThat(properties.getEndpoint().getScheme()).isEqualTo("http");
                    assertThat(properties.getEndpoint().getPort()).isEqualTo(80);
                    assertThat(properties.getConnectTimeout()).isEqualTo(1000);
                    assertThat(properties.getSocketTimeout()).isEqualTo(10000);
                    assertThat(properties.getMaxConnections()).isEqualTo(1);
                    assertThat(properties.getAuthorizationType()).isEqualTo(AuthorizationType.V1HMAC);
                    assertThat(properties.getApiKeyId()).isEqualTo("myKeyId");
                    assertThat(properties.getSecretApiKey()).isEqualTo("mySecretApiKey");
                    assertThat(properties.getProxy()).isNotNull();
                    assertThat(properties.getProxy().getUri()).isEqualTo("http://proxy-host");
                    assertThat(properties.getProxy().getUsername()).isEqualTo("proxy-user");
                    assertThat(properties.getProxy().getPassword()).isEqualTo("proxy-pass");
                    assertThat(properties.getHttps()).isNotNull();
                    assertThat(properties.getHttps().getProtocols()).isEqualTo(Collections.singletonList("TLSv1.2"));
                    assertThat(properties.getIntegrator()).isEqualTo("myIntegrator");
                    assertThat(properties.getShoppingCartExtension()).isNotNull();
                    assertThat(properties.getShoppingCartExtension().getCreator()).isEqualTo("myExtensionCreator");
                    assertThat(properties.getShoppingCartExtension().getName()).isEqualTo("myExtensionName");
                    assertThat(properties.getShoppingCartExtension().getVersion()).isEqualTo("myExtensionVersion");
                    assertThat(properties.getShoppingCartExtension().getExtensionId()).isEqualTo("myExtensionId");
                });
    }

    @Test
    void testAllPropertiesCamelCase() {
        contextRunner
                .withPropertyValues("connect.api.merchantId=myMerchantId", "connect.api.endpoint.host=localhost", "connect.api.endpoint.scheme=http",
                        "connect.api.endpoint.port=80", "connect.api.connectTimeout=1000", "connect.api.socketTimeout=10000",
                        "connect.api.maxConnections=1", "connect.api.authorizationType=V1HMAC", "connect.api.apiKeyId=myKeyId",
                        "connect.api.secretApiKey=mySecretApiKey", "connect.api.proxy.uri=http://proxy-host",
                        "connect.api.proxy.username=proxy-user", "connect.api.proxy.password=proxy-pass", "connect.api.https.protocols=TLSv1.2",
                        "connect.api.integrator=myIntegrator", "connect.api.shoppingCartExtension.creator=myExtensionCreator",
                        "connect.api.shoppingCartExtension.name=myExtensionName", "connect.api.shoppingCartExtension.version=myExtensionVersion",
                        "connect.api.shoppingCartExtension.extensionId=myExtensionId")
                .run(context -> {
                    ConnectSdkProperties properties = context.getBean(ConnectSdkProperties.class);
                    assertThat(properties.getEndpoint()).isNotNull();
                    assertThat(properties.getEndpoint().getHost()).isEqualTo("localhost");
                    assertThat(properties.getEndpoint().getScheme()).isEqualTo("http");
                    assertThat(properties.getEndpoint().getPort()).isEqualTo(80);
                    assertThat(properties.getConnectTimeout()).isEqualTo(1000);
                    assertThat(properties.getSocketTimeout()).isEqualTo(10000);
                    assertThat(properties.getMaxConnections()).isEqualTo(1);
                    assertThat(properties.getAuthorizationType()).isEqualTo(AuthorizationType.V1HMAC);
                    assertThat(properties.getApiKeyId()).isEqualTo("myKeyId");
                    assertThat(properties.getSecretApiKey()).isEqualTo("mySecretApiKey");
                    assertThat(properties.getProxy()).isNotNull();
                    assertThat(properties.getProxy().getUri()).isEqualTo("http://proxy-host");
                    assertThat(properties.getProxy().getUsername()).isEqualTo("proxy-user");
                    assertThat(properties.getProxy().getPassword()).isEqualTo("proxy-pass");
                    assertThat(properties.getHttps()).isNotNull();
                    assertThat(properties.getHttps().getProtocols()).isEqualTo(Collections.singletonList("TLSv1.2"));
                    assertThat(properties.getIntegrator()).isEqualTo("myIntegrator");
                    assertThat(properties.getShoppingCartExtension()).isNotNull();
                    assertThat(properties.getShoppingCartExtension().getCreator()).isEqualTo("myExtensionCreator");
                    assertThat(properties.getShoppingCartExtension().getName()).isEqualTo("myExtensionName");
                    assertThat(properties.getShoppingCartExtension().getVersion()).isEqualTo("myExtensionVersion");
                    assertThat(properties.getShoppingCartExtension().getExtensionId()).isEqualTo("myExtensionId");
                });
    }

    @Configuration
    @EnableConfigurationProperties(ConnectSdkProperties.class)
    static class PropertiesProvider {
        // no body needed
    }
}
