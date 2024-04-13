/*
 * ConnectSdkMarshallerAutoConfiguration.java
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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.worldline.connect.sdk.java.json.DefaultMarshaller;
import com.worldline.connect.sdk.java.json.Marshaller;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * <a href="https://github.com/Worldline-Global-Collectconnect-sdk-java/">connect-sdk-java</a>'s {@link Marshaller}.
 *
 * @author Rob Spoor
 */
@Configuration
@ConditionalOnMissingBean(Marshaller.class)
@SuppressWarnings("javadoc")
public class ConnectSdkMarshallerAutoConfiguration {

    @Bean
    public Marshaller connectSdkMarshaller() {
        return DefaultMarshaller.INSTANCE;
    }
}
