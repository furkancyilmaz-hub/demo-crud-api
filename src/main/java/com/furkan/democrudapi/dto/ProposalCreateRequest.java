package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.ProposalStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProposalCreateRequest(
        @NotBlank @Size(max = 30) String proposalNo,
        @NotNull ProposalStatus status,
        @NotNull LocalDate issueDate,
        @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal totalPremium
) {
}
