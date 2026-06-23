package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.UserPersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPersonalDetailsRepository extends JpaRepository<UserPersonalDetails, Long> {
    List<UserPersonalDetails> findByEpfNoAndUserStatus(String epfNo, Status userStatus);
}
