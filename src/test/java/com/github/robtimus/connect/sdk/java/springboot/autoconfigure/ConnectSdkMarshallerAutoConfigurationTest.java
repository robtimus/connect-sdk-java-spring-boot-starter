/*
 * ConnectSdkMarshallerAutoConfigurationTest.java
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ingenico.connect.gateway.sdk.java.Marshaller;
import com.ingenico.connect.gateway.sdk.java.defaultimpl.DefaultMarshaller;

@SuppressWarnings("nls")
class ConnectSdkMarshallerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConnectSdkMarshallerAutoConfiguration.class));

    @Test
    void testNoAutoConfigurationWithExistingBean() {
        contextRunner
                .withUserConfiguration(ExistingBeanProvider.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean("connectSdkMarshaller");
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).getBean(Marshaller.class).isSameAs(context.getBean(ExistingBeanProvider.class).marshaller());
                });
    }

    @Test
    void testAutoConfiguration() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasBean("connectSdkMarshaller");
                    assertThat(context).hasSingleBean(Marshaller.class);
                    assertThat(context).getBean(Marshaller.class).isSameAs(DefaultMarshaller.INSTANCE);
                });
    }

    @Configuration
    static class ExistingBeanProvider {

        @Bean
        Marshaller marshaller() {
            return mock(Marshaller.class);
        }
    }
}
