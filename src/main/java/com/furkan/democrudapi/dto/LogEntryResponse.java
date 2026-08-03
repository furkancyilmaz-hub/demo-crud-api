package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.entity.LogLevel;

import java.time.Instant;

public record LogEntryResponse(
        String correlationId,
        Instant timestamp,
        LogLevel level,
        String logger,
        String thread,
        String message
) {
    public static LogEntryResponse from(AppLog appLog) {
        return new LogEntryResponse(appLog.getCorrelationId(), appLog.getTimestamp(), appLog.getLevel(),
                appLog.getLogger(), appLog.getThread(), appLog.getMessage());
    }
}
