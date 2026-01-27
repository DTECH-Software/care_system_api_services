package com.dtech.claim.repository;

import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.response.AmountResponseDTO;
import com.dtech.claim.dto.response.CountTypeResponseDTO;
import com.dtech.claim.dto.response.LatestUpdatedResponseDTO;
import com.dtech.claim.enums.TreatmentType;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.InsuranceClaimsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface InsuranceClaimsRequestRepository extends JpaRepository<InsuranceClaimsRequest, Long>, JpaSpecificationExecutor<InsuranceClaimsRequest> {
    Page<InsuranceClaimsRequest> findAll(Specification<InsuranceClaimsRequest> spec, Pageable pageable);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ip.id = :insurancePeriod " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndStatus(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("statuses") List<Workflow> statuses);


    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndStatus(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("statuses") List<Workflow> statuses);

    boolean existsByEmployeeAndInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatus(ApplicationUser employee, String code, Workflow requestStatus);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON ic.insuranceClaimsDetails.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND ic.employee.userPersonalDetails.userCompanyDetails.companyTypes.code = :company " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ip.id = :insurancePeriod " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndStatusCompany(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("company") String company,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND icd.treatmentCategory.code = :treatmentCategory " +
            "AND ip.id = :insurancePeriod " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("treatmentCategory") String treatmentCategory,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("statuses") List<Workflow> statuses);


        @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND icd.treatmentCategory.code = :treatmentCategory " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("treatmentCategory") String treatmentCategory,
            @Param("statuses") List<Workflow> statuses);

    @Query(value = "SELECT " +
            "    COUNT(*) AS fullCount, " +
            "    COUNT(CASE WHEN ic.request_status = 'APPROVED' THEN 1 END) AS approvedCount, " +
            "    COUNT(CASE WHEN ic.request_status = 'REJECTED' THEN 1 END) AS rejectedCount, " +
            "    COUNT(CASE WHEN ic.request_status = 'UNDER_REVIEW' THEN 1 END) AS underReviewCount " +
            "FROM claims_request ic " +
            "LEFT JOIN application_user ap ON ic.employee = ap.id " +
            "LEFT JOIN user_personal_details up ON ap.user_personal_details = up.id " +
            "LEFT JOIN user_company_details uc ON up.user_company_details = uc.id " +
            "LEFT JOIN insurance_policy p ON uc.insurance_policy = p.id " +
            "LEFT JOIN claims_dependents cd ON ic.dependent = cd.id " +
            "LEFT JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id " +
            "LEFT JOIN treatment tr ON icd.treatment = tr.code " +
            "WHERE ap.id = :userId  " +
            "AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year} " +
            "AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month}) " +
            "AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory}) " +
            "AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId}) " +
            "AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR tr.code = :#{#dashboardSummaryDTO.treatmentType})",
            nativeQuery = true)
    CountTypeResponseDTO findSummary(@Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
                                     @Param("userId") Long userId,
                                     @Param("policy") String policy);

//    @Query(value = """
//    SELECT
//        COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//             AND (:treatment != 'CRIC' OR :treatment != 'CRIC')
//            THEN ic.request_amount
//        END), 0) AS sumOfUtilizeAmount,
//        COALESCE(idl.total_limit, 0) AS totalLimit,
//
//        (COALESCE(idl.total_limit, 0) - COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//             AND (:treatment != 'CRIC' OR :treatment != 'CRIC')
//            THEN ic.request_amount
//        END), 0)) AS remainingAmount
//
//    FROM (
//        SELECT global_limit AS total_limit
//        FROM insurance_details_limit
//        WHERE insurance_policy = :policy
//          AND treatment = :treatment
//          AND (:treatment != 'CRIC' OR treatment != 'CRIC')
//    ) idl
//
//    LEFT JOIN application_user ap ON ap.id = :userId
//    LEFT JOIN claims_request ic ON ic.employee = ap.id
//    LEFT JOIN claims_dependents cd ON ic.dependent = cd.id
//    LEFT JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id
//
//    WHERE
//        (ic.id IS NULL OR (
//            icd.treatment = :treatment
//            AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
//            AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
//            AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
//            AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
//            AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR icd.treatment = :#{#dashboardSummaryDTO.treatmentType})
//        ))
//""", nativeQuery = true)
//    AmountResponseDTO findSummaryByFacility(
//            @Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
//            @Param("userId") Long userId,
//            @Param("policy") String policy,
//            @Param("treatment") String treatment
//    );

