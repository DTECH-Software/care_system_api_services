package com.dtech.login.repository;

import com.dtech.login.model.ApplicationOtpSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ApplicationOtpSessionRepository extends JpaRepository<ApplicationOtpSession, Long> {
}
