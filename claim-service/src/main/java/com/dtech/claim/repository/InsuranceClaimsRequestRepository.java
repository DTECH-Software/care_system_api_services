package com.dtech.claim.repository;

import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.response.CountTypeResponseDTO;
import com.dtech.claim.dto.response.LatestUpdatedResponseDTO;
import com.dtech.claim.model.InsuranceClaimsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceClaimsRequestRepository extends JpaRepository<InsuranceClaimsRequest, Long>, JpaSpecificationExecutor<InsuranceClaimsRequest> {
    Page<InsuranceClaimsRequest> findAll(Specification<InsuranceClaimsRequest> spec, Pageable pageable);

    @Query(value = "SELECT  " +
            "    COUNT(ic.id) AS fullCount, " +
            "    COUNT(CASE WHEN ic.request_status = 'APPROVED' THEN 1 END) AS approvedCount, " +
            "    COUNT(CASE WHEN ic.request_status = 'REJECTED' THEN 1 END) AS rejectedCount, " +
            "    COUNT(CASE WHEN ic.request_status = 'UNDER_REVIEW' THEN 1 END) AS underReviewCount, " +
            "    SUM(CASE WHEN ic.request_status IN ('APPROVED', 'UNDER_REVIEW') THEN ic.request_amount END) AS sumOfUtilizeAmount " +
            "FROM claims_request ic " +
            "LEFT OUTER JOIN application_user ap ON ic.employee = ap.id " +
            "LEFT OUTER JOIN claims_dependents cd ON ic.dependent = cd.id " +
            "LEFT OUTER JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id " +
            "LEFT OUTER JOIN treatment tr ON icd.treatment = tr.id " +
            "WHERE ap.id = :userId " +
            "AND (YEAR(ic.created_date) = :#{#dashboardSummaryDTO.year}) " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(ic.created_date) = :#{#dashboardSummaryDTO.month})" +
            ") " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})" +
            ") " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})" +
            ") " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.treatmentType} IS NULL OR tr.treatment_code = :#{#dashboardSummaryDTO.treatmentType})" +
            ") ",
            nativeQuery = true)
    CountTypeResponseDTO findSummary(DashboardSummaryDTO dashboardSummaryDTO,
                                     @Param("userId") Long userId);

    @Query(value = "SELECT " +
            "ic.id AS id, " +
            "ic.request_id AS requestId, " +
            "tr.treatment_description AS treatment, " +
            "ic.remark AS remark, " +
            "icd.disease AS diagnosis, " +
            "ic.request_amount AS amount, " +
            "CASE WHEN ic.dependent IS NULL THEN 'You' ELSE CONCAT(cd.first_name, cd.last_name) END AS passion, " +
            "ic.created_date AS requestDate " +
            "FROM claims_request ic " +
            "LEFT OUTER JOIN application_user ap ON ic.employee = ap.id " +
            "LEFT OUTER JOIN claims_dependents cd ON ic.dependent = cd.id " +
            "LEFT OUTER JOIN insurance_claims_details icd ON ic.insurance_claims_details = icd.id  " +
            "LEFT OUTER JOIN treatment tr ON icd.treatment = tr.id " +
            "WHERE ap.id = :userId AND ic.request_status = :requestStatus " +
            "ORDER BY ic.last_modified_date DESC LIMIT 7 ", nativeQuery = true)
    List<LatestUpdatedResponseDTO> getLatestUpdatedRecordSummary(@Param("userId") Long userId,@Param("requestStatus") String requestStatus);

}
