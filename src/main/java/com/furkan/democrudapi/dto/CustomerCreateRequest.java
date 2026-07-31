package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.CustomerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
        @NotNull Long proposalId,
        @NotBlank @Size(max = 20) String identityNo,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Size(max = 40) String city,
        @NotNull CustomerStatus status
) {
}
