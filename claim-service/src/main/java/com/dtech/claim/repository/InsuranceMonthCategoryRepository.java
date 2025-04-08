/**
 * User: Himal_J
 * Date: 4/6/2025
 * Time: 2:55 PM
 * <p>
 */

package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.InsuranceMonthCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceMonthCategoryRepository extends JpaRepository<InsuranceMonthCategory, Long> {
        Optional<InsuranceMonthCategory> findByCodeAndStatus(String code, Status status);
}

