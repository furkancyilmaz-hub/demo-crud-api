package com.furkan.democrudapi.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DbLogAppender extends AppenderBase<ILoggingEvent> {

    private static final String INSERT_SQL =
            "INSERT INTO app_log (correlation_id, timestamp, level, logger, message) VALUES (?, ?, ?, ?, ?)";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final int CORRELATION_ID_MAX_LENGTH = 64;
    private static final int LOGGER_NAME_MAX_LENGTH = 200;

    private String url;
    private String username;
    private String password;

    private Connection connection;
    private PreparedStatement insertStatement;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void start() {
        try {
            openConnection();
            super.start();
        } catch (SQLException e) {
            addError("Failed to open DB connection for log appender", e);
        }
    }

    private void openConnection() throws SQLException {
        connection = DriverManager.getConnection(url, username, password);
        insertStatement = connection.prepareStatement(INSERT_SQL);
    }

    @Override
    public synchronized void stop() {
        closeQuietly();
        super.stop();
    }

    private void closeQuietly() {
        try {
            if (insertStatement != null) {
                insertStatement.close();
            }
        } catch (SQLException e) {
            addError("Failed to close DB log appender statement", e);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            addError("Failed to close DB log appender connection", e);
        }
    }

    @Override
    protected synchronized void append(ILoggingEvent event) {
        try {
            insert(event);
        } catch (SQLException firstFailure) {
            try {
                openConnection();
                insert(event);
            } catch (SQLException retryFailure) {
                addError("Failed to write log event to app_log", retryFailure);
            }
        }
    }

    private void insert(ILoggingEvent event) throws SQLException {
        String correlationId = truncate(event.getMDCPropertyMap().get(CORRELATION_ID_MDC_KEY), CORRELATION_ID_MAX_LENGTH);
        String loggerName = truncate(event.getLoggerName(), LOGGER_NAME_MAX_LENGTH);

        insertStatement.setString(1, correlationId);
        insertStatement.setTimestamp(2, Timestamp.from(event.getInstant()));
        insertStatement.setString(3, event.getLevel().toString());
        insertStatement.setString(4, loggerName);
        insertStatement.setString(5, event.getFormattedMessage());
        insertStatement.executeUpdate();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}