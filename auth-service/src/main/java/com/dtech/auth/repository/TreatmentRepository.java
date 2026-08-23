package com.dtech.auth.repository;


import com.dtech.auth.enums.Status;
import com.dtech.auth.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    Optional<Treatment> findByCode(String code);
    List<Treatment> findAllByStatus(Status status);
}
