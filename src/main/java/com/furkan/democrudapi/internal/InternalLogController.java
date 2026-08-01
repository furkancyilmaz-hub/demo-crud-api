package com.furkan.democrudapi.internal;

import com.furkan.democrudapi.dto.LogEntryResponse;
import com.furkan.democrudapi.entity.LogLevel;
import com.furkan.democrudapi.service.LogQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/logs")
@Validated
public class InternalLogController {

    private final LogQueryService logQueryService;

    public InternalLogController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping
    public List<LogEntryResponse> query(
            @RequestParam @NotBlank String correlationId,
            @RequestParam(required = false) LogLevel minLevel,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return logQueryService.query(correlationId, minLevel, limit);
    }
}