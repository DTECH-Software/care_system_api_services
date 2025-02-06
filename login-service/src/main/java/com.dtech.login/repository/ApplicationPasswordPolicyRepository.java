package com.dtech.login.repository;

import com.dtech.login.model.ApplicationPasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationPasswordPolicyRepository extends JpaRepository<ApplicationPasswordPolicy, Long> {
    @Query(value = "SELECT app FROM ApplicationPasswordPolicy app")
    Optional<ApplicationPasswordPolicy> findPasswordPolicy();
}
