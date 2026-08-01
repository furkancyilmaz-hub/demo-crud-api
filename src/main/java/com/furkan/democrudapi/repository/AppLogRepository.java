package com.furkan.democrudapi.repository;

import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.entity.LogLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AppLogRepository extends JpaRepository<AppLog, Long> {

    List<AppLog> findByCorrelationIdAndLevelInOrderByTimestampAscIdAsc(
            String correlationId, Collection<LogLevel> levels, Pageable pageable);
}