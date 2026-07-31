package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.Payment;
import com.furkan.democrudapi.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long customerId,
        BigDecimal amount,
        LocalDate dueDate,
        PaymentStatus status
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCustomer().getId(),
                payment.getAmount(),
                payment.getDueDate(),
                payment.getStatus()
        );
    }
}
