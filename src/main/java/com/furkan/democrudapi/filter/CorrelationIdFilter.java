package com.furkan.democrudapi.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.furkan.democrudapi.constants.CorrelationConstants.CORRELATION_ID_HEADER;
import static com.furkan.democrudapi.constants.CorrelationConstants.CORRELATION_ID_MDC_KEY;
import static com.furkan.democrudapi.constants.RequestLogConstants.DOC_PATH_PREFIXES;
import static com.furkan.democrudapi.constants.RequestLogConstants.INTERNAL_PATH_PREFIX;
import static com.furkan.democrudapi.constants.RequestLogConstants.REQUEST_COMPLETED_PREFIX;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId = StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString();

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Logged before the MDC is cleared, otherwise the line loses its correlation id.
            logRequestCompleted(request, response, startedAt);
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private void logRequestCompleted(HttpServletRequest request, HttpServletResponse response, long startedAt) {
        String path = request.getRequestURI();
        if (isNotRecorded(path)) {
            return;
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        log.info("{} method={} path={} status={} durationMs={}",
                REQUEST_COMPLETED_PREFIX, request.getMethod(), path, response.getStatus(), durationMs);
    }

    private boolean isNotRecorded(String path) {
        if (path.startsWith(INTERNAL_PATH_PREFIX)) {
            return true;
        }
        for (String prefix : DOC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
