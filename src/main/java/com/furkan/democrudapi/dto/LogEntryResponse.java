package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.entity.LogLevel;

import java.time.Instant;

public record LogEntryResponse(
        Instant timestamp,
        LogLevel level,
        String logger,
        String message
) {
    public static LogEntryResponse from(AppLog appLog) {
        return new LogEntryResponse(appLog.getTimestamp(), appLog.getLevel(), appLog.getLogger(),
                appLog.getMessage());
    }
}