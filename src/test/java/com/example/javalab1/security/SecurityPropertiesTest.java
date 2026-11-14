package com.example.javalab1.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues("market.security.issuer=http://localhost:8090",
                    "market.security.jwk-set-uri=http://localhost:8090/.well-known/jwks.json",
                    "market.security.audience=cosmo-cats-api");

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityProperties.class)
    static class PropertiesConfiguration {}

    @Test
    void missingApiKeyFailsStartupInsteadOfEnablingAnonymousAccess() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankApiKeyFailsStartup() {
        runner.withPropertyValues("market.security.api-key=   ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void explicitKeyAllowsBinding() {
        runner.withPropertyValues("market.security.api-key=local-test-key").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(SecurityProperties.class).apiKey()).isEqualTo("local-test-key");
        });
    }
}
