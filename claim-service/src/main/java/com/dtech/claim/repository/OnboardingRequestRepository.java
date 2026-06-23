package com.dtech.claim.repository;

import com.dtech.claim.model.OnboardingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingRequestRepository extends JpaRepository<OnboardingRequest, Long> {
}
