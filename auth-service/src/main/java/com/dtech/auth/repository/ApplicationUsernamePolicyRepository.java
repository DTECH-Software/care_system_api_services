/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 8:24 AM
 * <p>
 */

package com.dtech.auth.repository;

import com.dtech.auth.model.ApplicationUsernamePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUsernamePolicyRepository extends JpaRepository<ApplicationUsernamePolicy, Long> {
    @Query(value = "SELECT app FROM ApplicationUsernamePolicy app")
    Optional<ApplicationUsernamePolicy> findUsernamePolicy();
}
