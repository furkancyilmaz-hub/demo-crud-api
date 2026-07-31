package com.furkan.democrudapi.repository;

import com.furkan.democrudapi.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByCustomerId(Long customerId, Pageable pageable);

    boolean existsByCustomerId(Long customerId);
}
