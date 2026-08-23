/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 7:58 PM
 * <p>
 */

package com.dtech.claim.repository;

import com.dtech.claim.enums.Facility;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.ClaimsDependents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimDependentsRepository extends JpaRepository<ClaimsDependents,Long> {
 Optional<ClaimsDependents> findByIdAndApplicationUserAndStatus(Long id, ApplicationUser applicationUser, Workflow status);
 Optional<ClaimsDependents> findByIdAndApplicationUserAndStatusAndEligibleFacilityIn(Long id, ApplicationUser applicationUser, Workflow status, List<Facility> facility);
 List<ClaimsDependents> findByApplicationUserAndStatusAndEligibleFacilityInAndLiveStatus(ApplicationUser applicationUser, Workflow status, List<Facility> facility,Boolean live);
}
