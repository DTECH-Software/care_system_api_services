package com.dtech.message.repository;

import com.dtech.message.model.OnboardingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingRequestRepository extends JpaRepository<OnboardingRequest, Long> {
}
