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
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    Optional<ApplicationUser> findByUserPersonalDetails(UserPersonalDetails userPersonalDetails);
    boolean existsByUsernameEqualsIgnoreCase(String username);
    Optional<ApplicationUser> findByUsernameAndUserPersonalDetails_UserStatus(String username, Status status);
    boolean existsByPrimaryMobileAndUserPersonalDetails_UserStatus(String mobile,Status status);
    boolean existsByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(String email, Status status);
    Optional<ApplicationUser> findByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(String email, Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from ApplicationUser user where lower(user.username) = lower(:username) " +
            "and user.userPersonalDetails.userStatus = :status")
    Optional<ApplicationUser> findForOtpUpdateByUsername(@Param("username") String username,
                                                        @Param("status") Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from ApplicationUser user where lower(user.primaryEmail) = lower(:email) " +
            "and user.userPersonalDetails.userStatus = :status")
    Optional<ApplicationUser> findForOtpUpdateByEmail(@Param("email") String email,
                                                     @Param("status") Status status);

}
