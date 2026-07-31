package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.Proposal;
import com.furkan.democrudapi.entity.ProposalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProposalResponse(
        Long id,
        String proposalNo,
        ProposalStatus status,
        LocalDate issueDate,
        BigDecimal totalPremium
) {
    public static ProposalResponse from(Proposal proposal) {
        return new ProposalResponse(
                proposal.getId(),
                proposal.getProposalNo(),
                proposal.getStatus(),
                proposal.getIssueDate(),
                proposal.getTotalPremium()
        );
    }
}
