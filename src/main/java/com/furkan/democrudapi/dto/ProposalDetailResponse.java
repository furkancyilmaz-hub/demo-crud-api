package com.furkan.democrudapi.dto;

import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.CustomerStatus;
import com.furkan.democrudapi.entity.Proposal;
import com.furkan.democrudapi.entity.ProposalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProposalDetailResponse(
        Long id,
        String proposalNo,
        ProposalStatus status,
        LocalDate issueDate,
        BigDecimal totalPremium,
        List<CustomerSummary> customers
) {
    public static ProposalDetailResponse from(Proposal proposal, List<Customer> customers) {
        return new ProposalDetailResponse(
                proposal.getId(),
                proposal.getProposalNo(),
                proposal.getStatus(),
                proposal.getIssueDate(),
                proposal.getTotalPremium(),
                customers.stream().map(CustomerSummary::from).toList()
        );
    }

    public record CustomerSummary(
            Long id,
            String identityNo,
            String fullName,
            String city,
            CustomerStatus status
    ) {
        public static CustomerSummary from(Customer customer) {
            return new CustomerSummary(
                    customer.getId(),
                    customer.getIdentityNo(),
                    customer.getFullName(),
                    customer.getCity(),
                    customer.getStatus()
            );
        }
    }
}