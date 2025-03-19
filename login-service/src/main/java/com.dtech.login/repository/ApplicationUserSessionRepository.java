package com.dtech.login.repository;

import com.dtech.login.enums.Status;
import com.dtech.login.model.ApplicationUser;
import com.dtech.login.model.ApplicationUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationUserSessionRepository extends JpaRepository<ApplicationUserSession,Long> {
      void deleteAllByApplicationUser(ApplicationUser applicationUser);
      Optional<ApplicationUserSession> findByToken(String token);
      Optional<ApplicationUserSession> findByApplicationUserAndStatus(ApplicationUser applicationUser, Status status);
}
