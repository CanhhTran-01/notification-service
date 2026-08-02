package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    // View status change history of a notification
    List<NotificationLog> findByNotificationIdOrderByCreatedAtAsc(UUID notificationId);
}
