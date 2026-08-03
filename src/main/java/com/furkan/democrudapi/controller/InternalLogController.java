package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.LogEntryResponse;
import com.furkan.democrudapi.entity.LogLevel;
import com.furkan.democrudapi.exception.InvalidLogQueryException;
import com.furkan.democrudapi.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/internal/logs")
@Validated
@Tag(name = "Internal — logs", description = "Application log records consumed by the N+1 analysis")
public class InternalLogController {

    private final LogQueryService logQueryService;

    public InternalLogController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @Operation(summary = "Query log records by correlation id and/or time range")
    @GetMapping
    public List<LogEntryResponse> query(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) LogLevel minLevel,
            @RequestParam(defaultValue = "100") @Min(1) @Max(5000) int limit) {
        String requestedCorrelationId = StringUtils.hasText(correlationId) ? correlationId : null;
        validateSelection(requestedCorrelationId, from, to);
        return logQueryService.query(requestedCorrelationId, from, to, minLevel, limit);
    }

    /**
     * Accepted combinations: correlationId alone, from+to alone, or all three (intersection).
     * A half-open range is rejected rather than silently widened.
     */
    private void validateSelection(String correlationId, Instant from, Instant to) {
        if (correlationId == null && from == null && to == null) {
            throw new InvalidLogQueryException("Either 'correlationId' or the 'from'/'to' range is required");
        }
        if ((from == null) != (to == null)) {
            throw new InvalidLogQueryException("Parameters 'from' and 'to' must be provided together");
        }
        if (from != null && from.isAfter(to)) {
            throw new InvalidLogQueryException("Parameter 'from' must not be after 'to'");
        }
    }
}
