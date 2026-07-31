package com.furkan.democrudapi.exception;

import java.time.Instant;
import java.util.List;

public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        List<FieldViolation> violations
) {
    public static ValidationErrorResponse of(int status, String error, List<FieldViolation> violations) {
        return new ValidationErrorResponse(Instant.now(), status, error, violations);
    }

    public record FieldViolation(String field, String message) {
    }
}
