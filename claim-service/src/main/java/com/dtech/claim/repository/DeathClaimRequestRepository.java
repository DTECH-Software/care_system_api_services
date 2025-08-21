package com.dtech.claim.repository;


import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.response.AmountResponseDTO;
import com.dtech.claim.dto.response.CountTypeResponseDTO;
import com.dtech.claim.dto.response.LatestUpdatedResponseDTO;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.ClaimsDependents;
import com.dtech.claim.model.DeathClaimRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeathClaimRequestRepository extends JpaRepository<DeathClaimRequest, Long> , JpaSpecificationExecutor<DeathClaimRequest>  {
    Optional<DeathClaimRequest> findByClaimsDependentsAndEmployeeAndRequestStatusIn(ClaimsDependents claimsDependents, ApplicationUser applicationUser, List<Workflow> workflow);
    boolean existsByClaimsDependentsAndEmployeeAndRequestStatusIn(ClaimsDependents claimsDependents, ApplicationUser applicationUser, List<Workflow> workflow);

    @Query(value = "SELECT  " +
            "    COUNT(dc.id) AS fullCount, " +
            "    COUNT(CASE WHEN dc.request_status = 'APPROVED' THEN 1 END) AS approvedCount, " +
            "    COUNT(CASE WHEN dc.request_status = 'REJECTED' THEN 1 END) AS rejectedCount, " +
            "    COUNT(CASE WHEN dc.request_status = 'UNDER_REVIEW' THEN 1 END) AS underReviewCount " +
            "FROM death_claim_request dc " +
            "LEFT OUTER JOIN application_user ap ON dc.employee = ap.id " +
            "LEFT OUTER JOIN claims_dependents cd ON dc.dependent = cd.id " +
            "WHERE ap.id = :userId " +
            "AND (YEAR(dc.created_date) = :#{#dashboardSummaryDTO.year}) " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(dc.created_date) = :#{#dashboardSummaryDTO.month})" +
            ") " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})" +
            ") " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})" +
            ") ",
            nativeQuery = true)
    CountTypeResponseDTO findSummary(DashboardSummaryDTO dashboardSummaryDTO,
                                     @Param("userId") Long userId);


    @Query(value = "SELECT  " +
            "    SUM(CASE WHEN dc.request_status IN ('APPROVED') THEN dc.utilize_amount END) AS sumOfUtilizeAmount " +
            "FROM death_claim_request dc " +
            "LEFT OUTER JOIN application_user ap ON dc.employee = ap.id " +
            "LEFT OUTER JOIN claims_dependents cd ON dc.dependent = cd.id " +
            "WHERE ap.id = :userId " +
            "AND (YEAR(dc.created_date) = :#{#dashboardSummaryDTO.year}) " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.month} IS NULL OR MONTH(dc.created_date) = :#{#dashboardSummaryDTO.month})" +
            ") " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.relationCategory} IS NULL OR cd.relation_category = :#{#dashboardSummaryDTO.relationCategory})" +
            ") " +
            "AND ( " +
            "(:#{#dashboardSummaryDTO.claimDependentId} IS NULL OR cd.id = :#{#dashboardSummaryDTO.claimDependentId})" +
            ") ",
            nativeQuery = true)
    AmountResponseDTO findSummaryByDeath(DashboardSummaryDTO dashboardSummaryDTO,
                                         @Param("userId") Long userId);


    @Query(value = "SELECT " +
            "dc.id AS id, " +
            "dc.request_id AS requestId, " +
            "dc.remark AS remark, " +
            "dc.utilize_amount AS amount, " +
            "CONCAT(cd.first_name, ' ', cd.last_name) AS passion, " +
            "dc.created_date AS requestDate " +
            "FROM death_claim_request dc " +
            "LEFT OUTER JOIN application_user ap ON dc.employee = ap.id " +
            "LEFT OUTER JOIN claims_dependents cd ON dc.dependent = cd.id " +
            "WHERE ap.id = :userId AND dc.request_status = :requestStatus " +
            "ORDER BY dc.last_modified_date ASC LIMIT 7", nativeQuery = true)
    List<LatestUpdatedResponseDTO> getLatestUpdatedRecordSummary(@Param("userId") Long userId, @Param("requestStatus") String requestStatus);

}
