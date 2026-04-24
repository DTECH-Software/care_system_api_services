/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.enums.TreatmentCategory;
import com.dtech.claim.enums.TreatmentType;
import com.dtech.claim.model.*;
import com.dtech.claim.util.DateTimeUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceDetailsLimitRepository extends JpaRepository<InsuranceDetailsLimit, Long> {
    Optional<InsuranceDetailsLimit> findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(InsurancePolicy insurancePolicy,
                                                                                                               Status status, InsuranceStaffCategoryPeriod insuranceYear, Treatment treatment);
    Optional<InsuranceDetailsLimit> findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(InsurancePolicy insurancePolicy,
                                                                                                Status status, InsuranceStaffCategoryPeriod insuranceYear, String treatment);
    List<InsuranceDetailsLimit> findAllByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(InsurancePolicy insurancePolicy,
                                                                                                                            Status status,
                                                                                                                            InsuranceStaffCategoryPeriod insuranceYear,
                                                                                                                            String treatmentCode);
    List<InsuranceDetailsLimit> findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriod(InsurancePolicy insurancePolicy,Status status,InsuranceStaffCategoryPeriod insuranceYear);
//    List<InsuranceDetailsLimit> findByInsurancePolicyAndStatus(InsurancePolicy insurancePolicy,Status status);

    List<InsuranceDetailsLimit> findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodStatusAndInsuranceStaffCategoryPeriodFromDateLessThanEqualAndInsuranceStaffCategoryPeriodToDateGreaterThanEqualAndInsuranceStaffCategoryPeriodStaffCategoriesCode(
            InsurancePolicy insurancePolicy,
            Status insuranceDetailsLimitStatus,
            Status insuranceStaffCategoryPeriodStatus,
            Date fromDate,
            Date toDate,
            String staffCategoryCode
    );





}
