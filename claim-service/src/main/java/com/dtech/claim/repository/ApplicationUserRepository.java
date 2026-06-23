/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 9:47 AM
 * <p>
 */

package com.dtech.claim.repository;

import com.dtech.claim.enums.Status;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.UserPersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    Optional<ApplicationUser> findByUserPersonalDetails(UserPersonalDetails userPersonalDetails);
    Optional<ApplicationUser> findByIdAndUserPersonalDetails_UserStatus(Long id, Status status);
    boolean existsByUsernameEqualsIgnoreCase(String username);
    Optional<ApplicationUser> findByUsernameAndUserPersonalDetails_UserStatus(String username, Status status);
    Optional<ApplicationUser> findTopByUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatusAndIdNotOrderByIdDesc(String nic, Status status, Long id);
    boolean existsByPrimaryMobileAndUserPersonalDetails_UserStatus(String mobile,Status status);
    boolean existsByPrimaryEmail(String email);
}
