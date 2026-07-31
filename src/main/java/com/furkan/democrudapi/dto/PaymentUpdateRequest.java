package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.PaymentStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentUpdateRequest(
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull LocalDate dueDate,
        @NotNull PaymentStatus status
) {
}
