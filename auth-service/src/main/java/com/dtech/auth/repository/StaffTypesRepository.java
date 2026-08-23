package com.dtech.auth.repository;

import com.dtech.auth.enums.Status;
import com.dtech.auth.model.StaffTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffTypesRepository extends JpaRepository<StaffTypes, Long> {
    Optional<StaffTypes> findByCodeAndStatus(String code, Status status);
}
