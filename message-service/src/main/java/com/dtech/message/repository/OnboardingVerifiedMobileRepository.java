package com.dtech.message.repository;


import com.dtech.message.model.ApplicationOtpSession;
import com.dtech.message.model.OnboardingVerifiedMobile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingVerifiedMobileRepository extends JpaRepository<OnboardingVerifiedMobile, Long> {
    List<OnboardingVerifiedMobile> findByEpfNoAndNicEqualsIgnoreCaseAndMobileAndVerified(String efpNo, String nic, String mobile, boolean verified,Sort sort);
    Optional<OnboardingVerifiedMobile> findByApplicationOtpSession(ApplicationOtpSession applicationOtpSession);
}
