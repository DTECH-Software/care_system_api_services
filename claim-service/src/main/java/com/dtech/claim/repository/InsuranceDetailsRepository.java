/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.InsuranceDetails;
import com.dtech.claim.model.InsurancePeriod;
import com.dtech.claim.model.InsurancePolicy;
import com.dtech.claim.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceDetailsRepository extends JpaRepository<InsuranceDetails, Long> {
    Optional<InsuranceDetails> findByInsurancePolicyAndInsurancePeriodAndTreatmentAndStatus(InsurancePolicy insurancePolicy, InsurancePeriod insurancePeriod, Treatment treatment, Status status);
}
