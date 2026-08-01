package com.furkan.democrudapi.service;

import com.furkan.democrudapi.exception.InvalidExplainQueryException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class ExplainService {

    private static final Pattern FORBIDDEN_KEYWORDS = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|GRANT|COPY)\\b",
            Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate jdbcTemplate;

    public ExplainService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String explain(String sql) {
        String trimmed = validate(sql);

        jdbcTemplate.execute("SET LOCAL statement_timeout = '5s'");
        return jdbcTemplate.queryForObject(
                "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + trimmed, String.class);
    }

    private String validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new InvalidExplainQueryException("Query must not be blank");
        }

        String trimmed = sql.trim();
        boolean startsWithSelect = trimmed.regionMatches(true, 0, "SELECT", 0, 6);
        boolean startsWithWith = trimmed.regionMatches(true, 0, "WITH", 0, 4);
        if (!startsWithSelect && !startsWithWith) {
            throw new InvalidExplainQueryException("Only SELECT or WITH queries are allowed");
        }

        if (trimmed.contains(";")) {
            throw new InvalidExplainQueryException("Query must not contain multiple statements");
        }

        Matcher matcher = FORBIDDEN_KEYWORDS.matcher(trimmed);
        if (matcher.find()) {
            throw new InvalidExplainQueryException(
                    "Query contains a forbidden keyword: " + matcher.group(1).toUpperCase());
        }

        return trimmed;
    }
}
