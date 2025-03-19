package com.dtech.token.repository;

import com.dtech.token.enums.Status;
import com.dtech.token.model.ApplicationUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface ApplicationUserSessionRepository extends JpaRepository<ApplicationUserSession,Long> {
    Optional<ApplicationUserSession> findByTokenAndStatus(String token, Status status);
}
