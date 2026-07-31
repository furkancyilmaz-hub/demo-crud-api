package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.CustomerStatus;

public record CustomerResponse(
        Long id,
        Long proposalId,
        String identityNo,
        String fullName,
        String city,
        CustomerStatus status
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getProposal().getId(),
                customer.getIdentityNo(),
                customer.getFullName(),
                customer.getCity(),
                customer.getStatus()
        );
    }
}
