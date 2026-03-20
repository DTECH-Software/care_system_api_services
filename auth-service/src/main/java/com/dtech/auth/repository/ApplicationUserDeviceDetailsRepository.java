package com.dtech.auth.repository;

import com.dtech.auth.model.ApplicationUserDeviceDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUserDeviceDetailsRepository extends JpaRepository<ApplicationUserDeviceDetails, Long> {

    Optional<ApplicationUserDeviceDetails> findByDeviceId(String deviceId);
}
