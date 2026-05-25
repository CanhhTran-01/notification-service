package com.project.notificationservice.repository;

import com.project.notificationservice.domain.entity.NotificationTemplate;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.EventType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    // EmailSender query theo eventType(template_code) + channel để lấy đúng template
    Optional<NotificationTemplate> findByTemplateCodeAndChannelAndIsActiveTrue(EventType templateCode, Channel channel);
}
