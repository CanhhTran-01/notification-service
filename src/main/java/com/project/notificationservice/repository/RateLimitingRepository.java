package com.project.notificationservice.repository;

import com.project.notificationservice.entity.RateLimiting;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.ServiceSource;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface RateLimitingRepository extends JpaRepository<RateLimiting, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RateLimiting> findByRecipientIdAndChannelAndEventTypeAndSource(
            String recipientId, Channel channel, EventType eventType, ServiceSource source);
}
