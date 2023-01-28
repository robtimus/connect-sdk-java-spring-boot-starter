# connect-sdk-java-spring-boot-starter
[![Maven Central](https://img.shields.io/maven-central/v/com.github.robtimus/connect-sdk-java-spring-boot-starter)](https://search.maven.org/artifact/com.github.robtimus/connect-sdk-java-spring-boot-starter)
[![Build Status](https://github.com/robtimus/connect-sdk-java-spring-boot-starter/actions/workflows/build.yml/badge.svg)](https://github.com/robtimus/connect-sdk-java-spring-boot-starter/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Aconnect-sdk-java-spring-boot-starter&metric=alert_status)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Aconnect-sdk-java-spring-boot-starter)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Aconnect-sdk-java-spring-boot-starter&metric=coverage)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Aconnect-sdk-java-spring-boot-starter)
[![Known Vulnerabilities](https://snyk.io/test/github/robtimus/connect-sdk-java-spring-boot-starter/badge.svg)](https://snyk.io/test/github/robtimus/connect-sdk-java-spring-boot-starter)

A Spring Boot starter project for [connect-sdk-java](https://github.com/ingenico-epayments/connect-sdk-java).

For more details, see https://robtimus.github.io/connect-sdk-java-spring-boot-starter/.

## Quick start

First, add this Spring Boot starter as a [dependency](https://robtimus.github.io/connect-sdk-java-spring-boot-starter/dependency-info.html) to your project.

Next, add the following properties to your `application.properties` or `application.yml` file:

    connect.api.endpoint.host=<API endpoint hostname>
    connect.api.integrator=<your company name>
    connect.api.api-key-id=<your API key id>
    connect.api.secret-api-key=<your secret API key>

With these simple steps a [Client](https://ingenico-epayments.github.io/connect-sdk-java/apidocs/latest/com/ingenico/connect/gateway/sdk/java/Client.html) will be auto-configured.

To also have a [MerchantClient](https://ingenico-epayments.github.io/connect-sdk-java/apidocs/latest/com/ingenico/connect/gateway/sdk/java/merchant/MerchantClient.html) auto-configured, add the following property as well:

    connect.api.merchant-id=<your merchand id>

### Actuators

If you have the `org.springframework.boot:spring-boot-starter-actuator` dependency in your project and have provided all of the above properties, including `connect.api.merchant-id`, then a [health indicator](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html#production-ready-health) will be auto-configured. This will use the [test connection](https://epayments-api.developer-ingenico.com/s2sapi/v1/en_US/java/services/testconnection.html?paymentPlatform=ALL#services-testconnection) functionality to check your connectivity to the Ingenico Server API.
To prevent too many requests to the Ingenico Server API this is throttled to at most 1 request every minute. This can be changed by adding the following property to your project:

    # Every 5 minutes
    connect.api.health.min-interval=300

If you want to have an auto-configured `MerchantClient` but you don't want this health indicator, you can disable it by adding the following property to your project:

    management.health.connect-sdk.enabled=false
