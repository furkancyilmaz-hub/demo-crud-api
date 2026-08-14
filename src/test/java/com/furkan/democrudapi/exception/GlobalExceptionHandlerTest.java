package com.furkan.democrudapi.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapResourceNotFoundExceptionTo404() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Proposal not found: 1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Proposal not found: 1");
    }

    @Test
    void shouldMapInvalidReferenceExceptionTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidReference(
                new InvalidReferenceException("Proposal not found: 1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldMapInvalidSearchQueryExceptionTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidQuery(
                new InvalidSearchQueryException("Either 'city' or the 'proposalId'/'identityNo' pair is required"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("identityNo");
    }

    @Test
    void shouldMapInvalidLogQueryExceptionTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidQuery(
                new InvalidLogQueryException("Parameter 'from' must not be after 'to'"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldMapDuplicateProposalNoExceptionTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new DuplicateProposalNoException("Proposal no already exists: PN-001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldMapResourceInUseExceptionTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new ResourceInUseException("Proposal has existing customers: 1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldMapMissingServletRequestParameterExceptionTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("correlationId", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("correlationId");
    }

    @Test
    void shouldMapConstraintViolationExceptionTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldMapDataIntegrityViolationExceptionTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("constraint violated"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldMapUnexpectedExceptionTo500WithoutLeakingInternalDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("sensitive internal detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("sensitive internal detail");
    }
}
