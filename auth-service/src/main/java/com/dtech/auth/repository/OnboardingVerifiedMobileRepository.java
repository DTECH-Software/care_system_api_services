package com.dtech.auth.repository;

import com.dtech.auth.model.OnboardingVerifiedMobile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnboardingVerifiedMobileRepository extends JpaRepository<OnboardingVerifiedMobile, Long> {

    Optional<OnboardingVerifiedMobile> findByEpfNoAndNicAndMobile(int efpNo, String nic, String mobile);
}
