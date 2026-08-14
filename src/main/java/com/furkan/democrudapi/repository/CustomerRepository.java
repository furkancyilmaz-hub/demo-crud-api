package com.furkan.democrudapi.repository;

import com.furkan.democrudapi.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByProposalId(Long proposalId, Pageable pageable);

    Page<Customer> findByCity(String city, Pageable pageable);

    Page<Customer> findByProposalIdAndIdentityNo(Long proposalId, String identityNo, Pageable pageable);

    boolean existsByProposalId(Long proposalId);

    /**
     * Loads customers together with their payments in a single query.
     * Deliberately not pageable: combining a collection fetch with {@code Pageable}
     * makes Hibernate paginate in memory. Callers pass an already paged id list.
     */
    @EntityGraph(attributePaths = "payments")
    List<Customer> findByIdIn(Collection<Long> ids);
}
