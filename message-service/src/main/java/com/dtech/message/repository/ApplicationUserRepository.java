/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 9:47 AM
 * <p>
 */

package com.dtech.message.repository;

import com.dtech.message.enums.Status;
import com.dtech.message.model.ApplicationUser;
import com.dtech.message.model.UserPersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    Optional<ApplicationUser> findByUserPersonalDetails(UserPersonalDetails userPersonalDetails);
    boolean existsByUsernameEqualsIgnoreCase(String username);
    Optional<ApplicationUser> findByUsernameAndUserPersonalDetails_UserStatus(String username, Status status);
    boolean existsByPrimaryMobileAndUserPersonalDetails_UserStatus(String mobile,Status status);
    boolean existsByPrimaryEmailIgnoreCase(String email);
}
