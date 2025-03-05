package com.dtech.claim.repository;

import com.dtech.claim.model.InsuranceClaimsDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceClaimsDetailsRepository extends JpaRepository<InsuranceClaimsDetails, Long> {
}
