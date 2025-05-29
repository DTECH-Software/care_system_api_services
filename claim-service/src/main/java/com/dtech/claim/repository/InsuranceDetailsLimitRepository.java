/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceDetailsLimitRepository extends JpaRepository<InsuranceDetailsLimit, Long> {

    List<InsuranceDetailsLimit> findByInsurancePolicyAndStatusAndInsurancePeriod(InsurancePolicy insurancePolicy,
                                                                                     Status status,
                                                                                     InsurancePeriod insurancePeriodList);
}
