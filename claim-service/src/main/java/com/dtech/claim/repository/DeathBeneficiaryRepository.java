package com.dtech.claim.repository;

import com.dtech.claim.enums.Range;
import com.dtech.claim.enums.Status;
import com.dtech.claim.model.DeathBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeathBeneficiaryRepository extends JpaRepository<DeathBeneficiary, Long> {
    Optional<DeathBeneficiary> findByCodeAndRangeAndStatus(com.dtech.claim.enums.DeathBeneficiary deathBeneficiary, Range range, Status status);
    Optional<DeathBeneficiary> findByCodeAndStatus(com.dtech.claim.enums.DeathBeneficiary deathBeneficiary, Status status);
    List<DeathBeneficiary> findAllByStatus(Status status);
}
