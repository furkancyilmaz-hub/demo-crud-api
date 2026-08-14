package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.CustomerCreateRequest;
import com.furkan.democrudapi.dto.CustomerResponse;
import com.furkan.democrudapi.dto.CustomerUpdateRequest;
import com.furkan.democrudapi.dto.PaymentResponse;
import com.furkan.democrudapi.exception.InvalidSearchQueryException;
import com.furkan.democrudapi.service.CustomerService;
import com.furkan.democrudapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer CRUD and the customer-scoped payment listing")
public class CustomerController {

    private final CustomerService customerService;
    private final PaymentService paymentService;

    public CustomerController(CustomerService customerService, PaymentService paymentService) {
        this.customerService = customerService;
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create a customer")
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get a single customer by id")
    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @Operation(summary = "List customers without their payments, optionally filtered by proposal")
    @GetMapping
    public Page<CustomerResponse> list(@RequestParam(required = false) Long proposalId, Pageable pageable) {
        return customerService.list(proposalId, pageable);
    }

    @Operation(summary = "List customers with their payments, loading the payments one customer at a time")
    @GetMapping("/detail")
    public Page<CustomerResponse> listDetail(@RequestParam(required = false) Long proposalId, Pageable pageable) {
        return customerService.listDetail(proposalId, pageable);
    }

    @Operation(summary = "List customers with their payments, loading all payments in a single query")
    @GetMapping("/overview")
    public Page<CustomerResponse> listOverview(@RequestParam(required = false) Long proposalId, Pageable pageable) {
        return customerService.listOverview(proposalId, pageable);
    }

    @Operation(summary = "Search customers by city, or by identity no within a single proposal")
    @GetMapping("/search")
    public Page<CustomerResponse> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long proposalId,
            @RequestParam(required = false) String identityNo,
            Pageable pageable) {
        String requestedCity = StringUtils.hasText(city) ? city : null;
        String requestedIdentityNo = StringUtils.hasText(identityNo) ? identityNo : null;
        validateSelection(requestedCity, proposalId, requestedIdentityNo);
        return requestedCity != null
                ? customerService.searchByCity(requestedCity, pageable)
                : customerService.searchByIdentityNo(proposalId, requestedIdentityNo, pageable);
    }

    /**
     * Accepted combinations: {@code city} alone, or {@code proposalId} and {@code identityNo}
     * together. Mixing the two is rejected rather than silently letting one win, and an
     * identity no without a proposal is rejected because it would scan every proposal.
     */
    private void validateSelection(String city, Long proposalId, String identityNo) {
        if (city == null && proposalId == null && identityNo == null) {
            throw new InvalidSearchQueryException(
                    "Either 'city' or the 'proposalId'/'identityNo' pair is required");
        }
        if (city != null && (proposalId != null || identityNo != null)) {
            throw new InvalidSearchQueryException(
                    "Parameter 'city' must not be combined with 'proposalId' or 'identityNo'");
        }
        if (city == null && (proposalId == null || identityNo == null)) {
            throw new InvalidSearchQueryException(
                    "Parameters 'proposalId' and 'identityNo' must be provided together");
        }
    }

    @Operation(summary = "List the payments of a single customer")
    @GetMapping("/{id}/payments")
    public Page<PaymentResponse> listPayments(@PathVariable Long id, Pageable pageable) {
        return paymentService.list(id, pageable);
    }

    @Operation(summary = "Update a customer")
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(id, request);
    }

    @Operation(summary = "Delete a customer")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
