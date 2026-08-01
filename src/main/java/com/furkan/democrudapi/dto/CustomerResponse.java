package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.CustomerStatus;
import com.furkan.democrudapi.entity.Payment;

import java.util.List;

public record CustomerResponse(
        Long id,
        Long proposalId,
        String identityNo,
        String fullName,
        String city,
        CustomerStatus status,
        List<PaymentResponse> payments
) {
    public static CustomerResponse from(Customer customer) {
        return from(customer, List.of());
    }

    public static CustomerResponse from(Customer customer, List<Payment> payments) {
        return new CustomerResponse(
                customer.getId(),
                customer.getProposal().getId(),
                customer.getIdentityNo(),
                customer.getFullName(),
                customer.getCity(),
                customer.getStatus(),
                payments.stream().map(PaymentResponse::from).toList()
        );
    }
}