package com.dtech.login.repository;

import com.dtech.login.model.ApplicationPasswordHistory;
import com.dtech.login.model.ApplicationUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationPasswordHistoryRepository extends JpaRepository<ApplicationPasswordHistory, Long> {

    List<ApplicationPasswordHistory> findByApplicationUser(ApplicationUser applicationUser, Pageable pageable);

}
