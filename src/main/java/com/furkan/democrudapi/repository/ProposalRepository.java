package com.furkan.democrudapi.repository;

import com.furkan.democrudapi.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    boolean existsByProposalNo(String proposalNo);

    /**
     * Not pageable on purpose: {@code proposal_no} is unique, so at most one row matches.
     */
    Optional<Proposal> findByProposalNo(String proposalNo);
}
