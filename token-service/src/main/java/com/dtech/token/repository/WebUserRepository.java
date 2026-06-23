package com.dtech.token.repository;

import com.dtech.token.enums.Status;
import com.dtech.token.model.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebUserRepository extends JpaRepository<WebUser, Long> {
    Optional<WebUser> findByUsernameAndStatus(String username, Status status);
}
