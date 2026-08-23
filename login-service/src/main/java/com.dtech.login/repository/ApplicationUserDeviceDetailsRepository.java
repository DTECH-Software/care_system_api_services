/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 3:50 PM
 * <p>
 */

package com.dtech.login.repository;

import com.dtech.login.model.ApplicationUserDeviceDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationUserDeviceDetailsRepository extends JpaRepository<ApplicationUserDeviceDetails, Long> {
}
