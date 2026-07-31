package com.furkan.democrudapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "customer")
@Getter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @Column(name = "identity_no", nullable = false, length = 20)
    private String identityNo;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "city", nullable = false, length = 40)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status;

    protected Customer() {
    }

    public Customer(Proposal proposal, String identityNo, String fullName, String city, CustomerStatus status) {
        this.proposal = proposal;
        this.identityNo = identityNo;
        this.fullName = fullName;
        this.city = city;
        this.status = status;
    }

    public void updateDetails(String identityNo, String fullName, String city, CustomerStatus status) {
        this.identityNo = identityNo;
        this.fullName = fullName;
        this.city = city;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
