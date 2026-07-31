package com.furkan.democrudapi.repository;

import com.furkan.democrudapi.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByProposalId(Long proposalId, Pageable pageable);

    boolean existsByProposalId(Long proposalId);
}
