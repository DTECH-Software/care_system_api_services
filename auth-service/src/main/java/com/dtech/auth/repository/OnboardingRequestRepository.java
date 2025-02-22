package com.dtech.auth.repository;

import com.dtech.auth.model.OnboardingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingRequestRepository extends JpaRepository<OnboardingRequest, Long> {
}
