package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.RequestSummaryResponse;
import com.furkan.democrudapi.entity.AppLog;
import com.furkan.democrudapi.repository.AppLogRepository;
import com.furkan.democrudapi.service.RequestLogParser.ParsedRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.furkan.democrudapi.constants.RequestLogConstants.REQUEST_COMPLETED_PREFIX;

@Service
@Transactional(readOnly = true)
public class RequestQueryService {

    private final AppLogRepository appLogRepository;
    private final RequestLogParser requestLogParser;

    public RequestQueryService(AppLogRepository appLogRepository, RequestLogParser requestLogParser) {
        this.appLogRepository = appLogRepository;
        this.requestLogParser = requestLogParser;
    }

    public List<RequestSummaryResponse> query(Instant from, Instant to, int limit) {
        List<AppLog> rows = appLogRepository.findByMessageStartingWithAndTimestampBetweenOrderByTimestampAscIdAsc(
                REQUEST_COMPLETED_PREFIX, from, to, PageRequest.of(0, limit));

        List<RequestSummaryResponse> summaries = new ArrayList<>(rows.size());
        for (AppLog row : rows) {
            Optional<ParsedRequest> parsed = requestLogParser.parse(row.getMessage());
            if (parsed.isEmpty()) {
                continue;
            }
            ParsedRequest request = parsed.get();
            summaries.add(new RequestSummaryResponse(row.getCorrelationId(), request.method(), request.path(),
                    request.status(), request.durationMs(), row.getTimestamp()));
        }
        return summaries;
    }
}
