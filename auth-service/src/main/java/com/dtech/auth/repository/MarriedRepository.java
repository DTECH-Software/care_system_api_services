package com.dtech.auth.repository;

import com.dtech.auth.enums.Status;
import com.dtech.auth.model.Married;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarriedRepository extends JpaRepository<Married, Long> {
    Optional<Married> findByCodeAndStatus(String code, Status status);
    List<Married> findByAllAndStatus(Status status);
}
