package com.dtech.message.repository;

import com.dtech.message.model.ApplicationOtpSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ApplicationOtpSessionRepository extends JpaRepository<ApplicationOtpSession, Long> {
    Optional<ApplicationOtpSession> findTopByOtpAndPurposeAndValidatedAndConsumedFalseOrderByCreatedDateDesc(
            String otp, String purpose, boolean validated);

    Optional<ApplicationOtpSession> findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(
            Long applicationUserId, String purpose);

    Optional<ApplicationOtpSession> findTopByContextKeyAndPurposeOrderByCreatedDateDesc(
            String contextKey, String purpose);

    @Modifying
    @Query("update ApplicationOtpSession session set session.consumed = true " +
            "where session.applicationUserId = :applicationUserId and session.purpose = :purpose " +
            "and session.consumed = false")
    int consumeActiveUserSessions(@Param("applicationUserId") Long applicationUserId,
                                  @Param("purpose") String purpose);

    @Modifying
    @Query("update ApplicationOtpSession session set session.consumed = true " +
            "where session.contextKey = :contextKey and session.purpose = :purpose " +
            "and session.consumed = false")
    int consumeActiveContextSessions(@Param("contextKey") String contextKey,
                                     @Param("purpose") String purpose);
}
