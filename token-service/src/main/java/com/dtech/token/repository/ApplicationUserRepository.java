/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 9:47 AM
 * <p>
 */

package com.dtech.token.repository;

import com.dtech.token.model.ApplicationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {

    Optional<ApplicationUser> findByUsername(String username);

}
