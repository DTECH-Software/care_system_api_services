package com.dtech.auth.repository;

import com.dtech.auth.enums.Status;
import com.dtech.auth.model.CompanyTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyTypesRepository extends JpaRepository<CompanyTypes, Long> {
    Optional<CompanyTypes> findByCodeAndStatus(String code, Status status);
}
