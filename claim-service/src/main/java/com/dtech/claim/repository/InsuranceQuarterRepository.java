package com.dtech.claim.repository;

import com.dtech.claim.model.InsuranceDetailsLimit;
import com.dtech.claim.model.InsuranceQuarter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceQuarterRepository extends JpaRepository<InsuranceQuarter,Long> {

    @Query("SELECT e FROM InsuranceQuarter e " +
            "WHERE e.insuranceDetailsLimit = :limit " +
            "AND e.treatmentCategory.code = :code " +
            "AND :givenDate BETWEEN e.fromDate AND e.toDate " +
            "ORDER BY e.fromDate DESC, e.id DESC")
    List<InsuranceQuarter> findByDateWithinRangeAndCodeWithLimit(@Param("limit") InsuranceDetailsLimit limit,
                                                                 @Param("code") String code,
                                                                 @Param("givenDate") java.sql.Date givenDate);

    @Query("SELECT e FROM InsuranceQuarter e " +
            "WHERE e.insuranceDetailsLimit = :limit " +
            "AND e.treatmentCategory.code = :code ")
    Optional<InsuranceQuarter> findByCodeWithLimit(@Param("limit") InsuranceDetailsLimit limit,
                                                                     @Param("code") String code);

    Optional<InsuranceQuarter> findFirstByInsuranceDetailsLimitAndTreatmentCategory_CodeOrderByFromDateAsc(
            InsuranceDetailsLimit insuranceDetailsLimit,
            String code);
}
