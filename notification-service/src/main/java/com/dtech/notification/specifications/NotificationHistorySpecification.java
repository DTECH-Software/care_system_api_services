/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 10:13 AM
 * <p>
 */

package com.dtech.notification.specifications;

import com.dtech.notification.model.ApplicationUser;
import com.dtech.notification.model.NotificationHistory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class NotificationHistorySpecification {
    public static Specification<NotificationHistory> getSpecification(com.dtech.notification.dto.request.NotificationHistory filterDto, Long userId) {
        log.info("Notification history filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<NotificationHistory, ApplicationUser> employee = root.join("employee", JoinType.LEFT);

            if (filterDto.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), filterDto.getFromDate()));
            }

            if (filterDto.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), filterDto.getToDate()));
            }

            if (filterDto.getRead() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isReady"),filterDto.getRead()));
            }

            if (filterDto.getUnRead()) {
                predicates.add(criteriaBuilder.equal(root.get("isReady"),filterDto.getUnRead()));
            }

            predicates.add(criteriaBuilder.equal(employee.get("id"), userId));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<NotificationHistory> getSpecification(Long userId) {
        log.info("Notification history filter default : " + userId);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<NotificationHistory, ApplicationUser> employee = root.join("employee", JoinType.LEFT);
            predicates.add(criteriaBuilder.equal(employee.get("id"), userId));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
