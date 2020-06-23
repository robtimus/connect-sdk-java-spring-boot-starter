/*
 * ConnectSdkProperties.java
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

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.ingenico.connect.gateway.sdk.java.CommunicatorConfiguration;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.AuthorizationType;

/**
 * Properties for <a href="https://github.com/Ingenico-ePayments/connect-sdk-java/">connect-sdk-java</a>.
 * These match the properties available in {@link CommunicatorConfiguration}.
 *
 * @author Rob Spoor
 */
@ConfigurationProperties("connect.api")
public class ConnectSdkProperties {

    private Endpoint endpoint;

    /** Connect timeout for HTTP requests. */
    private int connectTimeout = 5_000;
    /** Socket/read timeout for HTTP requests. */
    private int socketTimeout = 300_000;
    /** Maximum number of concurrent HTTP connections. */
    private int maxConnections = CommunicatorConfiguration.DEFAULT_MAX_CONNECTIONS;

    /** Authorization type, should only be V1HMAC. */
    private AuthorizationType authorizationType = AuthorizationType.V1HMAC;
    /** Your API key id. */
    private String apiKeyId;
    /** Your secret API key. */
    private String secretApiKey;

    private Proxy proxy;

    private HTTPS https;

    /** Your company name. */
    private String integrator;

    private ShoppingCartExtension shoppingCartExtension;

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public AuthorizationType getAuthorizationType() {
        return authorizationType;
    }

    public void setAuthorizationType(AuthorizationType authorizationType) {
        this.authorizationType = authorizationType;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getSecretApiKey() {
        return secretApiKey;
    }

    public void setSecretApiKey(String secretApiKey) {
        this.secretApiKey = secretApiKey;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public HTTPS getHttps() {
        return https;
    }

    public void setHttps(HTTPS https) {
        this.https = https;
    }

    public String getIntegrator() {
        return integrator;
    }

    public void setIntegrator(String integrator) {
        this.integrator = integrator;
    }

    public ShoppingCartExtension getShoppingCartExtension() {
        return shoppingCartExtension;
    }

    public void setShoppingCartExtension(ShoppingCartExtension shoppingCartExtension) {
        this.shoppingCartExtension = shoppingCartExtension;
    }

    public static class Endpoint {

        /** Hostname of the API endpoint to use. */
        private String host;
        /** Scheme of the API endpoint to use. */
        private String scheme = "https";
        /** Port of the API endpoint to use. */
        private int port = -1;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class HTTPS {

        /** Supported HTTPS protocols. If not specified the SDK will specify defaults */
        private List<String> protocols;

        public List<String> getProtocols() {
            return protocols;
        }

        public void setProtocols(List<String> protocols) {
            this.protocols = protocols;
        }
    }

    public static class Proxy {

        /** URI for the HTTP proxy to use, if any. */
        private String uri;
        /** Username for the HTTP proxy to use, if any. */
        private String username;
        /** Password for the HTTP proxy to use, if any. */
        private String password;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class ShoppingCartExtension {

        /** For shopping cart extensions, the creator. */
        private String creator;
        /** For shopping cart extensions, the name. */
        private String name;
        /** For shopping cart extensions, the version. */
        private String version;
        /** For shopping cart extensions, the extension id. */
        private String extensionId;

        public String getCreator() {
            return creator;
        }

        public void setCreator(String creator) {
            this.creator = creator;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getExtensionId() {
            return extensionId;
        }

        public void setExtensionId(String extensionId) {
            this.extensionId = extensionId;
        }
    }
}
