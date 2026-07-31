package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.PaymentCreateRequest;
import com.furkan.democrudapi.dto.PaymentResponse;
import com.furkan.democrudapi.dto.PaymentUpdateRequest;
import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.Payment;
import com.furkan.democrudapi.exception.InvalidReferenceException;
import com.furkan.democrudapi.exception.ResourceNotFoundException;
import com.furkan.democrudapi.repository.CustomerRepository;
import com.furkan.democrudapi.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    public PaymentService(PaymentRepository paymentRepository, CustomerRepository customerRepository) {
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public PaymentResponse create(PaymentCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new InvalidReferenceException("Customer not found: " + request.customerId()));
        Payment payment = new Payment(customer, request.amount(), request.dueDate(), request.status());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public PaymentResponse getById(Long id) {
        return PaymentResponse.from(findPaymentOrThrow(id));
    }

    public Page<PaymentResponse> list(Long customerId, Pageable pageable) {
        Page<Payment> page = customerId != null
                ? paymentRepository.findByCustomerId(customerId, pageable)
                : paymentRepository.findAll(pageable);
        return page.map(PaymentResponse::from);
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentUpdateRequest request) {
        Payment payment = findPaymentOrThrow(id);
        payment.updateDetails(request.amount(), request.dueDate(), request.status());
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = findPaymentOrThrow(id);
        paymentRepository.delete(payment);
        log.debug("Deleted payment={}", id);
    }

    private Payment findPaymentOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }
}
