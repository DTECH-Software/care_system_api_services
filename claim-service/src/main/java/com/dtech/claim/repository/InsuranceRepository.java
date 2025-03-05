package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceRepository extends JpaRepository<InsurancePolicy,Long> {
    Optional<InsurancePolicy> findByIdAndStatus(Long id, Status status);
}
