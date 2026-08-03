package com.furkan.democrudapi.service;

import com.furkan.democrudapi.service.RequestLogParser.ParsedRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogParserTest {

    private final RequestLogParser parser = new RequestLogParser();

    @Test
    void shouldParseAllFieldsFromWellFormedLine() {
        Optional<ParsedRequest> result = parser.parse(
                "REQUEST_COMPLETED method=GET path=/customers status=200 durationMs=1180");

        assertThat(result).contains(new ParsedRequest("GET", "/customers", 200, 1180L));
    }

    @Test
    void shouldParsePathContainingQuerylessSegments() {
        Optional<ParsedRequest> result = parser.parse(
                "REQUEST_COMPLETED method=POST path=/api/customers/42/payments status=201 durationMs=7");

        assertThat(result).map(ParsedRequest::path).contains("/api/customers/42/payments");
    }

    @Test
    void shouldReturnEmptyWhenPrefixDoesNotMatch() {
        assertThat(parser.parse("select c1_0.id from customer c1_0")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenMessageIsNull() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenFieldIsMissing() {
        assertThat(parser.parse("REQUEST_COMPLETED method=GET status=200 durationMs=12")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenDurationIsNotNumeric() {
        assertThat(parser.parse("REQUEST_COMPLETED method=GET path=/customers status=200 durationMs=fast"))
                .isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenStatusIsNotNumeric() {
        assertThat(parser.parse("REQUEST_COMPLETED method=GET path=/customers status=OK durationMs=12")).isEmpty();
    }
}
