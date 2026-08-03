package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.RequestSummaryResponse;
import com.furkan.democrudapi.exception.InvalidLogQueryException;
import com.furkan.democrudapi.service.RequestQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@Validated
public class InternalRequestController {

    private final RequestQueryService requestQueryService;

    public InternalRequestController(RequestQueryService requestQueryService) {
        this.requestQueryService = requestQueryService;
    }

    @GetMapping
    public List<RequestSummaryResponse> query(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(5000) int limit) {
        if (from.isAfter(to)) {
            throw new InvalidLogQueryException("Parameter 'from' must not be after 'to'");
        }
        return requestQueryService.query(from, to, limit);
    }
}
