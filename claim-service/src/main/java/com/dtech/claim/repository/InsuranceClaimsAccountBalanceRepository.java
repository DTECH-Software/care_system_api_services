package com.dtech.claim.repository;

import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.ClaimsAccountBalance;
import com.dtech.claim.model.InsurancePeriod;
import com.dtech.claim.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceClaimsAccountBalanceRepository extends JpaRepository<ClaimsAccountBalance, Long> {
    Optional<ClaimsAccountBalance> findByEmployeeAndTreatmentAndInsurancePeriod(ApplicationUser user, Treatment treatment, InsurancePeriod insurancePeriod);
}
