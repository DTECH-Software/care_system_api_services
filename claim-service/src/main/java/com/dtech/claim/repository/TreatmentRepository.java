package com.dtech.claim.repository;

import com.dtech.claim.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    Optional<Treatment> findByTreatmentCode(String code);
}
