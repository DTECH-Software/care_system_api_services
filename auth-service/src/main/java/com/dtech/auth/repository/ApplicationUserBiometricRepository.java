package com.dtech.auth.repository;

import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ApplicationUserBiometric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUserBiometricRepository extends JpaRepository<ApplicationUserBiometric, Long> {

    Optional<ApplicationUserBiometric> findByApplicationUser(ApplicationUser applicationUser);
}
