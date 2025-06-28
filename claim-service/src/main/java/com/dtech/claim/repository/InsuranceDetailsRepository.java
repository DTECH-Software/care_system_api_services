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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceDetailsRepository extends JpaRepository<InsuranceDetails, Long> {

    @Query(value = "SELECT ind FROM InsuranceDetails ind " +
            "LEFT OUTER JOIN InsuranceDetailsLimit icdl ON ind.insuranceDetailsLimit.id = icdl.id " +
            "LEFT OUTER JOIN InsurancePolicy inp ON icdl.insurancePolicy.code = inp.code " +
            "LEFT OUTER JOIN Treatment tr ON icdl.treatment.treatmentCode = tr.treatmentCode " +
            "LEFT OUTER JOIN TreatmentCategory tc ON ind.treatmentCategory.code = tc.code " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod  ip ON icdl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE inp = :insurancePolicy AND tr = :treatment AND tc = :treatmentCategory " +
            "AND icdl.status = :status AND ip = :insurancePeriodList ",nativeQuery = false)
    Optional<InsuranceDetails> findByInsurancePolicyAndTreatmentAndTreatmentCategoryAndStatusAndInsurancePeriod(@Param("insurancePolicy") InsurancePolicy insurancePolicy,
                                                                                                                @Param("treatment")  Treatment treatment,
                                                                                                                @Param("treatmentCategory")TreatmentCategory treatmentCategory,
                                                                                                                @Param("status")Status status,
                                                                                                                @Param("insurancePeriodList") InsuranceYear insuranceYearList);
    @Query(value = "SELECT ind FROM InsuranceDetails ind " +
            "LEFT OUTER JOIN InsuranceDetailsLimit icdl ON ind.insuranceDetailsLimit.id = icdl.id " +
            "LEFT OUTER JOIN InsurancePolicy inp ON icdl.insurancePolicy.code = inp.code " +
            "LEFT OUTER JOIN Treatment tr ON icdl.treatment.treatmentCode = tr.treatmentCode " +
            "LEFT OUTER JOIN TreatmentCategory tc ON ind.treatmentCategory.code = tc.code " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod  ip ON icdl.insuranceStaffCategoryPeriod.id = ip.id " +
            "LEFT OUTER JOIN InsuranceMonthCategory im ON ind.insuranceMonthCategory.code = im.code " +
            "WHERE inp.code = :insurancePolicy AND tr.treatmentCode = :treatment AND tc.code = :treatmentCategory " +
            "AND icdl.status = :status AND ip.id = :insurancePeriodList AND im.code = :insuranceMonthCategory ",nativeQuery = false)
    Optional<InsuranceDetails> findByInsurancePolicyAndTreatmentAndTreatmentCategoryAndStatusAndInsurancePeriodAndInsuranceMonthCategory(@Param("insurancePolicy") String insurancePolicy,
                                                                                                                                         @Param("treatment")  String treatment,
                                                                                                                                         @Param("treatmentCategory")String treatmentCategory,
                                                                                                                                         @Param("status")Status status,
                                                                                                                                         @Param("insurancePeriodList")Long insurancePeriodList,
                                                                                                                                         @Param("insuranceMonthCategory")String insuranceMonthCategory);

}
