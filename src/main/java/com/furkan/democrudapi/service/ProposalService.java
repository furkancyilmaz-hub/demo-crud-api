package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.ProposalCreateRequest;
import com.furkan.democrudapi.dto.ProposalDetailResponse;
import com.furkan.democrudapi.dto.ProposalResponse;
import com.furkan.democrudapi.dto.ProposalUpdateRequest;
import com.furkan.democrudapi.entity.Proposal;
import com.furkan.democrudapi.exception.DuplicateProposalNoException;
import com.furkan.democrudapi.exception.ResourceInUseException;
import com.furkan.democrudapi.exception.ResourceNotFoundException;
import com.furkan.democrudapi.repository.CustomerRepository;
import com.furkan.democrudapi.repository.ProposalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProposalService {

    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

    private final ProposalRepository proposalRepository;
    private final CustomerRepository customerRepository;

    public ProposalService(ProposalRepository proposalRepository, CustomerRepository customerRepository) {
        this.proposalRepository = proposalRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public ProposalResponse create(ProposalCreateRequest request) {
        if (proposalRepository.existsByProposalNo(request.proposalNo())) {
            throw new DuplicateProposalNoException("Proposal no already exists: " + request.proposalNo());
        }
        Proposal proposal = new Proposal(
                request.proposalNo(), request.status(), request.issueDate(), request.totalPremium());
        try {
            Proposal saved = proposalRepository.save(proposal);
            return ProposalResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateProposalNoException("Proposal no already exists: " + request.proposalNo());
        }
    }

    public ProposalResponse getById(Long id) {
        return ProposalResponse.from(findProposalOrThrow(id));
    }

    public Page<ProposalResponse> list(Pageable pageable) {
        return proposalRepository.findAll(pageable).map(ProposalResponse::from);
    }

    /**
     * Reads the customers of each proposal straight off the lazy association, one
     * proposal at a time.
     */
    public Page<ProposalDetailResponse> listDetail(Pageable pageable) {
        return proposalRepository.findAll(pageable)
                .map(proposal -> ProposalDetailResponse.from(proposal, List.copyOf(proposal.getCustomers())));
    }

    @Transactional
    public ProposalResponse update(Long id, ProposalUpdateRequest request) {
        Proposal proposal = findProposalOrThrow(id);
        proposal.updateDetails(request.status(), request.issueDate(), request.totalPremium());
        return ProposalResponse.from(proposal);
    }

    @Transactional
    public void delete(Long id) {
        Proposal proposal = findProposalOrThrow(id);
        if (customerRepository.existsByProposalId(id)) {
            throw new ResourceInUseException("Proposal has existing customers: " + id);
        }
        proposalRepository.delete(proposal);
        log.debug("Deleted proposal={}", id);
    }

    private Proposal findProposalOrThrow(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + id));
    }
}
