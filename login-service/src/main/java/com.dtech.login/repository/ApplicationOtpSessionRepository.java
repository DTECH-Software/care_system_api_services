package com.dtech.login.repository;

import com.dtech.login.model.ApplicationOtpSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.stereotype.Repository;


@Repository
public interface ApplicationOtpSessionRepository extends JpaRepository<ApplicationOtpSession, Long> {
    Optional<ApplicationOtpSession> findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(
            Long applicationUserId, String purpose);
}
