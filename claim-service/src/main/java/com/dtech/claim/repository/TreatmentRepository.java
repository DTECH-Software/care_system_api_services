package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    Optional<Treatment> findByTreatmentCodeAndStatus(String code,Status status);
    List<Treatment> findAllByStatus(Status status);
}
