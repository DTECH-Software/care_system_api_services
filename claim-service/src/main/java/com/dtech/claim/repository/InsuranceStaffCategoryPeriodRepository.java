package com.dtech.claim.repository;

import com.dtech.claim.model.InsuranceStaffCategoryPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface InsuranceStaffCategoryPeriodRepository extends JpaRepository<InsuranceStaffCategoryPeriod, Long> {

    @Query("SELECT i FROM InsuranceStaffCategoryPeriod i WHERE :inputDate BETWEEN i.fromDate AND i.toDate and i.staffCategories.code = :staff ")
    Optional<InsuranceStaffCategoryPeriod> findByDateWithinRange(@Param("inputDate") Date inputDate,@Param("staff") String staff);
}
