package com.dtech.auth.repository;

import com.dtech.auth.model.ClaimsDependentsVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimsDependentsVerificationDocumentRepository extends JpaRepository<ClaimsDependentsVerificationDocument, Long> {
}
