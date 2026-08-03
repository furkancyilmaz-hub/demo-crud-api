package com.furkan.democrudapi.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.furkan.democrudapi.constants.RequestLogConstants.REQUEST_COMPLETED_PREFIX;

/**
 * Parses the fixed {@code REQUEST_COMPLETED method=… path=… status=… durationMs=…} log line
 * back into its fields. Lines that do not match are skipped rather than rejected: app_log also
 * holds unrelated rows and a single malformed line must not fail the whole query.
 */
@Component
public class RequestLogParser {

    private static final String METHOD_KEY = "method";
    private static final String PATH_KEY = "path";
    private static final String STATUS_KEY = "status";
    private static final String DURATION_KEY = "durationMs";

    public Optional<ParsedRequest> parse(String message) {
        if (message == null || !message.startsWith(REQUEST_COMPLETED_PREFIX)) {
            return Optional.empty();
        }

        Map<String, String> fields = new HashMap<>();
        for (String token : message.substring(REQUEST_COMPLETED_PREFIX.length()).trim().split(" ")) {
            int separator = token.indexOf('=');
            if (separator > 0) {
                fields.put(token.substring(0, separator), token.substring(separator + 1));
            }
        }

        String method = fields.get(METHOD_KEY);
        String path = fields.get(PATH_KEY);
        if (method == null || path == null) {
            return Optional.empty();
        }

        try {
            int status = Integer.parseInt(fields.getOrDefault(STATUS_KEY, ""));
            long durationMs = Long.parseLong(fields.getOrDefault(DURATION_KEY, ""));
            return Optional.of(new ParsedRequest(method, path, status, durationMs));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record ParsedRequest(String method, String path, int status, long durationMs) {
    }
}
