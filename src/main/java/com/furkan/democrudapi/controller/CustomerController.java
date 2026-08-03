package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.CustomerCreateRequest;
import com.furkan.democrudapi.dto.CustomerResponse;
import com.furkan.democrudapi.dto.CustomerUpdateRequest;
import com.furkan.democrudapi.dto.PaymentResponse;
import com.furkan.democrudapi.service.CustomerService;
import com.furkan.democrudapi.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
public class CustomerController {

    private final CustomerService customerService;
    private final PaymentService paymentService;

    public CustomerController(CustomerService customerService, PaymentService paymentService) {
        this.customerService = customerService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping
    public Page<CustomerResponse> list(@RequestParam(required = false) Long proposalId, Pageable pageable) {
        return customerService.list(proposalId, pageable);
    }

    @GetMapping("/detail")
    public Page<CustomerResponse> listDetail(@RequestParam(required = false) Long proposalId, Pageable pageable) {
        return customerService.listDetail(proposalId, pageable);
    }

    @GetMapping("/overview")
    public Page<CustomerResponse> listOverview(@RequestParam(required = false) Long proposalId, Pageable pageable) {
        return customerService.listOverview(proposalId, pageable);
    }

    @GetMapping("/search")
    public Page<CustomerResponse> searchByCity(@RequestParam String city, Pageable pageable) {
        return customerService.searchByCity(city, pageable);
    }

    @GetMapping("/{id}/payments")
    public Page<PaymentResponse> listPayments(@PathVariable Long id, Pageable pageable) {
        return paymentService.list(id, pageable);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
