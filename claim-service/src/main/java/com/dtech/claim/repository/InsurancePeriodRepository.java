/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 12:34 PM
 * <p>
 */

package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.InsuranceYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface InsurancePeriodRepository extends JpaRepository<InsuranceYear, Long> {
    Optional<InsuranceYear> findByCodeAndStatus(String code, Status status);

//    @Query("SELECT i FROM InsuranceYear i WHERE :inputDate BETWEEN i.fromDate AND i.toDate ")
//    Optional<InsuranceYear> findByDateWithinRange(@Param("inputDate") Date inputDate);
}