//    @Query(value = """
//    SELECT
//        COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//            THEN ic.request_amount
//        END), 0) AS sumOfUtilizeAmount,
//        COALESCE(idl.total_limit, 0) AS totalLimit,
//        (COALESCE(idl.total_limit, 0) - COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//            THEN ic.request_amount
//        END), 0)) AS remainingAmount
//    FROM (
//        SELECT global_limit AS total_limit
//        FROM insurance_details_limit
//        WHERE insurance_policy = :policy
//          AND treatment = :treatment
//    ) idl
//    LEFT JOIN application_user ap ON ap.id = :userId
//    LEFT JOIN claims_request ic ON ic.employee = ap.id
//    LEFT JOIN claims_dependents cd ON ic.dependent = cd.id
//    LEFT JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id
//        AND icd.treatment = :treatment
//        AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
//        AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
//        AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
//        AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
//        AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR icd.treatment = :#{#dashboardSummaryDTO.treatmentType})
//    WHERE ic.id IS NULL OR ic.id IS NOT NULL
//""", nativeQuery = true)
//    AmountResponseDTO findSummaryByFacility(
//            @Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
//            @Param("userId") Long userId,
//            @Param("policy") String policy,
//            @Param("treatment") String treatment
//    );

//    @Query(value = """
//    SELECT
//        COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//            THEN ic.request_amount
//            ELSE 0
//        END), 0) AS sumOfUtilizeAmount,
//        COALESCE(idl.total_limit, 0) AS totalLimit,
//        COALESCE(idl.total_limit, 0) - COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//            THEN ic.request_amount
//            ELSE 0
//        END), 0) AS remainingAmount
//    FROM (
//        SELECT global_limit AS total_limit
//        FROM insurance_details_limit
//        WHERE insurance_policy = :policy
//          AND treatment = :treatment
//    ) idl
//    LEFT JOIN application_user ap ON ap.id = :userId
//    LEFT JOIN claims_request ic ON ic.employee = ap.id
//    LEFT JOIN claims_dependents cd ON ic.dependent = cd.id
//    LEFT JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id
//        AND icd.treatment = :treatment
//        AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
//        AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
//        AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
//        AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
//        AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR icd.treatment = :#{#dashboardSummaryDTO.treatmentType})
//""", nativeQuery = true)
//    AmountResponseDTO findSummaryByFacility(
//            @Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
//            @Param("userId") Long userId,
//            @Param("policy") String policy,
//            @Param("treatment") String treatment
//    );

    @Query(value = """
SELECT
    COALESCE(SUM(CASE
        WHEN ic.request_status = 'APPROVED' 
             AND icd.treatment = :treatment 
             AND ap.id = :userId
        THEN ic.approved_amount
        ELSE 0
    END), 0) AS sumOfUtilizeAmount,
    
    COALESCE(idl.total_limit, 0) AS totalLimit,
    
    COALESCE(idl.total_limit, 0) - COALESCE(SUM(CASE
        WHEN ic.request_status = 'APPROVED' 
             AND icd.treatment = :treatment 
             AND ap.id = :userId
        THEN ic.approved_amount
        ELSE 0
    END), 0) AS remainingAmount

FROM (
    SELECT global_limit AS total_limit
    FROM insurance_details_limit idd
    JOIN insurance_staff_category_period isc 
        ON isc.id = idd.insurance_staff_category_period
    WHERE idd.insurance_policy = :policy 
      AND YEAR(isc.from_date) = :#{#dashboardSummaryDTO.year}
      AND idd.treatment = :treatment 
) idl

LEFT JOIN application_user ap 
    ON ap.id = :userId

LEFT JOIN claims_request ic 
    ON ic.employee = ap.id

LEFT JOIN claims_dependents cd 
    ON ic.dependent = cd.id

LEFT JOIN insurance_claims_details icd 
    ON ic.insurance_claims_details = icd.id
   AND icd.treatment = :treatment
   AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
   AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
   AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
   AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})

GROUP BY idl.total_limit
""", nativeQuery = true)
    AmountResponseDTO findSummaryByFacility(
            @Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
            @Param("userId") Long userId,
            @Param("policy") String policy,
            @Param("treatment") String treatment
    );


