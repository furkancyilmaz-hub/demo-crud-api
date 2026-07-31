package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.CustomerCreateRequest;
import com.furkan.democrudapi.dto.CustomerResponse;
import com.furkan.democrudapi.dto.CustomerUpdateRequest;
import com.furkan.democrudapi.entity.Customer;
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

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final ProposalRepository proposalRepository;
    private final PaymentRepository paymentRepository;

    public CustomerService(CustomerRepository customerRepository, ProposalRepository proposalRepository,
                            PaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.proposalRepository = proposalRepository;
        this.paymentRepository = paymentRepository;
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

    public Page<CustomerResponse> list(Long proposalId, Pageable pageable) {
        Page<Customer> page = proposalId != null
                ? customerRepository.findByProposalId(proposalId, pageable)
                : customerRepository.findAll(pageable);
        return page.map(CustomerResponse::from);
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
