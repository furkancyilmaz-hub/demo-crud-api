package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.LogEntryResponse;
import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.entity.LogLevel;
import com.furkan.democrudapi.repository.AppLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogQueryServiceTest {

    private static final Instant FROM = Instant.parse("2026-01-01T09:00:00Z");
    private static final Instant TO = Instant.parse("2026-01-01T11:00:00Z");

    @Mock
    private AppLogRepository appLogRepository;

    @InjectMocks
    private LogQueryService logQueryService;

    @Test
    void shouldFilterByCorrelationIdWhenQuerying() {
        AppLog appLog = newAppLog(1L, "cid-1", Instant.parse("2026-01-01T10:00:00Z"), LogLevel.INFO, "message");
        when(appLogRepository.findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(
                eq("cid-1"), any(), eq(PageRequest.of(0, 100))))
                .thenReturn(List.of(appLog));

        List<LogEntryResponse> result = logQueryService.query("cid-1", null, null, null, 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("message");
    }

    @Test
    void shouldFilterByRangeOnlyWhenCorrelationIdAbsent() {
        when(appLogRepository.findByTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(
                eq(FROM), eq(TO), any(), eq(PageRequest.of(0, 100))))
                .thenReturn(List.of());

        logQueryService.query(null, FROM, TO, null, 100);

        verify(appLogRepository).findByTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(
                eq(FROM), eq(TO), any(), any());
        verifyNoMoreInteractions(appLogRepository);
    }

    @Test
    void shouldCombineCorrelationIdAndRangeWhenBothProvided() {
        when(appLogRepository.findByCorrelationIdAndTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(
                eq("cid-1"), eq(FROM), eq(TO), any(), eq(PageRequest.of(0, 100))))
                .thenReturn(List.of());

        logQueryService.query("cid-1", FROM, TO, null, 100);

        verify(appLogRepository).findByCorrelationIdAndTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(
                eq("cid-1"), eq(FROM), eq(TO), any(), any());
        verifyNoMoreInteractions(appLogRepository);
    }

    @Test
    void shouldExpandToWarnAndErrorWhenMinLevelIsWarn() {
        when(appLogRepository.findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(any(), any(), any()))
                .thenReturn(List.of());

        logQueryService.query("cid-1", null, null, LogLevel.WARN, 100);

        ArgumentCaptor<Collection<LogLevel>> levelsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(appLogRepository).findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(
                eq("cid-1"), levelsCaptor.capture(), any());
        assertThat(levelsCaptor.getValue()).containsExactly(LogLevel.WARN, LogLevel.ERROR);
    }

    @Test
    void shouldIncludeAllLevelsWhenMinLevelAbsent() {
        when(appLogRepository.findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(any(), any(), any()))
                .thenReturn(List.of());

        logQueryService.query("cid-1", null, null, null, 100);

        ArgumentCaptor<Collection<LogLevel>> levelsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(appLogRepository).findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(
                eq("cid-1"), levelsCaptor.capture(), any());
        assertThat(levelsCaptor.getValue()).containsExactly(LogLevel.values());
    }

    @Test
    void shouldPassLimitThroughAsPageSize() {
        when(appLogRepository.findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(any(), any(), any()))
                .thenReturn(List.of());

        logQueryService.query("cid-1", null, null, null, 25);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(appLogRepository).findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(
                eq("cid-1"), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 25));
    }

    @Test
    void shouldMapAppLogFieldsToLogEntryResponseInOrder() {
        AppLog first = newAppLog(1L, "cid-1", Instant.parse("2026-01-01T10:00:00Z"), LogLevel.INFO, "first");
        AppLog second = newAppLog(2L, "cid-1", Instant.parse("2026-01-01T10:00:01Z"), LogLevel.ERROR, "second");
        when(appLogRepository.findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(any(), any(), any()))
                .thenReturn(List.of(first, second));

        List<LogEntryResponse> result = logQueryService.query("cid-1", null, null, null, 100);

        assertThat(result).extracting(LogEntryResponse::message).containsExactly("first", "second");
        assertThat(result.get(1).level()).isEqualTo(LogLevel.ERROR);
    }

    @Test
    void shouldExposeThreadAndCorrelationIdOnEachEntry() {
        AppLog appLog = newAppLog(1L, "cid-1", Instant.parse("2026-01-01T10:00:00Z"), LogLevel.DEBUG, "select 1");
        when(appLogRepository.findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(any(), any(), any()))
                .thenReturn(List.of(appLog));

        List<LogEntryResponse> result = logQueryService.query("cid-1", null, null, null, 100);

        assertThat(result.get(0).thread()).isEqualTo("http-nio-8080-exec-1");
        assertThat(result.get(0).correlationId()).isEqualTo("cid-1");
    }

    @Test
    void shouldReturnEmptyListWhenNoRowsMatch() {
        when(appLogRepository.findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(any(), any(), any()))
                .thenReturn(List.of());

        List<LogEntryResponse> result = logQueryService.query("cid-1", null, null, null, 100);

        assertThat(result).isEmpty();
    }

    private AppLog newAppLog(Long id, String correlationId, Instant timestamp, LogLevel level, String message) {
        AppLog appLog = new AppLog(correlationId, timestamp, level, "com.furkan.Test", "http-nio-8080-exec-1",
                message);
        ReflectionTestUtils.setField(appLog, "id", id);
        return appLog;
    }
}
