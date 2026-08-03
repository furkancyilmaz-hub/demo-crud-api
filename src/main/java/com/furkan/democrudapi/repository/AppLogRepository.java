package com.furkan.democrudapi.repository;

import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.entity.LogLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AppLogRepository extends JpaRepository<AppLog, Long> {

    List<AppLog> findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(
            String correlationId, Collection<LogLevel> levels, Pageable pageable);

    List<AppLog> findByTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(
            Instant from, Instant to, Collection<LogLevel> levels, Pageable pageable);

    List<AppLog> findByCorrelationIdAndTimestampBetweenAndLevelInOrderByTimestampAscIdAsc(
            String correlationId, Instant from, Instant to, Collection<LogLevel> levels, Pageable pageable);

    List<AppLog> findByMessageStartingWithAndTimestampBetweenOrderByTimestampAscIdAsc(
            String messagePrefix, Instant from, Instant to, Pageable pageable);
}