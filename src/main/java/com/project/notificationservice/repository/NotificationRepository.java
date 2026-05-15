package com.project.notificationservice.repository;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.NotificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByEventId(String id);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);
}
