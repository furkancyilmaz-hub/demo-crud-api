package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.ProposalCreateRequest;
import com.furkan.democrudapi.dto.ProposalDetailResponse;
import com.furkan.democrudapi.dto.ProposalResponse;
import com.furkan.democrudapi.dto.ProposalUpdateRequest;
import com.furkan.democrudapi.service.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/proposals")
@Tag(name = "Proposals", description = "Proposal CRUD")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @Operation(summary = "Create a proposal")
    @PostMapping
    public ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalCreateRequest request) {
        ProposalResponse response = proposalService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get a single proposal by id")
    @GetMapping("/{id}")
    public ProposalResponse getById(@PathVariable Long id) {
        return proposalService.getById(id);
    }

    @Operation(summary = "Get a single proposal by proposal no")
    @GetMapping("/search")
    public ProposalResponse searchByProposalNo(@RequestParam String proposalNo) {
        return proposalService.getByProposalNo(proposalNo);
    }

    @Operation(summary = "List proposals without their customers")
    @GetMapping
    public Page<ProposalResponse> list(Pageable pageable) {
        return proposalService.list(pageable);
    }

    @Operation(summary = "List proposals with their customers, loading the customers one proposal at a time")
    @GetMapping("/detail")
    public Page<ProposalDetailResponse> listDetail(Pageable pageable) {
        return proposalService.listDetail(pageable);
    }

    @Operation(summary = "Update a proposal")
    @PutMapping("/{id}")
    public ProposalResponse update(@PathVariable Long id, @Valid @RequestBody ProposalUpdateRequest request) {
        return proposalService.update(id, request);
    }

    @Operation(summary = "Delete a proposal")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        proposalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
