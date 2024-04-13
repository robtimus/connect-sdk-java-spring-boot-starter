/*
 * ConnectSdkMetadataProviderAutoConfigurationTest.java
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
import static org.mockito.Mockito.mock;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.worldline.connect.sdk.java.communication.MetadataProvider;
import com.worldline.connect.sdk.java.communication.MetadataProviderBuilder;
import com.worldline.connect.sdk.java.communication.RequestHeader;

@SuppressWarnings("nls")
class ConnectSdkMetadataProviderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkMetadataProviderAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, CustomizerConfiguration.class)
                .withPropertyValues("connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkMetadataProvider");
                    assertThat(context).hasSingleBean(MetadataProvider.class);
                    assertThat(context).getBean(MetadataProvider.class).isSameAs(context.getBean(ExistingBeanProvider.class).metadataProvider());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingProperties() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MetadataProvider.class);
                });
    }

    @Test
    void testAutoConfiguration() {
        contextRunner
                .withPropertyValues("connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkMetadataProvider");
                    assertThat(context).hasSingleBean(MetadataProvider.class);
                    MetadataProvider metadataProvider = context.getBean(MetadataProvider.class);
                    assertThat(metadataProvider).isExactlyInstanceOf(MetadataProvider.class);
                    Collection<RequestHeader> exectedMetadataHeaders = new MetadataProvider("Integrator").getServerMetadataHeaders();
                    assertThat((Object) metadataProvider.getServerMetadataHeaders()).hasToString(exectedMetadataHeaders.toString());
                });
        contextRunner
                .withUserConfiguration(CustomizerConfiguration.class)
                .withPropertyValues("connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkMetadataProvider");
                    assertThat(context).hasSingleBean(MetadataProvider.class);
                    MetadataProvider metadataProvider = context.getBean(MetadataProvider.class);
                    assertThat(metadataProvider).isExactlyInstanceOf(MetadataProvider.class);
                    Collection<RequestHeader> expectedMetadataHeaders = new MetadataProviderBuilder("Integrator")
                            .withAdditionalRequestHeader(new RequestHeader("custom-name", "custom-value"))
                            .build()
                            .getServerMetadataHeaders();
                    assertThat((Object) metadataProvider.getServerMetadataHeaders()).hasToString(expectedMetadataHeaders.toString());
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        MetadataProvider metadataProvider() {
            return mock(MetadataProvider.class);
        }
    }

    @Configuration
    static class CustomizerConfiguration {

        @Bean
        MetadataProviderBuilderCustomizer customizer() {
            return builder -> builder.withAdditionalRequestHeader(new RequestHeader("custom-name", "custom-value"));
        }
    }
}
