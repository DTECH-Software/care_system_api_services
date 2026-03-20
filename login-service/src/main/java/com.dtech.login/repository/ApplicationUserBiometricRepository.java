package com.dtech.login.repository;

import com.dtech.login.model.ApplicationUserBiometric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUserBiometricRepository extends JpaRepository<ApplicationUserBiometric, Long> {

    Optional<ApplicationUserBiometric> findByApplicationUser_UsernameAndUniqueCodeAndEnabledTrue(String username, String uniqueCode);
}
