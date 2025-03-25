package com.dtech.claim.repository;

import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.ClaimsDependents;
import com.dtech.claim.model.DeathClaimRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeathClaimRequestRepository extends JpaRepository<DeathClaimRequest, Long> , JpaSpecificationExecutor<DeathClaimRequest> {
    Optional<DeathClaimRequest> findByClaimsDependentsAndEmployeeAndRequestStatusIn(ClaimsDependents claimsDependents, ApplicationUser applicationUser, List<Workflow> workflow);
}
