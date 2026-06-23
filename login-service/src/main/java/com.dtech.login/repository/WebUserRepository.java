package com.dtech.login.repository;

import com.dtech.login.enums.Status;
import com.dtech.login.model.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebUserRepository extends JpaRepository<WebUser, Long> {
    Optional<WebUser> findByUsernameAndStatus(String username, Status status);
    Optional<WebUser> findByEmailIgnoreCaseAndStatus(String email, Status status);
}
