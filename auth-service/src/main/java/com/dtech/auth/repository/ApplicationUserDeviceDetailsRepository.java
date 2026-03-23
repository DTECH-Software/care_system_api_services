package com.dtech.auth.repository;

import com.dtech.auth.model.ApplicationUserDeviceDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationUserDeviceDetailsRepository extends JpaRepository<ApplicationUserDeviceDetails, Long> {

    List<ApplicationUserDeviceDetails> findAllByDeviceIdOrderByIdAsc(String deviceId);
}
