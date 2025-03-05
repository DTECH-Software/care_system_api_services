package com.dtech.claim.repository;

import com.dtech.claim.model.ClaimsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimsRequestRepository extends JpaRepository<ClaimsRequest, Long> {
}
