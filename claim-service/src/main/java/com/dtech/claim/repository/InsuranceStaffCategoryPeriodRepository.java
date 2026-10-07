package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.InsuranceStaffCategoryPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceStaffCategoryPeriodRepository extends JpaRepository<InsuranceStaffCategoryPeriod, Long> {

    @Query("SELECT i FROM InsuranceStaffCategoryPeriod i " +
            "WHERE :inputDate BETWEEN i.fromDate AND i.toDate " +
            "AND i.staffCategories.code = :staff " +
            "ORDER BY i.fromDate DESC, i.id DESC")
    List<InsuranceStaffCategoryPeriod> findByDateWithinRange(@Param("inputDate") java.sql.Date inputDate,@Param("staff") String staff);

    @Query("SELECT i FROM InsuranceStaffCategoryPeriod i WHERE :inputDate BETWEEN i.fromDate AND i.toDate ORDER BY i.fromDate DESC")
    List<InsuranceStaffCategoryPeriod> findByDateWithinRangeAnyStaff(@Param("inputDate") java.sql.Date inputDate);

    Optional<InsuranceStaffCategoryPeriod> findByStaffCategories_CodeAndStatus(String staffCategories, Status status);

    List<InsuranceStaffCategoryPeriod> findAllByStaffCategories_CodeAndStatus(String staffCategories, Status status);

    Optional<InsuranceStaffCategoryPeriod> findFirstByFromDateLessThanOrderByFromDateDesc(Date currentFromDate);
}
