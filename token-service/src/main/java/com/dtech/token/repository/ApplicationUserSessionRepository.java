package com.dtech.token.repository;

import com.dtech.token.model.ApplicationUser;
import com.dtech.token.model.ApplicationUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface ApplicationUserSessionRepository extends JpaRepository<ApplicationUserSession,Long> {
    Optional<ApplicationUserSession> findByToken(String token);
}
