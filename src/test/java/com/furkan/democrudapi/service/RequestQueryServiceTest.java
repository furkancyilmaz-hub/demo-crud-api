package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.RequestSummaryResponse;
import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.entity.LogLevel;
import com.furkan.democrudapi.repository.AppLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestQueryServiceTest {

    private static final Instant FROM = Instant.parse("2026-01-01T09:00:00Z");
    private static final Instant TO = Instant.parse("2026-01-01T11:00:00Z");

    @Mock
    private AppLogRepository appLogRepository;

    private RequestQueryService requestQueryService;

    @BeforeEach
    void setUp() {
        requestQueryService = new RequestQueryService(appLogRepository, new RequestLogParser());
    }

    @Test
    void shouldMapRequestCompletedLineToSummary() {
        AppLog row = newAppLog(1L, "cid-1", Instant.parse("2026-01-01T10:00:00Z"),
                "REQUEST_COMPLETED method=GET path=/customers status=200 durationMs=1180");
        when(appLogRepository.findByMessageStartingWithAndTimestampBetweenOrderByTimestampAscIdAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of(row));

        List<RequestSummaryResponse> result = requestQueryService.query(FROM, TO, 1000);

        assertThat(result).containsExactly(new RequestSummaryResponse("cid-1", "GET", "/customers", 200, 1180L,
                Instant.parse("2026-01-01T10:00:00Z")));
    }

    @Test
    void shouldSkipRowsThatDoNotParse() {
        AppLog valid = newAppLog(1L, "cid-1", Instant.parse("2026-01-01T10:00:00Z"),
                "REQUEST_COMPLETED method=GET path=/customers status=200 durationMs=5");
        AppLog malformed = newAppLog(2L, "cid-2", Instant.parse("2026-01-01T10:00:01Z"),
                "REQUEST_COMPLETED method=GET status=200");
        when(appLogRepository.findByMessageStartingWithAndTimestampBetweenOrderByTimestampAscIdAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of(valid, malformed));

        List<RequestSummaryResponse> result = requestQueryService.query(FROM, TO, 1000);

        assertThat(result).extracting(RequestSummaryResponse::correlationId).containsExactly("cid-1");
    }

    @Test
    void shouldQueryOnlyRequestCompletedRowsWithinRange() {
        when(appLogRepository.findByMessageStartingWithAndTimestampBetweenOrderByTimestampAscIdAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        requestQueryService.query(FROM, TO, 250);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(appLogRepository).findByMessageStartingWithAndTimestampBetweenOrderByTimestampAscIdAsc(
                eq("REQUEST_COMPLETED"), eq(FROM), eq(TO), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 250));
    }

    @Test
    void shouldReturnEmptyListWhenNoRequestsInRange() {
        when(appLogRepository.findByMessageStartingWithAndTimestampBetweenOrderByTimestampAscIdAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThat(requestQueryService.query(FROM, TO, 1000)).isEmpty();
    }

    private AppLog newAppLog(Long id, String correlationId, Instant timestamp, String message) {
        AppLog appLog = new AppLog(correlationId, timestamp, LogLevel.INFO,
                "com.furkan.democrudapi.filter.CorrelationIdFilter", "http-nio-8080-exec-1", message);
        ReflectionTestUtils.setField(appLog, "id", id);
        return appLog;
    }
}
