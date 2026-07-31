package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.ProposalStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProposalUpdateRequest(
        @NotNull ProposalStatus status,
        @NotNull LocalDate issueDate,
        @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal totalPremium
) {
}
