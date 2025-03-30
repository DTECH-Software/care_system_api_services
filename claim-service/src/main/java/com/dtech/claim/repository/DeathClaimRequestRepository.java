package com.dtech.claim.repository;

import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.response.CountTypeResponseDTO;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.ClaimsDependents;
import com.dtech.claim.model.DeathClaimRequest;
import com.dtech.claim.repository.custom.DeathClaimsRequestRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeathClaimRequestRepository extends JpaRepository<DeathClaimRequest, Long> , JpaSpecificationExecutor<DeathClaimRequest> , DeathClaimsRequestRepositoryCustom {
    Optional<DeathClaimRequest> findByClaimsDependentsAndEmployeeAndRequestStatusIn(ClaimsDependents claimsDependents, ApplicationUser applicationUser, List<Workflow> workflow);


//    @Query(value = "SELECT  " +
//            "    COUNT(ic.id) AS fullCount, " +
//            "    COUNT(CASE WHEN ic.request_status = 'APPROVED' THEN 1 END) AS approvedCount, " +
//            "    COUNT(CASE WHEN ic.request_status = 'REJECTED' THEN 1 END) AS rejectedCount, " +
//            "    COUNT(CASE WHEN ic.request_status = 'UNDER_REVIEW' THEN 1 END) AS underReviewCount, " +
//            "    SUM(CASE WHEN ic.request_status IN ('APPROVED', 'UNDER_REVIEW') THEN ic.request_amount END) AS sumOfUtilizeAmount " +
//            "FROM claims_request ic " +
//            "LEFT OUTER JOIN application_user ap ON ic.employee = ap.id " +
//            "LEFT OUTER JOIN claims_dependents cd ON ic.dependent = cd.id " +
//            "LEFT OUTER JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id " +
//            "LEFT OUTER JOIN treatment tr ON icd.treatment = tr.id " +
//            "WHERE ap.id = :userId " +
//            "AND (YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}) " +
//            "AND ( " +
//            "(:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})" +
//            ") " +
//            "AND ( " +
//            "(:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})" +
//            ") " +
//            "AND ( " +
//            "(:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})" +
//            ") " +
//            "AND ( " +
//            "(:#{#dashboardSummaryDTO.treatmentType} IS NULL OR tr.treatment_code = :#{#dashboardSummaryDTO.treatmentType})" +
//            ") ",
//            nativeQuery = true)
//    CountTypeResponseDTO findSummary(DashboardSummaryDTO dashboardSummaryDTO,
//                                     @Param("userId") Long userId);

}
