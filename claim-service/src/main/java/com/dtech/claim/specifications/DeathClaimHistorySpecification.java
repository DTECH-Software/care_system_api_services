/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 2:43 PM
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
public class DeathClaimHistorySpecification {
    public static Specification<DeathClaimRequest> getSpecification(ClaimHistory filterDto, Long userId) {
        log.info("Claim death history filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<ClaimsRequest, ClaimsDependents> claimDependents = root.join("claimsDependents", JoinType.LEFT);
            Join<ClaimsRequest, ApplicationUser> employee = root.join("employee", JoinType.LEFT);

            if (filterDto.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), filterDto.getFromDate()));
            }

            if (filterDto.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), filterDto.getToDate()));
            }

            if (filterDto.getRequestStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requestStatus"), Workflow.valueOf(filterDto.getRequestStatus())));
            }

            if (filterDto.getClaimsDependents() != null && !filterDto.getClaimsDependents().isEmpty()) {
                predicates.add(criteriaBuilder.equal(claimDependents.get("id"), filterDto.getClaimsDependents()));
            }

            predicates.add(criteriaBuilder.equal(employee.get("id"), userId));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<DeathClaimRequest> getSpecification(Long userId) {
        log.info("Claim death history filter default : " + userId);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<ClaimsRequest, ApplicationUser> employee = root.join("employee", JoinType.LEFT);
            predicates.add(criteriaBuilder.equal(employee.get("id"), userId));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}
