package com.furkan.democrudapi.dto;

import java.time.Instant;

public record RequestSummaryResponse(
        String correlationId,
        String method,
        String path,
        int status,
        long durationMs,
        Instant timestamp
) {
}
