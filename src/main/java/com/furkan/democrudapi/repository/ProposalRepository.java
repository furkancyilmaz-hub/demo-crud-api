package com.furkan.democrudapi.repository;

import com.furkan.democrudapi.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    boolean existsByProposalNo(String proposalNo);
}
