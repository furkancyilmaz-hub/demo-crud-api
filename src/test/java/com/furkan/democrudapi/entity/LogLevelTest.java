package com.furkan.democrudapi.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogLevelTest {

    @Test
    void shouldReturnOnlyErrorWhenMinLevelIsError() {
        assertThat(LogLevel.atLeast(LogLevel.ERROR)).containsExactly(LogLevel.ERROR);
    }

    @Test
    void shouldReturnWarnAndErrorWhenMinLevelIsWarn() {
        assertThat(LogLevel.atLeast(LogLevel.WARN)).containsExactly(LogLevel.WARN, LogLevel.ERROR);
    }

    @Test
    void shouldReturnAllLevelsWhenMinLevelIsTrace() {
        assertThat(LogLevel.atLeast(LogLevel.TRACE))
                .containsExactly(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR);
    }
}