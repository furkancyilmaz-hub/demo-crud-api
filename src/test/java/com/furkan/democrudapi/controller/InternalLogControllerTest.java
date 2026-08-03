package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.LogEntryResponse;
import com.furkan.democrudapi.entity.LogLevel;
import com.furkan.democrudapi.service.LogQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalLogController.class)
class InternalLogControllerTest {

    private static final String FROM = "2026-01-01T09:00:00Z";
    private static final String TO = "2026-01-01T11:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogQueryService logQueryService;

    @Test
    void shouldReturn200WithBodyWhenCorrelationIdProvided() throws Exception {
        when(logQueryService.query(eq("cid-1"), isNull(), isNull(), isNull(), eq(100)))
                .thenReturn(List.of(new LogEntryResponse("cid-1", Instant.parse("2026-01-01T10:00:00Z"),
                        LogLevel.INFO, "com.furkan.Test", "http-nio-8080-exec-1", "hello")));

        mockMvc.perform(get("/internal/logs").param("correlationId", "cid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("hello"))
                .andExpect(jsonPath("$[0].thread").value("http-nio-8080-exec-1"))
                .andExpect(jsonPath("$[0].correlationId").value("cid-1"));
    }

    @Test
    void shouldReturn200WhenMinLevelProvided() throws Exception {
        when(logQueryService.query(eq("cid-1"), isNull(), isNull(), eq(LogLevel.WARN), eq(100)))
                .thenReturn(List.of());

        mockMvc.perform(get("/internal/logs")
                        .param("correlationId", "cid-1")
                        .param("minLevel", "WARN"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200WhenRangeProvidedWithoutCorrelationId() throws Exception {
        when(logQueryService.query(isNull(), eq(Instant.parse(FROM)), eq(Instant.parse(TO)), isNull(), eq(100)))
                .thenReturn(List.of());

        mockMvc.perform(get("/internal/logs").param("from", FROM).param("to", TO))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAcceptLimitUpToNewHardCap() throws Exception {
        when(logQueryService.query(any(), any(), any(), any(), eq(5000))).thenReturn(List.of());

        mockMvc.perform(get("/internal/logs")
                        .param("correlationId", "cid-1")
                        .param("limit", "5000"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenLimitExceedsHardCap() throws Exception {
        mockMvc.perform(get("/internal/logs")
                        .param("correlationId", "cid-1")
                        .param("limit", "5001"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(logQueryService);
    }

    @Test
    void shouldReturn400WhenNeitherCorrelationIdNorRangeProvided() throws Exception {
        mockMvc.perform(get("/internal/logs"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(logQueryService);
    }

    @Test
    void shouldReturn400WhenRangeIsHalfOpen() throws Exception {
        mockMvc.perform(get("/internal/logs").param("from", FROM))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(logQueryService);
    }

    @Test
    void shouldReturn400WhenFromIsAfterTo() throws Exception {
        mockMvc.perform(get("/internal/logs").param("from", TO).param("to", FROM))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(logQueryService);
    }
}
