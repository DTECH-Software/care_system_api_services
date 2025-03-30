/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 10:13 AM
 * <p>
 */

package com.dtech.claim.specifications;

import com.dtech.claim.dto.search.ClaimHistory;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class InsuranceClaimHistorySpecification {
    public static Specification<InsuranceClaimsRequest> getSpecification(ClaimHistory filterDto, Long userId) {
        log.info("Claim history filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<InsuranceClaimsRequest,ClaimsDependents> claimDependents = root.join("claimsDependents", JoinType.LEFT);
            Join<InsuranceClaimsRequest, InsuranceClaimsDetails> insuranceClaimsDetails = root.join("insuranceClaimsDetails", JoinType.LEFT);
            Join<InsuranceClaimsRequest, ApplicationUser> employee = root.join("employee", JoinType.LEFT);

            if (filterDto.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), filterDto.getFromDate()));
            }

            if (filterDto.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), filterDto.getToDate()));
            }

            if (filterDto.getRequestId() != null && !filterDto.getRequestId().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("requestId"), filterDto.getRequestId()));
            }

            if (filterDto.getRequestAmount() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requestAmount"), filterDto.getRequestAmount()));
            }

            if (filterDto.getRequestStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requestStatus"), Workflow.valueOf(filterDto.getRequestStatus())));
            }

            if (filterDto.getClaimsDependents() != null && !filterDto.getClaimsDependents().isEmpty()) {
                predicates.add(criteriaBuilder.equal(claimDependents.get("id"), filterDto.getClaimsDependents()));
            }

            if (filterDto.getInsuranceClaimsDetails() != null && !filterDto.getInsuranceClaimsDetails().isEmpty()) {
                predicates.add(criteriaBuilder.equal(insuranceClaimsDetails.get("id"), filterDto.getInsuranceClaimsDetails()));
            }

            predicates.add(criteriaBuilder.equal(employee.get("id"), userId));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<InsuranceClaimsRequest> getSpecification(Long userId) {
        log.info("Claim history filter default : " + userId);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<InsuranceClaimsRequest, ApplicationUser> employee = root.join("employee", JoinType.LEFT);
            predicates.add(criteriaBuilder.equal(employee.get("id"), userId));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}
