package com.dtech.auth.repository;

import com.dtech.auth.model.ApplicationOtpSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ApplicationOtpSessionRepository extends JpaRepository<ApplicationOtpSession, Long> {
}