//    @Query(value = """
//SELECT
//    COALESCE(SUM(CASE
//        WHEN ic.request_status = 'APPROVED'
//             AND icd.treatment = :treatment
//             AND ap.id = :userId
//        THEN ic.approved_amount
//        ELSE 0
//    END), 0) AS sumOfUtilizeAmount,
//
//    COALESCE(idl.total_limit, 0) AS totalLimit,
//
//    COALESCE(idl.total_limit, 0) - COALESCE(SUM(CASE
//        WHEN ic.request_status = 'APPROVED'
//             AND icd.treatment = :treatment
//             AND ap.id = :userId
//        THEN ic.approved_amount
//        ELSE 0
//    END), 0) AS remainingAmount
//
//FROM (
//    SELECT global_limit AS total_limit
//    FROM insurance_details_limit idd
//    JOIN insurance_staff_category_period isc
//        ON isc.id = idd.insurance_staff_category_period
//    WHERE idd.insurance_policy = :policy
//      AND YEAR(isc.from_date) = :#{#dashboardSummaryDTO.year}
//      AND idd.treatment = :treatment
//) idl
//
//LEFT JOIN application_user ap
//    ON ap.id = :userId
//
//LEFT JOIN claims_request ic
//    ON ic.employee = ap.id
//
//LEFT JOIN claims_dependents cd
//    ON ic.dependent = cd.id
//
//LEFT JOIN insurance_claims_details icd
//    ON ic.insurance_claims_details = icd.id
//   AND icd.treatment = :treatment
//   AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
//   AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
//   AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
//   AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
//
//GROUP BY idl.total_limit
//""", nativeQuery = true)
//    AmountResponseDTO findSummaryByFacility(
//            @Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
//            @Param("userId") Long userId,
//            @Param("policy") String policy,
//            @Param("treatment") String treatment
//    );

