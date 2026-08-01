package com.furkan.democrudapi.entity;

import java.util.Arrays;
import java.util.List;

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    public static List<LogLevel> atLeast(LogLevel minLevel) {
        return Arrays.stream(values())
                .filter(level -> level.ordinal() >= minLevel.ordinal())
                .toList();
    }
}