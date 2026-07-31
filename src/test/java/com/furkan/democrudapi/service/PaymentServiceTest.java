package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.PaymentCreateRequest;
import com.furkan.democrudapi.dto.PaymentResponse;
import com.furkan.democrudapi.dto.PaymentUpdateRequest;
import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.CustomerStatus;
import com.furkan.democrudapi.entity.Payment;
import com.furkan.democrudapi.entity.PaymentStatus;
import com.furkan.democrudapi.entity.Proposal;
import com.furkan.democrudapi.entity.ProposalStatus;
import com.furkan.democrudapi.exception.InvalidReferenceException;
import com.furkan.democrudapi.exception.ResourceNotFoundException;
import com.furkan.democrudapi.repository.CustomerRepository;
import com.furkan.democrudapi.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentWhenCustomerExists() {
        Customer customer = newCustomer(1L);
        PaymentCreateRequest request = new PaymentCreateRequest(
                1L, BigDecimal.valueOf(100), LocalDate.now(), PaymentStatus.PENDING);
        Payment saved = newPayment(5L, customer, request);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponse response = paymentService.create(request);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.customerId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowInvalidReferenceExceptionWhenCustomerDoesNotExist() {
        PaymentCreateRequest request = new PaymentCreateRequest(
                99L, BigDecimal.valueOf(100), LocalDate.now(), PaymentStatus.PENDING);
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void shouldReturnPaymentWhenGetByIdWithExistingId() {
        Customer customer = newCustomer(1L);
        Payment payment = newPayment(5L, customer,
                new PaymentCreateRequest(1L, BigDecimal.valueOf(100), LocalDate.now(), PaymentStatus.PENDING));
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getById(5L);

        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaymentMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnPagedPaymentsFilteredByCustomerId() {
        Customer customer = newCustomer(1L);
        Payment payment = newPayment(5L, customer,
                new PaymentCreateRequest(1L, BigDecimal.valueOf(100), LocalDate.now(), PaymentStatus.PENDING));
        Page<Payment> page = new PageImpl<>(List.of(payment));
        when(paymentRepository.findByCustomerId(1L, PageRequest.of(0, 10))).thenReturn(page);

        Page<PaymentResponse> result = paymentService.list(1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldUpdatePaymentWhenIdExists() {
        Customer customer = newCustomer(1L);
        Payment payment = newPayment(5L, customer,
                new PaymentCreateRequest(1L, BigDecimal.valueOf(100), LocalDate.now(), PaymentStatus.PENDING));
        PaymentUpdateRequest request = new PaymentUpdateRequest(
                BigDecimal.valueOf(200), LocalDate.now().plusDays(1), PaymentStatus.PAID);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.update(5L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void shouldDeletePaymentWhenIdExists() {
        Customer customer = newCustomer(1L);
        Payment payment = newPayment(5L, customer,
                new PaymentCreateRequest(1L, BigDecimal.valueOf(100), LocalDate.now(), PaymentStatus.PENDING));
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));

        paymentService.delete(5L);

        verify(paymentRepository).delete(payment);
    }

    private Customer newCustomer(Long id) {
        Proposal proposal = new Proposal("PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        Customer customer = new Customer(proposal, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private Payment newPayment(Long id, Customer customer, PaymentCreateRequest request) {
        Payment payment = new Payment(customer, request.amount(), request.dueDate(), request.status());
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }
}