//    @Query(value = """
//    SELECT
//        COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//             AND tr.code = :treatment
//            THEN ic.request_amount
//        END), 0) AS sumOfUtilizeAmount,
//        limits.totalLimit AS totalLimit,
//        (limits.totalLimit - COALESCE(SUM(CASE
//            WHEN ic.request_status = 'APPROVED'
//             AND tr.code = :treatment
//            THEN ic.request_amount
//        END), 0)) AS remainingAmount
//    FROM claims_request ic
//    JOIN application_user ap ON ic.employee = ap.id
//    LEFT JOIN claims_dependents cd ON ic.dependent = cd.id
//    JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id
//    JOIN treatment tr ON icd.treatment = tr.code
//    LEFT JOIN insurance_details_limit idl ON idl.insurance_policy = :policy AND idl.treatment = :treatment
//    JOIN (
//        SELECT COALESCE(SUM(idl.global_limit), 0) AS totalLimit
//        FROM insurance_details_limit idl
//        JOIN insurance_policy ip2 ON ip2.code = idl.insurance_policy
//        WHERE ip2.code = :policy
//          AND idl.treatment = :treatment
//    ) AS limits ON 1=1
//    WHERE tr.code = :treatment and ap.id = :userId
//      AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
//      AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
//      AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
//      AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
//      AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR tr.code = :#{#dashboardSummaryDTO.treatmentType})
//    """, nativeQuery = true)
//    AmountResponseDTO findSummaryByFacilityCritical(@Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
//                                                    @Param("userId") Long userId,
//                                            @Param("policy") String policy,
//                                            @Param("treatment") String treatment);

    @Query(value = """
    SELECT 
        COALESCE(SUM(CASE 
            WHEN ic.request_status = 'APPROVED' 
             AND tr.code = :treatment 
            THEN ic.request_amount 
        END), 0) AS sumOfUtilizeAmount,
        limits.totalLimit AS totalLimit,
        (limits.totalLimit - COALESCE(SUM(CASE 
            WHEN ic.request_status = 'APPROVED' 
             AND tr.code = :treatment 
            THEN ic.request_amount 
        END), 0)) AS remainingAmount
    FROM claims_request ic
    JOIN application_user ap ON ic.employee = ap.id
    LEFT JOIN claims_dependents cd ON ic.dependent = cd.id
    JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id
    JOIN treatment tr ON icd.treatment = tr.code
    LEFT JOIN insurance_details_limit idl ON idl.insurance_policy = :policy AND idl.treatment = :treatment
    JOIN (
        SELECT COALESCE(SUM(idl.global_limit), 0) AS totalLimit
        FROM insurance_details_limit idl
        JOIN insurance_policy ip2 ON ip2.code = idl.insurance_policy
        WHERE ip2.code = :policy 
          AND idl.treatment = :treatment
    ) AS limits ON 1=1
    WHERE tr.code = :treatment and ap.id = :userId
      AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
      AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
      AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
      AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
      AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR tr.code = :#{#dashboardSummaryDTO.treatmentType})
    """, nativeQuery = true)
    AmountResponseDTO findSummaryByFacilityCritical(@Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
                                                    @Param("userId") Long userId,
                                                    @Param("policy") String policy,
                                                    @Param("treatment") String treatment);
    @Query(value = """
SELECT COUNT(*) AS approvedRequestCount
FROM claims_request ic
LEFT JOIN application_user ap ON ic.employee = ap.id
LEFT JOIN user_personal_details up ON ap.user_personal_details = up.id
LEFT JOIN user_company_details cp ON up.user_company_details = cp.id
LEFT JOIN claims_dependents cd ON ic.dependent = cd.id
LEFT JOIN user_personal_details upd ON ap.user_personal_details = upd.id
JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id
JOIN treatment tr ON icd.treatment = tr.code
LEFT JOIN insurance_details_limit idl ON idl.insurance_policy = :policy AND idl.treatment = :treatment
WHERE tr.code = :treatment
  AND cp.staff_category = 'NS'
  AND ic.request_status = 'APPROVED'
  AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
  AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
  AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
  AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
  AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR tr.code = :#{#dashboardSummaryDTO.treatmentType})
""", nativeQuery = true)
    int findApprovedRequestCountByTreatment(@Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
                                            @Param("policy") String policy,
                                            @Param("treatment") String treatment);

    @Query(value = """
    SELECT 
        COUNT(*) AS approvedRequestCount
    FROM claims_request ic
    JOIN application_user ap ON ic.employee = ap.id AND ic.request_status = 'APPROVED'
    LEFT JOIN claims_dependents cd ON ic.dependent = cd.id
    JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id
    LEFT JOIN user_personal_details upd ON ap.user_personal_details = upd.id
    LEFT JOIN user_company_details ucd ON upd.user_company_details = ucd.id
    JOIN treatment tr ON icd.treatment = tr.code
    LEFT JOIN insurance_details_limit idl ON idl.insurance_policy = :policy AND idl.treatment = :treatment
    WHERE tr.code = :treatment 
      AND ap.id = :userId 
      AND ucd.staff_category = 'SNR'
      AND YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}
      AND (:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})
      AND (:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})
      AND (:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})
      AND (:#{#dashboardSummaryDTO.treatmentType} IS NULL OR tr.code = :#{#dashboardSummaryDTO.treatmentType})
""", nativeQuery = true)
    int findApprovedRequestCountByTreatmentSNR(
            @Param("dashboardSummaryDTO") DashboardSummaryDTO dashboardSummaryDTO,
            @Param("userId") Long userId,
            @Param("policy") String policy,
            @Param("treatment") String treatment
    );

    @Query(value = "SELECT " +
            "ic.id AS id, " +
            "ic.request_id AS requestId, " +
            "tr.description AS treatment, " +
            "ic.remark AS remark, " +
            "icd.disease AS diagnosis, " +
            "CASE WHEN ic.request_status = 'APPROVED' THEN ic.approved_amount ELSE ic.request_amount END AS amount, " +
            "CASE WHEN ic.dependent IS NULL THEN 'You' ELSE CONCAT(cd.first_name, cd.last_name) END AS passion, " +
            "ic.created_date AS requestDate " +
            "FROM claims_request ic " +
            "LEFT OUTER JOIN application_user ap ON ic.employee = ap.id " +
            "LEFT OUTER JOIN claims_dependents cd ON ic.dependent = cd.id " +
            "LEFT OUTER JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id  " +
            "LEFT OUTER JOIN treatment tr ON icd.treatment = tr.code " +
            "WHERE ap.id = :userId AND ic.request_status = :requestStatus " +
            "ORDER BY ic.last_modified_date DESC LIMIT 7 ", nativeQuery = true)
    List<LatestUpdatedResponseDTO> getLatestUpdatedRecordSummary(@Param("userId") Long userId, @Param("requestStatus") String requestStatus);

    int countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatusIn(String treatmentCode, List<Workflow> workflow);
    int countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatusInAndEmployee(String treatmentCode, List<Workflow> workflow,ApplicationUser applicationUser);

}

