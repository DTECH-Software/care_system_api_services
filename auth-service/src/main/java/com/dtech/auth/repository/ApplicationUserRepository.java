/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 9:47 AM
 * <p>
 */

package com.dtech.auth.repository;

import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.UserPersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {

    Optional<ApplicationUser> findByUserPersonalDetails(UserPersonalDetails userPersonalDetails);

}
