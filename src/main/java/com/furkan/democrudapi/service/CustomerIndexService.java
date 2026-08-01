package com.furkan.democrudapi.service;

import com.furkan.democrudapi.config.BugProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomerIndexService {

    private static final Logger log = LoggerFactory.getLogger(CustomerIndexService.class);

    private final JdbcTemplate jdbcTemplate;
    private final BugProperties bugProperties;

    public CustomerIndexService(JdbcTemplate jdbcTemplate, BugProperties bugProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.bugProperties = bugProperties;
    }

    /**
     * Creates or drops {@code idx_customer_city} to flip SP009 at runtime, no restart needed.
     * {@code CONCURRENTLY} is deliberately not used: it cannot run inside a transaction.
     */
    public void setMissingIndexEnabled(boolean enabled) {
        if (enabled == bugProperties.isMissingIndex()) {
            return;
        }
        if (enabled) {
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_customer_city");
        } else {
            jdbcTemplate.execute("CREATE INDEX idx_customer_city ON customer(city)");
            jdbcTemplate.execute("ANALYZE customer");
        }
        bugProperties.setMissingIndex(enabled);
        log.info("Toggled bug flag missing-index to {}", enabled);
    }
}