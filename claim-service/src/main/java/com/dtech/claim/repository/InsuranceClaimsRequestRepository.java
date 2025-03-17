package com.dtech.claim.repository;

import com.dtech.claim.model.ClaimsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceClaimsRequestRepository extends JpaRepository<ClaimsRequest, Long> , JpaSpecificationExecutor<ClaimsRequest> {

    Page<ClaimsRequest> findAll(Specification<ClaimsRequest> spec, Pageable pageable);
}
