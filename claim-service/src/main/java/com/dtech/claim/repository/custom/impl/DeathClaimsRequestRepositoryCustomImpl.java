/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 8:49 AM
 * <p>
 */

package com.dtech.claim.repository.custom.impl;

import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.enums.RelationCategory;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.ClaimsDependents;
import com.dtech.claim.model.DeathClaimRequest;
import com.dtech.claim.model.InsuranceClaimsRequest;
import com.dtech.claim.repository.custom.DeathClaimsRequestRepositoryCustom;
import com.dtech.claim.util.DateTimeUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class DeathClaimsRequestRepositoryCustomImpl implements DeathClaimsRequestRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Object[] getCounts(DashboardSummaryDTO dashboardSummaryDTO, Long userId) {

        try {
            log.info("Death claims request represented {} {}", dashboardSummaryDTO, userId);
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = criteriaBuilder.createQuery(Object[].class);
            Root<DeathClaimRequest> root = cq.from(DeathClaimRequest.class);

            Join<DeathClaimRequest, ApplicationUser> employee = root.join("employee", JoinType.LEFT);
            Join<InsuranceClaimsRequest, ClaimsDependents> claimsDependents = root.join("claimsDependents", JoinType.LEFT);

            Expression<Integer> yearExpression = criteriaBuilder.function("YEAR", Integer.class, root.get("createdDate"));

          //  Integer year = dashboardSummaryDTO.getYear() != null ? dashboardSummaryDTO.getYear().getYear() : DateTimeUtil.getCurrentYear();
//            log.info("filter year: " + year);

            List<Predicate> predicates = new ArrayList<>();
//            predicates.add(criteriaBuilder.equal(yearExpression, year));
            predicates.add(criteriaBuilder.equal(employee.get("id"), userId));

            if(dashboardSummaryDTO.getRelationCategory() != null) {
                predicates.add(criteriaBuilder.equal(claimsDependents.get("relationCategory"), RelationCategory.valueOf(dashboardSummaryDTO.getRelationCategory())));
            }

            if(dashboardSummaryDTO.getMonth() != null) {
                Expression<Integer> monthExpression = criteriaBuilder.function("MONTH", Integer.class, root.get("createdDate"));
                predicates.add(criteriaBuilder.equal(monthExpression, dashboardSummaryDTO.getMonth()));
            }

            if(dashboardSummaryDTO.getClaimDependentId() != null){
                predicates.add(criteriaBuilder.equal(claimsDependents.get("id"), dashboardSummaryDTO.getClaimDependentId()));
            }

            cq.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
            log.info("filter employee:");

            //get full count
            Expression<Long> fullCount = criteriaBuilder.count(root);

            //get approved
            Expression<Long> approvedCount = criteriaBuilder.count(criteriaBuilder.selectCase()
                    .when(criteriaBuilder.equal(root.get("requestStatus"), Workflow.APPROVED), 1));

            //get approved
            Expression<Long> rejectedCount = criteriaBuilder.count(criteriaBuilder.selectCase()
                    .when(criteriaBuilder.equal(root.get("requestStatus"), Workflow.REJECTED), 1));

            //get approved
            Expression<Long> underReviewCount = criteriaBuilder.count(criteriaBuilder.selectCase()
                    .when(criteriaBuilder.equal(root.get("requestStatus"), Workflow.UNDER_REVIEW), 1));

            Predicate statusIn = root.get("requestStatus").in(Workflow.APPROVED, Workflow.UNDER_REVIEW);

            Expression<Double> sumOfUtilizeAmount = criteriaBuilder.sum(
                    criteriaBuilder.<Double> selectCase()
                            .when(criteriaBuilder.equal(root.get("utilizeAmount"), statusIn), 1.0)
                            .otherwise(0.0));

            cq.multiselect(fullCount, approvedCount, rejectedCount, underReviewCount,sumOfUtilizeAmount);
            log.info("filter query create success: ");
            TypedQuery<Object[]> query = em.createQuery(cq);
            return query.getSingleResult();
        }catch (Exception e) {
            log.error(e);
            throw e;
        }

    }
}
