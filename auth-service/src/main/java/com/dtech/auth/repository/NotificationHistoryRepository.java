package com.dtech.auth.repository;

import com.dtech.auth.enums.NotificationsType;
import com.dtech.auth.model.NotificationHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> , JpaSpecificationExecutor<NotificationHistory> {
    long countByTypeAndIsRead(NotificationsType type, boolean read);

    List<NotificationHistory> findAllByTypeOrderByLastModifiedByDesc(NotificationsType type, Pageable pageable);
}
