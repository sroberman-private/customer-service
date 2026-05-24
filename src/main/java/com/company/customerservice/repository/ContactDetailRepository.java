package com.company.customerservice.repository;

import com.company.customerservice.domain.ContactDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactDetailRepository extends JpaRepository<ContactDetail, UUID> {

    List<ContactDetail> findAllByCustomerId(UUID customerId);

    Optional<ContactDetail> findByIdAndCustomerId(UUID id, UUID customerId);
}
