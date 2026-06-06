package com.project.notificationservice.repository;

import com.project.notificationservice.domain.entity.NotificationPreference;
import com.project.notificationservice.domain.enums.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    // Query tất cả preferences của 1 user
    List<NotificationPreference> findByRecipientId(String recipientId);

    // Query tất cả active preferences của 1 user
    List<NotificationPreference> findByRecipientIdAndIsEnabledTrue(String recipientId);

    // Query 1 preference cụ thể theo recipientId + channel
    Optional<NotificationPreference> findByRecipientIdAndChannel(String recipientId, Channel channel);
}
