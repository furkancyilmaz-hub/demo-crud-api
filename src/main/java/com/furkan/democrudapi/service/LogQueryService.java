package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.LogEntryResponse;
import com.furkan.democrudapi.entity.LogLevel;
import com.furkan.democrudapi.repository.AppLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LogQueryService {

    private final AppLogRepository appLogRepository;

    public LogQueryService(AppLogRepository appLogRepository) {
        this.appLogRepository = appLogRepository;
    }

    public List<LogEntryResponse> query(String correlationId, LogLevel minLevel, int limit) {
        List<LogLevel> levels = minLevel != null ? LogLevel.atLeast(minLevel) : List.of(LogLevel.values());
        return appLogRepository
                .findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(correlationId, levels, PageRequest.of(0, limit))
                .stream()
                .map(LogEntryResponse::from)
                .toList();
    }
}