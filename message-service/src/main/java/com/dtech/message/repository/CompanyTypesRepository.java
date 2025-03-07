package com.dtech.message.repository;


import com.dtech.message.enums.Status;
import com.dtech.message.model.CompanyTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyTypesRepository extends JpaRepository<CompanyTypes, Long> {
    Optional<CompanyTypes> findByCodeAndStatus(String code, Status status);
}
