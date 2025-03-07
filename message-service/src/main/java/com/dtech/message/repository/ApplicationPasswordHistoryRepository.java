package com.dtech.message.repository;


import com.dtech.message.model.ApplicationPasswordHistory;
import com.dtech.message.model.ApplicationUser;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationPasswordHistoryRepository extends JpaRepository<ApplicationPasswordHistory, Long> {
    List<ApplicationPasswordHistory> findByApplicationUserAndPassword(ApplicationUser applicationUser, String password, Sort sort);

}
