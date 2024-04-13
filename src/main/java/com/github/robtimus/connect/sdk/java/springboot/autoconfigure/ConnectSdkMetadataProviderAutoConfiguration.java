/*
 * ConnectSdkMetadataProviderAutoConfiguration.java
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
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.worldline.connect.sdk.java.communication.MetadataProvider;
import com.worldline.connect.sdk.java.communication.MetadataProviderBuilder;
import com.worldline.connect.sdk.java.domain.ShoppingCartExtension;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * <a href="https://github.com/Worldline-Global-Collectconnect-sdk-java/">connect-sdk-java</a>'s {@link MetadataProvider}.
 *
 * @author Rob Spoor
 */
@Configuration
@ConditionalOnMissingBean(MetadataProvider.class)
@ConditionalOnProperty(name = "connect.api.integrator")
@EnableConfigurationProperties(ConnectSdkProperties.class)
@SuppressWarnings("javadoc")
public class ConnectSdkMetadataProviderAutoConfiguration {

    private final ConnectSdkProperties properties;

    @Autowired
    public ConnectSdkMetadataProviderAutoConfiguration(ConnectSdkProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Bean
    public MetadataProvider connectSdkMetadataProvider(List<MetadataProviderBuilderCustomizer> customizers) {
        MetadataProviderBuilder builder = new MetadataProviderBuilder(properties.getIntegrator());

        ConnectSdkProperties.ShoppingCartExtension shoppingCartExtension = properties.getShoppingCartExtension();
        if (shoppingCartExtension != null) {
            String creator = shoppingCartExtension.getCreator();
            String name = shoppingCartExtension.getName();
            String version = shoppingCartExtension.getVersion();
            String extensionId = shoppingCartExtension.getExtensionId();
            if (extensionId == null) {
                builder.withShoppingCartExtension(new ShoppingCartExtension(creator, name, version));
            } else {
                builder.withShoppingCartExtension(new ShoppingCartExtension(creator, name, version, extensionId));
            }
        }

        customize(builder, customizers);

        return builder.build();
    }

    private void customize(MetadataProviderBuilder builder, List<MetadataProviderBuilderCustomizer> customizers) {
        for (MetadataProviderBuilderCustomizer customizer : customizers) {
            customizer.customize(builder);
        }
    }
}
