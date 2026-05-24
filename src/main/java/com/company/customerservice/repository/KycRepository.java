package com.company.customerservice.repository;

import com.company.customerservice.domain.Kyc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KycRepository extends JpaRepository<Kyc, UUID> {

    Optional<Kyc> findByCustomerId(UUID customerId);
}
