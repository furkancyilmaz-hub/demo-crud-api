package com.furkan.democrudapi.service;

import com.furkan.democrudapi.config.BugProperties;
import com.furkan.democrudapi.dto.CustomerCreateRequest;
import com.furkan.democrudapi.dto.CustomerResponse;
import com.furkan.democrudapi.dto.CustomerUpdateRequest;
import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.Payment;
import com.furkan.democrudapi.entity.Proposal;
import com.furkan.democrudapi.exception.InvalidReferenceException;
import com.furkan.democrudapi.exception.ResourceInUseException;
import com.furkan.democrudapi.exception.ResourceNotFoundException;
import com.furkan.democrudapi.repository.CustomerRepository;
import com.furkan.democrudapi.repository.PaymentRepository;
import com.furkan.democrudapi.repository.ProposalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final ProposalRepository proposalRepository;
    private final PaymentRepository paymentRepository;
    private final BugProperties bugProperties;

    public CustomerService(CustomerRepository customerRepository, ProposalRepository proposalRepository,
                            PaymentRepository paymentRepository, BugProperties bugProperties) {
        this.customerRepository = customerRepository;
        this.proposalRepository = proposalRepository;
        this.paymentRepository = paymentRepository;
        this.bugProperties = bugProperties;
    }

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        Proposal proposal = proposalRepository.findById(request.proposalId())
                .orElseThrow(() -> new InvalidReferenceException("Proposal not found: " + request.proposalId()));
        Customer customer = new Customer(
                proposal, request.identityNo(), request.fullName(), request.city(), request.status());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    public CustomerResponse getById(Long id) {
        return CustomerResponse.from(findCustomerOrThrow(id));
    }

    public Page<CustomerResponse> searchByCity(String city, Pageable pageable) {
        return customerRepository.findByCity(city, pageable).map(CustomerResponse::from);
    }

    public Page<CustomerResponse> list(Long proposalId, boolean withPayments, Pageable pageable) {
        Page<Customer> page = proposalId != null
                ? customerRepository.findByProposalId(proposalId, pageable)
                : customerRepository.findAll(pageable);
        if (!withPayments) {
            return page.map(CustomerResponse::from);
        }
        Map<Long, List<Payment>> paymentsByCustomer = bugProperties.isNPlusOne()
                ? loadPaymentsPerCustomer(page.getContent())
                : loadPaymentsInSingleQuery(page.getContent());
        return page.map(customer -> CustomerResponse.from(
                customer, paymentsByCustomer.getOrDefault(customer.getId(), List.of())));
    }

    /**
     * Deliberate N+1 (SP008): initializing the lazy collection one customer at a time
     * runs a separate "select ... from payment where customer_id=?" per row.
     */
    private Map<Long, List<Payment>> loadPaymentsPerCustomer(List<Customer> customers) {
        Map<Long, List<Payment>> paymentsByCustomer = new LinkedHashMap<>();
        for (Customer customer : customers) {
            paymentsByCustomer.put(customer.getId(), List.copyOf(customer.getPayments()));
        }
        return paymentsByCustomer;
    }

    /**
     * Loads every payment of the page in one query, so the collections are already
     * initialized by the time they are read.
     */
    private Map<Long, List<Payment>> loadPaymentsInSingleQuery(List<Customer> customers) {
        if (customers.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = customers.stream().map(Customer::getId).toList();
        Map<Long, List<Payment>> paymentsByCustomer = new LinkedHashMap<>();
        for (Customer customer : customerRepository.findByIdIn(ids)) {
            paymentsByCustomer.put(customer.getId(), List.copyOf(customer.getPayments()));
        }
        return paymentsByCustomer;
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerUpdateRequest request) {
        Customer customer = findCustomerOrThrow(id);
        customer.updateDetails(request.identityNo(), request.fullName(), request.city(), request.status());
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findCustomerOrThrow(id);
        if (paymentRepository.existsByCustomerId(id)) {
            throw new ResourceInUseException("Customer has existing payments: " + id);
        }
        customerRepository.delete(customer);
        log.debug("Deleted customer={}", id);
    }

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
