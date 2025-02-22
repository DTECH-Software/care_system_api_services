package com.dtech.login.repository;

import com.dtech.login.model.ApplicationUser;
import com.dtech.login.model.ApplicationUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationUserSessionRepository extends JpaRepository<ApplicationUserSession,Long> {
      void deleteAllByApplicationUser(ApplicationUser applicationUser);
}
