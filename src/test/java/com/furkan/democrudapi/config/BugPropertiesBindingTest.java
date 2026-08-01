package com.furkan.democrudapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BugPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldBindNPlusOneFlagFromKebabCaseKey() {
        contextRunner.withPropertyValues("bug.n-plus-one=true")
                .run(context -> assertThat(context.getBean(BugProperties.class).isNPlusOne()).isTrue());
    }

    @Test
    void shouldDefaultToDisabledWhenKeyIsAbsent() {
        contextRunner.run(context -> assertThat(context.getBean(BugProperties.class).isNPlusOne()).isFalse());
    }

    @EnableConfigurationProperties(BugProperties.class)
    static class TestConfig {
    }
}