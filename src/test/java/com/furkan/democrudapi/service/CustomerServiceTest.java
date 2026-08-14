package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.CustomerCreateRequest;
import com.furkan.democrudapi.dto.CustomerResponse;
import com.furkan.democrudapi.dto.CustomerUpdateRequest;
import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.CustomerStatus;
import com.furkan.democrudapi.entity.Payment;
import com.furkan.democrudapi.entity.PaymentStatus;
import com.furkan.democrudapi.entity.Proposal;
import com.furkan.democrudapi.entity.ProposalStatus;
import com.furkan.democrudapi.exception.InvalidReferenceException;
import com.furkan.democrudapi.exception.ResourceInUseException;
import com.furkan.democrudapi.exception.ResourceNotFoundException;
import com.furkan.democrudapi.repository.CustomerRepository;
import com.furkan.democrudapi.repository.PaymentRepository;
import com.furkan.democrudapi.repository.ProposalRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldCreateCustomerWhenProposalExists() {
        Proposal proposal = newProposal(1L);
        CustomerCreateRequest request = new CustomerCreateRequest(
                1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE);
        Customer saved = newCustomer(10L, proposal, request);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse response = customerService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.proposalId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowInvalidReferenceExceptionWhenProposalDoesNotExist() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                99L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE);
        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void shouldReturnCustomerWhenGetByIdWithExistingId() {
        Proposal proposal = newProposal(1L);
        Customer customer = newCustomer(10L, proposal,
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getById(10L);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCustomerMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnPagedCustomersFilteredByProposalId() {
        Proposal proposal = newProposal(1L);
        Customer customer = newCustomer(10L, proposal,
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE));
        Page<Customer> page = new PageImpl<>(List.of(customer));
        when(customerRepository.findByProposalId(1L, PageRequest.of(0, 10))).thenReturn(page);

        Page<CustomerResponse> result = customerService.list(1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldReturnPagedCustomersFilteredByCity() {
        Proposal proposal = newProposal(1L);
        Customer customer = newCustomer(10L, proposal,
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Ankara", CustomerStatus.ACTIVE));
        Page<Customer> page = new PageImpl<>(List.of(customer));
        when(customerRepository.findByCity("Ankara", PageRequest.of(0, 10))).thenReturn(page);

        Page<CustomerResponse> result = customerService.searchByCity("Ankara", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().city()).isEqualTo("Ankara");
    }

    @Test
    void shouldReturnPagedCustomersFilteredByProposalIdAndIdentityNo() {
        Proposal proposal = newProposal(1L);
        Customer customer = newCustomer(10L, proposal,
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE));
        when(customerRepository.findByProposalIdAndIdentityNo(1L, "12345678901", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        Page<CustomerResponse> result = customerService.searchByIdentityNo(1L, "12345678901", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().proposalId()).isEqualTo(1L);
    }

    @Test
    void shouldReturnEmptyPageWhenNoCustomerMatchesIdentityNoInProposal() {
        when(customerRepository.findByProposalIdAndIdentityNo(1L, "00000000000", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<CustomerResponse> result = customerService.searchByIdentityNo(1L, "00000000000", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldNotLoadPaymentsWhenListingPlain() {
        Customer customer = newCustomerWithPayments();
        when(customerRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        Page<CustomerResponse> result = customerService.list(null, PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().payments()).isEmpty();
        verify(customerRepository, never()).findByIdIn(anyCollection());
    }

    @Test
    void shouldReadPaymentsFromEachCustomerWhenListingDetail() {
        Customer customer = newCustomerWithPayments();
        when(customerRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        Page<CustomerResponse> result = customerService.listDetail(null, PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().payments()).hasSize(2);
        verify(customerRepository, never()).findByIdIn(anyCollection());
    }

    @Test
    void shouldLoadPaymentsInOneQueryWhenListingOverview() {
        Customer customer = newCustomerWithPayments();
        when(customerRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(customerRepository.findByIdIn(List.of(10L))).thenReturn(List.of(customer));

        Page<CustomerResponse> result = customerService.listOverview(null, PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().payments()).hasSize(2);
        verify(customerRepository).findByIdIn(List.of(10L));
    }

    @Test
    void shouldReturnIdenticalContentFromDetailAndOverview() {
        Customer customer = newCustomerWithPayments();
        when(customerRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(customerRepository.findByIdIn(List.of(10L))).thenReturn(List.of(customer));

        List<CustomerResponse> detail = customerService.listDetail(null, PageRequest.of(0, 10)).getContent();
        List<CustomerResponse> overview = customerService.listOverview(null, PageRequest.of(0, 10)).getContent();

        assertThat(detail).isEqualTo(overview);
    }

    @Test
    void shouldUpdateCustomerWhenIdExists() {
        Proposal proposal = newProposal(1L);
        Customer customer = newCustomer(10L, proposal,
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE));
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "98765432109", "Jane Smith", "Ankara", CustomerStatus.PASSIVE);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.update(10L, request);

        assertThat(response.fullName()).isEqualTo("Jane Smith");
        assertThat(response.status()).isEqualTo(CustomerStatus.PASSIVE);
    }

    @Test
    void shouldDeleteCustomerWhenNoPaymentsExist() {
        Proposal proposal = newProposal(1L);
        Customer customer = newCustomer(10L, proposal,
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(paymentRepository.existsByCustomerId(10L)).thenReturn(false);

        customerService.delete(10L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void shouldThrowResourceInUseExceptionWhenDeletingCustomerWithPayments() {
        Proposal proposal = newProposal(1L);
        Customer customer = newCustomer(10L, proposal,
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(paymentRepository.existsByCustomerId(10L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.delete(10L))
                .isInstanceOf(ResourceInUseException.class);
    }

    private Proposal newProposal(Long id) {
        Proposal proposal = new Proposal("PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        ReflectionTestUtils.setField(proposal, "id", id);
        return proposal;
    }

    private Customer newCustomer(Long id, Proposal proposal, CustomerCreateRequest request) {
        Customer customer = new Customer(proposal, request.identityNo(), request.fullName(), request.city(),
                request.status());
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private Customer newCustomerWithPayments() {
        Customer customer = newCustomer(10L, newProposal(1L),
                new CustomerCreateRequest(1L, "12345678901", "Jane Doe", "Istanbul", CustomerStatus.ACTIVE));
        ReflectionTestUtils.setField(customer, "payments",
                List.of(newPayment(100L, customer), newPayment(101L, customer)));
        return customer;
    }

    private Payment newPayment(Long id, Customer customer) {
        Payment payment = new Payment(customer, new BigDecimal("125.50"), LocalDate.now(), PaymentStatus.PENDING);
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }
}
