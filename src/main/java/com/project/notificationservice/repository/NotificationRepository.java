package com.project.notificationservice.repository;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.NotificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventIdAndChannel(String eventId, Channel channel);

    List<Notification> findByStatusAndRetryCountLessThanAndNextRetryTimeBefore(
            NotificationStatus status, int maxRetries, LocalDateTime time);

    Optional<Notification> findByIdAndRecipientId(UUID id, String recipientId);

    Optional<Notification> findByIdAndStatus(UUID id, NotificationStatus status);

    // Channel luôn là IN_APP
    Page<Notification> findByRecipientIdAndChannelOrderByCreatedAtDesc(
            String recipientId, Channel channel, Pageable pageable);

    // Channel luôn là IN_APP
    long countByRecipientIdAndChannelAndIsReadFalse(String recipientId, Channel channel);
}
