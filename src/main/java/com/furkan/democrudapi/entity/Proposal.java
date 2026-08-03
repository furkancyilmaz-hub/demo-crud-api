package com.furkan.democrudapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposal")
@Getter
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "proposal", fetch = FetchType.LAZY)
    private List<Customer> customers = new ArrayList<>();

    @Column(name = "proposal_no", nullable = false, unique = true, length = 30)
    private String proposalNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProposalStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "total_premium", precision = 15, scale = 2)
    private BigDecimal totalPremium;

    protected Proposal() {
    }

    public Proposal(String proposalNo, ProposalStatus status, LocalDate issueDate, BigDecimal totalPremium) {
        this.proposalNo = proposalNo;
        this.status = status;
        this.issueDate = issueDate;
        this.totalPremium = totalPremium;
    }

    public void updateDetails(ProposalStatus status, LocalDate issueDate, BigDecimal totalPremium) {
        this.status = status;
        this.issueDate = issueDate;
        this.totalPremium = totalPremium;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Proposal other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
