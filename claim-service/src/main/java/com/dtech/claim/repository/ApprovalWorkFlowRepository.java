package com.dtech.claim.repository;

import com.dtech.claim.model.ApprovalWorkFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalWorkFlowRepository extends JpaRepository<ApprovalWorkFlow, Long> {
}
