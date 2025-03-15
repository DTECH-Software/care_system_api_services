package com.dtech.auth.repository;

import com.dtech.auth.model.Married;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarriedRepository extends JpaRepository<Married, Long> {
}
