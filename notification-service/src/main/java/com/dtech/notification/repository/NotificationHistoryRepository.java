package com.dtech.notification.repository;

import com.dtech.notification.model.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> , JpaSpecificationExecutor<NotificationHistory> {
}
