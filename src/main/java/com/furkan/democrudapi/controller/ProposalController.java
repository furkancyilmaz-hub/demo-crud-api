package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.ProposalCreateRequest;
import com.furkan.democrudapi.dto.ProposalDetailResponse;
import com.furkan.democrudapi.dto.ProposalResponse;
import com.furkan.democrudapi.dto.ProposalUpdateRequest;
import com.furkan.democrudapi.service.ProposalService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping
    public ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalCreateRequest request) {
        ProposalResponse response = proposalService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ProposalResponse getById(@PathVariable Long id) {
        return proposalService.getById(id);
    }

    @GetMapping
    public Page<ProposalResponse> list(Pageable pageable) {
        return proposalService.list(pageable);
    }

    @GetMapping("/detail")
    public Page<ProposalDetailResponse> listDetail(Pageable pageable) {
        return proposalService.listDetail(pageable);
    }

    @PutMapping("/{id}")
    public ProposalResponse update(@PathVariable Long id, @Valid @RequestBody ProposalUpdateRequest request) {
        return proposalService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        proposalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
