package com.furkan.democrudapi.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DbLogAppender extends AppenderBase<ILoggingEvent> {

    private static final String INSERT_SQL =
            "INSERT INTO app_log (correlation_id, timestamp, level, logger, message) VALUES (?, ?, ?, ?, ?)";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final int CORRELATION_ID_MAX_LENGTH = 64;
    private static final int LOGGER_NAME_MAX_LENGTH = 200;

    @Override
    protected void append(ILoggingEvent event) {
        DataSource dataSource = LogDataSourceHolder.get();
        if (dataSource == null) {
            addError("DataSource not ready yet, dropping log event for app_log");
            return;
        }

        String correlationId = truncate(event.getMDCPropertyMap().get(CORRELATION_ID_MDC_KEY), CORRELATION_ID_MAX_LENGTH);
        String loggerName = truncate(event.getLoggerName(), LOGGER_NAME_MAX_LENGTH);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, correlationId);
            statement.setTimestamp(2, Timestamp.from(event.getInstant()));
            statement.setString(3, event.getLevel().toString());
            statement.setString(4, loggerName);
            statement.setString(5, event.getFormattedMessage());
            statement.executeUpdate();
        } catch (SQLException e) {
            addError("Failed to write log event to app_log", e);
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}