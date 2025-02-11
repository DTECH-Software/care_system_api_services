/**
 * User: Himal_J
 * Date: 2/11/2025
 * Time: 1:05 PM
 * <p>
 */

package com.dtech.message.repository;

import com.dtech.message.enums.NotificationsType;
import com.dtech.message.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByType(NotificationsType title);
}
