/*
 * ConnectSdkMetaDataProviderAutoConfigurationTest.java
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
import com.ingenico.connect.gateway.sdk.java.MetaDataProvider;
import com.ingenico.connect.gateway.sdk.java.MetaDataProviderBuilder;
import com.ingenico.connect.gateway.sdk.java.RequestHeader;

@SuppressWarnings("nls")
class ConnectSdkMetaDataProviderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkMetaDataProviderAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class, CustomizerConfiguration.class)
                .withPropertyValues("connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkMetaDataProvider");
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    assertThat(context).getBean(MetaDataProvider.class).isSameAs(context.getBean(ExistingBeanProvider.class).metaDataProvider());
                });
    }

    @Test
    void testNoAutoConfigurationWithMissingProperties() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MetaDataProvider.class);
                });
    }

    @Test
    void testAutoConfiguration() {
        contextRunner
                .withPropertyValues("connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkMetaDataProvider");
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    MetaDataProvider metaDataProvider = context.getBean(MetaDataProvider.class);
                    assertThat(metaDataProvider).isExactlyInstanceOf(MetaDataProvider.class);
                    Collection<RequestHeader> exectedMetaDataHeaders = new MetaDataProvider("Integrator").getServerMetaDataHeaders();
                    assertThat((Object) metaDataProvider.getServerMetaDataHeaders()).hasToString(exectedMetaDataHeaders.toString());
                });
        contextRunner
                .withUserConfiguration(CustomizerConfiguration.class)
                .withPropertyValues("connect.api.integrator=Integrator")
                .run(context -> {
                    assertThat(context).hasBean("connectSdkMetaDataProvider");
                    assertThat(context).hasSingleBean(MetaDataProvider.class);
                    MetaDataProvider metaDataProvider = context.getBean(MetaDataProvider.class);
                    assertThat(metaDataProvider).isExactlyInstanceOf(MetaDataProvider.class);
                    Collection<RequestHeader> expectedMetaDataHeaders = new MetaDataProviderBuilder("Integrator")
                            .withAdditionalRequestHeader(new RequestHeader("custom-name", "custom-value"))
                            .build()
                            .getServerMetaDataHeaders();
                    assertThat((Object) metaDataProvider.getServerMetaDataHeaders()).hasToString(expectedMetaDataHeaders.toString());
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        MetaDataProvider metaDataProvider() {
            return mock(MetaDataProvider.class);
        }
    }

    @Configuration
    static class CustomizerConfiguration {

        @Bean
        MetaDataProviderBuilderCustomizer customizer() {
            return builder -> builder.withAdditionalRequestHeader(new RequestHeader("custom-name", "custom-value"));
        }
    }
}
