package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.LogEntryResponse;
import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.entity.LogLevel;
import com.furkan.democrudapi.repository.AppLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LogQueryService {

    private final AppLogRepository appLogRepository;

    public LogQueryService(AppLogRepository appLogRepository) {
        this.appLogRepository = appLogRepository;
    }

    /**
     * Either {@code correlationId} or the {@code from}/{@code to} range must be given; the caller
     * validates that. Rows are always ordered by {@code timestamp, id} so that lines landing in the
     * same millisecond keep their emission order.
     */
    public List<LogEntryResponse> query(String correlationId, Instant from, Instant to, LogLevel minLevel,
                                        int limit) {
        List<LogLevel> levels = minLevel != null ? LogLevel.atLeast(minLevel) : List.of(LogLevel.values());
        Pageable pageable = PageRequest.of(0, limit);
        List<AppLog> rows = findRows(correlationId, from, to, levels, pageable);
        return rows.stream()
                .map(LogEntryResponse::from)
                .toList();
    }

    private List<AppLog> findRows(String correlationId, Instant from, Instant to, List<LogLevel> levels,
                                  Pageable pageable) {
        if (correlationId == null) {
            return appLogRepository
                    .findByTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(from, to, levels, pageable);
        }
        if (from == null) {
            return appLogRepository
                    .findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(correlationId, levels, pageable);
        }
        return appLogRepository.findByCorrelationIdAndTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(
                correlationId, from, to, levels, pageable);
    }
}
