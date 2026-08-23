package com.dtech.message.repository;


import com.dtech.message.enums.Status;
import com.dtech.message.model.UserPersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPersonalDetailsRepository extends JpaRepository<UserPersonalDetails, Long> {

    Optional<UserPersonalDetails> findByEpfNoAndNicIgnoreCaseAndUserStatus(String efpNo, String nic, Status status);

}
