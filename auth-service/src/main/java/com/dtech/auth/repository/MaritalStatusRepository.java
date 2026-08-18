package com.dtech.auth.repository;

import com.dtech.auth.enums.Workflow;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.MaritalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaritalStatusRepository extends JpaRepository<MaritalStatus, Long> {
    boolean existsByApplicationUserAndStatus(ApplicationUser applicationUser, Workflow status);
}
