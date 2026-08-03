package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.RequestSummaryResponse;
import com.furkan.democrudapi.service.RequestQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalRequestController.class)
class InternalRequestControllerTest {

    private static final String FROM = "2026-01-01T09:00:00Z";
    private static final String TO = "2026-01-01T11:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestQueryService requestQueryService;

    @Test
    void shouldReturnCompletedRequestsWithinRange() throws Exception {
        when(requestQueryService.query(eq(Instant.parse(FROM)), eq(Instant.parse(TO)), eq(1000)))
                .thenReturn(List.of(new RequestSummaryResponse("cid-1", "GET", "/api/customers/detail", 200, 1180L,
                        Instant.parse("2026-01-01T10:00:00Z"))));

        mockMvc.perform(get("/internal/requests").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correlationId").value("cid-1"))
                .andExpect(jsonPath("$[0].path").value("/api/customers/detail"))
                .andExpect(jsonPath("$[0].status").value(200))
                .andExpect(jsonPath("$[0].durationMs").value(1180));
    }

    @Test
    void shouldReturn400WhenRangeMissing() throws Exception {
        mockMvc.perform(get("/internal/requests"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(requestQueryService);
    }

    @Test
    void shouldReturn400WhenFromIsAfterTo() throws Exception {
        mockMvc.perform(get("/internal/requests").param("from", TO).param("to", FROM))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(requestQueryService);
    }

    @Test
    void shouldReturn400WhenTimestampIsNotIso8601() throws Exception {
        mockMvc.perform(get("/internal/requests").param("from", "yesterday").param("to", TO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(requestQueryService);
    }

    @Test
    void shouldReturn400WhenLimitExceedsHardCap() throws Exception {
        mockMvc.perform(get("/internal/requests").param("from", FROM).param("to", TO).param("limit", "5001"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(requestQueryService);
    }

    @Test
    void shouldPassLimitThroughWhenProvided() throws Exception {
        when(requestQueryService.query(any(), any(), eq(250))).thenReturn(List.of());

        mockMvc.perform(get("/internal/requests").param("from", FROM).param("to", TO).param("limit", "250"))
                .andExpect(status().isOk());
    }
}
