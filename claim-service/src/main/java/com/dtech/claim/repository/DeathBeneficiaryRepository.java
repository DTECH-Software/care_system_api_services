package com.dtech.claim.repository;

import com.dtech.claim.model.DeathBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeathBeneficiaryRepository extends JpaRepository<DeathBeneficiary, Long> {
    Optional<DeathBeneficiary> findByCode(com.dtech.claim.enums.DeathBeneficiary deathBeneficiary);
}
